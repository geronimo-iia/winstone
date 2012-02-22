/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core.listener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;

import net.winstone.WinstoneException;
import net.winstone.core.HostGroup;
import net.winstone.core.ObjectPool;
import net.winstone.core.WinstoneInputStream;
import net.winstone.core.WinstoneOutputStream;
import net.winstone.core.WinstoneRequest;
import net.winstone.core.WinstoneResponse;
import net.winstone.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the main listener daemon thread. This is the class that gets
 * launched by the command line, and owns the server socket, etc.
 * 
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: Ajp13Listener.java,v 1.13 2008/09/05 02:39:47 rickknowles Exp $
 */
public class Ajp13Listener implements Listener, Runnable {

	protected static Logger logger = LoggerFactory.getLogger(Ajp13Listener.class);
	private final static int LISTENER_TIMEOUT = 5000; // every 5s reset the
														// listener socket
	private final static int DEFAULT_PORT = 8009;
	private final static int CONNECTION_TIMEOUT = 60000;
	private final static int BACKLOG_COUNT = 1000;
	private final static int KEEP_ALIVE_TIMEOUT = -1;
	// private final static int KEEP_ALIVE_SLEEP = 50;
	// private final static int KEEP_ALIVE_SLEEP_MAX = 500;
	private final static String TEMPORARY_URL_STASH = "winstone.ajp13.TemporaryURLAttribute";
	private final HostGroup hostGroup;
	private final ObjectPool objectPool;
	private final int listenPort;
	private boolean interrupted;
	private final String listenAddress;
	private ServerSocket serverSocket;

	/**
	 * 
	 * Build a new instance of Ajp13Listener.
	 * 
	 * @param args
	 *            args
	 * @param objectPool
	 *            objectPool instance
	 * @param hostGroup
	 *            hostGroup instance
	 */
	public Ajp13Listener(final Map<String, String> args, final ObjectPool objectPool, final HostGroup hostGroup) {
		// Load resources
		this.hostGroup = hostGroup;
		this.objectPool = objectPool;
		listenPort = StringUtils.intArg(args, "ajp13Port", Ajp13Listener.DEFAULT_PORT);
		listenAddress = StringUtils.stringArg(args, "ajp13ListenAddress", null);
	}

	@Override
	public boolean start() throws IOException {
		if (listenPort < 0) {
			return false;
		} else {
			ServerSocket ss = null;
			try {
				ss = listenAddress == null ? new ServerSocket(listenPort, Ajp13Listener.BACKLOG_COUNT) : new ServerSocket(listenPort, Ajp13Listener.BACKLOG_COUNT, InetAddress.getByName(listenAddress));
			} catch (final IOException e) {
				throw (IOException) new IOException("Failed to listen on port " + listenPort).initCause(e);
			}
			ss.setSoTimeout(Ajp13Listener.LISTENER_TIMEOUT);
			Ajp13Listener.logger.info("AJP13 Listener started: port={}", listenPort + "");
			serverSocket = ss;

			interrupted = false;
			final Thread thread = new Thread(this, StringUtils.replaceToken("ConnectorThread:ajp13-[#0]", Integer.toString(listenPort)));
			thread.setDaemon(true);
			thread.start();
			return true;
		}

	}

	/**
	 * The main run method. This handles the normal thread processing.
	 */
	@Override
	public void run() {
		try {
			// Enter the main loop
			while (!interrupted) {
				// Get the listener
				Socket s = null;
				try {
					s = serverSocket.accept();
				} catch (final java.io.InterruptedIOException err) {
					s = null;
				}
				// if we actually got a socket, process it. Otherwise go around
				// again
				if (s != null) {
					objectPool.handleRequest(s, this);
				}
			}
			// Close server socket
			serverSocket.close();
			serverSocket = null;
			Ajp13Listener.logger.info("AJP13 Listener shutdown successfully");
		} catch (final Throwable err) {
			Ajp13Listener.logger.error("Error during AJP13 listener init or shutdown", err);
		}
	}

