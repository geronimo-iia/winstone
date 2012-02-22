/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core.listener;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Enumeration;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import net.winstone.WinstoneException;
import net.winstone.core.HostGroup;
import net.winstone.core.ObjectPool;
import net.winstone.core.WinstoneRequest;
import net.winstone.util.Base64;
import net.winstone.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.x509.CertAndKeyGen;
import sun.security.x509.X500Name;

/**
 * Implements the main listener daemon thread. This is the class that gets
 * launched by the command line, and owns the server socket, etc.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HttpsListener.java,v 1.10 2007/06/13 15:27:35 rickknowles Exp $
 */
@SuppressWarnings("restriction")
public class HttpsListener extends HttpListener {

	private static Logger logger = LoggerFactory.getLogger(HttpListener.class);

	private final KeyStore keystore;
	private final char[] password;
	private final String keyManagerType;
	/**
	 * If true, request the client certificate ala "SSLVerifyClient require"
	 * Apache directive. If false, which is the default, don't do so.
	 * Technically speaking, there's the equivalent of
	 * "SSLVerifyClient optional", but IE doesn't recognize it and it always
	 * prompt the certificate chooser dialog box, so in practice it's useless.
	 * <p>
	 * See
	 * http://hudson.361315.n4.nabble.com/winstone-container-and-ssl-td383501
	 * .html for this failure mode in IE.
	 */
	private boolean performClientAuth;

	/**
	 * 
	 * Build a new instance of HttpsListener.
	 * 
	 * @param args
	 * @param objectPool
	 * @param hostGroup
	 * @throws IOException
	 */
	public HttpsListener(final Map<String, String> args, final ObjectPool objectPool, final HostGroup hostGroup) throws IOException {
		super(args, objectPool, hostGroup);
		if (listenPort < 0) {
			// not running HTTPS listener
			keystore = null;
			password = null;
			keyManagerType = null;
		} else {
			try {
				String pwd = StringUtils.stringArg(args, getConnectorName() + "KeyStorePassword", null);
				keyManagerType = StringUtils.stringArg(args, getConnectorName() + "KeyManagerType", "SunX509");
				performClientAuth = StringUtils.booleanArg(args, "httpsVerifyClient", false);

				final File opensslCert = StringUtils.fileArg(args, "httpsCertificate");
				final File opensslKey = StringUtils.fileArg(args, "httpsPrivateKey"); //
				final File keyStore = StringUtils.fileArg(args, "httpsKeyStore");

				if (((opensslCert != null) ^ (opensslKey != null))) {
					throw new WinstoneException("--httpsCertificate and --httpsPrivateKey need to be used together");
				}
				if ((keyStore != null) && (opensslKey != null)) {
					throw new WinstoneException("--httpsKeyStore and --httpsPrivateKey are mutually exclusive");
				}
				if (keyStore != null) {
					// load from Java style JKS
					if (!keyStore.exists() || !keyStore.isFile()) {
						throw new WinstoneException("No SSL key store found at " + keyStore.getPath());
					}
					password = pwd != null ? pwd.toCharArray() : null;
					keystore = KeyStore.getInstance("JKS");
					keystore.load(new FileInputStream(keyStore), password);
				} else if (opensslCert != null) {
					// load from openssl style key files
					final CertificateFactory cf = CertificateFactory.getInstance("X509");
					final Certificate cert = cf.generateCertificate(new FileInputStream(opensslCert));
					final PrivateKey key = HttpsListener.readPEMRSAPrivateKey(new FileReader(opensslKey));
					password = "changeit".toCharArray();
					keystore = KeyStore.getInstance("JKS");
					keystore.load(null);
					keystore.setKeyEntry("hudson", key, password, new Certificate[] { cert });
				} else {
					// use self-signed certificate
					password = "changeit".toCharArray();
					System.out.println("Using one-time self-signed certificate");
					final CertAndKeyGen ckg = new CertAndKeyGen("RSA", "SHA1WithRSA", null);
					ckg.generate(1024);
					final PrivateKey privKey = ckg.getPrivateKey();
					final X500Name xn = new X500Name("Test site", "Unknown", "Unknown", "Unknown");
					final X509Certificate cert = ckg.getSelfCertificate(xn, 3650L * 24 * 60 * 60);
					keystore = KeyStore.getInstance("JKS");
					keystore.load(null);
					keystore.setKeyEntry("hudson", privKey, password, new Certificate[] { cert });
				}
			} catch (final GeneralSecurityException e) {
				throw (IOException) new IOException("Failed to handle keys").initCause(e);
			}

		}
	}

