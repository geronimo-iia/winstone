/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import net.winstone.WinstoneResourceBundle;
import net.winstone.util.StringUtils;
import winstone.SimpleRequestDispatcher;
import net.winstone.core.WinstoneConstant;

/**
 * A simple servlet that writes out the body of the error
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ErrorServlet.java,v 1.3 2006/02/28 07:32:47 rickknowles Exp $
 */
public class ErrorServlet extends HttpServlet {
    
    private static final long serialVersionUID = -1210902945433492424L;
    
    private String template;
    private String serverVersion;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        template = WinstoneResourceBundle.getInstance().getString("WinstoneResponse.ErrorPage");
        serverVersion = WinstoneResourceBundle.getInstance().getString("ServerVersion");
    }
    
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        
        Integer sc = (Integer)request.getAttribute(WinstoneConstant.ERROR_STATUS_CODE);
        String msg = (String)request.getAttribute(WinstoneConstant.ERROR_MESSAGE);
        Throwable err = (Throwable)request.getAttribute(WinstoneConstant.ERROR_EXCEPTION);
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        if (err != null) {
            err.printStackTrace(pw);
        } else {
            pw.println("(none)");
        }
        pw.flush();
        // If we are here there was no error servlet, so show the default error page
        String output = StringUtils.replaceToken(template, sc != null ? sc.toString() : "", (msg == null ? "" : msg), sw.toString(), serverVersion, new Date().toString());
        
        response.setContentLength(output.getBytes(response.getCharacterEncoding()).length);
        Writer out = response.getWriter();
        out.write(output);
        out.flush();
    }
}
