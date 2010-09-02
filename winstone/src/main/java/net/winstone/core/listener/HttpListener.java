/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core.listener;

import net.winstone.core.WinstoneResponse;
import net.winstone.core.WinstoneRequest;
import net.winstone.core.WinstoneInputStream;
import net.winstone.core.WinstoneOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.winstone.WinstoneException;
import net.winstone.WinstoneResourceBundle;

import net.winstone.core.HostGroup;
import net.winstone.core.ObjectPool;
import net.winstone.util.StringUtils;

/**
 * Implements the main listener daemon thread. This is the class that gets launched by the command line, and owns the server socket, etc.
 * Note that this class is also used as the base class for the HTTPS listener.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HttpListener.java,v 1.15 2007/05/01 04:39:49 rickknowles Exp $
 */
public class HttpListener implements Listener, Runnable {

    private static Logger logger = LoggerFactory.getLogger(HttpListener.class);
    protected static int LISTENER_TIMEOUT = 5000; // every 5s reset the
    // listener socket
    protected static int CONNECTION_TIMEOUT = 60000;
    protected static int BACKLOG_COUNT = 5000;
    protected static boolean DEFAULT_HNL = false;
    protected static int KEEP_ALIVE_TIMEOUT = 10000;
    protected static int KEEP_ALIVE_SLEEP = 20;
    protected static int KEEP_ALIVE_SLEEP_MAX = 500;
    protected final HostGroup hostGroup;
    protected final ObjectPool objectPool;
    protected boolean doHostnameLookups;
    protected int listenPort;
    protected String listenAddress;
    protected boolean interrupted = Boolean.FALSE;
    private final String serverVersion;

    /**
     * Constructor
     */
    public HttpListener(final Map<String, String> args, final ObjectPool objectPool, final HostGroup hostGroup) throws IOException {
        super();
        serverVersion = WinstoneResourceBundle.getInstance().getString("ServerVersion");
        // Load resources
        this.hostGroup = hostGroup;
        this.objectPool = objectPool;
        this.listenPort = Integer.parseInt(StringUtils.stringArg(args, getConnectorName() + "Port", "" + getDefaultPort()));
        this.listenAddress = StringUtils.stringArg(args, getConnectorName() + "ListenAddress", null);
        this.doHostnameLookups = StringUtils.booleanArg(args, getConnectorName() + "DoHostnameLookups", DEFAULT_HNL);
    }

    @Override
    public boolean start() {
        if (this.listenPort < 0) {
            return false;
        } else {
            this.interrupted = false;
            Thread thread = new Thread(this, "ConnectorThread:[" + getConnectorName() + "-" + Integer.toString(this.listenPort) + "]");
            thread.setDaemon(true);
            thread.start();
            return true;
        }
    }

    /**
     * The default port to use - this is just so that we can override for the SSL connector.
     */
    protected int getDefaultPort() {
        return 8080;
    }

    /**
     * The name to use when getting properties - this is just so that we can override for the SSL connector.
     */
    protected final String getConnectorName() {
        return getConnectorScheme();
    }

    protected String getConnectorScheme() {
        return "http";
    }

    /**
     * Gets a server socket - this is mostly for the purpose of allowing an override in the SSL connector.
     */
    protected ServerSocket getServerSocket() throws IOException {
        ServerSocket ss = this.listenAddress == null ? new ServerSocket(this.listenPort, BACKLOG_COUNT) : new ServerSocket(this.listenPort, BACKLOG_COUNT, InetAddress.getByName(this.listenAddress));
        return ss;
    }

    /**
     * The main run method. This continually listens for incoming connections, and allocates any that it finds to a request handler thread,
     * before going back to listen again.
     */
    @Override
    public void run() {
        try {
            ServerSocket ss = getServerSocket();
            ss.setSoTimeout(LISTENER_TIMEOUT);
            logger.info("{} Listener started: port={}", getConnectorName().toUpperCase(), this.listenPort + "");

            // Enter the main loop
            while (!interrupted) {
                // Get the listener
                Socket s = null;
                try {
                    s = ss.accept();
                } catch (java.io.InterruptedIOException err) {
                    s = null;
                }

                // if we actually got a socket, process it. Otherwise go around
                // again
                if (s != null) {
                    this.objectPool.handleRequest(s, this);
                }
            }

            // Close server socket
            ss.close();
        } catch (Throwable err) {
            logger.error("Error during " + getConnectorName().toUpperCase() + " listener init or shutdown", err);
        }
        logger.info("{} Listener shutdown successfully", getConnectorName().toUpperCase());
    }

