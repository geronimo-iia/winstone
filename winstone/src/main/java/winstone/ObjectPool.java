/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import net.winstone.core.listener.Listener;
import net.winstone.core.WinstoneResponse;
import net.winstone.core.WinstoneRequest;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.winstone.WinstoneException;
import org.slf4j.LoggerFactory;

/**
 * Holds the object pooling code for Winstone. Presently this is only responses and requests, but may increase.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ObjectPool.java,v 1.9 2006/11/18 14:56:59 rickknowles Exp $
 */
public class ObjectPool implements Runnable {

    protected static org.slf4j.Logger logger = LoggerFactory.getLogger(ObjectPool.class);
    private static final long FLUSH_PERIOD = 60000L;
    private final static transient int STARTUP_REQUEST_HANDLERS_IN_POOL = 5;
    private final static transient int MAX_IDLE_REQUEST_HANDLERS_IN_POOL = 50;
    private final static transient int MAX_REQUEST_HANDLERS_IN_POOL = 1000;
    private final static transient long RETRY_PERIOD = 1000;
    private final static transient int START_REQUESTS_IN_POOL = 10;
    private final static transient int MAX_REQUESTS_IN_POOL = 1000;
    private final static transient int START_RESPONSES_IN_POOL = 10;
    private final static transient int MAX_RESPONSES_IN_POOL = 1000;
    private int maxIdleRequestHandlesInPool;
    private List<RequestHandlerThread> unusedRequestHandlerThreads;
    private List<RequestHandlerThread> usedRequestHandlerThreads;
    private List<WinstoneRequest> usedRequestPool;
    private List<WinstoneRequest> unusedRequestPool;
    private List<WinstoneResponse> usedResponsePool;
    private List<WinstoneResponse> unusedResponsePool;
    private final Object requestHandlerSemaphore = Boolean.TRUE;
    private final Object requestPoolSemaphore = Boolean.TRUE;
    private final Object responsePoolSemaphore = Boolean.TRUE;
    private int threadIndex = 0;
    private boolean simulateModUniqueId;
    private boolean saveSessions;
    private Thread thread;

