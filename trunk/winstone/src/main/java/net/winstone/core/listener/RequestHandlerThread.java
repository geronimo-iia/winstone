/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core.listener;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletResponse;

import net.winstone.accesslog.AccessLogger;
import net.winstone.core.HostConfiguration;
import net.winstone.core.ObjectPool;
import net.winstone.core.SimpleRequestDispatcher;
import net.winstone.core.WebAppConfiguration;
import net.winstone.core.WinstoneInputStream;
import net.winstone.core.WinstoneOutputStream;
import net.winstone.core.WinstoneRequest;
import net.winstone.core.WinstoneResponse;

import org.slf4j.LoggerFactory;

/**
 * The threads to which incoming requests get allocated.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: RequestHandlerThread.java,v 1.21 2007/04/23 02:55:35
 *          rickknowles Exp $
 */
public class RequestHandlerThread implements Runnable {

	protected static org.slf4j.Logger logger = LoggerFactory.getLogger(RequestHandlerThread.class);
	private final Thread thread;
	private final ObjectPool objectPool;
	private WinstoneInputStream inData;
	private WinstoneOutputStream outData;
	private WinstoneRequest req;
	private WinstoneResponse rsp;
	private Listener listener;
	private Socket socket;
	private final String threadName;
	private long requestStartTime;
	private final boolean simulateModUniqueId;
	private final boolean saveSessions;

	// private Object processingMonitor = new Boolean(true);

	/**
	 * Constructor - this is called by the handler pool, and just sets up for
	 * when a real request comes along.
	 */
	public RequestHandlerThread(final ObjectPool objectPool, final int threadIndex, final boolean simulateModUniqueId, final boolean saveSessions) {
		this.objectPool = objectPool;
		this.simulateModUniqueId = simulateModUniqueId;
		this.saveSessions = saveSessions;
		threadName = "RequestHandlerThread[" + threadIndex + "]";

		// allocate a thread to run on this object
		thread = new Thread(this, threadName);
		thread.setDaemon(true);
	}

	/**
	 * The main thread execution code.
	 */
	@Override
	public void run() {

		boolean interrupted = false;
		while (!interrupted) {
			// Start request processing
			InputStream inSocket = null;
			OutputStream outSocket = null;
			boolean iAmFirst = true;
			try {
				// Get input/output streams
				inSocket = socket.getInputStream();
				outSocket = socket.getOutputStream();

				// The keep alive loop - exiting from here means the connection
				// has closed
				boolean continueFlag = true;
				while (continueFlag && !interrupted) {
					try {
						final long requestId = System.currentTimeMillis();
						listener.allocateRequestResponse(socket, inSocket, outSocket, this, iAmFirst);
						if (req == null) {
							// Dead request - happens sometimes with ajp13 -
							// discard
							listener.deallocateRequestResponse(this, req, rsp, inData, outData);
							continue;
						}
						final String servletURI = listener.parseURI(this, req, rsp, inData, socket, iAmFirst);
						if (servletURI == null) {
							RequestHandlerThread.logger.debug("Keep alive timed out in thread: {}", threadName);

							// Keep alive timed out - deallocate and go into
							// wait state
							listener.deallocateRequestResponse(this, req, rsp, inData, outData);
							continueFlag = false;
							continue;
						}

						if (simulateModUniqueId) {
							req.setAttribute("UNIQUE_ID", "" + requestId);
						}
						final long headerParseTime = getRequestProcessTime();
						iAmFirst = false;

						final HostConfiguration hostConfig = req.getHostGroup().getHostByName(req.getServerName());
						RequestHandlerThread.logger.debug("Starting request on host:[{}] with id: {}", "" + requestId, hostConfig.getHostname());

						// Get the URI from the request, check for prefix, then
						// match it to a requestDispatcher
						WebAppConfiguration webAppConfig = hostConfig.getWebAppByURI(servletURI);
						if (webAppConfig == null) {
							webAppConfig = hostConfig.getWebAppByURI("/");
						}
						if (webAppConfig == null) {
							RequestHandlerThread.logger.warn("Request URL {} not found - doesn't match any webapp prefix", servletURI);
							rsp.sendError(HttpServletResponse.SC_NOT_FOUND, "Request URL " + servletURI + " not found.<br><br>");
							rsp.flushBuffer();
							req.discardRequestBody();
							writeToAccessLog(servletURI, req, rsp, null);

							// Process keep-alive
							continueFlag = listener.processKeepAlive(req, rsp, inSocket);
							listener.deallocateRequestResponse(this, req, rsp, inData, outData);
							RequestHandlerThread.logger.debug("Finishing request id:  {}", "" + requestId);
							RequestHandlerThread.logger.debug("Processed complete request: headerParseTime={}ms totalTime={}ms path={}", new Object[] { "" + headerParseTime, "" + getRequestProcessTime(), servletURI });
							continue;
						}
						req.setWebAppConfig(webAppConfig);

						// Now we've verified it's in the right webapp, send
						// request in scope notify
						final ServletRequestListener reqLsnrs[] = webAppConfig.getRequestListeners();
						for (int n = 0; n < reqLsnrs.length; n++) {
							final ClassLoader cl = Thread.currentThread().getContextClassLoader();
							Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
							reqLsnrs[n].requestInitialized(new ServletRequestEvent(webAppConfig, req));
							Thread.currentThread().setContextClassLoader(cl);
						}

						// Lookup a dispatcher, then process with it
						processRequest(webAppConfig, req, rsp, webAppConfig.getServletURIFromRequestURI(servletURI));
						writeToAccessLog(servletURI, req, rsp, webAppConfig);

						outData.finishResponse();
						inData.finishRequest();

						RequestHandlerThread.logger.debug("Finishing request id:  {}", "" + requestId);

						// Process keep-alive
						continueFlag = listener.processKeepAlive(req, rsp, inSocket);

						// Set last accessed time on session as start of this
						// request
						req.markSessionsAsRequestFinished(requestStartTime, saveSessions);

						// send request listener notifies
						for (int n = 0; n < reqLsnrs.length; n++) {
							final ClassLoader cl = Thread.currentThread().getContextClassLoader();
							Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
							reqLsnrs[n].requestDestroyed(new ServletRequestEvent(webAppConfig, req));
							Thread.currentThread().setContextClassLoader(cl);
						}

						req.setWebAppConfig(null);
						rsp.setWebAppConfig(null);
						req.setRequestAttributeListeners(null);

						listener.deallocateRequestResponse(this, req, rsp, inData, outData);
						RequestHandlerThread.logger.debug("Processed complete request: headerParseTime={}ms totalTime={}ms path={}", new Object[] { "" + headerParseTime, "" + getRequestProcessTime(), servletURI });
					} catch (final InterruptedIOException errIO) {
						continueFlag = false;
						RequestHandlerThread.logger.error("Socket read timed out - exiting request handler thread", errIO);
					} catch (final SocketException errIO) {
						continueFlag = false;
					}
				}
				listener.deallocateRequestResponse(this, req, rsp, inData, outData);
				listener.releaseSocket(socket, inSocket, outSocket); // shut
																		// sockets
			} catch (final Throwable err) {
				try {
					listener.deallocateRequestResponse(this, req, rsp, inData, outData);
				} catch (final Throwable errClose) {
				}
				try {
					listener.releaseSocket(socket, inSocket, outSocket); // shut
																			// sockets
				} catch (final Throwable errClose) {
				}
				RequestHandlerThread.logger.error("Error within request handler thread", err);
			}

			objectPool.releaseRequestHandler(this);

			if (!interrupted) {
				// Suspend this thread until we get assigned and woken up
				RequestHandlerThread.logger.debug("Thread entering wait state");
				try {
					synchronized (this) {
						this.wait();
					}
				} catch (final InterruptedException err) {
					interrupted = true;
				}
				RequestHandlerThread.logger.debug("Thread leaving wait state");
			}
		}
		RequestHandlerThread.logger.debug("Exiting thread");
	}