	/**
	 * Interrupts the listener thread. This will trigger a listener shutdown
	 * once the so timeout has passed.
	 */
	@Override
	public void destroy() {
		interrupted = true;
	}

	/**
	 * Called by the request handler thread, because it needs specific setup
	 * code for this connection's protocol (ie construction of request/response
	 * objects, in/out streams, etc). This implementation parses incoming AJP13
	 * packets, and builds an outputstream that is capable of writing back the
	 * response in AJP13 packets.
	 */
	@Override
	public void allocateRequestResponse(final Socket socket, final InputStream inSocket, final OutputStream outSocket, final RequestHandlerThread handler, final boolean iAmFirst) throws SocketException, IOException {
		final WinstoneRequest request = objectPool.getRequestFromPool();
		final WinstoneResponse response = objectPool.getResponseFromPool();
		response.setRequest(request);
		request.setHostGroup(hostGroup);
		// rsp.updateContentTypeHeader("text/html");
		if (iAmFirst) {
			socket.setSoTimeout(Ajp13Listener.CONNECTION_TIMEOUT);
		} else {
			socket.setSoTimeout(Ajp13Listener.KEEP_ALIVE_TIMEOUT);
		}
		Ajp13IncomingPacket headers = null;
		try {
			headers = new Ajp13IncomingPacket(inSocket, handler);
		} catch (final InterruptedIOException err) {
			// keep alive timeout ? ignore if not first
			if (iAmFirst) {
				throw err;
			} else {
				deallocateRequestResponse(handler, request, response, null, null);
				return;
			}
		} finally {
			try {
				socket.setSoTimeout(Ajp13Listener.CONNECTION_TIMEOUT);
			} catch (final Throwable err) {
			}
		}
		if (headers.getPacketLength() > 0) {
			headers.parsePacket("8859_1");
			parseSocketInfo(headers, request);
			request.parseHeaders(Arrays.asList(headers.getHeaders()));
			final String servletURI = parseURILine(headers, request, response);
			request.setAttribute(Ajp13Listener.TEMPORARY_URL_STASH, servletURI);

			// If content-length present and non-zero, download the other
			// packets
			WinstoneInputStream inData;
			final int contentLength = request.getContentLength();
			if (contentLength > 0) {
				final byte bodyContent[] = new byte[contentLength];
				int position = 0;
				while (position < contentLength) {
					outSocket.write(getBodyRequestPacket(Math.min(contentLength - position, 8184)));
					position = getBodyResponsePacket(inSocket, bodyContent, position);
					Ajp13Listener.logger.debug("Read {}/{} bytes from request body", "" + position, "" + contentLength);

				}
				inData = new WinstoneInputStream(bodyContent);
				inData.setContentLength(contentLength);
			} else {
				inData = new WinstoneInputStream(new byte[0]);
			}
			request.setInputStream(inData);

			// Build input/output streams, plus request/response
			final WinstoneOutputStream outData = new Ajp13OutputStream(socket.getOutputStream(), "8859_1");
			outData.setResponse(response);
			response.setOutputStream(outData);

			// Set the handler's member variables so it can execute the servlet
			handler.setRequest(request);
			handler.setResponse(response);
			handler.setInStream(inData);
			handler.setOutStream(outData);
		}

	}

	/**
	 * Called by the request handler thread, because it needs specific shutdown
	 * code for this connection's protocol (ie releasing input/output streams,
	 * etc).
	 */
	@Override
	public void deallocateRequestResponse(final RequestHandlerThread handler, final WinstoneRequest request, final WinstoneResponse response, final WinstoneInputStream inData, final WinstoneOutputStream outData) throws IOException {
		handler.setInStream(null);
		handler.setOutStream(null);
		handler.setRequest(null);
		handler.setResponse(null);
		if (request != null) {
			objectPool.releaseRequestToPool(request);
		}
		if (response != null) {
			objectPool.releaseResponseToPool(response);
		}
	}

