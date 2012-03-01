/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.testApplication.filters;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Simple timing and request dumping test filter
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: TimingFilter.java,v 1.2 2006/02/28 07:32:50 rickknowles Exp $
 */
public class TimingFilter implements Filter {
	private boolean dumpRequestParams;

	private ServletContext context;

	@Override
	public void init(final FilterConfig config) {
		final String dumpRequestParams = config.getInitParameter("dumpRequestParameters");
		this.dumpRequestParams = ((dumpRequestParams != null) && dumpRequestParams.equalsIgnoreCase("true"));
		context = config.getServletContext();
	}

	@Override
	public void destroy() {
		context = null;
	}

	/**
	 * Times the execution of the rest of the filter chain, optionally dumping
	 * the request parameters to the servlet context log
	 */
	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		if (dumpRequestParams) {
			for (final Enumeration<?> paramNames = request.getParameterNames(); paramNames.hasMoreElements();) {
				final String name = (String) paramNames.nextElement();
				context.log("Request parameter: " + name + "=" + request.getParameter(name));
			}
		}

		final long startTime = System.currentTimeMillis();
		chain.doFilter(request, response);
		context.log("Filter chain executed in " + (System.currentTimeMillis() - startTime) + "ms");
	}
}
