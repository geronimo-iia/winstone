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
 * This is used by the ACL filter to allow a retry by using a key lookup
 * on old request. It's only used when retrying an old request that was blocked
 * by the ACL filter.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: RetryRequestParams.java,v 1.2 2007/06/01 15:59:53 rickknowles Exp $
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
    @SuppressWarnings("unchecked")
    private final Map<String, Enumeration> headers;
    private final List<Locale> locales;
    private final Locale locale;
    private final byte[] bodyContent;

    /**
     * Constructor - this populates the wrapper from the object in session
     */
    @SuppressWarnings("unchecked")
    public RetryRequestParams(final ServletRequest request) throws IOException {
        this.protocol = request.getProtocol();
        this.locales = new ArrayList<Locale>(Collections.list(request.getLocales()));
        this.locale = request.getLocale();
        this.contentLength = request.getContentLength();
        this.contentType = request.getContentType();
        this.encoding = request.getCharacterEncoding();
        this.headers = new HashMap();
        this.scheme = request.getScheme();

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            this.method = httpRequest.getMethod();
            this.contextPath = httpRequest.getContextPath();
            this.servletPath = httpRequest.getServletPath();
            this.pathInfo = httpRequest.getPathInfo();
            this.queryString = httpRequest.getQueryString();

            for (Enumeration names = httpRequest.getHeaderNames(); names.hasMoreElements();) {
                String name = (String) names.nextElement();
                headers.put(name.toLowerCase(), httpRequest.getHeaders(name));
            }
        } else {
            this.method = null;
            this.contextPath = null;
            this.servletPath = null;
            this.pathInfo = null;
            this.queryString = null;
        }

        if (((this.method == null) || this.method.equalsIgnoreCase("POST")) && (this.contentLength != -1)) {
            InputStream inData = request.getInputStream();
            this.bodyContent = new byte[this.contentLength];
            int readCount = 0;
            int read = 0;
            while ((read = inData.read(this.bodyContent, readCount, this.contentLength - readCount)) >= 0) {
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
