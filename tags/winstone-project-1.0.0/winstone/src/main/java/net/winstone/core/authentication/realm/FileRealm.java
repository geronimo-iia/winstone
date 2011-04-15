/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core.authentication.realm;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.winstone.WinstoneException;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import net.winstone.core.authentication.AuthenticationPrincipal;
import net.winstone.core.authentication.AuthenticationRealm;

/**
 * @author rickk
 * @version $Id: FileRealm.java,v 1.4 2006/08/30 04:07:52 rickknowles Exp $
 */
public class FileRealm implements AuthenticationRealm {

    protected static org.slf4j.Logger logger = LoggerFactory.getLogger(FileRealm.class);
    private final static transient String FILE_NAME_ARGUMENT = "fileRealm.configFile";
    private final static transient String DEFAULT_FILE_NAME = "users.xml";
    private final static transient String ELEM_USER = "user";
    private final static transient String ATT_USERNAME = "username";
    private final static transient String ATT_PASSWORD = "password";
    private final static transient String ATT_ROLELIST = "roles";
    private final Map<String, String> passwords;
    private final Map<String, List<String>> roles;

    /**
     * Constructor - this sets up an authentication realm, using the file supplied on the command line as a source of
     * userNames/passwords/roles.
     */
    public FileRealm(final Set<String> rolesAllowed, final Map<String, String> args) {
        this.passwords = new HashMap<String, String>();
        this.roles = new HashMap<String, List<String>>();

        // Get the filename and parse the xml doc
        String realmFileName = args.get(FILE_NAME_ARGUMENT) == null ? DEFAULT_FILE_NAME : (String) args.get(FILE_NAME_ARGUMENT);
        File realmFile = new File(realmFileName);
        if (!realmFile.exists()) {
            throw new WinstoneException("FileRealm could not locate the user file " + realmFile.getPath() + " - disabling security");
        }
        try {
            InputStream inFile = new FileInputStream(realmFile);
            Document doc = this.parseStreamToXML(inFile);
            inFile.close();
            Node rootElm = doc.getDocumentElement();
            for (int n = 0; n < rootElm.getChildNodes().getLength(); n++) {
                Node child = rootElm.getChildNodes().item(n);

                if ((child.getNodeType() == Node.ELEMENT_NODE) && (child.getNodeName().equals(ELEM_USER))) {
                    String userName = null;
                    String password = null;
                    String roleList = null;
                    // Loop through for attributes
                    for (int j = 0; j < child.getAttributes().getLength(); j++) {
                        Node thisAtt = child.getAttributes().item(j);
                        if (thisAtt.getNodeName().equals(ATT_USERNAME)) {
                            userName = thisAtt.getNodeValue();
                        } else if (thisAtt.getNodeName().equals(ATT_PASSWORD)) {
                            password = thisAtt.getNodeValue();
                        } else if (thisAtt.getNodeName().equals(ATT_ROLELIST)) {
                            roleList = thisAtt.getNodeValue();
                        }
                    }

                    if ((userName == null) || (password == null) || (roleList == null)) {
                        logger.debug("Skipping user {} - details were incomplete", userName);
                    } else {
                        // Parse the role list into an array and sort it
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
                        this.passwords.put(userName, password);
                        this.roles.put(userName, Arrays.asList(roleArray));
                    }
                }
            }
            logger.debug("FileRealm initialised: users:" + this.passwords.size());
        } catch (java.io.IOException err) {
            throw new WinstoneException("Error loading FileRealm", err);
        }
    }

    /**
     * Get a parsed XML DOM from the given inputstream. Used to process the web.xml application deployment descriptors.
     */
    private Document parseStreamToXML(final InputStream in) {
        try {
            // Use JAXP to create a document builder
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setExpandEntityReferences(false);
            factory.setValidating(false);
            factory.setNamespaceAware(false);
            factory.setIgnoringComments(true);
            factory.setCoalescing(true);
            factory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(in);
        } catch (Throwable errParser) {
            throw new WinstoneException("Error parsing the users XML document", errParser);
        }
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
        return new AuthenticationPrincipal(userName, (String) this.passwords.get(userName), (List<String>) this.roles.get(userName));
    }
}
