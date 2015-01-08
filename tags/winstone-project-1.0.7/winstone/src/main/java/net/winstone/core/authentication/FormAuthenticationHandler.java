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

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.winstone.core.WebAppConfiguration;
import net.winstone.core.WinstoneRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * Handles FORM based authentication configurations. Fairly simple ... it just
 * redirects any unauthorized requests to the login page, and any bad logins to
 * the error page. The auth values are stored in the session in a special slot.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: FormAuthenticationHandler.java,v 1.7 2006/12/13 14:07:43
 *          rickknowles Exp $
 */
public class FormAuthenticationHandler extends BaseAuthenticationHandler {

	protected static Logger logger = LoggerFactory.getLogger(FormAuthenticationHandler.class);
	private static final transient String ELEM_FORM_LOGIN_CONFIG = "form-login-config";
	private static final transient String ELEM_FORM_LOGIN_PAGE = "form-login-page";
	private static final transient String ELEM_FORM_ERROR_PAGE = "form-error-page";
	private static final transient String FORM_ACTION = "j_security_check";
	private static final transient String FORM_USER = "j_username";
	private static final transient String FORM_PASS = "j_password";
	private static final transient String AUTHENTICATED_USER = "winstone.auth.FormAuthenticationHandler.AUTHENTICATED_USER";
	private static final transient String CACHED_REQUEST = "winstone.auth.FormAuthenticationHandler.CACHED_REQUEST";
	private String loginPage;
	private String errorPage;

	/**
	 * Constructor for the FORM authenticator
	 * 
	 * @param realm
	 *            The realm against which we are authenticating
	 * @param constraints
	 *            The array of security constraints that might apply
	 * @param resources
	 *            The list of resource strings for messages
	 * @param realmName
	 *            The name of the realm this handler claims
	 */
	@SuppressWarnings("rawtypes")
	public FormAuthenticationHandler(final Node loginConfigNode, final List constraintNodes, final Set rolesAllowed, final AuthenticationRealm realm) {
		super(loginConfigNode, constraintNodes, rolesAllowed, realm);

		for (int n = 0; n < loginConfigNode.getChildNodes().getLength(); n++) {
			final Node loginElm = loginConfigNode.getChildNodes().item(n);
			if (loginElm.getNodeName().equals(FormAuthenticationHandler.ELEM_FORM_LOGIN_CONFIG)) {
				for (int k = 0; k < loginElm.getChildNodes().getLength(); k++) {
					final Node formElm = loginElm.getChildNodes().item(k);
					if (formElm.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					} else if (formElm.getNodeName().equals(FormAuthenticationHandler.ELEM_FORM_LOGIN_PAGE)) {
						loginPage = WebAppConfiguration.getTextFromNode(formElm);
					} else if (formElm.getNodeName().equals(FormAuthenticationHandler.ELEM_FORM_ERROR_PAGE)) {
						errorPage = WebAppConfiguration.getTextFromNode(formElm);
					}
				}
			}
		}
		FormAuthenticationHandler.logger.debug("FormAuthenticationHandler initialised for realm: {}", realmName);
	}

	/**
	 * Evaluates any authentication constraints, intercepting if auth is
	 * required. The relevant authentication handler subclass's logic is used to
	 * actually authenticate.
	 * 
	 * @return A boolean indicating whether to continue after this request
	 */
	@Override
	public boolean processAuthentication(final ServletRequest request, final ServletResponse response, final String pathRequested) throws IOException, ServletException {
		if (pathRequested.equals(loginPage) || pathRequested.equals(errorPage)) {
			return Boolean.TRUE;
		} else {
			return super.processAuthentication(request, response, pathRequested);
		}
	}

	/**
	 * Call this once we know that we need to authenticate
	 */
	@Override
	protected void requestAuthentication(final HttpServletRequest request, final HttpServletResponse response, final String pathRequested) throws ServletException, IOException {
		// Save the critical details of the request into the session map
		ServletRequest unwrapped = request;
		while (unwrapped instanceof HttpServletRequestWrapper) {
			unwrapped = ((HttpServletRequestWrapper) unwrapped).getRequest();
		}
		final HttpSession session = request.getSession(Boolean.TRUE);
		session.setAttribute(FormAuthenticationHandler.CACHED_REQUEST, new RetryRequestParams(unwrapped));

		// Forward on to the login page
		FormAuthenticationHandler.logger.debug("Forwarding to the login page");
		final javax.servlet.RequestDispatcher rdLogin = request.getRequestDispatcher(loginPage);
		setNoCache(response);
		rdLogin.forward(request, response);
	}

