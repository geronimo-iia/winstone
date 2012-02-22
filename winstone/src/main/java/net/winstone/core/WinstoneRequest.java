/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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

import net.winstone.WinstoneException;
import net.winstone.core.authentication.AuthenticationPrincipal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the request interface required by the servlet spec.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneRequest.java,v 1.40 2008/10/01 14:46:13 rickknowles Exp
 *          $
 */
public class WinstoneRequest implements HttpServletRequest {

	protected static Logger logger = LoggerFactory.getLogger(WinstoneRequest.class);
	protected static final DateFormat headerDF = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
	protected static final Random rnd;

	static {
		WinstoneRequest.headerDF.setTimeZone(TimeZone.getTimeZone("GMT"));
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
	private final Set<WinstoneSession> usedSessions;

	/**
	 * InputStream factory method.
	 */
	public WinstoneRequest() throws IOException {
		attributes = new HashMap<String, Object>();
		parameters = new HashMap<String, String[]>();
		locales = new ArrayList<Locale>();
		attributesStack = new Stack<Map<String, Object>>();
		parametersStack = new Stack<Map<String, String[]>>();
		// this.forwardedParameters = new Hashtable();
		requestedSessionIds = new HashMap<String, String>();
		currentSessionIds = new HashMap<String, String>();
		usedSessions = new HashSet<WinstoneSession>();
		contentLength = -1;
		isSecure = false;
		try {
			md5Digester = MessageDigest.getInstance("MD5");
		} catch (final NoSuchAlgorithmException err) {
			throw new WinstoneException("MD5 digester unavailable - what the ...?");
		}
	}

	/**
	 * Resets the request to be reused
	 */
	public void cleanUp() {
		requestListeners = null;
		requestAttributeListeners = null;
		attributes.clear();
		parameters.clear();
		attributesStack.clear();
		parametersStack.clear();
		// this.forwardedParameters.clear();
		usedSessions.clear();
		headers = null;
		cookies = null;
		method = null;
		scheme = null;
		serverName = null;
		requestURI = null;
		servletPath = null;
		pathInfo = null;
		queryString = null;
		protocol = null;
		contentLength = -1;
		contentType = null;
		encoding = null;
		inputData = null;
		inputReader = null;
		servletConfig = null;
		webappConfig = null;
		hostGroup = null;
		serverPort = -1;
		remoteIP = null;
		remoteName = null;
		remotePort = -1;
		localAddr = null;
		localName = null;
		localPort = -1;
		parsedParameters = null;
		requestedSessionIds.clear();
		currentSessionIds.clear();
		deadRequestedSessionId = null;
		locales.clear();
		authorization = null;
		isSecure = false;
		authenticatedUser = null;
	}

	/**
	 * Steps through the header array, searching for the first header matching
	 */
	private String extractFirstHeader(final String name) {
		for (int n = 0; n < headers.length; n++) {
			if (headers[n].toUpperCase().startsWith(name.toUpperCase() + ':')) {
				return headers[n].substring(name.length() + 1).trim(); // 1 for
																		// colon
			}
		}
		return null;
	}

	private Collection<String> extractHeaderNameList() {
		final Collection<String> headerNames = new HashSet<String>();
		for (int n = 0; n < headers.length; n++) {
			final String name = headers[n];
			final int colonPos = name.indexOf(':');
			headerNames.add(name.substring(0, colonPos));
		}
		return headerNames;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public Map<String, String[]> getParameters() {
		return parameters;
	}

	//
	// public Map getForwardedParameters() {
	// return this.forwardedParameters;
	// }
	public Stack<Map<String, Object>> getAttributesStack() {
		return attributesStack;
	}

	public Stack<Map<String, String[]>> getParametersStack() {
		return parametersStack;
	}

	public Map<String, String> getCurrentSessionIds() {
		return currentSessionIds;
	}

	public Map<String, String> getRequestedSessionIds() {
		return requestedSessionIds;
	}

	public String getDeadRequestedSessionId() {
		return deadRequestedSessionId;
	}

	public HostGroup getHostGroup() {
		return hostGroup;
	}

	public WebAppConfiguration getWebAppConfig() {
		return webappConfig;
	}

	public ServletConfiguration getServletConfig() {
		return servletConfig;
	}

	public String getEncoding() {
		return encoding;
	}

	public Boolean getParsedParameters() {
		return parsedParameters;
	}

	public List<Locale> getListLocales() {
		return locales;
	}

	public void setInputStream(final WinstoneInputStream inputData) {
		this.inputData = inputData;
	}

	public void setHostGroup(final HostGroup hostGroup) {
		this.hostGroup = hostGroup;
	}

	public void setWebAppConfig(final WebAppConfiguration webappConfig) {
		this.webappConfig = webappConfig;
	}

	public void setServletConfig(final ServletConfiguration servletConfig) {
		this.servletConfig = servletConfig;
	}

	public void setServerPort(final int port) {
		serverPort = port;
	}

	public void setRemoteIP(final String remoteIP) {
		this.remoteIP = remoteIP;
	}

	public void setRemoteName(final String name) {
		remoteName = name;
	}

	public void setRemotePort(final int port) {
		remotePort = port;
	}

	public void setLocalAddr(final String ip) {
		localName = ip;
	}

	public void setLocalName(final String name) {
		localName = name;
	}

	public void setLocalPort(final int port) {
		localPort = port;
	}

	public void setMethod(final String method) {
		this.method = method;
	}

	public void setIsSecure(final boolean isSecure) {
		this.isSecure = isSecure;
	}

	public void setQueryString(final String queryString) {
		this.queryString = queryString;
	}

	public void setServerName(final String name) {
		serverName = name;
	}

	public void setRequestURI(final String requestURI) {
		this.requestURI = requestURI;
	}

	public void setScheme(final String scheme) {
		this.scheme = scheme;
	}

	public void setServletPath(final String servletPath) {
		this.servletPath = servletPath;
	}

	public void setPathInfo(final String pathInfo) {
		this.pathInfo = pathInfo;
	}

	public void setProtocol(final String protocolString) {
		protocol = protocolString;
	}

	public void setRemoteUser(final AuthenticationPrincipal user) {
		authenticatedUser = user;
	}

	public void setContentLength(final int len) {
		contentLength = len;
	}

	public void setContentType(final String type) {
		contentType = type;
	}

	public void setAuthorization(final String auth) {
		authorization = auth;
	}

	public void setLocales(final List<Locale> locales) {
		this.locales = locales;
	}

	public void setCurrentSessionIds(final Map<String, String> currentSessionIds) {
		this.currentSessionIds = currentSessionIds;
	}

	public void setRequestedSessionIds(final Map<String, String> requestedSessionIds) {
		this.requestedSessionIds = requestedSessionIds;
	}

	public void setDeadRequestedSessionId(final String deadRequestedSessionId) {
		this.deadRequestedSessionId = deadRequestedSessionId;
	}

	public void setEncoding(final String encoding) {
		this.encoding = encoding;
	}

	public void setParsedParameters(final Boolean parsed) {
		parsedParameters = parsed;
	}

	public void setRequestListeners(final ServletRequestListener rl[]) {
		requestListeners = rl;
	}

	public void setRequestAttributeListeners(final ServletRequestAttributeListener ral[]) {
		requestAttributeListeners = ral;
	}

	/**
	 * Gets parameters from the url encoded parameter string
	 */
	public static void extractParameters(final String urlEncodedParams, final String encoding, final Map<String, String[]> outputParams, final boolean overwrite) {
		WinstoneRequest.logger.debug("Parsing parameters: {} (using encoding {})", urlEncodedParams, encoding);
		final StringTokenizer st = new StringTokenizer(urlEncodedParams, "&", false);
		Set<String> overwrittenParamNames = null;
		while (st.hasMoreTokens()) {
			final String token = st.nextToken();
			final int equalPos = token.indexOf('=');
			try {
				final String decodedNameDefault = WinstoneRequest.decodeURLToken(equalPos == -1 ? token : token.substring(0, equalPos));
				final String decodedValueDefault = (equalPos == -1 ? "" : WinstoneRequest.decodeURLToken(token.substring(equalPos + 1)));
				final String decodedName = (encoding == null ? decodedNameDefault : new String(decodedNameDefault.getBytes("8859_1"), encoding));
				final String decodedValue = (encoding == null ? decodedValueDefault : new String(decodedValueDefault.getBytes("8859_1"), encoding));

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
					outputParams.put(decodedName, new String[] { decodedValue });
				} else {
					final String alreadyArray[] = already;
					final String oneMore[] = new String[alreadyArray.length + 1];
					System.arraycopy(alreadyArray, 0, oneMore, 0, alreadyArray.length);
					oneMore[oneMore.length - 1] = decodedValue;
					outputParams.put(decodedName, oneMore);
				}
			} catch (final UnsupportedEncodingException err) {
				WinstoneRequest.logger.error("Error parsing request parameters", err);
			}
		}
	}

	/**
	 * For decoding the URL encoding used on query strings.
	 * 
	 * @param in
	 *            input string
	 * @return decoded string
	 */
	public static String decodeURLToken(String in) {
		return decodeURLToken(in, true);
	}

	/**
	 * For decoding the URL encoding used on query strings
	 * 
	 * @param in
	 *            input string
	 * @param encoding
	 *            encoding
	 * @return decoded string
	 * @throws UnsupportedEncodingException
	 */
	public static String decodeURLToken(String in, String encoding) throws UnsupportedEncodingException {
		return decodeURLToken(in, encoding, true);
	}

	/**
	 * For decoding the URL encoding used on query strings (using UTF-8)
	 * 
	 * @param in
	 *            in input string
	 * @param isQueryString
	 *            Decode query string, where '+' is an escape for ' '. Otherwise
	 *            decode as path token, where '+' is not an escape character.
	 * @return decoded string
	 */
	public static String decodeURLToken(String in, boolean isQueryString) {
		try {
			return decodeURLToken(in, "UTF-8", isQueryString);
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(); // impossible
		}
	}

	/**
	 * For decoding the URL encoding.
	 * 
	 * @param in
	 *            in input string
	 * @param encoding
	 *            encoding
	 * @param isQueryString
	 *            Decode query string, where '+' is an escape for ' '. Otherwise
	 *            decode as path token, where '+' is not an escape character.
	 * @return decoded string
	 * @throws UnsupportedEncodingException
	 */
	public static String decodeURLToken(String in, String encoding, boolean isQueryString) throws UnsupportedEncodingException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (int n = 0; n < in.length(); n++) {
			char thisChar = in.charAt(n);
			if (thisChar == '+' && isQueryString)
				baos.write(' ');
			else if (thisChar == '%') {
				String token = in.substring(Math.min(n + 1, in.length()), Math.min(n + 3, in.length()));
				try {
					int decoded = Integer.parseInt(token, 16);
					baos.write(decoded);
					n += 2;
				} catch (RuntimeException err) {
					WinstoneRequest.logger.warn("Found an invalid character %{} in url parameter. Echoing through in escaped form", token);
					baos.write(thisChar);
				}
			} else
				baos.write(thisChar);
		}
		return new String(baos.toByteArray(), encoding);
	}

