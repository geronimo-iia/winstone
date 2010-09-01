/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.winstone.core.authentication.AuthenticationPrincipal;

import net.winstone.WinstoneException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the request interface required by the servlet spec.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneRequest.java,v 1.40 2008/10/01 14:46:13 rickknowles Exp $
 */
public class WinstoneRequest implements HttpServletRequest {

    protected static Logger logger = LoggerFactory.getLogger(WinstoneRequest.class);
    protected static final DateFormat headerDF = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    protected static final Random rnd;

    static {
        headerDF.setTimeZone(TimeZone.getTimeZone("GMT"));
        rnd = new Random(System.currentTimeMillis());
    }
    protected Map<String, Object> attributes;
    protected Map<String, String[]> parameters;
    protected Stack<Map<String, Object>> attributesStack;
    protected Stack<Map<String, String[]>> parametersStack;
    // protected Map forwardedParameters;
    protected String headers[];
    protected Cookie cookies[];
    protected String method;
    protected String scheme;
    protected String serverName;
    protected String requestURI;
    protected String servletPath;
    protected String pathInfo;
    protected String queryString;
    protected String protocol;
    protected int contentLength;
    protected String contentType;
    protected String encoding;
    protected int serverPort;
    protected String remoteIP;
    protected String remoteName;
    protected int remotePort;
    protected String localAddr;
    protected String localName;
    protected int localPort;
    protected Boolean parsedParameters;
    protected Map<String, String> requestedSessionIds;
    protected Map<String, String> currentSessionIds;
    protected String deadRequestedSessionId;
    protected List<Locale> locales;
    protected String authorization;
    protected boolean isSecure;
    protected WinstoneInputStream inputData;
    protected BufferedReader inputReader;
    protected ServletConfiguration servletConfig;
    protected WebAppConfiguration webappConfig;
    protected HostGroup hostGroup;
    protected AuthenticationPrincipal authenticatedUser;
    protected ServletRequestAttributeListener requestAttributeListeners[];
    protected ServletRequestListener requestListeners[];
    private MessageDigest md5Digester;
    private Set<WinstoneSession> usedSessions;

    /**
     * InputStream factory method.
     */
    public WinstoneRequest() throws IOException {
        this.attributes = new HashMap<String, Object>();
        this.parameters = new HashMap<String, String[]>();
        this.locales = new ArrayList<Locale>();
        this.attributesStack = new Stack<Map<String, Object>>();
        this.parametersStack = new Stack<Map<String, String[]>>();
        // this.forwardedParameters = new Hashtable();
        this.requestedSessionIds = new HashMap<String, String>();
        this.currentSessionIds = new HashMap<String, String>();
        this.usedSessions = new HashSet<WinstoneSession>();
        this.contentLength = -1;
        this.isSecure = false;
        try {
            this.md5Digester = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException err) {
            throw new WinstoneException("MD5 digester unavailable - what the ...?");
        }
    }

    /**
     * Resets the request to be reused
     */
    public void cleanUp() {
        this.requestListeners = null;
        this.requestAttributeListeners = null;
        this.attributes.clear();
        this.parameters.clear();
        this.attributesStack.clear();
        this.parametersStack.clear();
        // this.forwardedParameters.clear();
        this.usedSessions.clear();
        this.headers = null;
        this.cookies = null;
        this.method = null;
        this.scheme = null;
        this.serverName = null;
        this.requestURI = null;
        this.servletPath = null;
        this.pathInfo = null;
        this.queryString = null;
        this.protocol = null;
        this.contentLength = -1;
        this.contentType = null;
        this.encoding = null;
        this.inputData = null;
        this.inputReader = null;
        this.servletConfig = null;
        this.webappConfig = null;
        this.hostGroup = null;
        this.serverPort = -1;
        this.remoteIP = null;
        this.remoteName = null;
        this.remotePort = -1;
        this.localAddr = null;
        this.localName = null;
        this.localPort = -1;
        this.parsedParameters = null;
        this.requestedSessionIds.clear();
        this.currentSessionIds.clear();
        this.deadRequestedSessionId = null;
        this.locales.clear();
        this.authorization = null;
        this.isSecure = false;
        this.authenticatedUser = null;
    }

    /**
     * Steps through the header array, searching for the first header matching
     */
    private String extractFirstHeader(final String name) {
        for (int n = 0; n < this.headers.length; n++) {
            if (this.headers[n].toUpperCase().startsWith(name.toUpperCase() + ':')) {
                return this.headers[n].substring(name.length() + 1).trim(); // 1 for colon
            }
        }
        return null;
    }

    private Collection<String> extractHeaderNameList() {
        Collection<String> headerNames = new HashSet<String>();
        for (int n = 0; n < this.headers.length; n++) {
            String name = this.headers[n];
            int colonPos = name.indexOf(':');
            headerNames.add(name.substring(0, colonPos));
        }
        return headerNames;
    }

    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    public Map<String, String[]> getParameters() {
        return this.parameters;
    }