	/**
	 * Check the response - is it a response to the login page ?
	 * 
	 * @return A boolean indicating whether to continue with the request or not
	 */
	@Override
	protected boolean validatePossibleAuthenticationResponse(HttpServletRequest request, final HttpServletResponse response, final String pathRequested) throws ServletException, IOException {
		// Check if this is a j_security_check uri
		if (pathRequested.endsWith(FormAuthenticationHandler.FORM_ACTION)) {
			final String username = request.getParameter(FormAuthenticationHandler.FORM_USER);
			final String password = request.getParameter(FormAuthenticationHandler.FORM_PASS);

			// Send to error page if invalid
			final AuthenticationPrincipal principal = realm.authenticateByUsernamePassword(username, password);
			if (principal == null) {
				final javax.servlet.RequestDispatcher rdError = request.getRequestDispatcher(errorPage);
				rdError.forward(request, response);
			} // Send to stashed request
			else {
				// Iterate back as far as we can
				ServletRequest wrapperCheck = request;
				while (wrapperCheck instanceof HttpServletRequestWrapper) {
					wrapperCheck = ((HttpServletRequestWrapper) wrapperCheck).getRequest();
				}

				// Get the stashed request
				WinstoneRequest actualRequest = null;
				if (wrapperCheck instanceof WinstoneRequest) {
					actualRequest = (WinstoneRequest) wrapperCheck;
					actualRequest.setRemoteUser(principal);
				} else {
					FormAuthenticationHandler.logger.warn("Request type invalid - can't set authenticated user in request class: {}", wrapperCheck.getClass().getName());
				}
				final HttpSession session = request.getSession(Boolean.TRUE);
				String previousLocation = loginPage;
				final RetryRequestParams cachedRequest = (RetryRequestParams) session.getAttribute(FormAuthenticationHandler.CACHED_REQUEST);
				if ((cachedRequest != null) && (actualRequest != null)) {
					// Repopulate this request from the params we saved
					request = new RetryRequestWrapper(request, cachedRequest);
					previousLocation = (request.getServletPath() == null ? "" : request.getServletPath()) + (request.getPathInfo() == null ? "" : request.getPathInfo());
				} else {
					FormAuthenticationHandler.logger.debug("No cached request - redirecting to the login page.");
				}

				// do role check, since we don't know that this user has
				// permission
				if (doRoleCheck(request, response, previousLocation)) {
					principal.setAuthType(HttpServletRequest.FORM_AUTH);
					session.setAttribute(FormAuthenticationHandler.AUTHENTICATED_USER, principal);
					final javax.servlet.RequestDispatcher rdPrevious = request.getRequestDispatcher(previousLocation);
					rdPrevious.forward(request, response);
				} else {
					final javax.servlet.RequestDispatcher rdError = request.getRequestDispatcher(errorPage);
					rdError.forward(request, response);
				}
			}
			return Boolean.FALSE;
		} // If it's not a login, get the session, and look up the auth user
			// variable
		else {
			WinstoneRequest actualRequest = null;
			if (request instanceof WinstoneRequest) {
				actualRequest = (WinstoneRequest) request;
			} else if (request instanceof HttpServletRequestWrapper) {
				final HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
				if (wrapper.getRequest() instanceof WinstoneRequest) {
					actualRequest = (WinstoneRequest) wrapper.getRequest();
				} else {
					FormAuthenticationHandler.logger.warn("Request type invalid - can't set authenticated user in request class: {}", wrapper.getRequest().getClass().getName());
				}
			} else {
				FormAuthenticationHandler.logger.warn("Request type invalid - can't set authenticated user in request class: {}", request.getClass().getName());
			}

			final HttpSession session = actualRequest.getSession(Boolean.FALSE);
			if (session != null) {
				final AuthenticationPrincipal authenticatedUser = (AuthenticationPrincipal) session.getAttribute(FormAuthenticationHandler.AUTHENTICATED_USER);
				if (authenticatedUser != null) {
					actualRequest.setRemoteUser(authenticatedUser);
					FormAuthenticationHandler.logger.debug("Got authenticated user from session");
				}
			}
			return Boolean.TRUE;
		}
	}
}
