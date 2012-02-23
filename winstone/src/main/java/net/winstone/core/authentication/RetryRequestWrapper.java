/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core.authentication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import net.winstone.WinstoneException;
import net.winstone.core.WinstoneInputStream;
import net.winstone.core.WinstoneRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is used by the ACL filter to allow a retry by using a key lookup on old
 * request. It's only used when retrying an old request that was blocked by the
 * ACL filter.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: RetryRequestWrapper.java,v 1.3 2007/02/26 00:28:05 rickknowles
 *          Exp $
 */
public final class RetryRequestWrapper extends HttpServletRequestWrapper {

	protected static Logger logger = LoggerFactory.getLogger(RetryRequestWrapper.class);
	private final static transient String METHOD_HEAD = "GET";
	private final static transient String METHOD_GET = "GET";
	private final static transient String METHOD_POST = "POST";
	private final static transient String POST_PARAMETERS = "application/x-www-form-urlencoded";
	protected static final DateFormat headerDF = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

	static {
		RetryRequestWrapper.headerDF.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	private final RetryRequestParams oldRequest;
	// PARAMETER/BODY RELATED FUNCTIONS
	private String encoding;
	@SuppressWarnings("rawtypes")
	private Map parsedParams;
	private ServletInputStream inData;

	/**
	 * Constructor - this populates the wrapper from the object in session
	 */
	public RetryRequestWrapper(final HttpServletRequest request, final RetryRequestParams oldRequest) throws IOException {
		super(request);
		this.oldRequest = oldRequest;
		encoding = this.oldRequest.getEncoding();
	}

	private boolean hasBeenForwarded() {
		return (super.getAttribute("javax.servlet.forward.request_uri") != null);
	}

	@Override
	public String getScheme() {
		if (hasBeenForwarded()) {
			return super.getScheme();
		} else {
			return oldRequest.getScheme();
		}
	}

	@Override
	public String getMethod() {
		if (hasBeenForwarded()) {
			return super.getMethod();
		} else {
			return oldRequest.getMethod();
		}
	}

	@Override
	public String getContextPath() {
		if (hasBeenForwarded()) {
			return super.getContextPath();
		} else {
			return oldRequest.getContextPath();
		}
	}

	@Override
	public String getServletPath() {
		if (hasBeenForwarded()) {
			return super.getServletPath();
		} else {
			return oldRequest.getServletPath();
		}
	}

	@Override
	public String getPathInfo() {
		if (hasBeenForwarded()) {
			return super.getPathInfo();
		} else {
			return oldRequest.getPathInfo();
		}
	}

	@Override
	public String getQueryString() {
		if (hasBeenForwarded()) {
			return super.getQueryString();
		} else {
			return oldRequest.getQueryString();
		}
	}

	@Override
	public String getRequestURI() {
		if (hasBeenForwarded()) {
			return super.getRequestURI();
		} else {
			final String contextPath = oldRequest.getContextPath();
			final String servletPath = oldRequest.getServletPath();
			final String pathInfo = oldRequest.getPathInfo();
			final String queryString = oldRequest.getQueryString();
			return contextPath + servletPath + ((pathInfo == null) ? "" : pathInfo) + ((queryString == null) ? "" : ("?" + queryString));
		}
	}

	@Override
	public String getCharacterEncoding() {
		if (hasBeenForwarded()) {
			return super.getCharacterEncoding();
		} else {
			return oldRequest.getEncoding();
		}
	}

	@Override
	public void setCharacterEncoding(final String encoding) throws UnsupportedEncodingException {
		if (hasBeenForwarded()) {
			super.setCharacterEncoding(encoding);
		} else {
			this.encoding = encoding;
		}
	}

	@Override
	public int getContentLength() {
		if (hasBeenForwarded()) {
			return super.getContentLength();
		} else {
			return oldRequest.getContentLength();
		}
	}

	@Override
	public String getContentType() {
		if (hasBeenForwarded()) {
			return super.getContentType();
		} else {
			return oldRequest.getContentType();
		}
	}

	@Override
	public Locale getLocale() {
		if (hasBeenForwarded()) {
			return super.getLocale();
		} else {
			return oldRequest.getLocale();
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getLocales() {
		if (hasBeenForwarded()) {
			return super.getLocales();
		} else {
			return Collections.enumeration(oldRequest.getLocales());
		}
	}

	// -------------------------------------------------------------------
	// HEADER RELATED FUNCTIONS
	@Override
	public long getDateHeader(final String name) {
		if (hasBeenForwarded()) {
			return super.getDateHeader(name);
		} else {
			final String dateHeader = getHeader(name);
			if (dateHeader == null) {
				return -1;
			} else {
				try {
					synchronized (RetryRequestWrapper.headerDF) {
						return RetryRequestWrapper.headerDF.parse(dateHeader).getTime();
					}
				} catch (final java.text.ParseException err) {
					throw new IllegalArgumentException("Illegal date format: " + dateHeader);
				}
			}
		}
	}

	@Override
	public int getIntHeader(final String name) {
		if (hasBeenForwarded()) {
			return super.getIntHeader(name);
		} else {
			final String header = getHeader(name);
			return header == null ? -1 : Integer.parseInt(header);
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public String getHeader(final String name) {
		if (hasBeenForwarded()) {
			return super.getHeader(name);
		} else {
			final Enumeration e = getHeaders(name);
			return (e != null) && e.hasMoreElements() ? (String) e.nextElement() : null;
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Enumeration getHeaderNames() {
		if (hasBeenForwarded()) {
			return super.getHeaderNames();
		} else {
			return Collections.enumeration(oldRequest.getHeaders().keySet());
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Enumeration getHeaders(final String name) {
		if (hasBeenForwarded()) {
			return super.getHeaders(name);
		} else {
			return oldRequest.getHeaders().get(name.toLowerCase());
		}
	}

	@Override
	public String getParameter(final String name) {
		if (hasBeenForwarded()) {
			return super.getParameter(name);
		} else {
			parseRequestParameters();
			final Object param = parsedParams.get(name);
			if (param == null) {
				return null;
			} else if (param instanceof String) {
				return (String) param;
			} else if (param instanceof String[]) {
				return ((String[]) param)[0];
			} else {
				return param.toString();
			}
		}
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Enumeration getParameterNames() {
		if (hasBeenForwarded()) {
			return super.getParameterNames();
		} else {
			parseRequestParameters();
			return Collections.enumeration(parsedParams.keySet());
		}
	}

	@Override
	public String[] getParameterValues(final String name) {
		if (hasBeenForwarded()) {
			return super.getParameterValues(name);
		} else {
			parseRequestParameters();
			final Object param = parsedParams.get(name);
			if (param == null) {
				return null;
			} else if (param instanceof String) {
				return new String[] { (String) param };
			} else if (param instanceof String[]) {
				return (String[]) param;
			} else {
				throw new WinstoneException("Unknown param type: " + name + " - " + param.getClass());
			}
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Map getParameterMap() {
		if (hasBeenForwarded()) {
			return super.getParameterMap();
		} else {
			final Map<String, String[]> paramMap = new HashMap<String, String[]>();
			for (final Enumeration names = getParameterNames(); names.hasMoreElements();) {
				final String name = (String) names.nextElement();
				paramMap.put(name, getParameterValues(name));
			}
			return paramMap;
		}
	}

	@Override
	public BufferedReader getReader() throws IOException {
		if (hasBeenForwarded()) {
			return super.getReader();
		} else if (getCharacterEncoding() != null) {
			return new BufferedReader(new InputStreamReader(getInputStream(), encoding));
		} else {
			return new BufferedReader(new InputStreamReader(getInputStream()));
		}
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (hasBeenForwarded()) {
			return super.getInputStream();
		} else if (parsedParams != null) {
			RetryRequestWrapper.logger.debug("Called getInputStream after getParameter ... error");
		}

		if (inData == null) {
			inData = new WinstoneInputStream(oldRequest.getBodyContent());
		}

		return inData;
	}

	/**
	 * This takes the parameters in the body of the request and puts them into
	 * the parameters map.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void parseRequestParameters() {
		if (inData != null) {
			RetryRequestWrapper.logger.warn("Called getInputStream after getParameter ... error");
		}

		if (parsedParams == null) {
			final String contentType = oldRequest.getContentType();
			final String queryString = oldRequest.getQueryString();
			final String method = oldRequest.getMethod();
			final Map workingParameters = new HashMap();
			try {
				// Parse query string from request
				if ((method.equals(RetryRequestWrapper.METHOD_GET) || method.equals(RetryRequestWrapper.METHOD_HEAD) || method.equals(RetryRequestWrapper.METHOD_POST)) && (queryString != null)) {
					WinstoneRequest.extractParameters(queryString, encoding, workingParameters, Boolean.FALSE);
				}

				if (method.equals(RetryRequestWrapper.METHOD_POST) && (contentType != null) && (contentType.equals(RetryRequestWrapper.POST_PARAMETERS) || contentType.startsWith(RetryRequestWrapper.POST_PARAMETERS + ";"))) {
					// Parse params
					final String paramLine = (encoding == null ? new String(oldRequest.getBodyContent()) : new String(oldRequest.getBodyContent(), encoding));
					WinstoneRequest.extractParameters(paramLine.trim(), encoding, workingParameters, Boolean.FALSE);
				}

				parsedParams = workingParameters;
			} catch (final UnsupportedEncodingException err) {
				RetryRequestWrapper.logger.error("Error parsing request parameters", err);
				parsedParams = null;
			}
		}
	}
}
