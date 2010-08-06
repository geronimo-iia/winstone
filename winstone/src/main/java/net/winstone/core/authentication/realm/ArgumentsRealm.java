/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core.authentication.realm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.slf4j.LoggerFactory;

import winstone.WebAppConfiguration;
import net.winstone.core.authentication.AuthenticationPrincipal;
import net.winstone.core.authentication.AuthenticationRealm;

/**
 * Base class for authentication realms. Subclasses provide the source of authentication roles, usernames, passwords, etc, and when asked
 * for validation respond with a role if valid, or null otherwise.
 * 
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ArgumentsRealm.java,v 1.4 2007/06/01 15:55:41 rickknowles Exp $
 */
public class ArgumentsRealm implements AuthenticationRealm {

    protected static org.slf4j.Logger logger = LoggerFactory.getLogger(ArgumentsRealm.class);
    private static final transient String PASSWORD_PREFIX = "argumentsRealm.passwd.";
    private static final transient String ROLES_PREFIX = "argumentsRealm.roles.";
    private final Map<String, String> passwords;
    private final Map<String, List<String>> roles;

    /**
     * Constructor - this sets up an authentication realm, using the arguments supplied on the command line as a source of
     * userNames/passwords/roles.
     */
    public ArgumentsRealm(final Set<String> rolesAllowed, final Map<String, String> args) {
        this.passwords = new HashMap<String, String>();
        this.roles = new HashMap<String, List<String>>();
        for (Iterator<String> i = args.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            if (key.startsWith(PASSWORD_PREFIX)) {
                String userName = key.substring(PASSWORD_PREFIX.length());
                String password = (String) args.get(key);

                String roleList = WebAppConfiguration.stringArg(args, ROLES_PREFIX + userName, "");
                if (roleList.equals("")) {
                    logger.warn("WARNING: No roles detected in configuration for user {}", userName);
                } else {
                    StringTokenizer st = new StringTokenizer(roleList, ",");
                    List<String> rl = new ArrayList<String>();
                    for (; st.hasMoreTokens();) {
                        String currentRole = st.nextToken();
                        if (rolesAllowed.contains(currentRole)) {
                            rl.add(currentRole);
                        }
                    }
                    String[] roleArray = (String[]) rl.toArray();
                    Arrays.sort(roleArray);
                    this.roles.put(userName, Arrays.asList(roleArray));
                }
                this.passwords.put(userName, password);
            }
        }
        logger.debug("ArgumentsRealm initialised: users: " + this.passwords.size());
    }

    /**
     * Authenticate the user - do we know them ? Return a principal once we know them
     */
    @Override
    public AuthenticationPrincipal authenticateByUsernamePassword(final String userName, final String password) {
        if ((userName == null) || (password == null)) {
            return null;
        }

        String realPassword = (String) this.passwords.get(userName);
        if (realPassword == null) {
            return null;
        } else if (!realPassword.equals(password)) {
            return null;
        } else {
            return new AuthenticationPrincipal(userName, password, (List<String>) this.roles.get(userName));
        }
    }

    /**
     * Retrieve an authenticated user
     */
    @Override
    public AuthenticationPrincipal retrieveUser(final String userName) {
        if (userName == null) {
            return null;
        } else {
            return new AuthenticationPrincipal(userName, (String) this.passwords.get(userName), (List<String>) this.roles.get(userName));
        }
    }
}
