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

import net.winstone.accesslog.AccessLogger;
import net.winstone.core.ClientSocketException;
import net.winstone.core.HostConfiguration;
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

	private WinstoneInputStream inData;
	private WinstoneOutputStream outData;
	private WinstoneRequest req;
	private WinstoneResponse rsp;
	private Listener listener;
	private Socket socket;
	private long requestStartTime;
	private final boolean simulateModUniqueId;
	private final boolean saveSessions;

	/**
	 * Constructor - this is called by the handler pool, and just sets up for
	 * when a real request comes along.
	 */
	public RequestHandlerThread(boolean simulateModUniqueId, boolean saveSessions, Socket socket, Listener listener) {
		this.simulateModUniqueId = simulateModUniqueId;
		this.saveSessions = saveSessions;
		this.socket = socket;
		this.listener = listener;
	}

	/**
	 * The main thread execution code.
	 */
	@Override
	public void run() {
		// Start request processing
		InputStream inSocket = null;
		OutputStream outSocket = null;
		boolean iAmFirst = true;
		try {
			// Get input/output streams
			inSocket = socket.getInputStream();
			outSocket = socket.getOutputStream();

			// The keep alive loop - exiting from here means the connection has
			// closed
			boolean continueFlag = true;
			while (continueFlag) {
				try {
					long requestId = System.currentTimeMillis();
					this.listener.allocateRequestResponse(socket, inSocket, outSocket, this, iAmFirst);
					if (this.req == null) {
						// Dead request - happens sometimes with ajp13 - discard
						this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
						continue;
					}
					String servletURI = this.listener.parseURI(this, this.req, this.rsp, this.inData, this.socket, iAmFirst);
					if (servletURI == null) {
						RequestHandlerThread.logger.debug("Keep alive timed out in thread: {}", Thread.currentThread().getName());
						// Keep alive timed out - deallocate and go into wait
						// state
						this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
						continueFlag = false;
						continue;
					}

					if (this.simulateModUniqueId) {
						req.setAttribute("UNIQUE_ID", "" + requestId);
					}
					long headerParseTime = getRequestProcessTime();
					iAmFirst = false;

					HostConfiguration hostConfig = req.getHostGroup().getHostByName(req.getServerName());
					RequestHandlerThread.logger.debug("Starting request on host:[{}] with id: {}", "" + requestId, hostConfig.getHostname());

					// Get the URI from the request, check for prefix, then
					// match it to a requestDispatcher
					WebAppConfiguration webAppConfig = hostConfig.getWebAppByURI(servletURI);
					if (webAppConfig == null) {
						webAppConfig = hostConfig.getWebAppByURI("/");
					}
					if (webAppConfig == null) {
						RequestHandlerThread.logger.warn("Request URL {} not found - doesn't match any webapp prefix", servletURI);

						rsp.sendError(WinstoneResponse.SC_NOT_FOUND, "Request URL " + servletURI + " not found.<br><br>");
						rsp.flushBuffer();
						req.discardRequestBody();
						writeToAccessLog(servletURI, req, rsp, null);

						// Process keep-alive
						continueFlag = this.listener.processKeepAlive(req, rsp, inSocket);
						this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
						RequestHandlerThread.logger.debug("Finishing request id:  {}", "" + requestId);
						RequestHandlerThread.logger.debug("Processed complete request: headerParseTime={}ms totalTime={}ms path={}", new Object[] { "" + headerParseTime, "" + getRequestProcessTime(), servletURI });
						continue;
					}
					req.setWebAppConfig(webAppConfig);

					// Now we've verified it's in the right webapp, send
					// request in scope notify
					ServletRequestListener reqLsnrs[] = webAppConfig.getRequestListeners();
					for (ServletRequestListener reqLsnr1 : reqLsnrs) {
						ClassLoader cl = Thread.currentThread().getContextClassLoader();
						Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
						reqLsnr1.requestInitialized(new ServletRequestEvent(webAppConfig, req));
						Thread.currentThread().setContextClassLoader(cl);
					}

					// Lookup a dispatcher, then process with it
					processRequest(webAppConfig, req, rsp, webAppConfig.getServletURIFromRequestURI(servletURI));
					writeToAccessLog(servletURI, req, rsp, webAppConfig);

					this.outData.finishResponse();
					this.inData.finishRequest();
					RequestHandlerThread.logger.debug("Finishing request id:  {}", "" + requestId);

					// Process keep-alive
					continueFlag = this.listener.processKeepAlive(req, rsp, inSocket);

					// Set last accessed time on session as start of this
					// request
					req.markSessionsAsRequestFinished(this.requestStartTime, this.saveSessions);

					// send request listener notifies
					for (ServletRequestListener reqLsnr : reqLsnrs) {
						ClassLoader cl = Thread.currentThread().getContextClassLoader();
						Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
						reqLsnr.requestDestroyed(new ServletRequestEvent(webAppConfig, req));
						Thread.currentThread().setContextClassLoader(cl);
					}

					req.setWebAppConfig(null);
					rsp.setWebAppConfig(null);
					req.setRequestAttributeListeners(null);

					this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
					RequestHandlerThread.logger.debug("Processed complete request: headerParseTime={}ms totalTime={}ms path={}", new Object[] { "" + headerParseTime, "" + getRequestProcessTime(), servletURI });
				} catch (InterruptedIOException errIO) {
					continueFlag = false;
					RequestHandlerThread.logger.error("Socket read timed out - exiting request handler thread", errIO);
				} catch (SocketException errIO) {
					continueFlag = false;
				}
			}
			this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
			this.listener.releaseSocket(this.socket, inSocket, outSocket); // shut
																			// sockets
		} catch (Throwable err) {
			try {
				this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
			} catch (Throwable errClose) {
			}
			try {
				this.listener.releaseSocket(this.socket, inSocket, outSocket); // shut
																				// sockets
			} catch (Throwable errClose) {
			}
			if (!(err instanceof ClientSocketException)) {
				RequestHandlerThread.logger.error("Error within request handler thread", err);
			}
		}
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
		} catch (final ClientSocketException err) {
			// ignore this error. caused by a browser shutting down the
			// connection
		} catch (final Throwable err) {
			boolean ignore = Boolean.FALSE;
			for (Throwable t = err; t != null; t = t.getCause()) {
				if (t instanceof ClientSocketException) {
					ignore = Boolean.TRUE;
					break;
				}
			}
			if (!ignore) {
				RequestHandlerThread.logger.warn("Untrapped Error in Servlet", err);
				rdError = webAppConfig.getErrorDispatcherByClass(err);
			}
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
		rsp.getWinstoneOutputStream().setClosed(Boolean.TRUE);
		req.discardRequestBody();
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
