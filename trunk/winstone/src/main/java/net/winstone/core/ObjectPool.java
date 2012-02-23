/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.winstone.core.listener.Listener;
import net.winstone.core.listener.RequestHandlerThread;
import net.winstone.util.StringUtils;

import org.slf4j.LoggerFactory;

/**
 * Holds the object pooling code for Winstone. Presently this is only responses
 * and requests, but may increase.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ObjectPool.java,v 1.9 2006/11/18 14:56:59 rickknowles Exp $
 */
public class ObjectPool {

	protected static org.slf4j.Logger logger = LoggerFactory.getLogger(ObjectPool.class);

	/**
	 * FLUSH PERIOD 60 second.
	 */
	private static final long FLUSH_PERIOD = 60000L;

	/**
	 * Default startup request handler in pool (5).
	 */
	private final static transient int STARTUP_REQUEST_HANDLERS_IN_POOL = 5;
	/**
	 * Maximum idle request handler in pool (50).
	 */
	private final static transient int MAX_IDLE_REQUEST_HANDLERS_IN_POOL = 50;
	/**
	 * Maximum request handler in pool (1000).
	 */
	private final static transient int MAX_REQUEST_HANDLERS_IN_POOL = 1000;
	/**
	 * Retry period (1 second).
	 */
	private final static transient long RETRY_PERIOD = 1000;
	/**
	 * Start requests in pool (10).
	 */
	private final static transient int START_REQUESTS_IN_POOL = 10;
	/**
	 * Max requests in pool (1000).
	 */
	private final static transient int MAX_REQUESTS_IN_POOL = 1000;
	/**
	 * Start responses in pool (10).
	 */
	private final static transient int START_RESPONSES_IN_POOL = 10;
	/**
	 * Maximum responses in pool (1000).
	 */
	private final static transient int MAX_RESPONSES_IN_POOL = 1000;

	private final List<RequestHandlerThread> unusedRequestHandlerThreads;
	private final List<RequestHandlerThread> usedRequestHandlerThreads;
	private final List<WinstoneRequest> unusedRequestPool;
	private final List<WinstoneResponse> unusedResponsePool;
	private final Object requestHandlerSemaphore = Boolean.TRUE;
	private final Object requestPoolSemaphore = Boolean.TRUE;
	private final Object responsePoolSemaphore = Boolean.TRUE;

	private int threadIndex = 0;
	private final boolean simulateModUniqueId;
	private final boolean saveSessions;

	private Thread thread;

	int startupRequest = ObjectPool.STARTUP_REQUEST_HANDLERS_IN_POOL;
	int maxRequestHandlesInPool = ObjectPool.MAX_REQUEST_HANDLERS_IN_POOL;
	int maxIdleRequestHandlesInPool = ObjectPool.MAX_IDLE_REQUEST_HANDLERS_IN_POOL;

	/**
	 * max number of parameters allowed.
	 */
	private static int MAXPARAMALLOWED = WinstoneConstant.DEFAULT_MAXIMUM_PARAMETER_ALLOWED;

