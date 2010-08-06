/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletResponse;
import net.winstone.core.WinstoneConstant;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Node;

/**
 * This is the one that keeps a specific servlet instance's config, as well as holding the instance itself.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ServletConfiguration.java,v 1.16 2007/04/23 02:55:35 rickknowles Exp $
 */
public class ServletConfiguration implements javax.servlet.ServletConfig, Comparable<ServletConfiguration> {

    protected static org.slf4j.Logger logger = LoggerFactory.getLogger(ServletConfiguration.class);
    private static final transient String ELEM_NAME = "servlet-name";
    //private static final transient String ELEM_DISPLAY_NAME = "display-name";
    private static final transient String ELEM_CLASS = "servlet-class";
    private static final transient String ELEM_JSP_FILE = "jsp-file";
    //private static final transient String ELEM_DESCRIPTION = "description";
    private static final transient String ELEM_INIT_PARAM = "init-param";
    private static final transient String ELEM_INIT_PARAM_NAME = "param-name";
    private static final transient String ELEM_INIT_PARAM_VALUE = "param-value";
    private static final transient String ELEM_LOAD_ON_STARTUP = "load-on-startup";
    private static final transient String ELEM_RUN_AS = "run-as";
    private static final transient String ELEM_SECURITY_ROLE_REF = "security-role-ref";
    private static final transient String ELEM_ROLE_NAME = "role-name";
    private static final transient String ELEM_ROLE_LINK = "role-link";
    private static final transient String JSP_FILE = "org.apache.catalina.jsp_file";
    
    private String servletName;
    private String className;
    private Servlet instance;
    private final Map<String, String> initParams;
    private final WebAppConfiguration webAppConfig;
    private int loadOnStartup;
    private String jspFile;
    // private String runAsRole;
    private final Map<String, String> securityRoleRefs;
    /** runtime memeber */
    private final Object servletSemaphore = Boolean.TRUE;
    private boolean isSingleThreadModel = false;
    private boolean unavailable = false;
    private Throwable unavailableException = null;

    protected ServletConfiguration(final WebAppConfiguration webAppConfig) {
        this.webAppConfig = webAppConfig;
        this.initParams = new HashMap<String, String>();
        this.loadOnStartup = -1;
        this.securityRoleRefs = new HashMap<String, String>();
    }

    public ServletConfiguration(final WebAppConfiguration webAppConfig, final String servletName, final String className, final Map<String, String> initParams, final int loadOnStartup) {
        this(webAppConfig);
        if (initParams != null) {
            this.initParams.putAll(initParams);
        }
        this.servletName = servletName;
        this.className = className;
        this.jspFile = null;
        this.loadOnStartup = loadOnStartup;
    }

    public ServletConfiguration(final WebAppConfiguration webAppConfig, final Node elm) {
        this(webAppConfig);

        // Parse the web.xml file entry
        for (int n = 0; n < elm.getChildNodes().getLength(); n++) {
            Node child = elm.getChildNodes().item(n);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String nodeName = child.getNodeName();

            // Construct the servlet instances
            if (nodeName.equals(ELEM_NAME)) {
                this.servletName = WebAppConfiguration.getTextFromNode(child);
            } else if (nodeName.equals(ELEM_CLASS)) {
                this.className = WebAppConfiguration.getTextFromNode(child);
            } else if (nodeName.equals(ELEM_JSP_FILE)) {
                this.jspFile = WebAppConfiguration.getTextFromNode(child);
            } else if (nodeName.equals(ELEM_LOAD_ON_STARTUP)) {
                String index = child.getFirstChild() == null ? "-1" : WebAppConfiguration.getTextFromNode(child);
                this.loadOnStartup = Integer.parseInt(index);
            } else if (nodeName.equals(ELEM_INIT_PARAM)) {
                String paramName = "";
                String paramValue = "";
                for (int k = 0; k < child.getChildNodes().getLength(); k++) {
                    Node paramNode = child.getChildNodes().item(k);
                    if (paramNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    } else if (paramNode.getNodeName().equals(ELEM_INIT_PARAM_NAME)) {
                        paramName = WebAppConfiguration.getTextFromNode(paramNode);
                    } else if (paramNode.getNodeName().equals(ELEM_INIT_PARAM_VALUE)) {
                        paramValue = WebAppConfiguration.getTextFromNode(paramNode);
                    }
                }
                if (!paramName.equals("")) {
                    this.initParams.put(paramName, paramValue);
                }
            } else if (nodeName.equals(ELEM_RUN_AS)) {
                for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                    Node roleElm = child.getChildNodes().item(m);
                    if ((roleElm.getNodeType() == Node.ELEMENT_NODE) && (roleElm.getNodeName().equals(ELEM_ROLE_NAME))) {
                        // this.runAsRole = WebAppConfiguration.getTextFromNode(roleElm); // not used
                    }
                }
            } else if (nodeName.equals(ELEM_SECURITY_ROLE_REF)) {
                String name = "";
                String link = "";
                for (int k = 0; k < child.getChildNodes().getLength(); k++) {
                    Node roleRefNode = child.getChildNodes().item(k);
                    if (roleRefNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    } else if (roleRefNode.getNodeName().equals(ELEM_ROLE_NAME)) {
                        name = WebAppConfiguration.getTextFromNode(roleRefNode);
                    } else if (roleRefNode.getNodeName().equals(ELEM_ROLE_LINK)) {
                        link = WebAppConfiguration.getTextFromNode(roleRefNode);
                    }
                }
                if (!name.equals("") && !link.equals("")) {
                    this.initParams.put(name, link);
                }
            }
        }