	public void discardRequestBody() {
		if (getContentLength() > 0) {
			try {
				WinstoneRequest.logger.debug("Forcing request body parse");
				// If body not parsed
				if ((parsedParameters == null) || (parsedParameters.equals(Boolean.FALSE))) {
					// read full stream length
					try {
						final InputStream in = getInputStream();
						final byte buffer[] = new byte[2048];
						while (in.read(buffer) != -1) {
						}
					} catch (final IllegalStateException err) {
						final Reader in = getReader();
						final char buffer[] = new char[2048];
						while (in.read(buffer) != -1) {
						}
					}
				}
			} catch (final IOException err) {
				WinstoneRequest.logger.error("Forcing request body parse", err);
			}
		}
	}

	/**
	 * This takes the parameters in the body of the request and puts them into
	 * the parameters map.
	 */
	public void parseRequestParameters() {
		if ((parsedParameters != null) && !parsedParameters.booleanValue()) {
			WinstoneRequest.logger.warn("Called getInputStream after getParameter ... error");
			parsedParameters = Boolean.TRUE;
		} else if (parsedParameters == null) {
			final Map<String, String[]> workingParameters = new HashMap<String, String[]>();
			try {
				// Parse query string from request
				// if ((method.equals(METHOD_GET) || method.equals(METHOD_HEAD)
				// ||
				// method.equals(METHOD_POST)) &&
				if (queryString != null) {
					WinstoneRequest.extractParameters(queryString, encoding, workingParameters, false);
					WinstoneRequest.logger.debug("Param line: " + workingParameters);
				}

				if (method.equals(WinstoneConstant.METHOD_POST) && (contentType != null) && (contentType.equals(WinstoneConstant.POST_PARAMETERS) || contentType.startsWith(WinstoneConstant.POST_PARAMETERS + ";"))) {
					WinstoneRequest.logger.debug("Parsing request body for parameters");

					// Parse params
					final byte paramBuffer[] = new byte[contentLength];
					int readCount = this.inputData.readAsMuchAsPossible(paramBuffer, 0, contentLength);
					if (readCount != contentLength) {
						WinstoneRequest.logger.warn("Content-length said {}, actual length was {}", Integer.toString(contentLength), Integer.toString(readCount));
					}
					final String paramLine = (encoding == null ? new String(paramBuffer) : new String(paramBuffer, encoding));
					WinstoneRequest.extractParameters(paramLine.trim(), encoding, workingParameters, false);
					WinstoneRequest.logger.debug("Param line: " + workingParameters.toString());
				}

				parameters.putAll(workingParameters);
				parsedParameters = Boolean.TRUE;
			} catch (final Throwable err) {
				WinstoneRequest.logger.error("Error parsing body of the reques", err);
				parsedParameters = null;
			}
		}
	}

