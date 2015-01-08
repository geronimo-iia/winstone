/**
 * 
 */
package net.winstone.config.builder;

import net.winstone.config.ControlConfiguration;

/**
 * AddressConfigurationBuilder.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class ControlConfigurationBuilder extends CompositeBuilder {

	protected int port = -1;
	protected String address = null;

	/**
	 * Build a new instance of ControlConfigurationBuilder.
	 * 
	 * @param builder
	 */
	public ControlConfigurationBuilder(ServerConfigurationBuilder builder) {
		super(builder);
	}

	public ControlConfigurationBuilder setAddress(final int port, final String address) {
		this.port = port;
		this.address = address;
		return this;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public final ControlConfigurationBuilder setPort(final int port) {
		this.port = port;
		return this;
	}

	/**
	 * @param address
	 *            the address to set
	 */
	public final ControlConfigurationBuilder setAddress(final String address) {
		this.address = address;
		return this;
	}

	/**
	 * @see net.winstone.config.builder.CompositeBuilder#build()
	 */
	@Override
	public ServerConfigurationBuilder build() {
		return builder.setControlConfiguration(new ControlConfiguration(port, address));
	}
}
