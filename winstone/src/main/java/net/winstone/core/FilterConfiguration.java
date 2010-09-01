/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;

import net.winstone.WinstoneException;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Node;

/**
 * Corresponds to a filter object in the web app. Holds one instance only.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public class FilterConfiguration implements javax.servlet.FilterConfig {

    protected static org.slf4j.Logger logger = LoggerFactory.getLogger(FilterConfiguration.class);
    
    private final String ELEM_NAME = "filter-name";
    //private final String ELEM_DISPLAY_NAME = "display-name";
    private final String ELEM_CLASS = "filter-class";
    //private final String ELEM_DESCRIPTION = "description";
    private final String ELEM_INIT_PARAM = "init-param";
    private final String ELEM_INIT_PARAM_NAME = "param-name";
    private final String ELEM_INIT_PARAM_VALUE = "param-value";
    private final Map<String, String> initParameters = new HashMap<String, String>();
    private final ServletContext context;
    private final ClassLoader loader;
    private final Object filterSemaphore = Boolean.TRUE;
    private boolean unavailableException = Boolean.FALSE;
    private String filterName;
    private String classFile;
    private Filter instance;

    /**
     * Constructor
     */
    public FilterConfiguration(final ServletContext context, final ClassLoader loader, final Node elm) {
        this.context = context;
        this.loader = loader;

        // Parse the web.xml file entry
        for (int n = 0; n < elm.getChildNodes().getLength(); n++) {
            Node child = elm.getChildNodes().item(n);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String nodeName = child.getNodeName();

            // Construct the servlet instances
            if (nodeName.equals(ELEM_NAME)) {
                this.filterName = WebAppConfiguration.getTextFromNode(child);
            } else if (nodeName.equals(ELEM_CLASS)) {
                this.classFile = WebAppConfiguration.getTextFromNode(child);
            } else if (nodeName.equals(ELEM_INIT_PARAM)) {
                String paramName = null;
                String paramValue = null;
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
                if ((paramName != null) && (paramValue != null)) {
                    this.initParameters.put(paramName, paramValue);
                }
            }
        }
        logger.debug("Loaded filter instance {} class: {}]", this.filterName, this.classFile);
    }

    @Override
    public String getFilterName() {
        return this.filterName;
    }

    @Override
    public String getInitParameter(String paramName) {
        return (String) this.initParameters.get(paramName);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(this.initParameters.keySet());
    }

    @Override
    public ServletContext getServletContext() {
        return this.context;
    }

    /**
     * Implements the first-time-init of an instance, and wraps it in a dispatcher.
     */
    public Filter getFilter() throws ServletException {
        synchronized (this.filterSemaphore) {
            if (isUnavailable()) {
                throw new WinstoneException("This filter has been marked unavailable because of an earlier error");
            } else if (this.instance == null) {
                try {
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(this.loader);

                    Class<?> filterClass = Class.forName(classFile, true, this.loader);
                    this.instance = (Filter) filterClass.newInstance();
                    logger.debug("{}: init", filterName);

                    // Initialise with the correct classloader
                    this.instance.init(this);
                    Thread.currentThread().setContextClassLoader(cl);
                } catch (ClassNotFoundException err) {
                    logger.error("Failed to load class: " + classFile, err);
                } catch (IllegalAccessException err) {
                    logger.error("Failed to load class: " + classFile, err);
                } catch (InstantiationException err) {
                    logger.error("Failed to load class: " + classFile, err);
                } catch (ServletException err) {
                    this.instance = null;
                    if (err instanceof UnavailableException) {
                        setUnavailable();
                    }
                    throw err;
                }
            }
        }
        return this.instance;
    }

    /**
     * Called when it's time for the container to shut this servlet down.
     */
    public void destroy() {
        synchronized (this.filterSemaphore) {
            setUnavailable();
        }
    }

    @Override
    public String toString() {
        return "FilterConfiguration[filterName=" + filterName + ", classFile=" + classFile + ']';
    }

    public boolean isUnavailable() {
        return this.unavailableException;
    }

    protected void setUnavailable() {
        this.unavailableException = true;
        if (this.instance != null) {
            logger.debug("{}: destroy", filterName);
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.loader);
            this.instance.destroy();
            Thread.currentThread().setContextClassLoader(cl);
            this.instance = null;
        }
    }

    public void execute(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.loader);
        try {
            getFilter().doFilter(request, response, chain);
        } catch (UnavailableException err) {
            setUnavailable();
            throw new ServletException("Error in filter - marking unavailable", err);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((filterName == null) ? 0 : filterName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FilterConfiguration other = (FilterConfiguration) obj;
        if (filterName == null) {
            if (other.filterName != null) {
                return false;
            }
        } else if (!filterName.equals(other.filterName)) {
            return false;
        }
        return true;
    }
}