    /**
     * Interrupts the listener thread. This will trigger a listener shutdown once the so timeout has passed.
     */
    @Override
    public void destroy() {
        this.interrupted = true;
    }

    /**
     * Called by the request handler thread, because it needs specific setup code for this connection's protocol (ie construction of
     * request/response objects, in/out streams, etc). This implementation parses incoming AJP13 packets, and builds an outputstream that is
     * capable of writing back the response in AJP13 packets.
     */
    @Override
    public void allocateRequestResponse(Socket socket, InputStream inSocket, OutputStream outSocket, RequestHandlerThread handler, boolean iAmFirst) throws SocketException, IOException {
        logger.trace("Allocating request/response", Thread.currentThread().getName());

        socket.setSoTimeout(CONNECTION_TIMEOUT);

        // Build input/output streams, plus request/response
        WinstoneInputStream inData = new WinstoneInputStream(inSocket);
        WinstoneOutputStream outData = new WinstoneOutputStream(outSocket, false);
        WinstoneRequest request = this.objectPool.getRequestFromPool();
        WinstoneResponse rsp = this.objectPool.getResponseFromPool();
        outData.setResponse(rsp);
        request.setInputStream(inData);
        rsp.setOutputStream(outData);
        rsp.setRequest(request);
        // rsp.updateContentTypeHeader("text/html");
        request.setHostGroup(this.hostGroup);

        // Set the handler's member variables so it can execute the servlet
        handler.setRequest(request);
        handler.setResponse(rsp);
        handler.setInStream(inData);
        handler.setOutStream(outData);

        // If using this listener, we must set the server header now, because it
        // must be the first header. Ajp13 listener can defer to the Apache Server
        // header
        rsp.setHeader("Server", serverVersion);
    }

    /**
     * Called by the request handler thread, because it needs specific shutdown code for this connection's protocol (ie releasing
     * input/output streams, etc).
     */
    @Override
    public void deallocateRequestResponse(RequestHandlerThread handler, WinstoneRequest req, WinstoneResponse rsp, WinstoneInputStream inData, WinstoneOutputStream outData) throws IOException {
        handler.setInStream(null);
        handler.setOutStream(null);
        handler.setRequest(null);
        handler.setResponse(null);
        if (req != null) {
            this.objectPool.releaseRequestToPool(req);
        }
        if (rsp != null) {
            this.objectPool.releaseResponseToPool(rsp);
        }
    }

    @Override
    public String parseURI(RequestHandlerThread handler, WinstoneRequest req, WinstoneResponse rsp, WinstoneInputStream inData, Socket socket, boolean iAmFirst) throws IOException {
        parseSocketInfo(socket, req);

        // Read the header line (because this is the first line of the request,
        // apply keep-alive timeouts to it if we are not the first request)
        if (!iAmFirst) {
            socket.setSoTimeout(KEEP_ALIVE_TIMEOUT);
        }

        byte uriBuffer[] = null;
        try {
            logger.debug("Waiting for a URI line");
            uriBuffer = inData.readLine();
        } catch (InterruptedIOException err) {
            // keep alive timeout ? ignore if not first
            if (iAmFirst) {
                throw err;
            } else {
                return null;
            }
        } finally {
            try {
                socket.setSoTimeout(CONNECTION_TIMEOUT);
            } catch (Throwable err) {
            }
        }
        handler.setRequestStartTime();

        // Get header data (eg protocol, method, uri, headers, etc)
        String uriLine = new String(uriBuffer);
        if (uriLine.trim().equals("")) {
            throw new SocketException("Empty URI Line");
        }
        String servletURI = parseURILine(uriLine, req, rsp);
        parseHeaders(req, inData);
        rsp.extractRequestKeepAliveHeader(req);
        int contentLength = req.getContentLength();
        if (contentLength != -1) {
            inData.setContentLength(contentLength);
        }
        return servletURI;
    }

