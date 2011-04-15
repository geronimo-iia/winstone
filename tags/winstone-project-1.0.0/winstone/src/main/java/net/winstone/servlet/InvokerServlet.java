/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.winstone.core.Mapping;
import net.winstone.core.ServletConfiguration;
import net.winstone.core.SimpleRequestDispatcher;
import net.winstone.core.WebAppConfiguration;

/**
 * If a URI matches a servlet class name, mount an instance of that servlet, and try to process the request using that servlet.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: InvokerServlet.java,v 1.6 2006/03/24 17:24:24 rickknowles Exp $
 */
public class InvokerServlet extends HttpServlet {

    private static final long serialVersionUID = -2502687199563269260L;
    protected static Logger logger = LoggerFactory.getLogger(InvokerServlet.class);
    // private static final String FORWARD_PATH_INFO = "javax.servlet.forward.path_info";
    private static final String INCLUDE_PATH_INFO = "javax.servlet.include.path_info";
    private Map<String, ServletConfiguration> mountedInstances;
    private final Boolean mountedInstancesSemaphore = Boolean.TRUE;
    // private String prefix;
    // private String invokerPrefix;

    /**
     * Set up a blank map of servlet configuration instances
     */
    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        this.mountedInstances = new HashMap<String, ServletConfiguration>();
        // this.prefix = config.getInitParameter("prefix");
        // this.invokerPrefix = config.getInitParameter("invokerPrefix");
    }

    /**
     * Destroy any mounted instances we might be holding, then destroy myself
     */
    @Override
    public void destroy() {
        if (this.mountedInstances != null) {
            synchronized (this.mountedInstancesSemaphore) {
                for (Iterator<ServletConfiguration> i = this.mountedInstances.values().iterator(); i.hasNext();) {
                    ((ServletConfiguration) i.next()).destroy();
                }
                this.mountedInstances.clear();
            }
        }
        this.mountedInstances = null;
        // this.prefix = null;
        // this.invokerPrefix = null;
    }

    /**
     * Get an instance of the servlet configuration object
     */
    protected ServletConfiguration getInvokableInstance(final String servletName) throws ServletException, IOException {
        ServletConfiguration sc = null;
        synchronized (this.mountedInstancesSemaphore) {
            if (this.mountedInstances.containsKey(servletName)) {
                sc = this.mountedInstances.get(servletName);
            }
        }

        if (sc == null) {
            // If found, mount an instance
            try {
                // Class servletClass = Class.forName(servletName, true,
                // Thread.currentThread().getContextClassLoader());
                sc = new ServletConfiguration((WebAppConfiguration) this.getServletContext(), getServletConfig().getServletName() + ":" + servletName, servletName, new HashMap<String, String>(), -1);
                this.mountedInstances.put(servletName, sc);
                logger.debug("{}: Mounting servlet class {}", servletName, getServletConfig().getServletName());
                // just to trigger the servlet.init()
                sc.ensureInitialization();
            } catch (Throwable err) {
                sc = null;
            }
        }
        return sc;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse rsp) throws ServletException, IOException {
        boolean isInclude = (req.getAttribute(INCLUDE_PATH_INFO) != null);
        // boolean isForward = (req.getAttribute(FORWARD_PATH_INFO) != null);
        String servletName = null;

        if (isInclude) {
            servletName = (String) req.getAttribute(INCLUDE_PATH_INFO);
        } // else if (isForward)
        // servletName = (String) req.getAttribute(FORWARD_PATH_INFO);
        else if (req.getPathInfo() != null) {
            servletName = req.getPathInfo();
        } else {
            servletName = "";
        }
        if (servletName.startsWith("/")) {
            servletName = servletName.substring(1);
        }
        ServletConfiguration invokedServlet = getInvokableInstance(servletName);

        if (invokedServlet == null) {
            String msg = "There was no invokable servlet found matching the URL: " + servletName;
            logger.warn(msg);
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
        } else {
            SimpleRequestDispatcher rd = new SimpleRequestDispatcher((WebAppConfiguration) getServletContext(), invokedServlet);
            rd.setForNamedDispatcher(new Mapping[0], new Mapping[0]);
            rd.forward(req, rsp);
        }
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse rsp) throws ServletException, IOException {
        doGet(req, rsp);
    }
}
