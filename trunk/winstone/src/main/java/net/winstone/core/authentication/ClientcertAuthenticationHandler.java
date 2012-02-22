/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core.authentication;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import net.winstone.core.WinstoneRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ClientcertAuthenticationHandler.java,v 1.3 2006/02/28 07:32:47
 *          rickknowles Exp $
 */
public final class ClientcertAuthenticationHandler extends BaseAuthenticationHandler {

	protected static Logger logger = LoggerFactory.getLogger(ClientcertAuthenticationHandler.class);

	@SuppressWarnings("rawtypes")
	public ClientcertAuthenticationHandler(final Node loginConfigNode, final List constraintNodes, final Set rolesAllowed, final AuthenticationRealm realm) {
		super(loginConfigNode, constraintNodes, rolesAllowed, realm);
		ClientcertAuthenticationHandler.logger.debug("ClientcertAuthenticationHandler initialised for realm: {}", realmName);
	}

	/**
	 * Call this once we know that we need to authenticate
	 */
	@Override
	protected void requestAuthentication(final HttpServletRequest request, final HttpServletResponse response, final String pathRequested) throws IOException {
		// Return unauthorized, and set the realm name
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "This content can only be viewed by authorized users.");
	}

	/**
	 * Handling the (possible) response
	 */
	@Override
	protected boolean validatePossibleAuthenticationResponse(final HttpServletRequest request, final HttpServletResponse response, final String pathRequested) throws IOException { // Check
																																													// for
																																													// certificates
																																													// in
																																													// the
																																													// request
																																													// attributes
		final X509Certificate certificateArray[] = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
		if ((certificateArray != null) && (certificateArray.length > 0)) {
			boolean failed = false;
			for (int n = 0; n < certificateArray.length; n++) {
				try {
					certificateArray[n].checkValidity();
				} catch (final Throwable err) {
					failed = true;
				}
			}
			if (!failed) {
				final AuthenticationPrincipal principal = realm.retrieveUser(certificateArray[0].getSubjectDN().getName());
				if (principal != null) {
					principal.setAuthType(HttpServletRequest.CLIENT_CERT_AUTH);
					if (request instanceof WinstoneRequest) {
						((WinstoneRequest) request).setRemoteUser(principal);
					} else if (request instanceof HttpServletRequestWrapper) {
						final HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
						if (wrapper.getRequest() instanceof WinstoneRequest) {
							((WinstoneRequest) wrapper.getRequest()).setRemoteUser(principal);
						} else {
							ClientcertAuthenticationHandler.logger.warn("Request type invalid - can't set authenticated user in request class: {}", wrapper.getRequest().getClass().getName());
						}
					} else {
						ClientcertAuthenticationHandler.logger.warn("Request type invalid - can't set authenticated user in request class: {}", request.getClass().getName());
					}
				}
			}
		}
		return true;
	}
}