	/**
	 * Actually process the request. This takes the request and response, and
	 * feeds them to the desired servlet, which then processes them or throws
	 * them off to another servlet.
	 */
	private void processRequest(final WebAppConfiguration webAppConfig, final WinstoneRequest req, final WinstoneResponse rsp, final String path) throws IOException, ServletException {
		SimpleRequestDispatcher rd = null;
		javax.servlet.RequestDispatcher rdError = null;
		try {
			rd = webAppConfig.getInitialDispatcher(path, req, rsp);

			// Null RD means an error or we have been redirected to a welcome
			// page
			if (rd != null) {
				RequestHandlerThread.logger.debug("Processing with RD: {}", rd.getName());
				rd.forward(req, rsp);
			}
			// if null returned, assume we were redirected
		} catch (final Throwable err) {
			RequestHandlerThread.logger.warn("Untrapped Error in Servlet", err);
			rdError = webAppConfig.getErrorDispatcherByClass(err);
		}

		// If there was any kind of error, execute the error dispatcher here
		if (rdError != null) {
			try {
				if (rsp.isCommitted()) {
					rdError.include(req, rsp);
				} else {
					rsp.resetBuffer();
					rdError.forward(req, rsp);
				}
			} catch (final Throwable err) {
				RequestHandlerThread.logger.error("Error in the error servlet ", err);
			}
			// rsp.sendUntrappedError(err, req, rd != null ? rd.getName() :
			// null);
		}
		rsp.flushBuffer();
		rsp.getWinstoneOutputStream().setClosed(true);
		req.discardRequestBody();
	}

	/**
	 * Assign a socket to the handler
	 */
	public void commenceRequestHandling(final Socket socket, final Listener listener) {
		this.listener = listener;
		this.socket = socket;
		if (thread.isAlive()) {
			synchronized (this) {
				notifyAll();
			}
		} else {
			thread.start();
		}
	}

	public void setRequest(final WinstoneRequest request) {
		req = request;
	}

	public void setResponse(final WinstoneResponse response) {
		rsp = response;
	}

	public void setInStream(final WinstoneInputStream inStream) {
		inData = inStream;
	}

	public void setOutStream(final WinstoneOutputStream outStream) {
		outData = outStream;
	}

	public void setRequestStartTime() {
		requestStartTime = System.currentTimeMillis();
	}

	public long getRequestProcessTime() {
		return System.currentTimeMillis() - requestStartTime;
	}

	/**
	 * Trigger the thread destruction for this handler
	 */
	public void destroy() {
		if (thread.isAlive()) {
			thread.interrupt();
		}
	}

	protected void writeToAccessLog(final String originalURL, final WinstoneRequest request, final WinstoneResponse response, final WebAppConfiguration webAppConfig) {
		if (webAppConfig != null) {
			// Log a row containing appropriate data
			final AccessLogger accessLogger = webAppConfig.getAccessLogger();
			if (accessLogger != null) {
				accessLogger.log(originalURL, request, response);
			}
		}
	}
}
