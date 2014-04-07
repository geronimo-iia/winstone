/**
 * 
 */
package net.winstone.config.builder;

import net.winstone.config.AddressConfiguration;

/**
 * ClusterNodeConfigurationBuilder.
 * 
 *@author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class ClusterNodeConfigurationBuilder {

	protected int port = -1;
	
	protected String address = null;

	private ClusterConfigurationBuilder builder;

	/**
	 * Build a new instance of ClusterNodeConfigurationBuilder.
	 * 
	 * @param builder
	 */
	public ClusterNodeConfigurationBuilder(ClusterConfigurationBuilder builder) {
		super();
		this.builder = builder;
	}

	/**
	 * 
	 * @param port
	 * @param address
	 * @return
	 */
	public ClusterNodeConfigurationBuilder setAddress(final int port, final String address) {
		this.port = port;
		this.address = address;
		return this;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public final ClusterNodeConfigurationBuilder setPort(final int port) {
		this.port = port;
		return this;
	}

	/**
	 * @param address
	 *            the address to set
	 */
	public final ClusterNodeConfigurationBuilder setAddress(final String address) {
		this.address = address;
		return this;
	}

	public ClusterConfigurationBuilder build() {
		return builder.addAddressConfiguration(new AddressConfiguration(port, address));
	}
}
