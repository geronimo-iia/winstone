/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core.listener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.winstone.WinstoneException;

import net.winstone.core.HostGroup;
import net.winstone.core.ObjectPool;
import net.winstone.core.WinstoneRequest;
import net.winstone.util.StringUtils;

/**
 * Implements the main listener daemon thread. This is the class that gets launched by the command line, and owns the server socket, etc.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HttpsListener.java,v 1.10 2007/06/13 15:27:35 rickknowles Exp $
 */
public class HttpsListener extends HttpListener {

    private static Logger logger = LoggerFactory.getLogger(HttpListener.class);
    private final String keystore;
    private final String password;
    private final String keyManagerType;

    /**
     * Constructor
     */
    public HttpsListener(final Map<String, String> args, final ObjectPool objectPool, final HostGroup hostGroup) throws IOException {
        super(args, objectPool, hostGroup);
        this.keystore = StringUtils.stringArg(args, getConnectorName() + "KeyStore", "winstone.ks");
        this.password = StringUtils.stringArg(args, getConnectorName() + "KeyStorePassword", null);
        this.keyManagerType = StringUtils.stringArg(args, getConnectorName() + "KeyManagerType", "SunX509");
    }

    /**
     * The default port to use - this is just so that we can override for the SSL connector.
     */
    @Override
    protected int getDefaultPort() {
        return -1; // https disabled by default
    }

    /**
     * The name to use when getting properties - this is just so that we can override for the SSL connector.
     */
    @Override
    protected String getConnectorScheme() {
        return "https";
    }

    /**
     * Gets a server socket - this gets as SSL socket instead of the standard socket returned in the base class.
     */
    @Override
    protected ServerSocket getServerSocket() throws IOException {
        // Just to make sure it's set before we start
        SSLContext context = getSSLContext(this.keystore, this.password);
        SSLServerSocketFactory factory = context.getServerSocketFactory();
        SSLServerSocket ss = (SSLServerSocket) (this.listenAddress == null ? factory.createServerSocket(this.listenPort, BACKLOG_COUNT) : factory.createServerSocket(this.listenPort, BACKLOG_COUNT, InetAddress.getByName(this.listenAddress)));
        ss.setEnableSessionCreation(true);
        ss.setWantClientAuth(true);
        return ss;
    }

    /**
     * Extracts the relevant socket stuff and adds it to the request object. This method relies on the base class for everything other than
     * SSL related attributes
     */
    @Override
    protected void parseSocketInfo(Socket socket, WinstoneRequest req) throws IOException {
        super.parseSocketInfo(socket, req);
        if (socket instanceof SSLSocket) {
            SSLSocket s = (SSLSocket) socket;
            SSLSession ss = s.getSession();
            if (ss != null) {
                Certificate certChain[] = null;
                try {
                    certChain = ss.getPeerCertificates();
                } catch (Throwable err) {/* do nothing */

                }

                if (certChain != null) {
                    req.setAttribute("javax.servlet.request.X509Certificate", certChain);
                    req.setAttribute("javax.servlet.request.cipher_suite", ss.getCipherSuite());
                    req.setAttribute("javax.servlet.request.ssl_session", new String(ss.getId()));
                    req.setAttribute("javax.servlet.request.key_size", getKeySize(ss.getCipherSuite()));
                }
            }
            req.setIsSecure(true);
        }
    }

    /**
     * Just a mapping of key sizes for cipher types. Taken indirectly from the TLS specs.
     */
    private Integer getKeySize(String cipherSuite) {
        if (cipherSuite.indexOf("_WITH_NULL_") != -1) {
            return new Integer(0);
        } else if (cipherSuite.indexOf("_WITH_IDEA_CBC_") != -1) {
            return new Integer(128);
        } else if (cipherSuite.indexOf("_WITH_RC2_CBC_40_") != -1) {
            return new Integer(40);
        } else if (cipherSuite.indexOf("_WITH_RC4_40_") != -1) {
            return new Integer(40);
        } else if (cipherSuite.indexOf("_WITH_RC4_128_") != -1) {
            return new Integer(128);
        } else if (cipherSuite.indexOf("_WITH_DES40_CBC_") != -1) {
            return new Integer(40);
        } else if (cipherSuite.indexOf("_WITH_DES_CBC_") != -1) {
            return new Integer(56);
        } else if (cipherSuite.indexOf("_WITH_3DES_EDE_CBC_") != -1) {
            return new Integer(168);
        } else {
            return null;
        }
    }

    /**
     * Used to get the base ssl context in which to create the server socket. This is basically just so we can have a custom location for
     * key stores.
     */
    public SSLContext getSSLContext(String keyStoreName, String password) throws IOException {
        try {
            // Check the key manager factory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(this.keyManagerType);

            File ksFile = new File(keyStoreName);
            if (!ksFile.exists() || !ksFile.isFile()) {
                throw new WinstoneException("No SSL key store found at " + ksFile.getPath());
            }
            InputStream in = new FileInputStream(ksFile);
            char[] passwordChars = password == null ? null : password.toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(in, passwordChars);
            kmf.init(ks, passwordChars);
            logger.debug("Keys/certificates found: {}", ks.size() + "");
            for (Enumeration<String> e = ks.aliases(); e.hasMoreElements();) {
                String alias = e.nextElement();
                logger.debug("Key: {} - {}", alias, ks.getCertificate(alias) + "");
            }

            SSLContext context = SSLContext.getInstance("SSL");
            context.init(kmf.getKeyManagers(), null, null);
            Arrays.fill(passwordChars, 'x');
            return context;
        } catch (IOException err) {
            throw err;
        } catch (Throwable err) {
            throw new WinstoneException("Error getting the SSL context object", err);
        }
    }
}
