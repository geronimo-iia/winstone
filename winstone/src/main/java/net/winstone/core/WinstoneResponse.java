/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import net.winstone.WinstoneResourceBundle;
import net.winstone.log.Logger;
import net.winstone.log.LoggerFactory;
import net.winstone.util.StringUtils;
import winstone.HostConfiguration;
import winstone.Launcher;
import winstone.SimpleRequestDispatcher;
import winstone.WebAppConfiguration;
import winstone.WinstoneSession;

/**
 * Response for servlet
 * TODO optimize logging call and load of shared string
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneResponse.java,v 1.28 2005/04/19 07:33:41 rickknowles Exp $
 */
public class WinstoneResponse implements HttpServletResponse {

    private static final DateFormat HTTP_DF = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    private static final DateFormat VERSION0_DF = new SimpleDateFormat("EEE, dd-MMM-yy HH:mm:ss z", Locale.US);
    protected static Logger logger = LoggerFactory.getLogger(WinstoneResponse.class);

    static {
        HTTP_DF.setTimeZone(TimeZone.getTimeZone("GMT"));
        VERSION0_DF.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    public static final transient String X_POWERED_BY_HEADER_VALUE = WinstoneResourceBundle.getInstance().getString("PoweredByHeader");
    private int statusCode;
    private WinstoneRequest req;
    private WebAppConfiguration webAppConfig;
    private WinstoneOutputStream outputStream;
    private PrintWriter outputWriter;
    private List<String> headers;
    private String explicitEncoding;
    private String implicitEncoding;
    private List<Cookie> cookies;
    private Locale locale;
    private String protocol;
    private String reqKeepAliveHeader;
    private Integer errorStatusCode;

    /**
     * Constructor
     */
    public WinstoneResponse() {

        this.headers = new ArrayList<String>();
        this.cookies = new ArrayList<Cookie>();

        this.statusCode = SC_OK;
        this.locale = null; // Locale.getDefault();
        this.explicitEncoding = null;
        this.protocol = null;
        this.reqKeepAliveHeader = null;
    }

    /**
     * Resets the request to be reused
     */
    public void cleanUp() {
        this.req = null;
        this.webAppConfig = null;
        this.outputStream = null;
        this.outputWriter = null;
        this.headers.clear();
        this.cookies.clear();
        this.protocol = null;
        this.reqKeepAliveHeader = null;

        this.statusCode = SC_OK;
        this.errorStatusCode = null;
        this.locale = null; // Locale.getDefault();
        this.explicitEncoding = null;
        this.implicitEncoding = null;
    }

    private String getEncodingFromLocale(Locale local) {
        String localeString = local.getLanguage() + "_" + local.getCountry();
        Map<String, String> encMap = this.webAppConfig.getLocaleEncodingMap();
        logger.debug(StringUtils.replaceToken("Scanning for locale-encoding match: [#0] in [#1]", localeString, encMap + ""));

        String fullMatch = (String) encMap.get(localeString);
        if (fullMatch != null) {
            logger.debug(StringUtils.replaceToken("Found locale-encoding match: [#0]", fullMatch));
            return fullMatch;
        } else {
            localeString = local.getLanguage();
            logger.debug(StringUtils.replaceToken("Scanning for locale-encoding match: [#0] in [#1]", localeString, encMap + ""));
            String match = (String) encMap.get(localeString);
            if (match != null) {
                logger.debug(StringUtils.replaceToken("Found locale-encoding match: [#0]", match));
            }
            return match;
        }
    }

    public void setErrorStatusCode(int statusCode) {
        this.errorStatusCode = new Integer(statusCode);
        this.statusCode = statusCode;
    }

    public WinstoneOutputStream getWinstoneOutputStream() {
        return this.outputStream;
    }

    public void setOutputStream(WinstoneOutputStream outData) {
        this.outputStream = outData;
    }

    public void setWebAppConfig(WebAppConfiguration webAppConfig) {
        this.webAppConfig = webAppConfig;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void extractRequestKeepAliveHeader(WinstoneRequest req) {
        this.reqKeepAliveHeader = req.getHeader(WinstoneConstant.KEEP_ALIVE_HEADER);
    }

    public List<String> getHeaders() {
        return this.headers;
    }

    public List<Cookie> getCookies() {
        return this.cookies;
    }

    public WinstoneRequest getRequest() {
        return this.req;
    }

    public void setRequest(WinstoneRequest req) {
        this.req = req;
    }

    public void startIncludeBuffer() {
        this.outputStream.startIncludeBuffer();
    }

    public void finishIncludeBuffer() throws IOException {
        if (isIncluding()) {
            if (this.outputWriter != null) {
                this.outputWriter.flush();
            }
            this.outputStream.finishIncludeBuffer();
        }
    }

    public void clearIncludeStackForForward() throws IOException {
        this.outputStream.clearIncludeStackForForward();
    }

    protected static String getCharsetFromContentTypeHeader(String type, StringBuffer remainder) {
        if (type == null) {
            return null;
        }
        // Parse type to set encoding if needed
        StringTokenizer st = new StringTokenizer(type, ";");
        String localEncoding = null;
        while (st.hasMoreTokens()) {
            String clause = st.nextToken().trim();
            if (clause.startsWith("charset=")) {
                localEncoding = clause.substring(8);
            } else {
                if (remainder.length() > 0) {
                    remainder.append(";");
                }
                remainder.append(clause);
            }
        }
        if ((localEncoding == null) || !localEncoding.startsWith("\"") || !localEncoding.endsWith("\"")) {
            return localEncoding;
        } else {
            return localEncoding.substring(1, localEncoding.length() - 1);
        }
    }

    /**
     * This ensures the bare minimum correct http headers are present
     */
    public void validateHeaders() {
        // Need this block for WebDAV support. "Connection:close" header is ignored
        String lengthHeader = getHeader(WinstoneConstant.CONTENT_LENGTH_HEADER);
        if ((lengthHeader == null) && (this.statusCode >= 300)) {
            int bodyBytes = this.outputStream.getOutputStreamLength();
            if (getBufferSize() > bodyBytes) {
                logger.debug(StringUtils.replaceToken("Keep-alive requested but no content length set. Setting to [#0] bytes", "" + bodyBytes));
                forceHeader(WinstoneConstant.CONTENT_LENGTH_HEADER, "" + bodyBytes);
                lengthHeader = getHeader(WinstoneConstant.CONTENT_LENGTH_HEADER);
            }
        }

        forceHeader(WinstoneConstant.KEEP_ALIVE_HEADER, !closeAfterRequest() ? WinstoneConstant.KEEP_ALIVE_OPEN : WinstoneConstant.KEEP_ALIVE_CLOSE);
        String contentType = getHeader(WinstoneConstant.CONTENT_TYPE_HEADER);
        if (this.statusCode != SC_MOVED_TEMPORARILY) {
            if (contentType == null) {
                // Bypass normal encoding
                forceHeader(WinstoneConstant.CONTENT_TYPE_HEADER, "text/html;charset=" + getCharacterEncoding());
            } else if (contentType.startsWith("text/")) {
                // replace charset in content
                StringBuffer remainder = new StringBuffer();
                getCharsetFromContentTypeHeader(contentType, remainder);
                forceHeader(WinstoneConstant.CONTENT_TYPE_HEADER, remainder.toString() + ";charset=" + getCharacterEncoding());
            }
        }
        if (getHeader(WinstoneConstant.DATE_HEADER) == null) {
            forceHeader(WinstoneConstant.DATE_HEADER, formatHeaderDate(new Date()));
        }
        if (getHeader(WinstoneConstant.X_POWERED_BY_HEADER) == null) {
            forceHeader(WinstoneConstant.X_POWERED_BY_HEADER, X_POWERED_BY_HEADER_VALUE);
        }
        if (this.locale != null) {
            String lang = this.locale.getLanguage();
            if ((this.locale.getCountry() != null) && !this.locale.getCountry().equals("")) {
                lang = lang + "-" + this.locale.getCountry();
            }
            forceHeader(WinstoneConstant.CONTENT_LANGUAGE_HEADER, lang);
        }

        // If we don't have a webappConfig, exit here, cause we definitely don't
        // have a session
        if (req.getWebAppConfig() == null) {
            return;
        }
        // Write out the new session cookie if it's present
        HostConfiguration hostConfig = req.getHostGroup().getHostByName(req.getServerName());
        for (Iterator<String> i = req.getCurrentSessionIds().keySet().iterator(); i.hasNext();) {
            String prefix = i.next();
            String sessionId = req.getCurrentSessionIds().get(prefix);
            WebAppConfiguration ownerContext = hostConfig.getWebAppByURI(prefix);
            if (ownerContext != null) {
                WinstoneSession session = ownerContext.getSessionById(sessionId, true);
                if ((session != null) && session.isNew()) {
                    session.setIsNew(false);
                    Cookie cookie = new Cookie(WinstoneSession.SESSION_COOKIE_NAME, session.getId());
                    cookie.setMaxAge(-1);
                    cookie.setSecure(req.isSecure());
                    cookie.setVersion(0); // req.isSecure() ? 1 : 0);
                    cookie.setPath(req.getWebAppConfig().getContextPath().equals("") ? "/" : req.getWebAppConfig().getContextPath());
                    this.cookies.add(cookie); // don't call addCookie because we might be including
                }
            }
        }

        // Look for expired sessions: ie ones where the requested and current ids are different
        for (Iterator<String> i = req.getRequestedSessionIds().keySet().iterator(); i.hasNext();) {
            String prefix = i.next();
            String sessionId = req.getRequestedSessionIds().get(prefix);
            if (!req.getCurrentSessionIds().containsKey(prefix)) {
                Cookie cookie = new Cookie(WinstoneSession.SESSION_COOKIE_NAME, sessionId);
                cookie.setMaxAge(0); // explicitly expire this cookie
                cookie.setSecure(req.isSecure());
                cookie.setVersion(0); // req.isSecure() ? 1 : 0);
                cookie.setPath(prefix.equals("") ? "/" : prefix);
                this.cookies.add(cookie); // don't call addCookie because we might be including
            }
        }
        logger.debug(StringUtils.replaceToken("Headers prepared for writing: [#0]", "" + this.headers + ""));
    }

    /**
     * Writes out the http header for a single cookie
     */
    public String writeCookie(Cookie cookie) throws IOException {
        logger.debug(StringUtils.replaceToken("Writing cookie to output: [#0]", "" + cookie + ""));
        StringBuffer out = new StringBuffer();

        // Set-Cookie or Set-Cookie2
        if (cookie.getVersion() >= 1) {
            out.append(WinstoneConstant.OUT_COOKIE_HEADER1).append(": "); // TCK doesn't like set-cookie2
        } else {
            out.append(WinstoneConstant.OUT_COOKIE_HEADER1).append(": ");
        }

        // name/value pair
        if (cookie.getVersion() == 0) {
            out.append(cookie.getName()).append("=").append(cookie.getValue());
        } else {
            out.append(cookie.getName()).append("=");
            quote(cookie.getValue(), out);
        }

        if (cookie.getVersion() >= 1) {
            out.append("; Version=1");
            if (cookie.getDomain() != null) {
                out.append("; Domain=");
                quote(cookie.getDomain(), out);
            }
            if (cookie.getSecure()) {
                out.append("; Secure");
            }

            if (cookie.getMaxAge() >= 0) {
                out.append("; Max-Age=").append(cookie.getMaxAge());
            } else {
                out.append("; Discard");
            }
            if (cookie.getPath() != null) {
                out.append("; Path=");
                quote(cookie.getPath(), out);
            }
        } else {
            if (cookie.getDomain() != null) {
                out.append("; Domain=");
                out.append(cookie.getDomain());
            }
            if (cookie.getMaxAge() > 0) {
                long expiryMS = System.currentTimeMillis() + (1000 * (long) cookie.getMaxAge());
                String expiryDate = null;
                synchronized (VERSION0_DF) {
                    expiryDate = VERSION0_DF.format(new Date(expiryMS));
                }
                out.append("; Expires=").append(expiryDate);
            } else if (cookie.getMaxAge() == 0) {
                String expiryDate = null;
                synchronized (VERSION0_DF) {
                    expiryDate = VERSION0_DF.format(new Date(5000));
                }
                out.append("; Expires=").append(expiryDate);
            }
            if (cookie.getPath() != null) {
                out.append("; Path=").append(cookie.getPath());
            }
            if (cookie.getSecure()) {
                out.append("; Secure");
            }
        }
        return out.toString();
    }

    private static String formatHeaderDate(Date dateIn) {
        String date = null;
        synchronized (HTTP_DF) {
            date = HTTP_DF.format(dateIn);
        }
        return date;
    }

    /**
     * Quotes the necessary strings in a cookie header. The quoting is only applied if the string contains special characters.
     */
    protected static void quote(String value, StringBuffer out) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            out.append(value);
        } else {
            boolean containsSpecial = false;
            for (int n = 0; n < value.length(); n++) {
                char thisChar = value.charAt(n);
                if ((thisChar < 32) || (thisChar >= 127) || (specialCharacters.indexOf(thisChar) != -1)) {
                    containsSpecial = true;
                    break;
                }
            }
            if (containsSpecial) {
                out.append('"').append(value).append('"');
            } else {
                out.append(value);
            }
        }
    }
    private static final String specialCharacters = "()<>@,;:\\\"/[]?={} \t";

