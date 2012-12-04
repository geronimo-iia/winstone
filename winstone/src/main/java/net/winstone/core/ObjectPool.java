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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.winstone.core.listener.Listener;
import net.winstone.core.listener.RequestHandlerThread;
import net.winstone.util.BoundedExecutorService;
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
	 * Default startup request handler in pool (5).
	 */
	private final static transient int STARTUP_REQUEST_HANDLERS_IN_POOL = 5;
	/**
	 * Maximum idle request handler in pool (20).
	 */
	private final static transient int MAX_IDLE_REQUEST_HANDLERS_IN_POOL = 20;
	/**
	 * Maximum request handler in pool (200).
	 */
	private final static transient int MAX_REQUEST_HANDLERS_IN_POOL = 200;

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

	private final ExecutorService requestHandler;
	private List<WinstoneRequest> unusedRequestPool;
	private List<WinstoneResponse> unusedResponsePool;
	private final Object requestPoolSemaphore = new Object();
	private final Object responsePoolSemaphore = new Object();

	private final boolean simulateModUniqueId;
	private final boolean saveSessions;

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

		// idle thread will only hang around for 60 secs
		ExecutorService es = new ThreadPoolExecutor(MAX_IDLE_REQUEST_HANDLERS_IN_POOL, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {
			private int threadIndex;

			public synchronized Thread newThread(Runnable r) {
				String threadName = "RequestHandlerThread[" + (++threadIndex) + "]";
				// allocate a thread to run on this object
				Thread thread = new Thread(r, threadName);
				thread.setDaemon(true);
				return thread;
			}
		});

		requestHandler = new BoundedExecutorService(es, MAX_REQUEST_HANDLERS_IN_POOL);
		// Build the request/response pools
		unusedRequestPool = new ArrayList<WinstoneRequest>();
		unusedResponsePool = new ArrayList<WinstoneResponse>();

		// Get handler pool options
		startupRequest = StringUtils.intArg(args, "handlerCountStartup", ObjectPool.STARTUP_REQUEST_HANDLERS_IN_POOL);
		maxRequestHandlesInPool = StringUtils.intArg(args, "handlerCountMax", ObjectPool.MAX_REQUEST_HANDLERS_IN_POOL);
		maxIdleRequestHandlesInPool = StringUtils.intArg(args, "handlerCountMaxIdle", ObjectPool.MAX_IDLE_REQUEST_HANDLERS_IN_POOL);

		// Initialize the request/response pools
		for (int n = 0; n < ObjectPool.START_REQUESTS_IN_POOL; n++) {
			unusedRequestPool.add(new WinstoneRequest(ObjectPool.MAXPARAMALLOWED));
		}
		for (int n = 0; n < ObjectPool.START_RESPONSES_IN_POOL; n++) {
			unusedResponsePool.add(new WinstoneResponse());
		}
	}

	/**
	 * Destroy Object Pool.
	 */
	public void destroy() {
		requestHandler.shutdown();
	}

	/**
	 * Once the socket request comes in, this method is called. It reserves a
	 * request handler, then delegates the socket to that class. When it
	 * finishes, the handler is released back into the pool.
	 */
	public void handleRequest(final Socket socket, final Listener listener) throws IOException, InterruptedException {
		try {
			requestHandler.submit(new RequestHandlerThread(this.simulateModUniqueId, this.saveSessions, socket, listener));
		} catch (RejectedExecutionException e) {
			logger.warn("WARNING: Request handler pool limit exceeded - waiting for retry");
			socket.close();
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
				winstoneRequest = this.unusedRequestPool.remove(unused - 1);
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
				rsp = this.unusedResponsePool.remove(unused - 1);
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