	/**
	 * Go through the list of headers, and build the headers/cookies arrays for
	 * the request object.
	 */
	public void parseHeaders(final List<String> headerList) {
		// Iterate through headers
		final List<String> outHeaderList = new ArrayList<String>();
		final List<Cookie> cookieList = new ArrayList<Cookie>();
		for (final Iterator<String> i = headerList.iterator(); i.hasNext();) {
			final String header = i.next();
			final int colonPos = header.indexOf(':');
			final String name = header.substring(0, colonPos);
			final String value = header.substring(colonPos + 1).trim();

			// Add it to out headers if it's not a cookie
			outHeaderList.add(header);
			// if (!name.equalsIgnoreCase(IN_COOKIE_HEADER1)
			// && !name.equalsIgnoreCase(IN_COOKIE_HEADER2))

			if (name.equalsIgnoreCase(WinstoneConstant.AUTHORIZATION_HEADER)) {
				authorization = value;
			} else if (name.equalsIgnoreCase(WinstoneConstant.LOCALE_HEADER)) {
				locales = parseLocales(value);
			} else if (name.equalsIgnoreCase(WinstoneConstant.CONTENT_LENGTH_HEADER)) {
				contentLength = Integer.parseInt(value);
			} else if (name.equalsIgnoreCase(WinstoneConstant.HOST_HEADER)) {
				if (value.indexOf('[') != -1 && value.indexOf(']') != -1) {
					// IPv6 host as per rfc2732
					this.serverName = value.substring(value.indexOf('[') + 1, value.indexOf(']'));
					int nextColonPos = value.indexOf("]:");
					if ((nextColonPos == -1) || (nextColonPos == value.length() - 1)) {
						if (this.scheme != null) {
							if (this.scheme.equals("http")) {
								this.serverPort = 80;
							} else if (this.scheme.equals("https")) {
								this.serverPort = 443;
							}
						}
					} else {
						this.serverPort = Integer.parseInt(value.substring(nextColonPos + 2));
					}
				} else {
					// IPv4 host
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
				}
			} else if (name.equalsIgnoreCase(WinstoneConstant.CONTENT_TYPE_HEADER)) {
				contentType = value;
				final int semicolon = value.lastIndexOf(';');
				if (semicolon != -1) {
					final String encodingClause = value.substring(semicolon + 1).trim();
					if (encodingClause.startsWith("charset=")) {
						encoding = encodingClause.substring(8);
					}
				}
			} else if (name.equalsIgnoreCase(WinstoneConstant.IN_COOKIE_HEADER1) || name.equalsIgnoreCase(WinstoneConstant.IN_COOKIE_HEADER2)) {
				parseCookieLine(value, cookieList);
			}
		}
		headers = outHeaderList.toArray(new String[0]);
		if (cookieList.isEmpty()) {
			cookies = null;
		} else {
			cookies = cookieList.toArray(new Cookie[0]);
		}
	}

