/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.auth;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import net.winstone.util.Base64;

import org.w3c.dom.Node;

import winstone.Logger;
import net.winstone.core.WinstoneRequest;

/**
 * Handles HTTP basic authentication.
 * 
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: BasicAuthenticationHandler.java,v 1.5 2007/04/11 13:14:26 rickknowles Exp $
 */
public class BasicAuthenticationHandler extends BaseAuthenticationHandler {
    public BasicAuthenticationHandler(Node loginConfigNode,
            List constraintNodes, Set rolesAllowed,
            AuthenticationRealm realm) {
        super(loginConfigNode, constraintNodes, rolesAllowed, realm);
        Logger.log(Logger.DEBUG, AUTH_RESOURCES,
                "BasicAuthenticationHandler.Initialised", realmName);
    }

    /**
     * Call this once we know that we need to authenticate
     */
    protected void requestAuthentication(HttpServletRequest request,
            HttpServletResponse response, String pathRequested)
            throws IOException {
        // Return unauthorized, and set the realm name
        response.setHeader("WWW-Authenticate", "Basic Realm=\""
                + this.realmName + "\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, AUTH_RESOURCES
                .getString("BasicAuthenticationHandler.UnauthorizedMessage"));
    }

    /**
     * Handling the (possible) response
     */
    protected boolean validatePossibleAuthenticationResponse(
            HttpServletRequest request, HttpServletResponse response,
            String pathRequested) throws IOException {
        String authorization = request.getHeader("Authorization");
        if ((authorization != null)
                && authorization.toLowerCase().startsWith("basic")) {
            
            char[] inBytes = authorization.substring(5).trim().toCharArray();
            byte[] outBytes = new byte[(int) (inBytes.length * 0.75f)]; // always mod 4 = 0
            int length = Base64.decode(inBytes, outBytes, 0, inBytes.length, 0);

            String decoded = new String(outBytes, 0, length);
            int delimPos = decoded.indexOf(':');
            if (delimPos != -1) {
                AuthenticationPrincipal principal = this.realm
                        .authenticateByUsernamePassword(decoded.substring(0,
                                delimPos).trim(), decoded.substring(
                                delimPos + 1).trim());
                if (principal != null) {
                    principal.setAuthType(HttpServletRequest.BASIC_AUTH);
                    if (request instanceof WinstoneRequest)
                        ((WinstoneRequest) request).setRemoteUser(principal);
                    else if (request instanceof HttpServletRequestWrapper) {
                        HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
                        if (wrapper.getRequest() instanceof WinstoneRequest)
                            ((WinstoneRequest) wrapper.getRequest())
                                    .setRemoteUser(principal);
                        else
                            Logger.log(Logger.WARNING, AUTH_RESOURCES,
                                    "BasicAuthenticationHandler.CantSetUser",
                                    wrapper.getRequest().getClass().getName());
                    } else
                        Logger.log(Logger.WARNING, AUTH_RESOURCES,
                                "BasicAuthenticationHandler.CantSetUser",
                                request.getClass().getName());
                }
            }
        }
        return true;
    }

}