    /**
     * Based on request/response headers and the protocol, determine whether or not this connection should operate in keep-alive mode.
     */
    public boolean closeAfterRequest() {
        String inKeepAliveHeader = this.reqKeepAliveHeader;
        String outKeepAliveHeader = getHeader(WinstoneConstant.KEEP_ALIVE_HEADER);
        boolean hasContentLength = (getHeader(WinstoneConstant.CONTENT_LENGTH_HEADER) != null);
        if (this.protocol.startsWith("HTTP/0")) {
            return true;
        } else if ((inKeepAliveHeader == null) && (outKeepAliveHeader == null)) {
            return this.protocol.equals("HTTP/1.0") ? true : !hasContentLength;
        } else if (outKeepAliveHeader != null) {
            return outKeepAliveHeader.equalsIgnoreCase(WinstoneConstant.KEEP_ALIVE_CLOSE) || !hasContentLength;
        } else if (inKeepAliveHeader != null) {
            return inKeepAliveHeader.equalsIgnoreCase(WinstoneConstant.KEEP_ALIVE_CLOSE) || !hasContentLength;
        } else {
            return false;
        }
    }

    // ServletResponse interface methods
    @Override
    public void flushBuffer() throws IOException {
        if (this.outputWriter != null) {
            this.outputWriter.flush();
        }
        this.outputStream.flush();
    }