	private static String nextToken(final StringTokenizer st) {
		if (st.hasMoreTokens()) {
			return st.nextToken();
		} else {
			return null;
		}
	}

	private void parseCookieLine(final String headerValue, final List<Cookie> cookieList) {
		final StringTokenizer st = new StringTokenizer(headerValue, ";", false);
		int version = 0;
		String cookieLine = WinstoneRequest.nextToken(st);

		// check cookie version flag
		if ((cookieLine != null) && cookieLine.startsWith("$Version=")) {
			final int equalPos = cookieLine.indexOf('=');
			try {
				version = Integer.parseInt(WinstoneRequest.extractFromQuotes(cookieLine.substring(equalPos + 1).trim()));
			} catch (final NumberFormatException err) {
				version = 0;
			}
			cookieLine = WinstoneRequest.nextToken(st);
		}

		// process remainder - parameters
		while (cookieLine != null) {
			cookieLine = cookieLine.trim();
			int equalPos = cookieLine.indexOf('=');
			if (equalPos == -1) {
				// next token
				cookieLine = WinstoneRequest.nextToken(st);
			} else {
				final String name = cookieLine.substring(0, equalPos);
				final String value = WinstoneRequest.extractFromQuotes(cookieLine.substring(equalPos + 1));
				final Cookie thisCookie = new Cookie(name, value);
				thisCookie.setVersion(version);
				thisCookie.setSecure(isSecure());
				cookieList.add(thisCookie);

				// check for path / domain / port
				cookieLine = WinstoneRequest.nextToken(st);
				while ((cookieLine != null) && cookieLine.trim().startsWith("$")) {
					cookieLine = cookieLine.trim();
					equalPos = cookieLine.indexOf('=');
					final String attrValue = equalPos == -1 ? "" : cookieLine.substring(equalPos + 1).trim();
					if (cookieLine.startsWith("$Path")) {
						thisCookie.setPath(WinstoneRequest.extractFromQuotes(attrValue));
					} else if (cookieLine.startsWith("$Domain")) {
						thisCookie.setDomain(WinstoneRequest.extractFromQuotes(attrValue));
					}
					cookieLine = WinstoneRequest.nextToken(st);
				}
				WinstoneRequest.logger.debug("Found cookie: " + thisCookie.toString());
				if (thisCookie.getName().equals(WinstoneSession.SESSION_COOKIE_NAME)) {
					// Find a context that manages this key
					final HostConfiguration hostConfig = hostGroup.getHostByName(serverName);
					final WebAppConfiguration ownerContext = hostConfig.getWebAppBySessionKey(thisCookie.getValue());
					if (ownerContext != null) {
						requestedSessionIds.put(ownerContext.getContextPath(), thisCookie.getValue());
						currentSessionIds.put(ownerContext.getContextPath(), thisCookie.getValue());
					} // If not found, it was probably dead
					else {
						deadRequestedSessionId = thisCookie.getValue();
					}
					// this.requestedSessionId = thisCookie.getValue();
					// this.currentSessionId = thisCookie.getValue();
					WinstoneRequest.logger.debug("Found session cookie: {} {}", thisCookie.getValue(), ownerContext == null ? "" : "prefix:" + ownerContext.getContextPath());
				}
			}
		}
	}