    //
    // public Map getForwardedParameters() {
    // return this.forwardedParameters;
    // }
    public Stack<Map<String, Object>> getAttributesStack() {
        return this.attributesStack;
    }

    public Stack<Map<String, String[]>> getParametersStack() {
        return this.parametersStack;
    }

    public Map<String, String> getCurrentSessionIds() {
        return this.currentSessionIds;
    }

    public Map<String, String> getRequestedSessionIds() {
        return this.requestedSessionIds;
    }

    public String getDeadRequestedSessionId() {
        return this.deadRequestedSessionId;
    }

    public HostGroup getHostGroup() {
        return this.hostGroup;
    }

    public WebAppConfiguration getWebAppConfig() {
        return this.webappConfig;
    }

    public ServletConfiguration getServletConfig() {
        return this.servletConfig;
    }

    public String getEncoding() {
        return this.encoding;
    }

    public Boolean getParsedParameters() {
        return this.parsedParameters;
    }

    public List<Locale> getListLocales() {
        return this.locales;
    }

    public void setInputStream(WinstoneInputStream inputData) {
        this.inputData = inputData;
    }

    public void setHostGroup(HostGroup hostGroup) {
        this.hostGroup = hostGroup;
    }

    public void setWebAppConfig(WebAppConfiguration webappConfig) {
        this.webappConfig = webappConfig;
    }

    public void setServletConfig(ServletConfiguration servletConfig) {
        this.servletConfig = servletConfig;
    }

    public void setServerPort(int port) {
        this.serverPort = port;
    }

    public void setRemoteIP(String remoteIP) {
        this.remoteIP = remoteIP;
    }

    public void setRemoteName(String name) {
        this.remoteName = name;
    }

    public void setRemotePort(int port) {
        this.remotePort = port;
    }

    public void setLocalAddr(String ip) {
        this.localName = ip;
    }

    public void setLocalName(String name) {
        this.localName = name;
    }

