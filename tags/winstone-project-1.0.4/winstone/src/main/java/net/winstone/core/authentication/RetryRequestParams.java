/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core.authentication;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * This is used by the ACL filter to allow a retry by using a key lookup on old
 * request. It's only used when retrying an old request that was blocked by the
 * ACL filter.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: RetryRequestParams.java,v 1.2 2007/06/01 15:59:53 rickknowles
 *          Exp $
 */
public final class RetryRequestParams implements java.io.Serializable {

	private static final long serialVersionUID = 7900915469307853808L;
	private final String method;
	private final String scheme;
	private final String contextPath;
	private final String servletPath;
	private final String pathInfo;
	private final String queryString;
	private final String protocol;
	private final int contentLength;
	private final String contentType;
	private final String encoding;
	@SuppressWarnings("rawtypes")
	private final Map<String, Enumeration> headers;
	private final List<Locale> locales;
	private final Locale locale;
	private final byte[] bodyContent;

	/**
	 * Constructor - this populates the wrapper from the object in session
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public RetryRequestParams(final ServletRequest request) throws IOException {
		protocol = request.getProtocol();
		locales = new ArrayList<Locale>(Collections.list(request.getLocales()));
		locale = request.getLocale();
		contentLength = request.getContentLength();
		contentType = request.getContentType();
		encoding = request.getCharacterEncoding();
		headers = new HashMap();
		scheme = request.getScheme();

		if (request instanceof HttpServletRequest) {
			final HttpServletRequest httpRequest = (HttpServletRequest) request;
			method = httpRequest.getMethod();
			contextPath = httpRequest.getContextPath();
			servletPath = httpRequest.getServletPath();
			pathInfo = httpRequest.getPathInfo();
			queryString = httpRequest.getQueryString();

			for (final Enumeration names = httpRequest.getHeaderNames(); names.hasMoreElements();) {
				final String name = (String) names.nextElement();
				headers.put(name.toLowerCase(), httpRequest.getHeaders(name));
			}
		} else {
			method = null;
			contextPath = null;
			servletPath = null;
			pathInfo = null;
			queryString = null;
		}

		if (((method == null) || method.equalsIgnoreCase("POST")) && (contentLength != -1)) {
			final InputStream inData = request.getInputStream();
			bodyContent = new byte[contentLength];
			int readCount = 0;
			int read = 0;
			while ((read = inData.read(bodyContent, readCount, contentLength - readCount)) >= 0) {
				readCount += read;
			}
			inData.close();
		} else {
			bodyContent = null;
		}
	}

	public byte[] getBodyContent() {
		return bodyContent;
	}

	public int getContentLength() {
		return contentLength;
	}

	public String getContentType() {
		return contentType;
	}

	public String getEncoding() {
		return encoding;
	}

	@SuppressWarnings("rawtypes")
	public Map<String, Enumeration> getHeaders() {
		return headers;
	}

	public Locale getLocale() {
		return locale;
	}

	public List<Locale> getLocales() {
		return locales;
	}

	public String getMethod() {
		return method;
	}

	public String getPathInfo() {
		return pathInfo;
	}

	public String getProtocol() {
		return protocol;
	}

	public String getQueryString() {
		return queryString;
	}

	public String getScheme() {
		return scheme;
	}

	public String getServletPath() {
		return servletPath;
	}

	public String getContextPath() {
		return contextPath;
	}
}
