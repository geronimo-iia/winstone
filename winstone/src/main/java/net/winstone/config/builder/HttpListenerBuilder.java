/**
 * 
 */
package net.winstone.config.builder;

import net.winstone.config.HttpListenerConfiguration;

/**
 * HttpListenerBuilder.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class HttpListenerBuilder {
	protected int port = -1;
	protected String address = null;
	protected Boolean hostnameLookups = Boolean.FALSE;

	/**
	 * Build a new instance of HttpListenerBuilder.
	 */
	public HttpListenerBuilder() {
		super();
	}

	public HttpListenerBuilder setAddress(final int port, final String address) {
		this.port = port;
		this.address = address;
		return this;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public final HttpListenerBuilder setPort(final int port) {
		this.port = port;
		return this;
	}

	/**
	 * @param address
	 *            the address to set
	 */
	public final HttpListenerBuilder setAddress(final String address) {
		this.address = address;
		return this;
	}

	public final HttpListenerBuilder enableHostnameLookups() {
		hostnameLookups = Boolean.TRUE;
		return this;
	}

	public final HttpListenerBuilder disableHostnameLookups() {
		hostnameLookups = Boolean.FALSE;
		return this;
	}

	public HttpListenerConfiguration getHttpListenerConfiguration() {
		return new HttpListenerConfiguration(port, address, hostnameLookups);
	}
}