    public void setLocalPort(int port) {
        this.localPort = port;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setIsSecure(boolean isSecure) {
        this.isSecure = isSecure;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public void setServerName(String name) {
        this.serverName = name;
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }

    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }

    public void setProtocol(String protocolString) {
        this.protocol = protocolString;
    }

    public void setRemoteUser(AuthenticationPrincipal user) {
        this.authenticatedUser = user;
    }

    public void setContentLength(int len) {
        this.contentLength = len;
    }

    public void setContentType(String type) {
        this.contentType = type;
    }

    public void setAuthorization(String auth) {
        this.authorization = auth;
    }

    public void setLocales(List<Locale> locales) {
        this.locales = locales;
    }

    public void setCurrentSessionIds(Map<String, String> currentSessionIds) {
        this.currentSessionIds = currentSessionIds;
    }

    public void setRequestedSessionIds(Map<String, String> requestedSessionIds) {
        this.requestedSessionIds = requestedSessionIds;
    }

    public void setDeadRequestedSessionId(String deadRequestedSessionId) {
        this.deadRequestedSessionId = deadRequestedSessionId;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setParsedParameters(Boolean parsed) {
        this.parsedParameters = parsed;
    }

    public void setRequestListeners(ServletRequestListener rl[]) {
        this.requestListeners = rl;
    }

    public void setRequestAttributeListeners(ServletRequestAttributeListener ral[]) {
        this.requestAttributeListeners = ral;
    }

    /**
     * Gets parameters from the url encoded parameter string
     */
    public static void extractParameters(String urlEncodedParams, String encoding, Map<String, String[]> outputParams, boolean overwrite) {
        logger.debug("Parsing parameters: {} (using encoding {})", urlEncodedParams, encoding);
        StringTokenizer st = new StringTokenizer(urlEncodedParams, "&", false);
        Set<String> overwrittenParamNames = null;
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int equalPos = token.indexOf('=');
            try {
                String decodedNameDefault = decodeURLToken(equalPos == -1 ? token : token.substring(0, equalPos));
                String decodedValueDefault = (equalPos == -1 ? "" : decodeURLToken(token.substring(equalPos + 1)));
                String decodedName = (encoding == null ? decodedNameDefault : new String(decodedNameDefault.getBytes("8859_1"), encoding));
                String decodedValue = (encoding == null ? decodedValueDefault : new String(decodedValueDefault.getBytes("8859_1"), encoding));

                String[] already = null;
                if (overwrite) {
                    if (overwrittenParamNames == null) {
                        overwrittenParamNames = new HashSet<String>();
                    }
                    if (!overwrittenParamNames.contains(decodedName)) {
                        overwrittenParamNames.add(decodedName);
                        outputParams.remove(decodedName);
                    }
                }
                already = outputParams.get(decodedName);
                if (already == null) {
                    outputParams.put(decodedName, new String[]{
                                decodedValue
                            });
                } else {
                    String alreadyArray[] = (String[]) already;
                    String oneMore[] = new String[alreadyArray.length + 1];
                    System.arraycopy(alreadyArray, 0, oneMore, 0, alreadyArray.length);
                    oneMore[oneMore.length - 1] = decodedValue;
                    outputParams.put(decodedName, oneMore);
                }
            } catch (UnsupportedEncodingException err) {
                logger.error("Error parsing request parameters", err);
            }
        }
    }

    /**
     * For decoding the URL encoding used on query strings
     */
    public static String decodeURLToken(String in) {
        int len = in.length();
        StringBuilder workspace = new StringBuilder(len);
        for (int n = 0; n < len; n++) {
            char thisChar = in.charAt(n);
            if (thisChar == '+') {
                workspace.append(' ');
            } else if (thisChar == '%') {
                String token = "";
                int inc = 2, beg = 1, end = 3;
                if ((n + 1 < len) && (in.charAt(n + 1) == 'u')) {
                    beg = 2;
                    end = 6;
                    inc = 5;
                }
                token = in.substring(Math.min(n + beg, len), Math.min(n + end, len));
                try {
                    workspace.append((char) (Integer.parseInt(token, 16)));
                    n += inc;
                } catch (RuntimeException err) {
                    logger.warn("Found an invalid character %{} in url parameter. Echoing through in escaped form", token);
                    workspace.append(thisChar);
                }
            } else {
                workspace.append(thisChar);
            }
        }
        return workspace.toString();
    }

    public void discardRequestBody() {
        if (getContentLength() > 0) {
            try {
                logger.debug("Forcing request body parse");
                // If body not parsed
                if ((this.parsedParameters == null) || (this.parsedParameters.equals(Boolean.FALSE))) {
                    // read full stream length
                    try {
                        InputStream in = getInputStream();
                        byte buffer[] = new byte[2048];
                        while (in.read(buffer) != -1) {
                        }
                    } catch (IllegalStateException err) {
                        Reader in = getReader();
                        char buffer[] = new char[2048];
                        while (in.read(buffer) != -1) {
                        }
                    }
                }
            } catch (IOException err) {
                logger.error("Forcing request body parse", err);
            }
        }
    }

    /**
     * This takes the parameters in the body of the request and puts them into the parameters map.
     */
    public void parseRequestParameters() {
        if ((parsedParameters != null) && !parsedParameters.booleanValue()) {
            logger.warn("Called getInputStream after getParameter ... error");
            this.parsedParameters = Boolean.TRUE;
        } else if (parsedParameters == null) {
            Map<String, String[]> workingParameters = new HashMap<String, String[]>();
            try {
                // Parse query string from request
                // if ((method.equals(METHOD_GET) || method.equals(METHOD_HEAD) ||
                // method.equals(METHOD_POST)) &&
                if (this.queryString != null) {
                    extractParameters(this.queryString, this.encoding, workingParameters, false);
                    logger.debug("Param line: " + workingParameters);
                }

                if (method.equals(WinstoneConstant.METHOD_POST) && (contentType != null) && (contentType.equals(WinstoneConstant.POST_PARAMETERS) || contentType.startsWith(WinstoneConstant.POST_PARAMETERS + ";"))) {
                    logger.debug("Parsing request body for parameters");

                    // Parse params
                    byte paramBuffer[] = new byte[contentLength];
                    int readCount = this.inputData.read(paramBuffer);
                    if (readCount != contentLength) {
                        logger.warn("Content-length said {}, actual length was {}", Integer.toString(contentLength), Integer.toString(readCount));
                    }
                    String paramLine = (this.encoding == null ? new String(paramBuffer) : new String(paramBuffer, this.encoding));
                    extractParameters(paramLine.trim(), this.encoding, workingParameters, false);
                    logger.debug("Param line: " + workingParameters.toString());
                }

                this.parameters.putAll(workingParameters);
                this.parsedParameters = Boolean.TRUE;
            } catch (Throwable err) {
                logger.error("Error parsing body of the reques", err);
                this.parsedParameters = null;
            }
        }
    }

    /**
     * Go through the list of headers, and build the headers/cookies arrays for the request object.
     */
    public void parseHeaders(List<String> headerList) {
        // Iterate through headers
        List<String> outHeaderList = new ArrayList<String>();
        List<Cookie> cookieList = new ArrayList<Cookie>();
        for (Iterator<String> i = headerList.iterator(); i.hasNext();) {
            String header = (String) i.next();
            int colonPos = header.indexOf(':');
            String name = header.substring(0, colonPos);
            String value = header.substring(colonPos + 1).trim();

            // Add it to out headers if it's not a cookie
            outHeaderList.add(header);
            // if (!name.equalsIgnoreCase(IN_COOKIE_HEADER1)
            // && !name.equalsIgnoreCase(IN_COOKIE_HEADER2))

            if (name.equalsIgnoreCase(WinstoneConstant.AUTHORIZATION_HEADER)) {
                this.authorization = value;
            } else if (name.equalsIgnoreCase(WinstoneConstant.LOCALE_HEADER)) {
                this.locales = parseLocales(value);
            } else if (name.equalsIgnoreCase(WinstoneConstant.CONTENT_LENGTH_HEADER)) {
                this.contentLength = Integer.parseInt(value);
            } else if (name.equalsIgnoreCase(WinstoneConstant.HOST_HEADER)) {
                int nextColonPos = value.indexOf(':');
                if ((nextColonPos == -1) || (nextColonPos == value.length() - 1)) {
                    this.serverName = value;
                    if (this.scheme != null) {
                        if (this.scheme.equals("http")) {
                            this.serverPort = 80;
                        } else if (this.scheme.equals("https")) {
                            this.serverPort = 443;
                        }
                    }
                } else {
                    this.serverName = value.substring(0, nextColonPos);
                    this.serverPort = Integer.parseInt(value.substring(nextColonPos + 1));
                }
            } else if (name.equalsIgnoreCase(WinstoneConstant.CONTENT_TYPE_HEADER)) {
                this.contentType = value;
                int semicolon = value.lastIndexOf(';');
                if (semicolon != -1) {
                    String encodingClause = value.substring(semicolon + 1).trim();
                    if (encodingClause.startsWith("charset=")) {
                        this.encoding = encodingClause.substring(8);
                    }
                }
            } else if (name.equalsIgnoreCase(WinstoneConstant.IN_COOKIE_HEADER1) || name.equalsIgnoreCase(WinstoneConstant.IN_COOKIE_HEADER2)) {
                parseCookieLine(value, cookieList);
            }
        }
        this.headers = (String[]) outHeaderList.toArray(new String[0]);
        if (cookieList.isEmpty()) {
            this.cookies = null;
        } else {
            this.cookies = (Cookie[]) cookieList.toArray(new Cookie[0]);
        }
    }

    private static String nextToken(StringTokenizer st) {
        if (st.hasMoreTokens()) {
            return st.nextToken();
        } else {
            return null;
        }
    }

    private void parseCookieLine(String headerValue, List<Cookie> cookieList) {
        StringTokenizer st = new StringTokenizer(headerValue, ";", false);
        int version = 0;
        String cookieLine = nextToken(st);

        // check cookie version flag
        if ((cookieLine != null) && cookieLine.startsWith("$Version=")) {
            int equalPos = cookieLine.indexOf('=');
            try {
                version = Integer.parseInt(extractFromQuotes(cookieLine.substring(equalPos + 1).trim()));
            } catch (NumberFormatException err) {
                version = 0;
            }
            cookieLine = nextToken(st);
        }

        // process remainder - parameters
        while (cookieLine != null) {
            cookieLine = cookieLine.trim();
            int equalPos = cookieLine.indexOf('=');
            if (equalPos == -1) {
                // next token
                cookieLine = nextToken(st);
            } else {
                String name = cookieLine.substring(0, equalPos);
                String value = extractFromQuotes(cookieLine.substring(equalPos + 1));
                Cookie thisCookie = new Cookie(name, value);
                thisCookie.setVersion(version);
                thisCookie.setSecure(isSecure());
                cookieList.add(thisCookie);

                // check for path / domain / port
                cookieLine = nextToken(st);
                while ((cookieLine != null) && cookieLine.trim().startsWith("$")) {
                    cookieLine = cookieLine.trim();
                    equalPos = cookieLine.indexOf('=');
                    String attrValue = equalPos == -1 ? "" : cookieLine.substring(equalPos + 1).trim();
                    if (cookieLine.startsWith("$Path")) {
                        thisCookie.setPath(extractFromQuotes(attrValue));
                    } else if (cookieLine.startsWith("$Domain")) {
                        thisCookie.setDomain(extractFromQuotes(attrValue));
                    }
                    cookieLine = nextToken(st);
                }
                logger.debug("Found cookie: " + thisCookie.toString());
                if (thisCookie.getName().equals(WinstoneSession.SESSION_COOKIE_NAME)) {
                    // Find a context that manages this key
                    HostConfiguration hostConfig = this.hostGroup.getHostByName(this.serverName);
                    WebAppConfiguration ownerContext = hostConfig.getWebAppBySessionKey(thisCookie.getValue());
                    if (ownerContext != null) {
                        this.requestedSessionIds.put(ownerContext.getContextPath(), thisCookie.getValue());
                        this.currentSessionIds.put(ownerContext.getContextPath(), thisCookie.getValue());
                    } // If not found, it was probably dead
                    else {
                        this.deadRequestedSessionId = thisCookie.getValue();
                    }
                    // this.requestedSessionId = thisCookie.getValue();
                    // this.currentSessionId = thisCookie.getValue();
                    logger.debug("Found session cookie: {} {}", thisCookie.getValue(), ownerContext == null ? "" : "prefix:" + ownerContext.getContextPath());
                }
            }
        }
    }

    private static String extractFromQuotes(String input) {
        if ((input != null) && input.startsWith("\"") && input.endsWith("\"")) {
            return input.substring(1, input.length() - 1);
        } else {
            return input;
        }
    }

    private List<Locale> parseLocales(String header) {
        // Strip out the whitespace
        StringBuilder lb = new StringBuilder();
        for (int n = 0; n < header.length(); n++) {
            char c = header.charAt(n);
            if (!Character.isWhitespace(c)) {
                lb.append(c);
            }
        }

        // Tokenize by commas
        Map<Float, List<Locale>> localeEntries = new HashMap<Float, List<Locale>>();
        StringTokenizer commaTK = new StringTokenizer(lb.toString(), ",", false);
        for (; commaTK.hasMoreTokens();) {
            String clause = commaTK.nextToken();

            // Tokenize by semicolon
            Float quality = new Float(1);
            if (clause.indexOf(";q=") != -1) {
                int pos = clause.indexOf(";q=");
                try {
                    quality = new Float(clause.substring(pos + 3));
                } catch (NumberFormatException err) {
                    quality = new Float(0);
                }
                clause = clause.substring(0, pos);
            }

            // Build the locale
            String language = "";
            String country = "";
            String variant = "";
            int dpos = clause.indexOf('-');
            if (dpos == -1) {
                language = clause;
            } else {
                language = clause.substring(0, dpos);
                String remainder = clause.substring(dpos + 1);
                int d2pos = remainder.indexOf('-');
                if (d2pos == -1) {
                    country = remainder;
                } else {
                    country = remainder.substring(0, d2pos);
                    variant = remainder.substring(d2pos + 1);
                }
            }
            Locale loc = new Locale(language, country, variant);

            // Put into list by quality
            List<Locale> localeList = (List<Locale>) localeEntries.get(quality);
            if (localeList == null) {
                localeList = new ArrayList<Locale>();
                localeEntries.put(quality, localeList);
            }
            localeList.add(loc);
        }

        // Extract and build the list
        Float orderKeys[] = (Float[]) localeEntries.keySet().toArray(new Float[0]);
        Arrays.sort(orderKeys);
        List<Locale> outputLocaleList = new ArrayList<Locale>();
        for (int n = 0; n < orderKeys.length; n++) {
            // Skip backwards through the list of maps and add to the output list
            int reversedIndex = (orderKeys.length - 1) - n;
            if ((orderKeys[reversedIndex].floatValue() <= 0) || (orderKeys[reversedIndex].floatValue() > 1)) {
                continue;
            }
            List<Locale> localeList = (List<Locale>) localeEntries.get(orderKeys[reversedIndex]);
            for (Iterator<Locale> i = localeList.iterator(); i.hasNext();) {
                outputLocaleList.add(i.next());
            }
        }

        return outputLocaleList;
    }

    public void addIncludeQueryParameters(String queryString) {
        Map<String, String[]> lastParams = new HashMap<String, String[]>();
        if (!this.parametersStack.isEmpty()) {
            lastParams.putAll((Map<String, String[]>) this.parametersStack.peek());
        }
        Map<String, String[]> newQueryParams = new HashMap<String, String[]>();
        if (queryString != null) {
            extractParameters(queryString, this.encoding, newQueryParams, false);
        }
        lastParams.putAll(newQueryParams);
        this.parametersStack.push(lastParams);
    }

    public void addIncludeAttributes(String requestURI, String contextPath, String servletPath, String pathInfo, String queryString) {
        Map<String, Object> includeAttributes = new HashMap<String, Object>();
        if (requestURI != null) {
            includeAttributes.put(WinstoneConstant.INCLUDE_REQUEST_URI, requestURI);
        }
        if (contextPath != null) {
            includeAttributes.put(WinstoneConstant.INCLUDE_CONTEXT_PATH, contextPath);
        }
        if (servletPath != null) {
            includeAttributes.put(WinstoneConstant.INCLUDE_SERVLET_PATH, servletPath);
        }
        if (pathInfo != null) {
            includeAttributes.put(WinstoneConstant.INCLUDE_PATH_INFO, pathInfo);
        }
        if (queryString != null) {
            includeAttributes.put(WinstoneConstant.INCLUDE_QUERY_STRING, queryString);
        }
        this.attributesStack.push(includeAttributes);
    }

    public void removeIncludeQueryString() {
        if (!this.parametersStack.isEmpty()) {
            this.parametersStack.pop();
        }
    }

    public void clearIncludeStackForForward() {
        this.parametersStack.clear();
        this.attributesStack.clear();
    }

    public void setForwardQueryString(String forwardQueryString) {
        // this.forwardedParameters.clear();

        // Parse query string from include / forward
        if (forwardQueryString != null) {
            String oldQueryString = this.queryString == null ? "" : this.queryString;
            boolean needJoiner = !forwardQueryString.equals("") && !oldQueryString.equals("");
            this.queryString = forwardQueryString + (needJoiner ? "&" : "") + oldQueryString;

            if (this.parsedParameters != null) {
                logger.debug("Parsing parameters: {} (using encoding {})", forwardQueryString, this.encoding);
                extractParameters(forwardQueryString, this.encoding, this.parameters, true);
                logger.debug("Param line: {}", this.parameters != null ? this.parameters.toString() : "");
            }
        }

    }

    public void removeIncludeAttributes() {
        if (!this.attributesStack.isEmpty()) {
            this.attributesStack.pop();
        }
    }

    // Implementation methods for the servlet request stuff
    @Override
    public Object getAttribute(String name) {
        if (!this.attributesStack.isEmpty()) {
            Map<String, Object> includedAttributes = (Map<String, Object>) this.attributesStack.peek();
            Object value = includedAttributes.get(name);
            if (value != null) {
                return value;
            }
        }
        return this.attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        Map<String, Object> result = new HashMap<String, Object>(this.attributes);
        if (!this.attributesStack.isEmpty()) {
            Map<String, Object> includedAttributes = (Map<String, Object>) this.attributesStack.peek();
            result.putAll(includedAttributes);
        }
        return Collections.enumeration(result.keySet());
    }

    @Override
    public void removeAttribute(String name) {
        Object value = attributes.get(name);
        if (value == null) {
            return;
        }

        // fire event
        if (this.requestAttributeListeners != null) {
            for (int n = 0; n < this.requestAttributeListeners.length; n++) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(getWebAppConfig().getLoader());
                this.requestAttributeListeners[n].attributeRemoved(new ServletRequestAttributeEvent(this.webappConfig, this, name, value));
                Thread.currentThread().setContextClassLoader(cl);
            }
        }

        this.attributes.remove(name);
    }

