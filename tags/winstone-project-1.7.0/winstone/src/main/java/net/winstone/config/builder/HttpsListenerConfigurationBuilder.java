/**
 * 
 */
package net.winstone.config.builder;

import java.io.File;

import net.winstone.config.HttpsListenerConfiguration;

/**
 * HttpsListenerConfigurationBuilder.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class HttpsListenerConfigurationBuilder extends CompositeBuilder {
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
	 * Build a new instance of HttpsListenerConfigurationBuilder.
	 * 
	 * @param builder
	 */
	public HttpsListenerConfigurationBuilder(ServerConfigurationBuilder builder) {
		super(builder);
	}

	public HttpsListenerConfigurationBuilder setAddress(final int port, final String address) {
		this.port = port;
		this.address = address;
		return this;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public final HttpsListenerConfigurationBuilder setPort(final int port) {
		this.port = port;
		return this;
	}

	/**
	 * @param address
	 *            the address to set
	 */
	public final HttpsListenerConfigurationBuilder setAddress(final String address) {
		this.address = address;
		return this;
	}

	public final HttpsListenerConfigurationBuilder enableHostnameLookups() {
		hostnameLookups = Boolean.TRUE;
		return this;
	}

	public final HttpsListenerConfigurationBuilder disableHostnameLookups() {
		hostnameLookups = Boolean.FALSE;
		return this;
	}

	/**
	 * @param keyStore
	 *            the keyStore to set
	 */
	public final HttpsListenerConfigurationBuilder setKeyStore(final File keyStore) {
		this.keyStore = keyStore;
		return this;
	}

	/**
	 * @param keyStorePassword
	 *            the keyStorePassword to set
	 */
	public final HttpsListenerConfigurationBuilder setKeyStorePassword(final String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
		return this;
	}

	/**
	 * @param keyManagerType
	 *            the keyManagerType to set
	 */
	public final HttpsListenerConfigurationBuilder setKeyManagerType(final String keyManagerType) {
		this.keyManagerType = keyManagerType;
		return this;
	}

	/**
	 * @param verifyClient
	 *            the verifyClient to set
	 */
	public final HttpsListenerConfigurationBuilder setVerifyClient(final Boolean verifyClient) {
		this.verifyClient = verifyClient;
		return this;
	}

	/**
	 * @param certificate
	 *            the certificate to set
	 */
	public final HttpsListenerConfigurationBuilder setCertificate(final File certificate) {
		this.certificate = certificate;
		return this;
	}

	/**
	 * @param privateKey
	 *            the privateKey to set
	 */
	public final HttpsListenerConfigurationBuilder setPrivateKey(final File privateKey) {
		this.privateKey = privateKey;
		return this;
	}

	/**
	 * @see net.winstone.config.builder.CompositeBuilder#build()
	 */
	@Override
	public ServerConfigurationBuilder build() {
		return builder.setHttpsListenerConfiguration(new HttpsListenerConfiguration(port, keyManagerType, hostnameLookups, keyStore, keyStorePassword, keyManagerType, verifyClient, certificate, privateKey));
	}
}