    /**
     * Constructs an instance of the object pool, including handlers, requests and responses
     */
    public ObjectPool(Map<String, String> args) throws IOException {
        this.simulateModUniqueId = WebAppConfiguration.booleanArg(args, "simulateModUniqueId", false);
        this.saveSessions = WebAppConfiguration.useSavedSessions(args);

        // Build the initial pool of handler threads
        this.unusedRequestHandlerThreads = new ArrayList<RequestHandlerThread>();
        this.usedRequestHandlerThreads = new ArrayList<RequestHandlerThread>();

        // Build the request/response pools
        this.usedRequestPool = new ArrayList<WinstoneRequest>();
        this.usedResponsePool = new ArrayList<WinstoneResponse>();
        this.unusedRequestPool = new ArrayList<WinstoneRequest>();
        this.unusedResponsePool = new ArrayList<WinstoneResponse>();

        // Get handler pool options
        int startupRequest = STARTUP_REQUEST_HANDLERS_IN_POOL;
        if (args.get("handlerCountStartup") != null) {
            startupRequest = Integer.parseInt((String) args.get("handlerCountStartup"));
        }
        maxIdleRequestHandlesInPool = MAX_IDLE_REQUEST_HANDLERS_IN_POOL;
        if (args.get("handlerCountMax") != null) {
            maxIdleRequestHandlesInPool = Integer.parseInt((String) args.get("handlerCountMax"));
        }
        if (args.get("handlerCountMaxIdle") != null) {
            maxIdleRequestHandlesInPool = Integer.parseInt((String) args.get("handlerCountMaxIdle"));
        }

        // Start the base set of handler threads
        for (int n = 0; n < startupRequest; n++) {
            this.unusedRequestHandlerThreads.add(new RequestHandlerThread(this, this.threadIndex++, this.simulateModUniqueId, this.saveSessions));
        }

        // Initialise the request/response pools
        for (int n = 0; n < START_REQUESTS_IN_POOL; n++) {
            this.unusedRequestPool.add(new WinstoneRequest());
        }
        for (int n = 0; n < START_RESPONSES_IN_POOL; n++) {
            this.unusedResponsePool.add(new WinstoneResponse());
        }

        this.thread = new Thread(this, "WinstoneObjectPoolMgmt");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    @Override
    public void run() {
        boolean interrupted = false;
        while (!interrupted) {
            try {
                Thread.sleep(FLUSH_PERIOD);
                removeUnusedRequestHandlers();
            } catch (InterruptedException err) {
                interrupted = true;
            }
        }
        this.thread = null;
    }

    private void removeUnusedRequestHandlers() {
        // Check max idle requestHandler count
        synchronized (this.requestHandlerSemaphore) {
            // If we have too many idle request handlers
            while (this.unusedRequestHandlerThreads.size() > maxIdleRequestHandlesInPool) {
                RequestHandlerThread rh = (RequestHandlerThread) this.unusedRequestHandlerThreads.get(0);
                rh.destroy();
                this.unusedRequestHandlerThreads.remove(rh);
            }
        }
    }

    public void destroy() {
        synchronized (this.requestHandlerSemaphore) {
            Collection<RequestHandlerThread> usedHandlers = new ArrayList<RequestHandlerThread>(this.usedRequestHandlerThreads);
            for (Iterator<RequestHandlerThread> i = usedHandlers.iterator(); i.hasNext();) {
                releaseRequestHandler(i.next());
            }
            Collection<RequestHandlerThread> unusedHandlers = new ArrayList<RequestHandlerThread>(this.unusedRequestHandlerThreads);
            for (Iterator<RequestHandlerThread> i = unusedHandlers.iterator(); i.hasNext();) {
                (i.next()).destroy();
            }
            this.unusedRequestHandlerThreads.clear();
        }
        if (this.thread != null) {
            this.thread.interrupt();
        }
    }

    /**
     * Once the socket request comes in, this method is called. It reserves a request handler, then delegates the socket to that class. When
     * it finishes, the handler is released back into the pool.
     */
    public void handleRequest(Socket socket, Listener listener) throws IOException, InterruptedException {
        RequestHandlerThread rh = null;
        synchronized (this.requestHandlerSemaphore) {
            // If we have any spare, get it from the pool
            int unused = this.unusedRequestHandlerThreads.size();
            if (unused > 0) {
                rh = (RequestHandlerThread) this.unusedRequestHandlerThreads.remove(unused - 1);
                this.usedRequestHandlerThreads.add(rh);
                logger.debug("RHPool: Using pooled handler thread - used: {} unused: {}", "" + this.usedRequestHandlerThreads.size(), "" + this.unusedRequestHandlerThreads.size());
            } // If we are out (and not over our limit), allocate a new one
            else if (this.usedRequestHandlerThreads.size() < MAX_REQUEST_HANDLERS_IN_POOL) {
                rh = new RequestHandlerThread(this, this.threadIndex++, this.simulateModUniqueId, this.saveSessions);
                this.usedRequestHandlerThreads.add(rh);
                logger.debug("RHPool: Spawning new handler thread - used: {} unused: {}]", "" + this.usedRequestHandlerThreads.size(), "" + this.unusedRequestHandlerThreads.size());
            } // otherwise throw fail message - we've blown our limit
            else {
                // Possibly insert a second chance here ? Delay and one retry ?
                // Remember to release the lock first
                logger.warn("WARNING: Request handler pool limit exceeded - waiting for retry");
                // socket.close();
                // throw new UnavailableException("NoHandlersAvailable");
            }
        }

        if (rh != null) {
            rh.commenceRequestHandling(socket, listener);
        } else {
            // Sleep for a set period and try again from the pool
            Thread.sleep(RETRY_PERIOD);

            synchronized (this.requestHandlerSemaphore) {
                if (this.usedRequestHandlerThreads.size() < MAX_REQUEST_HANDLERS_IN_POOL) {
                    rh = new RequestHandlerThread(this, this.threadIndex++, this.simulateModUniqueId, this.saveSessions);
                    this.usedRequestHandlerThreads.add(rh);
                    logger.debug("RHPool: Spawning new handler thread - used: {} unused: {}", "" + this.usedRequestHandlerThreads.size(), "" + this.unusedRequestHandlerThreads.size());
                }
            }
            if (rh != null) {
                rh.commenceRequestHandling(socket, listener);
            } else {
                logger.error("Request ignored because there were no more request handlers available in the pool");
                socket.close();
            }
        }
    }

    /**
     * Release the handler back into the pool
     */
    public void releaseRequestHandler(RequestHandlerThread rh) {
        synchronized (this.requestHandlerSemaphore) {
            this.usedRequestHandlerThreads.remove(rh);
            this.unusedRequestHandlerThreads.add(rh);
            logger.debug("RHPool: Releasing handler thread - used: {} unused: {}", "" + this.usedRequestHandlerThreads.size(), "" + this.unusedRequestHandlerThreads.size());
        }
    }

    /**
     * An attempt at pooling request objects for reuse.
     */
    public WinstoneRequest getRequestFromPool() throws IOException {
        WinstoneRequest req = null;
        synchronized (this.requestPoolSemaphore) {
            // If we have any spare, get it from the pool
            int unused = this.unusedRequestPool.size();
            if (unused > 0) {
                req = (WinstoneRequest) this.unusedRequestPool.remove(unused - 1);
                this.usedRequestPool.add(req);
                logger.debug("ReqPool: Using pooled request - available: {}", "" + this.unusedRequestPool.size());
            } // If we are out, allocate a new one
            else if (this.usedRequestPool.size() < MAX_REQUESTS_IN_POOL) {
                req = new WinstoneRequest();
                this.usedRequestPool.add(req);
                logger.debug("ReqPool: Spawning new request - available: {}", "" + this.usedRequestPool.size());
            } else {
                throw new WinstoneException("RspPool: Max responses in pool exceeded - denied");
            }
        }
        return req;
    }

    public void releaseRequestToPool(WinstoneRequest req) {
        req.cleanUp();
        synchronized (this.requestPoolSemaphore) {
            this.usedRequestPool.remove(req);
            this.unusedRequestPool.add(req);
            logger.debug("ReqPool: Request released - available: {}", "" + this.unusedRequestPool.size());
        }
    }

    /**
     * An attempt at pooling request objects for reuse.
     */
    public WinstoneResponse getResponseFromPool() throws IOException {
        WinstoneResponse rsp = null;
        synchronized (this.responsePoolSemaphore) {
            // If we have any spare, get it from the pool
            int unused = this.unusedResponsePool.size();
            if (unused > 0) {
                rsp = (WinstoneResponse) this.unusedResponsePool.remove(unused - 1);
                this.usedResponsePool.add(rsp);
                logger.debug("RspPool: Using pooled response - available: {}", "" + this.unusedResponsePool.size());
            } // If we are out, allocate a new one
            else if (this.usedResponsePool.size() < MAX_RESPONSES_IN_POOL) {
                rsp = new WinstoneResponse();
                this.usedResponsePool.add(rsp);
                logger.debug("RspPool: Spawning new response - available: {}", "" + this.usedResponsePool.size());
            } else {
                throw new WinstoneException("RspPool: Max responses in pool exceeded - denied");
            }
        }
        return rsp;
    }

    public void releaseResponseToPool(WinstoneResponse rsp) {
        rsp.cleanUp();
        synchronized (this.responsePoolSemaphore) {
            this.usedResponsePool.remove(rsp);
            this.unusedResponsePool.add(rsp);
            logger.debug("RspPool: Response released - available: {}", "" + this.unusedResponsePool.size());
        }
    }
}