    @Override
    public void setAttribute(String name, Object o) {
        if ((name != null) && (o != null)) {
            Object oldValue = attributes.get(name);
            attributes.put(name, o); // make sure it's set at the top level

            // fire event
            if (this.requestAttributeListeners != null) {
                if (oldValue == null) {
                    for (int n = 0; n < this.requestAttributeListeners.length; n++) {
                        ClassLoader cl = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(getWebAppConfig().getLoader());
                        this.requestAttributeListeners[n].attributeAdded(new ServletRequestAttributeEvent(this.webappConfig, this, name, o));
                        Thread.currentThread().setContextClassLoader(cl);
                    }
                } else {
                    for (int n = 0; n < this.requestAttributeListeners.length; n++) {
                        ClassLoader cl = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(getWebAppConfig().getLoader());
                        this.requestAttributeListeners[n].attributeReplaced(new ServletRequestAttributeEvent(this.webappConfig, this, name, oldValue));
                        Thread.currentThread().setContextClassLoader(cl);
                    }
                }
            }
        } else if (name != null) {
            removeAttribute(name);
        }
    }

    @Override
    public String getCharacterEncoding() {
        return this.encoding;
    }

    @Override
    public void setCharacterEncoding(String encoding) throws UnsupportedEncodingException {
        "blah".getBytes(encoding); // throws an exception if the encoding is unsupported
        if (this.inputReader == null) {
            logger.debug("Setting the request encoding from {} to {}", this.encoding, encoding);
            this.encoding = encoding;
        }
    }

