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
package net.winstone.filters.gzip;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import net.winstone.core.WinstoneRequest;

/**
 * A filter that checks if the request will accept a gzip encoded response, and if so wraps the response in a gzip encoding response
 * wrapper.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: GzipFilter.java,v 1.1 2005/08/24 06:43:34 rickknowles Exp $
 */
public class GzipFilter implements Filter {
    
    private static final String ACCEPT_ENCODING = "Accept-Encoding";
    
    private ServletContext context;
    
    @Override
    public void init(final FilterConfig config) throws ServletException {
        this.context = config.getServletContext();
    }
    
    @Override
    public void destroy() {
        this.context = null;
    }
    
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        //Add cast to WinstoneRequest avoid type cast.
        Enumeration<String> headers = ((WinstoneRequest)request).getHeaders(ACCEPT_ENCODING);
        boolean acceptsGzipEncoding = false;
        while (headers.hasMoreElements() && !acceptsGzipEncoding) {
            acceptsGzipEncoding = (((String)headers.nextElement()).indexOf("gzip") != -1);
        }
        if (acceptsGzipEncoding) {
            GzipResponseWrapper encodedResponse = new GzipResponseWrapper((HttpServletResponse)response, this.context);
            chain.doFilter(request, encodedResponse);
            encodedResponse.close();
        } else {
            chain.doFilter(request, response);
        }
    }
}
