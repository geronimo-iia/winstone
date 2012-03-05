/**
 * 
 */
package net.winstone.config.builder;

import net.winstone.config.Ajp13ListenerConfiguration;

/**
 * Ajp13ListenerConfigurationBuilder.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class Ajp13ListenerConfigurationBuilder extends CompositeBuilder {

	protected int port = -1;
	protected String address = null;

	/**
	 * Build a new instance of Ajp13ListenerConfigurationBuilder.
	 * 
	 * @param builder
	 */
	public Ajp13ListenerConfigurationBuilder(ServerConfigurationBuilder builder) {
		super(builder);
	}

	/**
	 * Set Address.
	 * @param port
	 * @param address
	 * @return
	 */
	public Ajp13ListenerConfigurationBuilder setAddress(final int port, final String address) {
		this.port = port;
		this.address = address;
		return this;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public final Ajp13ListenerConfigurationBuilder setPort(final int port) {
		this.port = port;
		return this;
	}

	/**
	 * @param address
	 *            the address to set
	 */
	public final Ajp13ListenerConfigurationBuilder setAddress(final String address) {
		this.address = address;
		return this;
	}

	/**
	 * @see net.winstone.config.builder.CompositeBuilder#build()
	 */
	@Override
	public ServerConfigurationBuilder build() {
		return builder.setAjp13ListenerAddressConfiguration(new Ajp13ListenerConfiguration(port, address));
	}
}