    @Override
    public void setBufferSize(int size) {
        this.outputStream.setBufferSize(size);
    }

    @Override
    public int getBufferSize() {
        return this.outputStream.getBufferSize();
    }

    @Override
    public String getCharacterEncoding() {
        String enc = getCurrentEncoding();
        return (enc == null ? "ISO-8859-1" : enc);
    }

    @Override
    public void setCharacterEncoding(String encoding) {
        if ((this.outputWriter == null) && !isCommitted()) {
            logger.debug(StringUtils.replaceToken("Setting response character encoding to [#0]", encoding));
            this.explicitEncoding = encoding;
            correctContentTypeHeaderEncoding(encoding);
        }
    }

    private void correctContentTypeHeaderEncoding(String encoding) {
        String contentType = getContentType();
        if (contentType != null) {
            StringBuffer remainderHeader = new StringBuffer();
            getCharsetFromContentTypeHeader(contentType, remainderHeader);
            if (remainderHeader.length() != 0) {
                forceHeader(WinstoneConstant.CONTENT_TYPE_HEADER, remainderHeader + ";charset=" + encoding);
            }
        }
    }

    @Override
    public String getContentType() {
        return getHeader(WinstoneConstant.CONTENT_TYPE_HEADER);
    }

    @Override
    public void setContentType(String type) {
        setHeader(WinstoneConstant.CONTENT_TYPE_HEADER, type);
    }