	/**
	 * Constructs an instance of the object pool, including handlers, requests
	 * and responses
	 */
	public ObjectPool(final Map<String, String> args) throws IOException {
		super();
		// load simulateModUniqueId
		simulateModUniqueId = StringUtils.booleanArg(args, "simulateModUniqueId", Boolean.FALSE);
		// load maxParamAllowed
		int maxParamAllowed = StringUtils.intArg(args, "maxParamAllowed", WinstoneConstant.DEFAULT_MAXIMUM_PARAMETER_ALLOWED);
		if (maxParamAllowed < 1) {
			logger.error("MaxParamAllowed should be greather than 1. Set to default value {}", WinstoneConstant.DEFAULT_MAXIMUM_PARAMETER_ALLOWED);
			maxParamAllowed = WinstoneConstant.DEFAULT_MAXIMUM_PARAMETER_ALLOWED;
		}
		ObjectPool.MAXPARAMALLOWED = maxParamAllowed;
		// load saveSessions
		saveSessions = WebAppConfiguration.useSavedSessions(args);

		// Build the initial pool of handler threads
		unusedRequestHandlerThreads = new ArrayList<RequestHandlerThread>();
		usedRequestHandlerThreads = new ArrayList<RequestHandlerThread>();

		// Build the request/response pools
		unusedRequestPool = new ArrayList<WinstoneRequest>();
		unusedResponsePool = new ArrayList<WinstoneResponse>();

		// Get handler pool options
		startupRequest = StringUtils.intArg(args, "handlerCountStartup", ObjectPool.STARTUP_REQUEST_HANDLERS_IN_POOL);
		maxRequestHandlesInPool = StringUtils.intArg(args, "handlerCountMax", ObjectPool.MAX_REQUEST_HANDLERS_IN_POOL);
		maxIdleRequestHandlesInPool = StringUtils.intArg(args, "handlerCountMaxIdle", ObjectPool.MAX_IDLE_REQUEST_HANDLERS_IN_POOL);

		// Start the base set of handler threads
		for (int n = 0; n < startupRequest; n++) {
			unusedRequestHandlerThreads.add(new RequestHandlerThread(this, threadIndex++, simulateModUniqueId, saveSessions));
		}

		// Initialize the request/response pools
		for (int n = 0; n < ObjectPool.START_REQUESTS_IN_POOL; n++) {
			unusedRequestPool.add(new WinstoneRequest(ObjectPool.MAXPARAMALLOWED));
		}
		for (int n = 0; n < ObjectPool.START_RESPONSES_IN_POOL; n++) {
			unusedResponsePool.add(new WinstoneResponse());
		}

		thread = new Thread(new Runnable() {
			/**
			 * Every 60s, remove unused request handler.
			 * 
			 * @see java.lang.Runnable#run()
			 */
			@Override
			public void run() {
				boolean interrupted = Boolean.FALSE;
				while (!interrupted) {
					try {
						Thread.sleep(ObjectPool.FLUSH_PERIOD);
						removeUnusedRequestHandlers();
					} catch (final InterruptedException err) {
						interrupted = Boolean.TRUE;
					}
				}
				thread = null;
			}
		}, "WinstoneObjectPoolMgmt");
		thread.setDaemon(Boolean.TRUE);
		thread.start();
	}

	/**
	 * remove Unused RequestHandlers.
	 */
	private void removeUnusedRequestHandlers() {
		// Check max idle requestHandler count
		synchronized (requestHandlerSemaphore) {
			// If we have too many idle request handlers
			while (unusedRequestHandlerThreads.size() > maxIdleRequestHandlesInPool) {
				final RequestHandlerThread rh = unusedRequestHandlerThreads.get(0);
				rh.destroy();
				unusedRequestHandlerThreads.remove(rh);
			}
		}
	}

	/**
	 * Destroy Object Pool.
	 */
	public void destroy() {
		synchronized (requestHandlerSemaphore) {
			for (final RequestHandlerThread handlerThread : usedRequestHandlerThreads) {
				releaseRequestHandler(handlerThread);
			}
			for (final RequestHandlerThread handlerThread : unusedRequestHandlerThreads) {
				handlerThread.destroy();
			}
			unusedRequestHandlerThreads.clear();
		}
		if (thread != null) {
			thread.interrupt();
		}
	}

	/**
	 * Once the socket request comes in, this method is called. It reserves a
	 * request handler, then delegates the socket to that class. When it
	 * finishes, the handler is released back into the pool.
	 */
	public void handleRequest(final Socket socket, final Listener listener) throws IOException, InterruptedException {
		RequestHandlerThread rh = null;
		synchronized (requestHandlerSemaphore) {
			// If we have any spare, get it from the pool
			final int unused = unusedRequestHandlerThreads.size();
			if (unused > 0) {
				rh = unusedRequestHandlerThreads.remove(unused - 1);
				usedRequestHandlerThreads.add(rh);
				ObjectPool.logger.debug("RHPool: Using pooled handler thread - used: {} unused: {}", "" + usedRequestHandlerThreads.size(), "" + unusedRequestHandlerThreads.size());
			} // If we are out (and not over our limit), allocate a new one
			else if (usedRequestHandlerThreads.size() < ObjectPool.MAX_REQUEST_HANDLERS_IN_POOL) {
				rh = new RequestHandlerThread(this, threadIndex++, simulateModUniqueId, saveSessions);
				usedRequestHandlerThreads.add(rh);
				ObjectPool.logger.debug("RHPool: Spawning new handler thread - used: {} unused: {}]", "" + usedRequestHandlerThreads.size(), "" + unusedRequestHandlerThreads.size());
			} // otherwise throw fail message - we've blown our limit
			else {
				// Possibly insert a second chance here ? Delay and one retry ?
				// Remember to release the lock first
				ObjectPool.logger.warn("WARNING: Request handler pool limit exceeded - waiting for retry");
				// socket.close();
				// throw new UnavailableException("NoHandlersAvailable");
			}
		}

		if (rh != null) {
			rh.commenceRequestHandling(socket, listener);
		} else {
			// Sleep for a set period and try again from the pool
			Thread.sleep(ObjectPool.RETRY_PERIOD);
			synchronized (requestHandlerSemaphore) {
				if (usedRequestHandlerThreads.size() < ObjectPool.MAX_REQUEST_HANDLERS_IN_POOL) {
					rh = new RequestHandlerThread(this, threadIndex++, simulateModUniqueId, saveSessions);
					usedRequestHandlerThreads.add(rh);
					ObjectPool.logger.debug("RHPool: Spawning new handler thread - used: {} unused: {}", "" + usedRequestHandlerThreads.size(), "" + unusedRequestHandlerThreads.size());
				}
			}
			if (rh != null) {
				rh.commenceRequestHandling(socket, listener);
			} else {
				ObjectPool.logger.error("Request ignored because there were no more request handlers available in the pool");
				socket.close();
			}
		}
	}