	private static String extractFromQuotes(final String input) {
		if ((input != null) && input.startsWith("\"") && input.endsWith("\"")) {
			return input.substring(1, input.length() - 1);
		} else {
			return input;
		}
	}

	private List<Locale> parseLocales(final String header) {
		// Strip out the whitespace
		final StringBuilder lb = new StringBuilder();
		for (int n = 0; n < header.length(); n++) {
			final char c = header.charAt(n);
			if (!Character.isWhitespace(c)) {
				lb.append(c);
			}
		}

		// Tokenize by commas
		final Map<Float, List<Locale>> localeEntries = new HashMap<Float, List<Locale>>();
		final StringTokenizer commaTK = new StringTokenizer(lb.toString(), ",", false);
		for (; commaTK.hasMoreTokens();) {
			String clause = commaTK.nextToken();

			// Tokenize by semicolon
			Float quality = new Float(1);
			if (clause.indexOf(";q=") != -1) {
				final int pos = clause.indexOf(";q=");
				try {
					quality = new Float(clause.substring(pos + 3));
				} catch (final NumberFormatException err) {
					quality = new Float(0);
				}
				clause = clause.substring(0, pos);
			}

			// Build the locale
			String language = "";
			String country = "";
			String variant = "";
			final int dpos = clause.indexOf('-');
			if (dpos == -1) {
				language = clause;
			} else {
				language = clause.substring(0, dpos);
				final String remainder = clause.substring(dpos + 1);
				final int d2pos = remainder.indexOf('-');
				if (d2pos == -1) {
					country = remainder;
				} else {
					country = remainder.substring(0, d2pos);
					variant = remainder.substring(d2pos + 1);
				}
			}
			final Locale loc = new Locale(language, country, variant);

			// Put into list by quality
			List<Locale> localeList = localeEntries.get(quality);
			if (localeList == null) {
				localeList = new ArrayList<Locale>();
				localeEntries.put(quality, localeList);
			}
			localeList.add(loc);
		}

		// Extract and build the list
		final Float orderKeys[] = localeEntries.keySet().toArray(new Float[0]);
		Arrays.sort(orderKeys);
		final List<Locale> outputLocaleList = new ArrayList<Locale>();
		for (int n = 0; n < orderKeys.length; n++) {
			// Skip backwards through the list of maps and add to the output
			// list
			final int reversedIndex = (orderKeys.length - 1) - n;
			if ((orderKeys[reversedIndex].floatValue() <= 0) || (orderKeys[reversedIndex].floatValue() > 1)) {
				continue;
			}
			final List<Locale> localeList = localeEntries.get(orderKeys[reversedIndex]);
			for (final Iterator<Locale> i = localeList.iterator(); i.hasNext();) {
				outputLocaleList.add(i.next());
			}
		}

		return outputLocaleList;
	}

	public void addIncludeQueryParameters(final String queryString) {
		final Map<String, String[]> lastParams = new HashMap<String, String[]>();
		if (!parametersStack.isEmpty()) {
			lastParams.putAll(parametersStack.peek());
		}
		final Map<String, String[]> newQueryParams = new HashMap<String, String[]>();
		if (queryString != null) {
			WinstoneRequest.extractParameters(queryString, encoding, newQueryParams, false);
		}
		lastParams.putAll(newQueryParams);
		parametersStack.push(lastParams);
	}

	public void addIncludeAttributes(final String requestURI, final String contextPath, final String servletPath, final String pathInfo, final String queryString) {
		final Map<String, Object> includeAttributes = new HashMap<String, Object>();
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
		attributesStack.push(includeAttributes);
	}