        if ((this.jspFile != null) && (this.className == null)) {
            this.className = WinstoneConstant.JSP_SERVLET_CLASS;
            WebAppConfiguration.addJspServletParams(this.initParams);
        }
        logger.debug("Loaded servlet instance {} class: {}", this.servletName, this.className);
    }

    @Override
    public String getInitParameter(String name) {
        return initParams.get(name);
    }

    @Override
    public Enumeration<?> getInitParameterNames() {
        return Collections.enumeration(initParams.keySet());
    }

    @Override
    public ServletContext getServletContext() {
        return this.webAppConfig;
    }

    @Override
    public String getServletName() {
        return this.servletName;
    }

    public void ensureInitialization() {

        if (this.instance != null) {
            return; // already init'd
        }

        synchronized (this.servletSemaphore) {

            if (this.instance != null) {
                return; // already init'd
            }

            // Check if we were decommissioned while blocking
            if (this.unavailableException != null) {
                return;
            }

            // If no instance, class load, then call init()
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());

            Servlet newInstance = null;
            Throwable otherError = null;
            try {
                Class<?> servletClass = Class.forName(className, true, this.webAppConfig.getLoader());
                newInstance = (Servlet) servletClass.newInstance();
                this.isSingleThreadModel = Class.forName("javax.servlet.SingleThreadModel").isInstance(newInstance);

                // Initialise with the correct classloader
                logger.debug("{}: init", this.servletName);
                newInstance.init(this);
                this.instance = newInstance;
            } catch (ClassNotFoundException err) {
                logger.warn("Failed to load class: {}", this.className, err);
                setUnavailable(newInstance);
                this.unavailableException = err;
            } catch (IllegalAccessException err) {
                logger.warn("Failed to load class: {}", this.className, err);
                setUnavailable(newInstance);
                this.unavailableException = err;
            } catch (InstantiationException err) {
                logger.warn("Failed to load class: {}", this.className, err);
                setUnavailable(newInstance);
                this.unavailableException = err;
            } catch (ServletException err) {
                logger.warn("Failed to initialise servlet {}", this.servletName, err);
                this.instance = null; // so that we don't call the destroy method
                setUnavailable(newInstance);
                this.unavailableException = err;
            } catch (RuntimeException err) {
                otherError = err;
                throw err;
            } catch (Error err) {
                otherError = err;
                throw err;
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
                if ((otherError == null) && (this.unavailableException == null)) {
                    this.instance = newInstance;
                }
            }
        }
        return;
    }

    public void execute(ServletRequest request, ServletResponse response, String requestURI) throws ServletException, IOException {

        ensureInitialization();

        // If init failed, return 500 error
        if (this.unavailable) {
            // ((HttpServletResponse) response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            // resources.getString("StaticResourceServlet.PathNotFound", requestURI));
            SimpleRequestDispatcher rd = this.webAppConfig.getErrorDispatcherByClass(this.unavailableException);
            rd.forward(request, response);
            return;
        }

        if (this.jspFile != null) {
            request.setAttribute(JSP_FILE, this.jspFile);
        }

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());

        try {
            if (this.isSingleThreadModel) {
                synchronized (this) {
                    this.instance.service(request, response);
                }
            } else {
                this.instance.service(request, response);
            }
        } catch (UnavailableException err) {
            // catch locally and rethrow as a new ServletException, so
            // we only invalidate the throwing servlet
            setUnavailable(this.instance);
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND, "File " + requestURI + " not found");
            // throw new ServletException(resources.getString(
            // "SimpleRequestDispatcher.ForwardError"), err);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    public int getLoadOnStartup() {
        return this.loadOnStartup;
    }

    public Map<String, String> getSecurityRoleRefs() {
        return this.securityRoleRefs;
    }

    /**
     * This was included so that the servlet instances could be sorted on their loadOnStartup values. Otherwise used.
     */
    @Override
    public int compareTo(ServletConfiguration objTwo) {
        Integer one = new Integer(this.loadOnStartup);
        Integer two = new Integer(objTwo.loadOnStartup);
        return one.compareTo(two);
    }

    /**
     * Called when it's time for the container to shut this servlet down.
     */
    public void destroy() {
        synchronized (this.servletSemaphore) {
            setUnavailable(this.instance);
        }
    }

    protected void setUnavailable(Servlet unavailableServlet) {

        this.unavailable = true;
        if (unavailableServlet != null) {
            logger.debug("{}: destroy", this.servletName);
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            try {
                unavailableServlet.destroy();
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
                this.instance = null;
            }
        }

        // remove from webapp
        this.webAppConfig.removeServletConfigurationAndMappings(this);
    }
}