	/**
	 * This is kind of a hack, since we have already parsed the uri to get the
	 * input stream. Just pass back the request uri
	 */
	@Override
	public String parseURI(final RequestHandlerThread handler, final WinstoneRequest req, final WinstoneResponse rsp, final WinstoneInputStream inData, final Socket socket, final boolean iAmFirst) throws IOException {
		final String uri = (String) req.getAttribute(Ajp13Listener.TEMPORARY_URL_STASH);
		req.removeAttribute(Ajp13Listener.TEMPORARY_URL_STASH);
		return uri;
	}

	/**
	 * Called by the request handler thread, because it needs specific shutdown
	 * code for this connection's protocol if the keep-alive period expires (ie
	 * closing sockets, etc). This implementation simply shuts down the socket
	 * and streams.
	 */
	@Override
	public void releaseSocket(final Socket socket, final InputStream inSocket, final OutputStream outSocket) throws IOException {
		inSocket.close();
		outSocket.close();
		socket.close();
	}

	/**
	 * Extract the header details relating to socket stuff from the ajp13 header
	 * packet
	 */
	private void parseSocketInfo(final Ajp13IncomingPacket headers, final WinstoneRequest req) {
		req.setServerPort(headers.getServerPort());
		req.setRemoteIP(headers.getRemoteAddress());
		req.setServerName(headers.getServerName());
		req.setLocalPort(headers.getServerPort());
		req.setLocalAddr(headers.getServerName());
		req.setRemoteIP(headers.getRemoteAddress());
		if ((headers.getRemoteHost() != null) && !headers.getRemoteHost().equals("")) {
			req.setRemoteName(headers.getRemoteHost());
		} else {
			req.setRemoteName(headers.getRemoteAddress());
		}
		req.setScheme(headers.isSSL() ? "https" : "http");
		req.setIsSecure(headers.isSSL());
	}

	private String parseURILine(final Ajp13IncomingPacket headers, final WinstoneRequest request, final WinstoneResponse response) throws UnsupportedEncodingException {
		request.setMethod(headers.getMethod());
		request.setProtocol(headers.getProtocol());
		response.setProtocol(headers.getProtocol());
		response.extractRequestKeepAliveHeader(request);
		// req.setServletPath(headers.getURI());
		// req.setRequestURI(headers.getURI());

		// Get query string if supplied
		for (final Object o : headers.getAttributes().keySet()) {
			final String attName = (String) o;
			if (attName.equals("query_string")) {
				final String qs = headers.getAttributes().get("query_string");
				request.setQueryString(qs);
				// req.getParameters().putAll(WinstoneRequest.extractParameters(qs,
				// req.getEncoding(), mainResources));
				// req.setRequestURI(headers.getURI() + "?" + qs);
			} else if (attName.equals("ssl_cert")) {
				final String certValue = headers.getAttributes().get("ssl_cert");
				final InputStream certStream = new ByteArrayInputStream(certValue.getBytes("8859_1"));
				final X509Certificate certificateArray[] = new X509Certificate[1];
				try {
					certificateArray[0] = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(certStream);
				} catch (final CertificateException err) {
					Ajp13Listener.logger.debug("Skipping invalid SSL certificate: {}", certValue);
				}
				request.setAttribute("javax.servlet.request.X509Certificate", certificateArray);
				request.setIsSecure(true);
			} else if (attName.equals("ssl_cipher")) {
				final String cipher = headers.getAttributes().get("ssl_cipher");
				request.setAttribute("javax.servlet.request.cipher_suite", cipher);
				request.setAttribute("javax.servlet.request.key_size", getKeySize(cipher));
				request.setIsSecure(true);
			} else if (attName.equals("ssl_session")) {
				request.setAttribute("javax.servlet.request.ssl_session", headers.getAttributes().get("ssl_session"));
				request.setIsSecure(true);
			} else {
				Ajp13Listener.logger.debug("Unknown request attribute ignored: {}={}", attName, "" + headers.getAttributes().get(attName));
			}
		}
		return headers.getURI();
	}

