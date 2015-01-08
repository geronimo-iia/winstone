/*
 * Generator Runtime Servlet Framework
 * Copyright (C) 2004 Rick Knowles
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * Version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License Version 2 for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * Version 2 along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.winstone.filters.multipart;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Checks the content type, and wraps the request in a MultipartRequestWrapper
 * if it's a multipart request.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: MultipartRequestFilter.java,v 1.1 2005/08/24 06:43:34
 *          rickknowles Exp $
 */
public class MultipartRequestFilter implements Filter {

	private int maxContentLength = -1;
	private String tooBigPage;
	private ServletContext context;

	@Override
	public void init(final FilterConfig config) throws ServletException {
		tooBigPage = config.getInitParameter("tooBigPage");
		String pmaxContentLength = config.getInitParameter("maxContentLength");
		if (pmaxContentLength == null) {
			pmaxContentLength = "-1";
		}
		maxContentLength = Integer.parseInt(pmaxContentLength);
		context = config.getServletContext();
	}

	@Override
	public void destroy() {
		context = null;
		maxContentLength = -1;
		tooBigPage = null;
	}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		final String contentType = request.getContentType();
		if ((contentType != null) && contentType.startsWith("multipart/form-data")) {
			if (maxContentLength >= 0) {
				final int contentLength = request.getContentLength();
				if ((contentLength != -1) && (contentLength > maxContentLength)) {
					if (tooBigPage == null) {
						((HttpServletResponse) response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Upload size (" + contentLength + " bytes) was larger " + "than the maxContentLength (" + maxContentLength + " bytes)");
					} else {
						final RequestDispatcher rdTooBig = context.getRequestDispatcher(tooBigPage);
						if (rdTooBig == null) {
							((HttpServletResponse) response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Upload size (" + contentLength + " bytes) was larger " + "than the maxContentLength (" + maxContentLength + " bytes) - page "
									+ tooBigPage + " not found");
						} else {
							rdTooBig.forward(request, response);
						}
					}
				} else {
					chain.doFilter(new MultipartRequestWrapper(request), response);
				}
			} else {
				chain.doFilter(new MultipartRequestWrapper(request), response);
			}
		} else {
			chain.doFilter(request, response);
		}
	}
}