	private static PrivateKey readPEMRSAPrivateKey(final Reader reader) throws IOException, GeneralSecurityException {
		// TODO: should have more robust format error handling
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			final BufferedReader r = new BufferedReader(reader);
			String line;
			boolean in = false;
			while ((line = r.readLine()) != null) {
				if (line.startsWith("-----")) {
					in = !in;
					continue;
				}
				if (in) {
					final char[] inBytes = line.toCharArray();
					final byte[] outBytes = new byte[(inBytes.length * 3) / 4];
					final int length = Base64.decode(inBytes, outBytes, 0, inBytes.length, 0);
					baos.write(outBytes, 0, length);
				}
			}
		} finally {
			reader.close();
		}

		final DerInputStream dis = new DerInputStream(baos.toByteArray());
		final DerValue[] seq = dis.getSequence(0);

		// int v = seq[0].getInteger();
		final BigInteger mod = seq[1].getBigInteger();
		// pubExpo
		final BigInteger privExpo = seq[3].getBigInteger();
		// p1, p2, exp1, exp2, crtCoef

		final KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(new RSAPrivateKeySpec(mod, privExpo));
	}

	/**
	 * The default port to use - this is just so that we can override for the
	 * SSL connector.
	 */
	@Override
	protected int getDefaultPort() {
		return -1; // https disabled by default
	}

	/**
	 * The name to use when getting properties - this is just so that we can
	 * override for the SSL connector.
	 */
	@Override
	protected String getConnectorScheme() {
		return "https";
	}

	/**
	 * Gets a server socket - this gets as SSL socket instead of the standard
	 * socket returned in the base class.
	 */
	@Override
	protected ServerSocket getServerSocket() throws IOException {
		// Just to make sure it's set before we start
		final SSLContext context = getSSLContext();
		final SSLServerSocketFactory factory = context.getServerSocketFactory();
		final SSLServerSocket ss = (SSLServerSocket) (listenAddress == null ? factory.createServerSocket(listenPort, HttpListener.BACKLOG_COUNT) : factory.createServerSocket(listenPort, HttpListener.BACKLOG_COUNT,
				InetAddress.getByName(listenAddress)));
		ss.setEnableSessionCreation(true);
		if (performClientAuth) {
			ss.setNeedClientAuth(true);
		}
		return ss;
	}

	/**
	 * Extracts the relevant socket stuff and adds it to the request object.
	 * This method relies on the base class for everything other than SSL
	 * related attributes
	 */
	@Override
	protected void parseSocketInfo(final Socket socket, final WinstoneRequest req) throws IOException {
		super.parseSocketInfo(socket, req);
		if (socket instanceof SSLSocket) {
			final SSLSocket s = (SSLSocket) socket;
			final SSLSession ss = s.getSession();
			if (ss != null) {
				Certificate certChain[] = null;
				try {
					certChain = ss.getPeerCertificates();
				} catch (final Throwable err) {/* do nothing */
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
	 * Just a mapping of key sizes for cipher types. Taken indirectly from the
	 * TLS specs.
	 */
	private Integer getKeySize(final String cipherSuite) {
		if (cipherSuite.indexOf("_WITH_NULL_") != -1) {
			return 0;
		} else if (cipherSuite.indexOf("_WITH_IDEA_CBC_") != -1) {
			return 128;
		} else if (cipherSuite.indexOf("_WITH_RC2_CBC_40_") != -1) {
			return 40;
		} else if (cipherSuite.indexOf("_WITH_RC4_40_") != -1) {
			return 40;
		} else if (cipherSuite.indexOf("_WITH_RC4_128_") != -1) {
			return 128;
		} else if (cipherSuite.indexOf("_WITH_DES40_CBC_") != -1) {
			return 40;
		} else if (cipherSuite.indexOf("_WITH_DES_CBC_") != -1) {
			return 56;
		} else if (cipherSuite.indexOf("_WITH_3DES_EDE_CBC_") != -1) {
			return 168;
		} else {
			return null;
		}
	}

	/**
	 * Used to get the base ssl context in which to create the server socket.
	 * This is basically just so we can have a custom location for key stores.
	 */
	public SSLContext getSSLContext() {
		try {
			// Check the key manager factory
			final KeyManagerFactory kmf = KeyManagerFactory.getInstance(keyManagerType);
			kmf.init(keystore, password);
			HttpsListener.logger.debug("Keys/certificates found: ", keystore.size() + "");
			for (final Enumeration<String> e = keystore.aliases(); e.hasMoreElements();) {
				final String alias = e.nextElement();
				HttpsListener.logger.debug("Keys : {} - {}", alias, keystore.getCertificate(alias) + "");
			}

			final SSLContext context = SSLContext.getInstance("SSL");
			context.init(kmf.getKeyManagers(), null, null);
			return context;
		} catch (final Throwable err) {
			throw new WinstoneException("Error getting the SSL context object", err);
		}
	}

}
