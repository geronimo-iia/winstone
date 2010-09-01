/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core.authentication;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Node;

import net.winstone.core.Mapping;
import net.winstone.core.WebAppConfiguration;

/**
 * Models a restriction on a particular set of resources in the webapp.
 * 
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: SecurityConstraint.java,v 1.7 2006/08/10 06:38:30 rickknowles Exp $
 */
public final class SecurityConstraint {

    protected static Logger logger = LoggerFactory.getLogger(SecurityConstraint.class);
    private final static transient String ELEM_DISPLAY_NAME = "display-name";
    private final static transient String ELEM_WEB_RESOURCES = "web-resource-collection";
    // private final static transient String ELEM_WEB_RESOURCE_NAME = "web-resource-name";
    private final static transient String ELEM_URL_PATTERN = "url-pattern";
    private final static transient String ELEM_HTTP_METHOD = "http-method";
    private final static transient String ELEM_AUTH_CONSTRAINT = "auth-constraint";
    private final static transient String ELEM_ROLE_NAME = "role-name";
    private final static transient String ELEM_USER_DATA_CONSTRAINT = "user-data-constraint";
    private final static transient String ELEM_TRANSPORT_GUARANTEE = "transport-guarantee";
    private final static transient String GUARANTEE_NONE = "NONE";
    private String displayName;
    private String methodSets[];
    private Mapping urlPatterns[];
    private String rolesAllowed[];
    private boolean needsSSL;

    /**
     * Constructor
     */
    public SecurityConstraint(final Node elm, final Set rolesAllowed, final int counter) {
        super();
        this.needsSSL = false;
        Set localUrlPatternList = new HashSet();
        Set localMethodSetList = new HashSet();
        Set localRolesAllowed = new HashSet();

        for (int i = 0; i < elm.getChildNodes().getLength(); i++) {
            Node child = elm.getChildNodes().item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            } else if (child.getNodeName().equals(ELEM_DISPLAY_NAME)) {
                this.displayName = WebAppConfiguration.getTextFromNode(child);
            } else if (child.getNodeName().equals(ELEM_WEB_RESOURCES)) {
                String methodSet = null;

                // Parse the element and extract
                for (int k = 0; k < child.getChildNodes().getLength(); k++) {
                    Node resourceChild = child.getChildNodes().item(k);
                    if (resourceChild.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    String resourceChildNodeName = resourceChild.getNodeName();
                    if (resourceChildNodeName.equals(ELEM_URL_PATTERN)) {
                        localUrlPatternList.add(Mapping.createFromURL(
                                "Security", WebAppConfiguration.getTextFromNode(resourceChild)));
                    } else if (resourceChildNodeName.equals(ELEM_HTTP_METHOD)) {
                        methodSet = (methodSet == null ? "." : methodSet)
                                + WebAppConfiguration.getTextFromNode(resourceChild) + ".";
                    }
                }
                localMethodSetList.add(methodSet == null ? ".ALL." : methodSet);
            } else if (child.getNodeName().equals(ELEM_AUTH_CONSTRAINT)) {
                // Parse the element and extract
                for (int k = 0; k < child.getChildNodes().getLength(); k++) {
                    Node roleChild = child.getChildNodes().item(k);
                    if ((roleChild.getNodeType() != Node.ELEMENT_NODE)
                            || !roleChild.getNodeName().equals(ELEM_ROLE_NAME)) {
                        continue;
                    }
                    String roleName = WebAppConfiguration.getTextFromNode(roleChild);
                    if (roleName.equals("*")) {
                        localRolesAllowed.addAll(rolesAllowed);
                    } else {
                        localRolesAllowed.add(roleName);
                    }
                }
            } else if (child.getNodeName().equals(ELEM_USER_DATA_CONSTRAINT)) {
                // Parse the element and extract
                for (int k = 0; k < child.getChildNodes().getLength(); k++) {
                    Node roleChild = child.getChildNodes().item(k);
                    if ((roleChild.getNodeType() == Node.ELEMENT_NODE)
                            && roleChild.getNodeName().equals(ELEM_TRANSPORT_GUARANTEE)) {
                        this.needsSSL = !WebAppConfiguration.getTextFromNode(roleChild).equalsIgnoreCase(GUARANTEE_NONE);
                    }
                }
            }
        }
        this.urlPatterns = (Mapping[]) localUrlPatternList.toArray(new Mapping[0]);
        this.methodSets = (String[]) localMethodSetList.toArray(new String[0]);
        this.rolesAllowed = (String[]) localRolesAllowed.toArray(new String[0]);

        if (this.displayName == null) {
            this.displayName = "Security Constraint #" + counter;
        }
    }

    /**
     * Call this to evaluate the security constraint - is this operation allowed ?
     */
    public boolean isAllowed(final HttpServletRequest request) {
        for (int n = 0; n < this.rolesAllowed.length; n++) {
            if (request.isUserInRole(this.rolesAllowed[n])) {
                logger.debug("Passed security constraint: {} role: {}", this.displayName, this.rolesAllowed[n]);
                return true;
            }
        }
        logger.debug("Failed security constraint: {}", this.displayName);
        return false;
    }

    /**
     * Call this to evaluate the security constraint - is this constraint applicable to this url ?
     */
    public boolean isApplicable(final String url, final String method) {
        for (int n = 0; n < this.urlPatterns.length; n++) {
            if (this.urlPatterns[n].match(url, null, null)
                    && methodCheck(method, this.methodSets[n])) {
                return true;
            }
        }

        return false;
    }

    private boolean methodCheck(final String protocol, final String methodSet) {
        return methodSet.equals(".ALL.")
                || (methodSet.indexOf("." + protocol.toUpperCase() + ".") != -1);
    }

    public boolean needsSSL() {
        return this.needsSSL;
    }

    public String getName() {
        return this.displayName;
    }
}
