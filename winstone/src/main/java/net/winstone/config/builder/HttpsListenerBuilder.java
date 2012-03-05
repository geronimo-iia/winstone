/**
 * 
 */
package net.winstone.config.builder;

import java.io.File;

import net.winstone.config.HttpsListenerConfiguration;

/**
 * HttpsListenerBuilder.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class HttpsListenerBuilder {
	protected int port = -1;
	protected String address = null;
	protected Boolean hostnameLookups = Boolean.FALSE;

	protected File keyStore = null;

	protected String keyStorePassword = null;

	protected String keyManagerType = null;

	protected Boolean verifyClient = Boolean.FALSE;

	protected File certificate = null;

	protected File privateKey = null;

	/**
	 * Build a new instance of HttpsListenerBuilder.
	 */
	public HttpsListenerBuilder() {
		super();
	}

	public HttpsListenerBuilder setAddress(final int port, final String address) {
		this.port = port;
		this.address = address;
		return this;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public final HttpsListenerBuilder setPort(final int port) {
		this.port = port;
		return this;
	}

	/**
	 * @param address
	 *            the address to set
	 */
	public final HttpsListenerBuilder setAddress(final String address) {
		this.address = address;
		return this;
	}

	public final HttpsListenerBuilder enableHostnameLookups() {
		hostnameLookups = Boolean.TRUE;
		return this;
	}

	public final HttpsListenerBuilder disableHostnameLookups() {
		hostnameLookups = Boolean.FALSE;
		return this;
	}

	/**
	 * @param keyStore
	 *            the keyStore to set
	 */
	public final HttpsListenerBuilder setKeyStore(final File keyStore) {
		this.keyStore = keyStore;
		return this;
	}

	/**
	 * @param keyStorePassword
	 *            the keyStorePassword to set
	 */
	public final HttpsListenerBuilder setKeyStorePassword(final String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
		return this;
	}

	/**
	 * @param keyManagerType
	 *            the keyManagerType to set
	 */
	public final HttpsListenerBuilder setKeyManagerType(final String keyManagerType) {
		this.keyManagerType = keyManagerType;
		return this;
	}

	/**
	 * @param verifyClient
	 *            the verifyClient to set
	 */
	public final HttpsListenerBuilder setVerifyClient(final Boolean verifyClient) {
		this.verifyClient = verifyClient;
		return this;
	}

	/**
	 * @param certificate
	 *            the certificate to set
	 */
	public final HttpsListenerBuilder setCertificate(final File certificate) {
		this.certificate = certificate;
		return this;
	}

	/**
	 * @param privateKey
	 *            the privateKey to set
	 */
	public final HttpsListenerBuilder setPrivateKey(final File privateKey) {
		this.privateKey = privateKey;
		return this;
	}

	public HttpsListenerConfiguration getHttpsListenerConfiguration() {
		return new HttpsListenerConfiguration(port, keyManagerType, hostnameLookups, keyStore, keyStorePassword, keyManagerType, verifyClient, certificate, privateKey);
	}
}