    /**
     * Called by the request handler thread, because it needs specific shutdown code for this connection's protocol if the keep-alive period
     * expires (ie closing sockets, etc). This implementation simply shuts down the socket and streams.
     */
    @Override
    public void releaseSocket(Socket socket, InputStream inSocket, OutputStream outSocket) throws IOException {
        // Logger.log(Logger.FULL_DEBUG, "Releasing socket: " +
        // Thread.currentThread().getName());
        IOException ioException = null;

        try {
            inSocket.close();
        } catch (IOException e) {
            ioException = e;
        }
        try {
            outSocket.close();
        } catch (IOException e) {
            ioException = e;
        }
        try {
            socket.close();
        } catch (IOException e) {
            ioException = e;
        }
        if (ioException != null) {
            throw ioException;
        }
    }

    protected void parseSocketInfo(Socket socket, WinstoneRequest request) throws IOException {
        logger.debug("Parsing socket info");
        request.setScheme(getConnectorScheme());
        request.setServerPort(socket.getLocalPort());
        request.setLocalPort(socket.getLocalPort());
        request.setLocalAddr(socket.getLocalAddress().getHostAddress());
        request.setRemoteIP(socket.getInetAddress().getHostAddress());
        request.setRemotePort(socket.getPort());
        if (this.doHostnameLookups) {
            request.setServerName(socket.getLocalAddress().getHostName());
            request.setRemoteName(socket.getInetAddress().getHostName());
            request.setLocalName(socket.getLocalAddress().getHostName());
        } else {
            request.setServerName(socket.getLocalAddress().getHostAddress());
            request.setRemoteName(socket.getInetAddress().getHostAddress());
            request.setLocalName(socket.getLocalAddress().getHostAddress());
        }
    }

    /**
     * Tries to wait for extra requests on the same socket. If any are found before the timeout expires, it exits with a true, indicating a
     * new request is waiting. If the protocol does not support keep-alives, or the request instructed us to close the connection, or the
     * timeout expires, return a false, instructing the handler thread to begin shutting down the socket and relase itself.
     */
    @Override
    public boolean processKeepAlive(WinstoneRequest request, WinstoneResponse response, InputStream inSocket) throws IOException, InterruptedException {
        // Try keep alive if allowed
        boolean continueFlag = !response.closeAfterRequest();
        return continueFlag;
    }

    /**
     * Processes the uri line into it's component parts, determining protocol, method and uri.
     */
    public static String parseURILine(final String uriLine, final WinstoneRequest request, final WinstoneResponse response) {
        logger.trace("URI Line:", uriLine.trim());

        // Method
        int spacePos = uriLine.indexOf(' ');
        if (spacePos == -1) {
            throw new WinstoneException("Error URI Line: " + uriLine);
        }
        String method = uriLine.substring(0, spacePos).toUpperCase();
        String fullURI = null;

        // URI
        String remainder = uriLine.substring(spacePos + 1);
        spacePos = remainder.indexOf(' ');
        if (spacePos == -1) {
            fullURI = trimHostName(remainder.trim());
            request.setProtocol("HTTP/0.9");
            response.setProtocol("HTTP/0.9");
        } else {
            fullURI = trimHostName(remainder.substring(0, spacePos).trim());
            String protocol = remainder.substring(spacePos + 1).trim().toUpperCase();
            request.setProtocol(protocol);
            response.setProtocol(protocol);
        }

        request.setMethod(method);
        // req.setRequestURI(fullURI);
        return fullURI;
    }

    public static String trimHostName(final String input) {
        if (input == null) {
            return null;
        } else if (input.startsWith("/")) {
            return input;
        }

        int hostStart = input.indexOf("://");
        if (hostStart == -1) {
            return input;
        }
        String hostName = input.substring(hostStart + 3);
        int pathStart = hostName.indexOf('/');
        if (pathStart == -1) {
            return "/";
        } else {
            return hostName.substring(pathStart);
        }
    }

    /**
     * Parse the incoming stream into a list of headers (stopping at the first blank line), then call the parseHeaders(req, list) method on
     * that list.
     */
    public void parseHeaders(WinstoneRequest req, WinstoneInputStream inData) throws IOException {
        List<String> headerList = new ArrayList<String>();

        if (!req.getProtocol().startsWith("HTTP/0")) {
            // Loop to get headers
            byte headerBuffer[] = inData.readLine();
            String headerLine = new String(headerBuffer);

            while (headerLine.trim().length() > 0) {
                if (headerLine.indexOf(':') != -1) {
                    headerList.add(headerLine.trim());
                    logger.debug("Header: " + headerLine.trim());

                }
                headerBuffer = inData.readLine();
                headerLine = new String(headerBuffer);
            }
        }

        // If no headers available, parse an empty list
        req.parseHeaders(headerList);
    }
}