	public void removeIncludeQueryString() {
		if (!parametersStack.isEmpty()) {
			parametersStack.pop();
		}
	}

	public void clearIncludeStackForForward() {
		parametersStack.clear();
		attributesStack.clear();
	}

	public void setForwardQueryString(final String forwardQueryString) {
		// this.forwardedParameters.clear();

		// Parse query string from include / forward
		if (forwardQueryString != null) {
			final String oldQueryString = queryString == null ? "" : queryString;
			final boolean needJoiner = !forwardQueryString.equals("") && !oldQueryString.equals("");
			queryString = forwardQueryString + (needJoiner ? "&" : "") + oldQueryString;

			if (parsedParameters != null) {
				WinstoneRequest.logger.debug("Parsing parameters: {} (using encoding {})", forwardQueryString, encoding);
				WinstoneRequest.extractParameters(forwardQueryString, encoding, parameters, true);
				WinstoneRequest.logger.debug("Param line: {}", parameters != null ? parameters.toString() : "");
			}
		}

	}

	public void removeIncludeAttributes() {
		if (!attributesStack.isEmpty()) {
			attributesStack.pop();
		}
	}

	// Implementation methods for the servlet request stuff
	@Override
	public Object getAttribute(final String name) {
		if (!attributesStack.isEmpty()) {
			final Map<String, Object> includedAttributes = attributesStack.peek();
			final Object value = includedAttributes.get(name);
			if (value != null) {
				return value;
			}
		}
		return attributes.get(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		final Map<String, Object> result = new HashMap<String, Object>(attributes);
		if (!attributesStack.isEmpty()) {
			final Map<String, Object> includedAttributes = attributesStack.peek();
			result.putAll(includedAttributes);
		}
		return Collections.enumeration(result.keySet());
	}

	@Override
	public void removeAttribute(final String name) {
		final Object value = attributes.get(name);
		if (value == null) {
			return;
		}

		// fire event
		if (requestAttributeListeners != null) {
			for (int n = 0; n < requestAttributeListeners.length; n++) {
				final ClassLoader cl = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(getWebAppConfig().getLoader());
				requestAttributeListeners[n].attributeRemoved(new ServletRequestAttributeEvent(webappConfig, this, name, value));
				Thread.currentThread().setContextClassLoader(cl);
			}
		}

		attributes.remove(name);
	}

	@Override
	public void setAttribute(final String name, final Object o) {
		if ((name != null) && (o != null)) {
			final Object oldValue = attributes.get(name);
			attributes.put(name, o); // make sure it's set at the top level

			// fire event
			if (requestAttributeListeners != null) {
				if (oldValue == null) {
					for (int n = 0; n < requestAttributeListeners.length; n++) {
						final ClassLoader cl = Thread.currentThread().getContextClassLoader();
						Thread.currentThread().setContextClassLoader(getWebAppConfig().getLoader());
						requestAttributeListeners[n].attributeAdded(new ServletRequestAttributeEvent(webappConfig, this, name, o));
						Thread.currentThread().setContextClassLoader(cl);
					}
				} else {
					for (int n = 0; n < requestAttributeListeners.length; n++) {
						final ClassLoader cl = Thread.currentThread().getContextClassLoader();
						Thread.currentThread().setContextClassLoader(getWebAppConfig().getLoader());
						requestAttributeListeners[n].attributeReplaced(new ServletRequestAttributeEvent(webappConfig, this, name, oldValue));
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
		return encoding;
	}

	@Override
	public void setCharacterEncoding(final String encoding) throws UnsupportedEncodingException {
		"blah".getBytes(encoding); // throws an exception if the encoding is
									// unsupported
		if (inputReader == null) {
			WinstoneRequest.logger.debug("Setting the request encoding from {} to {}", this.encoding, encoding);
			this.encoding = encoding;
		}
	}

	@Override
	public int getContentLength() {
		return contentLength;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public Locale getLocale() {
		return locales.isEmpty() ? Locale.getDefault() : (Locale) locales.get(0);
	}

	@Override
	public Enumeration<Locale> getLocales() {
		final List<Locale> sendLocales = locales;
		if (sendLocales.isEmpty()) {
			sendLocales.add(Locale.getDefault());
		}
		return Collections.enumeration(sendLocales);
	}

	@Override
	public String getProtocol() {
		return protocol;
	}

	@Override
	public String getScheme() {
		return scheme;
	}

	@Override
	public boolean isSecure() {
		return isSecure;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		if (inputReader != null) {
			return inputReader;
		} else {
			if (parsedParameters != null) {
				if (parsedParameters.equals(Boolean.TRUE)) {
					WinstoneRequest.logger.warn("Called getReader after getParameter ... error");
				} else {
					throw new IllegalStateException("Called getReader() after getInputStream() on request");
				}
			}
			if (encoding != null) {
				inputReader = new BufferedReader(new InputStreamReader(inputData, encoding));
			} else {
				inputReader = new BufferedReader(new InputStreamReader(inputData));
			}
			parsedParameters = Boolean.FALSE;
			return inputReader;
		}
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (inputReader != null) {
			throw new IllegalStateException("Called getInputStream() after getReader() on request");
		}
		if (parsedParameters != null) {
			if (parsedParameters.equals(Boolean.TRUE)) {
				WinstoneRequest.logger.warn("Called getInputStream after getParameter ... error");
			}
		}
		if (method.equals(WinstoneConstant.METHOD_POST) && WinstoneConstant.POST_PARAMETERS.equals(contentType)) {
			this.parsedParameters = new Boolean(false);
		}
		return inputData;
	}

	@Override
	public String getParameter(final String name) {
		parseRequestParameters();
		String[] param = null;
		if (!parametersStack.isEmpty()) {
			param = parametersStack.peek().get(name);
		}
		// if ((param == null) && this.forwardedParameters.get(name) != null) {
		// param = this.forwardedParameters.get(name);
		// }
		if (param == null) {
			param = parameters.get(name);
		}
		if (param == null) {
			return null;
		} else {
			return param[0];
		}
	}

	@Override
	public Enumeration<String> getParameterNames() {
		parseRequestParameters();
		final Set<String> parameterKeys = new HashSet<String>(parameters.keySet());
		// parameterKeys.addAll(this.forwardedParameters.keySet());
		if (!parametersStack.isEmpty()) {
			parameterKeys.addAll(parametersStack.peek().keySet());
		}
		return Collections.enumeration(parameterKeys);
	}

	@Override
	public String[] getParameterValues(final String name) {
		parseRequestParameters();
		String[] param = null;
		if (!parametersStack.isEmpty()) {
			param = parametersStack.peek().get(name);
		}
		// if ((param == null) && this.forwardedParameters.get(name) != null) {
		// param = this.forwardedParameters.get(name);
		// }
		if (param == null) {
			param = parameters.get(name);
		}
		if (param == null) {
			return null;
		}

		return param;
	}

	@Override
	public Map<String, Object> getParameterMap() {
		final Map<String, Object> paramMap = new HashMap<String, Object>();
		for (final Enumeration<String> names = getParameterNames(); names.hasMoreElements();) {
			final String name = names.nextElement();
			paramMap.put(name, getParameterValues(name));
		}
		return paramMap;
	}

	@Override
	public String getServerName() {
		return serverName;
	}

	@Override
	public int getServerPort() {
		return serverPort;
	}

	@Override
	public String getRemoteAddr() {
		return remoteIP;
	}

	@Override
	public String getRemoteHost() {
		return remoteName;
	}

	@Override
	public int getRemotePort() {
		return remotePort;
	}

	@Override
	public String getLocalAddr() {
		return localAddr;
	}

	@Override
	public String getLocalName() {
		return localName;
	}

	@Override
	public int getLocalPort() {
		return localPort;
	}

	@Override
	public javax.servlet.RequestDispatcher getRequestDispatcher(final String path) {
		if (path.startsWith("/")) {
			return webappConfig.getRequestDispatcher(path);
		}

		// Take the servlet path + pathInfo, and make an absolute path
		final String fullPath = getServletPath() + (getPathInfo() == null ? "" : getPathInfo());
		final int lastSlash = fullPath.lastIndexOf('/');
		final String currentDir = (lastSlash == -1 ? "/" : fullPath.substring(0, lastSlash + 1));
		return webappConfig.getRequestDispatcher(currentDir + path);
	}

	// Now the stuff for HttpServletRequest
	@Override
	public String getContextPath() {
		return webappConfig.getContextPath();
	}

	@Override
	public Cookie[] getCookies() {
		return cookies;
	}

	@Override
	public long getDateHeader(final String name) {
		final String dateHeader = getHeader(name);
		if (dateHeader == null) {
			return -1;
		} else {
			try {
				Date date = null;
				synchronized (WinstoneRequest.headerDF) {
					date = WinstoneRequest.headerDF.parse(dateHeader);
				}
				return date.getTime();
			} catch (final java.text.ParseException err) {
				throw new IllegalArgumentException("Can't convert to date - " + dateHeader);
			}
		}
	}

	@Override
	public int getIntHeader(final String name) {
		final String header = getHeader(name);
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
		final List<String> result = new ArrayList<String>();
		for (int n = 0; n < headers.length; n++) {
			if (headers[n].toUpperCase().startsWith(name.toUpperCase() + ':')) {
				result.add(headers[n].substring(name.length() + 1).trim()); // 1
																			// for
																			// colon
			}
		}
		return Collections.enumeration(result);
	}

	@Override
	public String getMethod() {
		return method;
	}

	@Override
	public String getPathInfo() {
		return pathInfo;
	}

	@Override
	public String getPathTranslated() {
		return webappConfig.getRealPath(pathInfo);
	}

	@Override
	public String getQueryString() {
		return queryString;
	}

	@Override
	public String getRequestURI() {
		return requestURI;
	}

	@Override
	public String getServletPath() {
		return servletPath;
	}

	@Override
	public String getRequestedSessionId() {
		final String actualSessionId = requestedSessionIds.get(webappConfig.getContextPath());
		if (actualSessionId != null) {
			return actualSessionId;
		} else {
			return deadRequestedSessionId;
		}
	}

	@Override
	public StringBuffer getRequestURL() {
		final StringBuffer url = new StringBuffer();
		url.append(getScheme()).append("://");
		url.append(getServerName());
		if (!((getServerPort() == 80) && getScheme().equals("http")) && !((getServerPort() == 443) && getScheme().equals("https"))) {
			url.append(':').append(getServerPort());
		}
		url.append(getRequestURI()); // need encoded form, so can't use servlet
										// path + path info
		return url;
	}

	@Override
	public Principal getUserPrincipal() {
		return authenticatedUser;
	}

	@Override
	public boolean isUserInRole(final String role) {
		if (authenticatedUser == null) {
			return false;
		} else if (servletConfig.getSecurityRoleRefs() == null) {
			return authenticatedUser.isUserIsInRole(role);
		} else {
			final String replacedRole = servletConfig.getSecurityRoleRefs().get(role);
			return authenticatedUser.isUserIsInRole(replacedRole == null ? role : replacedRole);
		}
	}

	@Override
	public String getAuthType() {
		return authenticatedUser == null ? null : authenticatedUser.getAuthType();
	}

	@Override
	public String getRemoteUser() {
		return authenticatedUser == null ? null : authenticatedUser.getName();
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
		final String requestedId = getRequestedSessionId();
		if (requestedId == null) {
			return false;
		}
		final WinstoneSession ws = webappConfig.getSessionById(requestedId, false);
		return (ws != null);
		// if (ws == null) {
		// return false;
		// } else {
		// return (validationCheck(ws, System.currentTimeMillis(), false) !=
		// null);
		// }
	}

	@Override
	public HttpSession getSession() {
		return getSession(true);
	}

	@Override
	public HttpSession getSession(final boolean create) {
		String cookieValue = currentSessionIds.get(webappConfig.getContextPath());

		// Handle the null case
		if (cookieValue == null) {
			if (!create) {
				return null;
			} else {
				cookieValue = makeNewSession().getId();
			}
		}

		// Now get the session object
		WinstoneSession session = webappConfig.getSessionById(cookieValue, false);
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
			usedSessions.add(session);
			session.addUsed(this);
		}
		return session;
	}

	/**
	 * Make a new session, and return the id
	 */
	public WinstoneSession makeNewSession() {
		final String cookieValue = "Winstone_" + remoteIP + "_" + serverPort + "_" + System.currentTimeMillis() + WinstoneRequest.rnd.nextLong();
		final byte digestBytes[] = md5Digester.digest(cookieValue.getBytes());

		// Write out in hex format
		final char outArray[] = new char[32];
		for (int n = 0; n < digestBytes.length; n++) {
			final int hiNibble = (digestBytes[n] & 0xFF) >> 4;
			final int loNibble = (digestBytes[n] & 0xF);
			outArray[2 * n] = (hiNibble > 9 ? (char) (hiNibble + 87) : (char) (hiNibble + 48));
			outArray[(2 * n) + 1] = (loNibble > 9 ? (char) (loNibble + 87) : (char) (loNibble + 48));
		}

		final String newSessionId = new String(outArray);
		currentSessionIds.put(webappConfig.getContextPath(), newSessionId);
		return webappConfig.makeNewSession(newSessionId);
	}

	public void markSessionsAsRequestFinished(final long lastAccessedTime, final boolean saveSessions) {
		for (final Iterator<WinstoneSession> i = usedSessions.iterator(); i.hasNext();) {
			final WinstoneSession session = i.next();
			session.setLastAccessedDate(lastAccessedTime);
			session.removeUsed(this);
			if (saveSessions) {
				session.saveToTemp();
			}
		}
		usedSessions.clear();
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	@Override
	public String getRealPath(final String path) {
		return webappConfig.getRealPath(path);
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	@Override
	public boolean isRequestedSessionIdFromUrl() {
		return isRequestedSessionIdFromURL();
	}
}
