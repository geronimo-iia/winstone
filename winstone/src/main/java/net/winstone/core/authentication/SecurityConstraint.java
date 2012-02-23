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

import net.winstone.core.Mapping;
import net.winstone.core.WebAppConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * Models a restriction on a particular set of resources in the webapp.
 * 
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: SecurityConstraint.java,v 1.7 2006/08/10 06:38:30 rickknowles
 *          Exp $
 */
public final class SecurityConstraint {

	protected static Logger logger = LoggerFactory.getLogger(SecurityConstraint.class);
	private final static transient String ELEM_DISPLAY_NAME = "display-name";
	private final static transient String ELEM_WEB_RESOURCES = "web-resource-collection";
	// private final static transient String ELEM_WEB_RESOURCE_NAME =
	// "web-resource-name";
	private final static transient String ELEM_URL_PATTERN = "url-pattern";
	private final static transient String ELEM_HTTP_METHOD = "http-method";
	private final static transient String ELEM_AUTH_CONSTRAINT = "auth-constraint";
	private final static transient String ELEM_ROLE_NAME = "role-name";
	private final static transient String ELEM_USER_DATA_CONSTRAINT = "user-data-constraint";
	private final static transient String ELEM_TRANSPORT_GUARANTEE = "transport-guarantee";
	private final static transient String GUARANTEE_NONE = "NONE";
	private String displayName;
	private final String methodSets[];
	private final Mapping urlPatterns[];
	private final String rolesAllowed[];
	private boolean needsSSL;

	/**
	 * Constructor
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SecurityConstraint(final Node elm, final Set rolesAllowed, final int counter) {
		super();
		needsSSL = Boolean.FALSE;
		final Set localUrlPatternList = new HashSet();
		final Set localMethodSetList = new HashSet();
		final Set localRolesAllowed = new HashSet();

		for (int i = 0; i < elm.getChildNodes().getLength(); i++) {
			final Node child = elm.getChildNodes().item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			} else if (child.getNodeName().equals(SecurityConstraint.ELEM_DISPLAY_NAME)) {
				displayName = WebAppConfiguration.getTextFromNode(child);
			} else if (child.getNodeName().equals(SecurityConstraint.ELEM_WEB_RESOURCES)) {
				String methodSet = null;

				// Parse the element and extract
				for (int k = 0; k < child.getChildNodes().getLength(); k++) {
					final Node resourceChild = child.getChildNodes().item(k);
					if (resourceChild.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					final String resourceChildNodeName = resourceChild.getNodeName();
					if (resourceChildNodeName.equals(SecurityConstraint.ELEM_URL_PATTERN)) {
						localUrlPatternList.add(Mapping.createFromURL("Security", WebAppConfiguration.getTextFromNode(resourceChild)));
					} else if (resourceChildNodeName.equals(SecurityConstraint.ELEM_HTTP_METHOD)) {
						methodSet = (methodSet == null ? "." : methodSet) + WebAppConfiguration.getTextFromNode(resourceChild) + ".";
					}
				}
				localMethodSetList.add(methodSet == null ? ".ALL." : methodSet);
			} else if (child.getNodeName().equals(SecurityConstraint.ELEM_AUTH_CONSTRAINT)) {
				// Parse the element and extract
				for (int k = 0; k < child.getChildNodes().getLength(); k++) {
					final Node roleChild = child.getChildNodes().item(k);
					if ((roleChild.getNodeType() != Node.ELEMENT_NODE) || !roleChild.getNodeName().equals(SecurityConstraint.ELEM_ROLE_NAME)) {
						continue;
					}
					final String roleName = WebAppConfiguration.getTextFromNode(roleChild);
					if (roleName.equals("*")) {
						localRolesAllowed.addAll(rolesAllowed);
					} else {
						localRolesAllowed.add(roleName);
					}
				}
			} else if (child.getNodeName().equals(SecurityConstraint.ELEM_USER_DATA_CONSTRAINT)) {
				// Parse the element and extract
				for (int k = 0; k < child.getChildNodes().getLength(); k++) {
					final Node roleChild = child.getChildNodes().item(k);
					if ((roleChild.getNodeType() == Node.ELEMENT_NODE) && roleChild.getNodeName().equals(SecurityConstraint.ELEM_TRANSPORT_GUARANTEE)) {
						needsSSL = !WebAppConfiguration.getTextFromNode(roleChild).equalsIgnoreCase(SecurityConstraint.GUARANTEE_NONE);
					}
				}
			}
		}
		urlPatterns = (Mapping[]) localUrlPatternList.toArray(new Mapping[0]);
		methodSets = (String[]) localMethodSetList.toArray(new String[0]);
		this.rolesAllowed = (String[]) localRolesAllowed.toArray(new String[0]);

		if (displayName == null) {
			displayName = "Security Constraint #" + counter;
		}
	}

	/**
	 * Call this to evaluate the security constraint - is this operation allowed
	 * ?
	 */
	public boolean isAllowed(final HttpServletRequest request) {
		for (int n = 0; n < rolesAllowed.length; n++) {
			if (request.isUserInRole(rolesAllowed[n])) {
				SecurityConstraint.logger.debug("Passed security constraint: {} role: {}", displayName, rolesAllowed[n]);
				return Boolean.TRUE;
			}
		}
		SecurityConstraint.logger.debug("Failed security constraint: {}", displayName);
		return Boolean.FALSE;
	}

	/**
	 * Call this to evaluate the security constraint - is this constraint
	 * applicable to this url ?
	 */
	public boolean isApplicable(final String url, final String method) {
		for (int n = 0; n < urlPatterns.length; n++) {
			if (urlPatterns[n].match(url, null, null) && methodCheck(method, methodSets[n])) {
				return Boolean.TRUE;
			}
		}

		return Boolean.FALSE;
	}

	private boolean methodCheck(final String protocol, final String methodSet) {
		return methodSet.equals(".ALL.") || (methodSet.indexOf("." + protocol.toUpperCase() + ".") != -1);
	}

	public boolean needsSSL() {
		return needsSSL;
	}

	public String getName() {
		return displayName;
	}
}