    @Override
    public int getContentLength() {
        return this.contentLength;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public Locale getLocale() {
        return this.locales.isEmpty() ? Locale.getDefault() : (Locale) this.locales.get(0);
    }

    @Override
    public Enumeration<Locale> getLocales() {
        List<Locale> sendLocales = this.locales;
        if (sendLocales.isEmpty()) {
            sendLocales.add(Locale.getDefault());
        }
        return Collections.enumeration(sendLocales);
    }

    @Override
    public String getProtocol() {
        return this.protocol;
    }

    @Override
    public String getScheme() {
        return this.scheme;
    }

    @Override
    public boolean isSecure() {
        return this.isSecure;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (this.inputReader != null) {
            return this.inputReader;
        } else {
            if (this.parsedParameters != null) {
                if (this.parsedParameters.equals(Boolean.TRUE)) {
                    logger.warn("Called getReader after getParameter ... error");
                } else {
                    throw new IllegalStateException("Called getReader() after getInputStream() on request");
                }
            }
            if (this.encoding != null) {
                this.inputReader = new BufferedReader(new InputStreamReader(this.inputData, this.encoding));
            } else {
                this.inputReader = new BufferedReader(new InputStreamReader(this.inputData));
            }
            this.parsedParameters = Boolean.FALSE;
            return this.inputReader;
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (this.inputReader != null) {
            throw new IllegalStateException("Called getInputStream() after getReader() on request");
        }
        if (this.parsedParameters != null) {
            if (this.parsedParameters.equals(Boolean.TRUE)) {
                logger.warn("Called getInputStream after getParameter ... error");
            }
        }
        this.parsedParameters = Boolean.FALSE;
        return this.inputData;
    }

    @Override
    public String getParameter(String name) {
        parseRequestParameters();
        String[] param = null;
        if (!this.parametersStack.isEmpty()) {
            param = this.parametersStack.peek().get(name);
        }
        // if ((param == null) && this.forwardedParameters.get(name) != null) {
        // param = this.forwardedParameters.get(name);
        // }
        if (param == null) {
            param = this.parameters.get(name);
        }
        if (param == null) {
            return null;
        } else {
            return ((String[]) param)[0];
        }
    }

    @Override
    public Enumeration<String> getParameterNames() {
        parseRequestParameters();
        Set<String> parameterKeys = new HashSet<String>(this.parameters.keySet());
        // parameterKeys.addAll(this.forwardedParameters.keySet());
        if (!this.parametersStack.isEmpty()) {
            parameterKeys.addAll(this.parametersStack.peek().keySet());
        }
        return Collections.enumeration(parameterKeys);
    }

    @Override
    public String[] getParameterValues(String name) {
        parseRequestParameters();
        String[] param = null;
        if (!this.parametersStack.isEmpty()) {
            param = this.parametersStack.peek().get(name);
        }
        // if ((param == null) && this.forwardedParameters.get(name) != null) {
        // param = this.forwardedParameters.get(name);
        // }
        if (param == null) {
            param = this.parameters.get(name);
        }
        if (param == null) {
            return null;
        }

        return (String[]) param;
    }

    @Override
    public Map<String, Object> getParameterMap() {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        for (Enumeration<String> names = this.getParameterNames(); names.hasMoreElements();) {
            String name = (String) names.nextElement();
            paramMap.put(name, getParameterValues(name));
        }
        return paramMap;
    }

    @Override
    public String getServerName() {
        return this.serverName;
    }

    @Override
    public int getServerPort() {
        return this.serverPort;
    }

    @Override
    public String getRemoteAddr() {
        return this.remoteIP;
    }

    @Override
    public String getRemoteHost() {
        return this.remoteName;
    }

    @Override
    public int getRemotePort() {
        return this.remotePort;
    }

    @Override
    public String getLocalAddr() {
        return this.localAddr;
    }

    @Override
    public String getLocalName() {
        return this.localName;
    }

    @Override
    public int getLocalPort() {
        return this.localPort;
    }

    @Override
    public javax.servlet.RequestDispatcher getRequestDispatcher(String path) {
        if (path.startsWith("/")) {
            return this.webappConfig.getRequestDispatcher(path);
        }

        // Take the servlet path + pathInfo, and make an absolute path
        String fullPath = getServletPath() + (getPathInfo() == null ? "" : getPathInfo());
        int lastSlash = fullPath.lastIndexOf('/');
        String currentDir = (lastSlash == -1 ? "/" : fullPath.substring(0, lastSlash + 1));
        return this.webappConfig.getRequestDispatcher(currentDir + path);
    }

    // Now the stuff for HttpServletRequest
    @Override
    public String getContextPath() {
        return this.webappConfig.getContextPath();
    }

    @Override
    public Cookie[] getCookies() {
        return this.cookies;
    }

    @Override
    public long getDateHeader(String name) {
        String dateHeader = getHeader(name);
        if (dateHeader == null) {
            return -1;
        } else {
            try {
                Date date = null;
                synchronized (headerDF) {
                    date = headerDF.parse(dateHeader);
                }
                return date.getTime();
            } catch (java.text.ParseException err) {
                throw new IllegalArgumentException("Can't convert to date - " + dateHeader);
            }
        }
    }

    @Override
    public int getIntHeader(final String name) {
        String header = getHeader(name);
        return header == null ? -1 : Integer.parseInt(header);
    }

    @Override
    public String getHeader(final String name) {
        return extractFirstHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(extractHeaderNameList());
    }

    @Override
    public Enumeration<String> getHeaders(final String name) {
        List<String> result = new ArrayList<String>();
        for (int n = 0; n < this.headers.length; n++) {
            if (this.headers[n].toUpperCase().startsWith(name.toUpperCase() + ':')) {
                result.add(this.headers[n].substring(name.length() + 1).trim()); // 1 for colon
            }
        }
        return Collections.enumeration(result);
    }

    @Override
    public String getMethod() {
        return this.method;
    }

    @Override
    public String getPathInfo() {
        return this.pathInfo;
    }

    @Override
    public String getPathTranslated() {
        return this.webappConfig.getRealPath(this.pathInfo);
    }

    @Override
    public String getQueryString() {
        return this.queryString;
    }

    @Override
    public String getRequestURI() {
        return this.requestURI;
    }

    @Override
    public String getServletPath() {
        return this.servletPath;
    }

    @Override
    public String getRequestedSessionId() {
        String actualSessionId = (String) this.requestedSessionIds.get(this.webappConfig.getContextPath());
        if (actualSessionId != null) {
            return actualSessionId;
        } else {
            return this.deadRequestedSessionId;
        }
    }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        url.append(getScheme()).append("://");
        url.append(getServerName());
        if (!((getServerPort() == 80) && getScheme().equals("http")) && !((getServerPort() == 443) && getScheme().equals("https"))) {
            url.append(':').append(getServerPort());
        }
        url.append(getRequestURI()); // need encoded form, so can't use servlet path + path info
        return url;
    }

    @Override
    public Principal getUserPrincipal() {
        return this.authenticatedUser;
    }

    @Override
    public boolean isUserInRole(final String role) {
        if (this.authenticatedUser == null) {
            return false;
        } else if (this.servletConfig.getSecurityRoleRefs() == null) {
            return this.authenticatedUser.isUserIsInRole(role);
        } else {
            String replacedRole = this.servletConfig.getSecurityRoleRefs().get(role);
            return this.authenticatedUser.isUserIsInRole(replacedRole == null ? role : replacedRole);
        }
    }

    @Override
    public String getAuthType() {
        return this.authenticatedUser == null ? null : this.authenticatedUser.getAuthType();
    }

    @Override
    public String getRemoteUser() {
        return this.authenticatedUser == null ? null : this.authenticatedUser.getName();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return (getRequestedSessionId() != null);
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        String requestedId = getRequestedSessionId();
        if (requestedId == null) {
            return false;
        }
        WinstoneSession ws = this.webappConfig.getSessionById(requestedId, false);
        return (ws != null);
        // if (ws == null) {
        // return false;
        // } else {
        // return (validationCheck(ws, System.currentTimeMillis(), false) != null);
        // }
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public HttpSession getSession(final boolean create) {
        String cookieValue = (String) this.currentSessionIds.get(this.webappConfig.getContextPath());

        // Handle the null case
        if (cookieValue == null) {
            if (!create) {
                return null;
            } else {
                cookieValue = makeNewSession().getId();
            }
        }

        // Now get the session object
        WinstoneSession session = this.webappConfig.getSessionById(cookieValue, false);
        if (session != null) {
            // long nowDate = System.currentTimeMillis();
            // session = validationCheck(session, nowDate, create);
            // if (session == null) {
            // this.currentSessionIds.remove(this.webappConfig.getContextPath());
            // }
        }
        if (create && (session == null)) {
            session = makeNewSession();
        }
        if (session != null) {
            this.usedSessions.add(session);
            session.addUsed(this);
        }
        return session;
    }

    /**
     * Make a new session, and return the id
     */
    public WinstoneSession makeNewSession() {
        String cookieValue = "Winstone_" + this.remoteIP + "_" + this.serverPort + "_" + System.currentTimeMillis() + rnd.nextLong();
        byte digestBytes[] = this.md5Digester.digest(cookieValue.getBytes());

        // Write out in hex format
        char outArray[] = new char[32];
        for (int n = 0; n < digestBytes.length; n++) {
            int hiNibble = (digestBytes[n] & 0xFF) >> 4;
            int loNibble = (digestBytes[n] & 0xF);
            outArray[2 * n] = (hiNibble > 9 ? (char) (hiNibble + 87) : (char) (hiNibble + 48));
            outArray[2 * n + 1] = (loNibble > 9 ? (char) (loNibble + 87) : (char) (loNibble + 48));
        }

        String newSessionId = new String(outArray);
        this.currentSessionIds.put(this.webappConfig.getContextPath(), newSessionId);
        return this.webappConfig.makeNewSession(newSessionId);
    }

    public void markSessionsAsRequestFinished(final long lastAccessedTime, final boolean saveSessions) {
        for (Iterator<WinstoneSession> i = this.usedSessions.iterator(); i.hasNext();) {
            WinstoneSession session = i.next();
            session.setLastAccessedDate(lastAccessedTime);
            session.removeUsed(this);
            if (saveSessions) {
                session.saveToTemp();
            }
        }
        this.usedSessions.clear();
    }

    /**
     * @deprecated
     */
    @Override
    public String getRealPath(final String path) {
        return this.webappConfig.getRealPath(path);
    }

    /**
     * @deprecated
     */
    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }
}
