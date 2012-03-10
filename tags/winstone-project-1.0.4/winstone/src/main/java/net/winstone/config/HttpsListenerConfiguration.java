/**
 * 
 */
package net.winstone.config;

import java.io.File;

/**
 * HttpsListenerConfiguration.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class HttpsListenerConfiguration extends HttpListenerConfiguration {

	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = 8220099147201529114L;

	/**
	 * the location of the SSL KeyStore file
	 */
	private final File keyStore;
	/**
	 * the password for the SSL KeyStore file. Default is null
	 */
	private final String keyStorePassword;
	/**
	 * the SSL KeyManagerFactory type (eg SunX509, IbmX509). Default is SunX509.
	 */
	private final String keyManagerType;
	/**
	 * If true, request the client certificate ala "SSLVerifyClient require"
	 * Apache directive. Default is false
	 */
	private final Boolean verifyClient;
	/**
	 * Path of HTTPS certificate
	 */
	private final File certificate;
	/**
	 * Path of private key\
	 */
	private final File privateKey;

	/**
	 * Build a new instance of HttpsListenerConfiguration.
	 * 
	 * @param port
	 * @param address
	 * @param enableHostnameLookup
	 * @param keyStore
	 * @param keyStorePassword
	 * @param keyManagerType
	 * @param verifyClient
	 * @param certificate
	 * @param privateKey
	 */
	public HttpsListenerConfiguration(int port, String address, Boolean enableHostnameLookup, File keyStore, String keyStorePassword, String keyManagerType, Boolean verifyClient, File certificate, File privateKey) {
		super(port, address, enableHostnameLookup);
		this.keyStore = keyStore;
		this.keyStorePassword = keyStorePassword;
		this.keyManagerType = keyManagerType;
		this.verifyClient = verifyClient;
		this.certificate = certificate;
		this.privateKey = privateKey;
	}

	/**
	 * @return the keyStore
	 */
	public final File getKeyStore() {
		return keyStore;
	}

	/**
	 * @return the keyStorePassword
	 */
	public final String getKeyStorePassword() {
		return keyStorePassword;
	}

	/**
	 * @return the keyManagerType
	 */
	public final String getKeyManagerType() {
		return keyManagerType;
	}

	/**
	 * @return the verifyClient
	 */
	public final Boolean getVerifyClient() {
		return verifyClient;
	}

	/**
	 * @return the certificate
	 */
	public final File getCertificate() {
		return certificate;
	}

	/**
	 * @return the privateKey
	 */
	public final File getPrivateKey() {
		return privateKey;
	}

}
