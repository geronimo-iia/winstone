/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core.authentication;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import net.winstone.core.WinstoneRequest;
import net.winstone.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * Handles HTTP basic authentication.
 * 
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: BasicAuthenticationHandler.java,v 1.5 2007/04/11 13:14:26
 *          rickknowles Exp $
 */
public final class BasicAuthenticationHandler extends BaseAuthenticationHandler {

	protected static Logger logger = LoggerFactory.getLogger(BasicAuthenticationHandler.class);

	@SuppressWarnings("rawtypes")
	public BasicAuthenticationHandler(final Node loginConfigNode, final List constraintNodes, final Set rolesAllowed, final AuthenticationRealm realm) {
		super(loginConfigNode, constraintNodes, rolesAllowed, realm);
		BasicAuthenticationHandler.logger.debug("BasicAuthenticationHandler initialised for realm: {}", realmName);
	}

	/**
	 * Call this once we know that we need to authenticate
	 */
	@Override
	protected void requestAuthentication(final HttpServletRequest request, final HttpServletResponse response, final String pathRequested) throws IOException {
		// Return unauthorized, and set the realm name
		response.setHeader("WWW-Authenticate", "Basic Realm=\"" + realmName + "\"");
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "This content can only be viewed by authorized users.");
	}

	/**
	 * Handling the (possible) response
	 */
	@Override
	protected boolean validatePossibleAuthenticationResponse(final HttpServletRequest request, final HttpServletResponse response, final String pathRequested) throws IOException {
		final String authorization = request.getHeader("Authorization");
		if ((authorization != null) && authorization.toLowerCase().startsWith("basic")) {

			final char[] inBytes = authorization.substring(5).trim().toCharArray();
			final byte[] outBytes = new byte[(int) (inBytes.length * 0.75f)]; // always
																				// mod
																				// 4
																				// =
																				// 0
			final int length = Base64.decode(inBytes, outBytes, 0, inBytes.length, 0);

			final String decoded = new String(outBytes, 0, length);
			final int delimPos = decoded.indexOf(':');
			if (delimPos != -1) {
				final AuthenticationPrincipal principal = realm.authenticateByUsernamePassword(decoded.substring(0, delimPos).trim(), decoded.substring(delimPos + 1).trim());
				if (principal != null) {
					principal.setAuthType(HttpServletRequest.BASIC_AUTH);
					if (request instanceof WinstoneRequest) {
						((WinstoneRequest) request).setRemoteUser(principal);
					} else if (request instanceof HttpServletRequestWrapper) {
						final HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
						if (wrapper.getRequest() instanceof WinstoneRequest) {
							((WinstoneRequest) wrapper.getRequest()).setRemoteUser(principal);
						} else {
							BasicAuthenticationHandler.logger.warn("Request type invalid - can't set authenticated user in request class: {}", wrapper.getRequest().getClass().getName());
						}
					} else {
						BasicAuthenticationHandler.logger.warn("Request type invalid - can't set authenticated user in request class: {}", request.getClass().getName());
					}
				}
			}
		}
		return Boolean.TRUE;
	}
}
