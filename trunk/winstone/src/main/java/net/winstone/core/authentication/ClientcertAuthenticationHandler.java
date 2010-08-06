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

import org.w3c.dom.Node;

import net.winstone.core.WinstoneRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ClientcertAuthenticationHandler.java,v 1.3 2006/02/28 07:32:47 rickknowles Exp $
 */
public final class ClientcertAuthenticationHandler extends BaseAuthenticationHandler {

    protected static Logger logger = LoggerFactory.getLogger(ClientcertAuthenticationHandler.class);

    public ClientcertAuthenticationHandler(Node loginConfigNode, List constraintNodes, Set rolesAllowed, AuthenticationRealm realm) {
        super(loginConfigNode, constraintNodes, rolesAllowed, realm);
        logger.debug("ClientcertAuthenticationHandler initialised for realm: {}", realmName);
    }

    /**
     * Call this once we know that we need to authenticate
     */
    @Override
    protected void requestAuthentication(HttpServletRequest request, HttpServletResponse response, String pathRequested) throws IOException {
        // Return unauthorized, and set the realm name
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "This content can only be viewed by authorized users.");
    }

    /**
     * Handling the (possible) response
     */
    @Override
    protected boolean validatePossibleAuthenticationResponse(HttpServletRequest request, HttpServletResponse response, String pathRequested) throws IOException {        // Check for certificates in the request attributes
        X509Certificate certificateArray[] = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        if ((certificateArray != null) && (certificateArray.length > 0)) {
            boolean failed = false;
            for (int n = 0; n < certificateArray.length; n++) {
                try {
                    certificateArray[n].checkValidity();
                } catch (Throwable err) {
                    failed = true;
                }
            }
            if (!failed) {
                AuthenticationPrincipal principal = this.realm.retrieveUser(certificateArray[0].getSubjectDN().getName());
                if (principal != null) {
                    principal.setAuthType(HttpServletRequest.CLIENT_CERT_AUTH);
                    if (request instanceof WinstoneRequest) {
                        ((WinstoneRequest) request).setRemoteUser(principal);
                    } else if (request instanceof HttpServletRequestWrapper) {
                        HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
                        if (wrapper.getRequest() instanceof WinstoneRequest) {
                            ((WinstoneRequest) wrapper.getRequest()).setRemoteUser(principal);
                        } else {
                            logger.warn("Request type invalid - can't set authenticated user in request class: {}", wrapper.getRequest().getClass().getName());
                        }
                    } else {
                        logger.warn("Request type invalid - can't set authenticated user in request class: {}", request.getClass().getName());
                    }
                }
            }
        }
        return true;
    }
}