    @Override
    public Locale getLocale() {
        return this.locale == null ? Locale.getDefault() : this.locale;
    }

    private boolean isIncluding() {
        return this.outputStream.isIncluding();
    }

    @Override
    public void setLocale(Locale loc) {
        if (isIncluding()) {
            return;
        } else if (isCommitted()) {
            logger.warn("Response.setLocale() ignored, because getWriter already called");
        } else {
            if ((this.outputWriter == null) && (this.explicitEncoding == null)) {
                String localeEncoding = getEncodingFromLocale(loc);
                if (localeEncoding != null) {
                    this.implicitEncoding = localeEncoding;
                    correctContentTypeHeaderEncoding(localeEncoding);
                }
            }
            this.locale = loc;
        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        logger.debug("Called ServletResponse.getOutputStream()");
        return this.outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        logger.debug("Called ServletResponse.getWriter()");
        if (this.outputWriter != null) {
            return this.outputWriter;
        } else {
            this.outputWriter = new WinstoneResponseWriter(this.outputStream, this);
            return this.outputWriter;
        }
    }

    @Override
    public boolean isCommitted() {
        return this.outputStream.isCommitted();
    }

    @Override
    public void reset() {
        if (!isIncluding()) {
            resetBuffer();
            this.statusCode = SC_OK;
            this.headers.clear();
            this.cookies.clear();
        }
    }

    @Override
    public void resetBuffer() {
        if (!isIncluding()) {
            if (isCommitted()) {
                throw new IllegalStateException("Response cannot be reset - it is already committed");
            }

            // Disregard any output temporarily while we flush
            this.outputStream.setDisregardMode(true);

            if (this.outputWriter != null) {
                this.outputWriter.flush();
            }

            this.outputStream.setDisregardMode(false);
            this.outputStream.reset();
        }
    }

    @Override
    public void setContentLength(int len) {
        setIntHeader(WinstoneConstant.CONTENT_LENGTH_HEADER, len);
    }

    // HttpServletResponse interface methods
    @Override
    public void addCookie(Cookie cookie) {
        if (!isIncluding()) {
            this.cookies.add(cookie);
        }
    }

    @Override
    public boolean containsHeader(String name) {
        for (int n = 0; n < this.headers.size(); n++) {
            if (((String) this.headers.get(n)).startsWith(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addDateHeader(String name, long date) {
        addHeader(name, formatHeaderDate(new Date(date)));
    } // df.format(new Date(date)));}

    @Override
    public void addIntHeader(String name, int value) {
        addHeader(name, "" + value);
    }

    @Override
    public void addHeader(String name, String value) {
        if (isIncluding()) {
            logger.debug(StringUtils.replaceToken("Header ignored inside include - [#0]: [#1] ", name, value));
        } else if (isCommitted()) {
            logger.debug(StringUtils.replaceToken("Header ignored after response committed - [#0]: [#1] ", name, value));
        } else if (value != null) {
            if (name.equals(WinstoneConstant.CONTENT_TYPE_HEADER)) {
                StringBuffer remainderHeader = new StringBuffer();
                String headerEncoding = getCharsetFromContentTypeHeader(value, remainderHeader);
                if (this.outputWriter != null) {
                    value = remainderHeader + ";charset=" + getCharacterEncoding();
                } else if (headerEncoding != null) {
                    this.explicitEncoding = headerEncoding;
                }
            }
            this.headers.add(name + ": " + value);
        }
    }

    @Override
    public void setDateHeader(String name, long date) {
        setHeader(name, formatHeaderDate(new Date(date)));
    }

    @Override
    public void setIntHeader(String name, int value) {
        setHeader(name, "" + value);
    }

    @Override
    public void setHeader(String name, String value) {
        if (isIncluding()) {
            logger.debug(StringUtils.replaceToken("Header ignored inside include - [#0]: [#1] ", name, value));
        } else if (isCommitted()) {
            logger.debug(StringUtils.replaceToken("Header ignored after response committed - [#0]: [#1] ", name, value));
        } else {
            boolean found = false;
            for (int n = 0; (n < this.headers.size()); n++) {
                String header = (String) this.headers.get(n);
                if (header.startsWith(name + ": ")) {
                    if (found) {
                        this.headers.remove(n);
                        continue;
                    }
                    if (name.equals(WinstoneConstant.CONTENT_TYPE_HEADER)) {
                        if (value != null) {
                            StringBuffer remainderHeader = new StringBuffer();
                            String headerEncoding = getCharsetFromContentTypeHeader(value, remainderHeader);
                            if (this.outputWriter != null) {
                                value = remainderHeader + ";charset=" + getCharacterEncoding();
                            } else if (headerEncoding != null) {
                                this.explicitEncoding = headerEncoding;
                            }
                        }
                    }

                    if (value != null) {
                        this.headers.set(n, name + ": " + value);
                    } else {
                        this.headers.remove(n);
                    }
                    found = true;
                }
            }
            if (!found) {
                addHeader(name, value);
            }
        }
    }

    private void forceHeader(String name, String value) {
        boolean found = false;
        for (int n = 0; (n < this.headers.size()); n++) {
            String header = (String) this.headers.get(n);
            if (header.startsWith(name + ": ")) {
                found = true;
                this.headers.set(n, name + ": " + value);
            }
        }
        if (!found) {
            this.headers.add(name + ": " + value);
        }
    }

    private String getCurrentEncoding() {
        if (this.explicitEncoding != null) {
            return this.explicitEncoding;
        } else if (this.implicitEncoding != null) {
            return this.implicitEncoding;
        } else if ((this.req != null) && (this.req.getCharacterEncoding() != null)) {
            try {
                "0".getBytes(this.req.getCharacterEncoding());
                return this.req.getCharacterEncoding();
            } catch (UnsupportedEncodingException err) {
                return null;
            }
        } else {
            return null;
        }
    }

    public String getHeader(String name) {
        for (int n = 0; n < this.headers.size(); n++) {
            String header = (String) this.headers.get(n);
            if (header.startsWith(name + ": ")) {
                return header.substring(name.length() + 2);
            }
        }
        return null;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return url;
    }

    @Override
    public String encodeURL(String url) {
        return url;
    }

    public int getStatus() {
        return this.statusCode;
    }

    public Integer getErrorStatusCode() {
        return this.errorStatusCode;
    }

    @Override
    public void setStatus(int sc) {
        if (!isIncluding() && (this.errorStatusCode == null)) {
            // if (!isIncluding()) {
            this.statusCode = sc;
            // if (this.errorStatusCode != null) {
            // this.errorStatusCode = new Integer(sc);
            // }
        }
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        if (isIncluding()) {
            logger.error("Ignoring redirect in include: " + location);
            return;
        } else if (isCommitted()) {
            throw new IllegalStateException("OutputStream already committed");
        }
        resetBuffer();

        // Build location
        StringBuilder fullLocation = new StringBuilder();
        if (location.startsWith("http://") || location.startsWith("https://")) {
            fullLocation.append(location);
        } else {
            if (location.trim().equals(".")) {
                location = "";
            }

            fullLocation.append(this.req.getScheme()).append("://");
            fullLocation.append(this.req.getServerName());
            if (!((this.req.getServerPort() == 80) && this.req.getScheme().equals("http")) && !((this.req.getServerPort() == 443) && this.req.getScheme().equals("https"))) {
                fullLocation.append(':').append(this.req.getServerPort());
            }
            if (location.startsWith("/")) {
                fullLocation.append(location);
            } else {
                fullLocation.append(this.req.getRequestURI());
                int questionPos = fullLocation.toString().indexOf("?");
                if (questionPos != -1) {
                    fullLocation.delete(questionPos, fullLocation.length());
                }
                fullLocation.delete(fullLocation.toString().lastIndexOf("/") + 1, fullLocation.length());
                fullLocation.append(location);
            }
        }
        if (this.req != null) {
            this.req.discardRequestBody();
        }
        this.statusCode = HttpServletResponse.SC_MOVED_TEMPORARILY;
        setHeader(WinstoneConstant.LOCATION_HEADER, fullLocation.toString());
        setContentLength(0);
        getWriter().flush();
    }

    @Override
    public void sendError(int sc) throws IOException {
        sendError(sc, null);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        if (isIncluding()) {
            logger.error(StringUtils.replaceToken("Error in include: [#0] [#1]", "" + sc, msg));
            return;
        }
        logger.debug(StringUtils.replaceToken("Sending error message to browser: code [#0], message: [#1]", "" + sc, msg));

        if ((this.webAppConfig != null) && (this.req != null)) {

            SimpleRequestDispatcher rd = this.webAppConfig.getErrorDispatcherByCode(sc, msg, null);
            if (rd != null) {
                try {
                    rd.forward(this.req, this);
                    return;
                } catch (IllegalStateException err) {
                    throw err;
                } catch (IOException err) {
                    throw err;
                } catch (Throwable err) {
                    logger.warn(StringUtils.replaceToken("Sending error message to browser: code [#0], message: [#1]", rd.getName(), sc + ""), err);
                    return;
                }
            }
        }
        // If we are here there was no webapp and/or no request object, so
        // show the default error page
        if (this.errorStatusCode == null) {
            this.statusCode = sc;
        }
        String output = StringUtils.replaceToken(WinstoneResourceBundle.getInstance().getString("WinstoneResponse.ErrorPage",
                sc + "", (msg == null ? "" : msg), "", WinstoneResourceBundle.getInstance().getString("ServerVersion"), "" + new Date()));
        setContentLength(output.getBytes(getCharacterEncoding()).length);
        Writer out = getWriter();
        out.write(output);
        out.flush();
    }

    /**
     * @deprecated
     */
    @Override
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
    }

    /**
     * @deprecated
     */
    @Override
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    /**
     * @deprecated
     */
    @Override
    public void setStatus(int sc, String sm) {
        setStatus(sc);
    }
}