	/**
	 * Release the handler back into the pool
	 */
	public void releaseRequestHandler(final RequestHandlerThread rh) {
		synchronized (requestHandlerSemaphore) {
			usedRequestHandlerThreads.remove(rh);
			unusedRequestHandlerThreads.add(rh);
			ObjectPool.logger.debug("RHPool: Releasing handler thread - used: {} unused: {}", "" + usedRequestHandlerThreads.size(), "" + unusedRequestHandlerThreads.size());
		}
	}

	/**
	 * An attempt at pooling request objects for reuse.
	 * 
	 * @return a WinstoneRequest instance.
	 */
	public WinstoneRequest getRequestFromPool() throws IOException {
		WinstoneRequest winstoneRequest = null;
		synchronized (requestPoolSemaphore) {
			// If we have any spare, get it from the pool
			final int unused = unusedRequestPool.size();
			if (unused > 0) {
				winstoneRequest = unusedRequestPool.remove(unused - 1);
				ObjectPool.logger.debug("ReqPool: Using pooled request - available: {}", "" + unusedRequestPool.size());
			} else {
				// If we are out, allocate a new one
				winstoneRequest = new WinstoneRequest(MAXPARAMALLOWED);
				ObjectPool.logger.debug("ReqPool: Spawning new request - available: {}", "" + unusedRequestPool.size());
			}
		}
		return winstoneRequest;
	}

	/**
	 * Release specified request to pool. Add it to unused if pool size is under
	 * the limit of MAX_REQUESTS_IN_POOL objects.
	 * 
	 * @param winstoneRequest
	 *            winstone Request
	 */
	public void releaseRequestToPool(final WinstoneRequest winstoneRequest) {
		winstoneRequest.cleanUp();
		synchronized (requestPoolSemaphore) {
			if (unusedRequestPool.size() < ObjectPool.MAX_REQUESTS_IN_POOL) {
				unusedRequestPool.add(winstoneRequest);
			}
			ObjectPool.logger.debug("ReqPool: Request released - available: {}", "" + unusedRequestPool.size());
		}
	}

	/**
	 * An attempt at pooling request objects for reuse.
	 * 
	 * @return a WinstoneResponse instance.
	 */
	public WinstoneResponse getResponseFromPool() throws IOException {
		WinstoneResponse rsp = null;
		synchronized (responsePoolSemaphore) {
			// If we have any spare, get it from the pool
			final int unused = unusedResponsePool.size();
			if (unused > 0) {
				rsp = unusedResponsePool.remove(unused - 1);
				ObjectPool.logger.debug("RspPool: Using pooled response - available: {}", "" + unusedResponsePool.size());
			} // If we are out, allocate a new one
			else {
				rsp = new WinstoneResponse();
				ObjectPool.logger.debug("RspPool: Spawning new response - available: {}", "" + unusedResponsePool.size());
			}
		}
		return rsp;
	}

	/**
	 * Release WinstoneResponse instance. Add it to unused if pool size is under
	 * the limit of MAX_REQUESTS_IN_POOL objects.
	 * 
	 * @param winstoneResponse
	 */
	public void releaseResponseToPool(final WinstoneResponse winstoneResponse) {
		winstoneResponse.cleanUp();
		synchronized (responsePoolSemaphore) {
			if (unusedResponsePool.size() < ObjectPool.MAX_RESPONSES_IN_POOL) {
				unusedResponsePool.add(winstoneResponse);
			}
			ObjectPool.logger.debug("RspPool: Response released - available: {}", "" + unusedResponsePool.size());
		}
	}

	/**
	 * 
	 * @return maximum Parameter Allowed.
	 */
	public static int getMaximumAllowedParameter() {
		return MAXPARAMALLOWED;
	}
}
