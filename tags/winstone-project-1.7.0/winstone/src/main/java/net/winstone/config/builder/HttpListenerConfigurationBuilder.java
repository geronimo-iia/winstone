/**
 * 
 */
package net.winstone.config.builder;

import net.winstone.config.HttpListenerConfiguration;

/**
 * HttpListenerConfigurationBuilder.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class HttpListenerConfigurationBuilder extends CompositeBuilder {
	protected int port = 8080;
	protected String address = null;
	protected Boolean hostnameLookups = Boolean.FALSE;

	/**
	 * Build a new instance of HttpListenerConfigurationBuilder.
	 * 
	 * @param builder
	 * @param port
	 * @param address
	 * @param hostnameLookups
	 */
	public HttpListenerConfigurationBuilder(ServerConfigurationBuilder builder) {
		super(builder);
	}

	public HttpListenerConfigurationBuilder setAddress(final int port, final String address) {
		this.port = port;
		this.address = address;
		return this;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public final HttpListenerConfigurationBuilder setPort(final int port) {
		this.port = port;
		return this;
	}

	/**
	 * @param address
	 *            the address to set
	 */
	public final HttpListenerConfigurationBuilder setAddress(final String address) {
		this.address = address;
		return this;
	}

	public final HttpListenerConfigurationBuilder enableHostnameLookups() {
		hostnameLookups = Boolean.TRUE;
		return this;
	}

	public final HttpListenerConfigurationBuilder disableHostnameLookups() {
		hostnameLookups = Boolean.FALSE;
		return this;
	}

	/**
	 * @see net.winstone.config.builder.CompositeBuilder#build()
	 */
	@Override
	public ServerConfigurationBuilder build() {
		return builder.setHttpListenerConfiguration(new HttpListenerConfiguration(port, address, hostnameLookups));
	}
}