	private Integer getKeySize(final String cipherSuite) {
		if (cipherSuite.indexOf("_WITH_NULL_") != -1) {
			return 0;
		} else if (cipherSuite.indexOf("_WITH_IDEA_CBC_") != -1) {
			return 128;
		} else if (cipherSuite.indexOf("_WITH_RC2_CBC_40_") != -1) {
			return 40;
		} else if (cipherSuite.indexOf("_WITH_RC4_40_") != -1) {
			return 40;
		} else if (cipherSuite.indexOf("_WITH_RC4_128_") != -1) {
			return 128;
		} else if (cipherSuite.indexOf("_WITH_DES40_CBC_") != -1) {
			return 40;
		} else if (cipherSuite.indexOf("_WITH_DES_CBC_") != -1) {
			return 56;
		} else if (cipherSuite.indexOf("_WITH_3DES_EDE_CBC_") != -1) {
			return 168;
		} else {
			return null;
		}
	}

	/**
	 * Tries to wait for extra requests on the same socket. If any are found
	 * before the timeout expires, it exits with a true, indicating a new
	 * request is waiting. If the timeout expires, return a false, instructing
	 * the handler thread to begin shutting down the socket and relase itself.
	 */
	@Override
	public boolean processKeepAlive(final WinstoneRequest request, final WinstoneResponse response, final InputStream inSocket) throws IOException, InterruptedException {
		return true;
	}

	/**
	 * Build the packet needed for asking for a body chunk
	 */
	private byte[] getBodyRequestPacket(final int desiredPacketLength) {
		final byte getBodyRequestPacket[] = new byte[] { 0x41, 0x42, 0x00, 0x03, 0x06, 0x00, 0x00 };
		Ajp13OutputStream.setIntBlock(desiredPacketLength, getBodyRequestPacket, 5);
		return getBodyRequestPacket;
	}

	/**
	 * Process the server response to a get_body_chunk request. This loads the
	 * packet from the stream, and unpacks it into the buffer at the right
	 * place.
	 */
	private int getBodyResponsePacket(final InputStream in, final byte buffer[], final int offset) throws IOException {
		final DataInputStream din = new DataInputStream(in);
		// Get the incoming packet flag
		final byte headerBuffer[] = new byte[4];
		din.readFully(headerBuffer);
		if ((headerBuffer[0] != 0x12) || (headerBuffer[1] != 0x34)) {
			throw new WinstoneException("Invalid AJP header");
		}

		// Read in the whole packet
		final int packetLength = ((headerBuffer[2] & 0xFF) << 8) + (headerBuffer[3] & 0xFF);
		if (packetLength == 0) {
			return offset;
		}

		// Look for packet length
		final byte bodyLengthBuffer[] = new byte[2];
		din.readFully(bodyLengthBuffer);
		final int bodyLength = ((bodyLengthBuffer[0] & 0xFF) << 8) + (bodyLengthBuffer[1] & 0xFF);
		din.readFully(buffer, offset, bodyLength);

		return bodyLength + offset;
	}

	/**
	 * Useful method for dumping out the contents of a packet in hex form
	 */
	public static void packetDump(final byte packetBytes[], final int packetLength) {
		String dump = "";
		for (int n = 0; n < packetLength; n += 16) {
			String line = Integer.toHexString((n >> 4) & 0xF) + "0:";
			for (int j = 0; j < Math.min(packetLength - n, 16); j++) {
				line = line + " " + ((packetBytes[n + j] & 0xFF) < 16 ? "0" : "") + Integer.toHexString(packetBytes[n + j] & 0xFF);
			}

			line = line + "    ";
			for (int j = 0; j < Math.min(packetLength - n, 16); j++) {
				final byte me = (byte) (packetBytes[n + j] & 0xFF);
				line = line + (((me > 32) && (me < 123)) ? (char) me : '.');
			}
			dump = dump + line + "\r\n";
		}
		System.out.println(dump);
	}
}
