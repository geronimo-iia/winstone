/**
 * 
 */
package net.winstone.config.builder;

import net.winstone.config.AddressConfiguration;

/**
 * Ajp13ConfigurationListener.
 * 
 * @author JGT
 * 
 */
public class Ajp13ConfigurationListener {
	protected int port = -1;
	protected String address = null;

	/**
	 * Build a new instance of Ajp13ConfigurationListener.
	 */
	public Ajp13ConfigurationListener() {
		super();
	}

	public Ajp13ConfigurationListener setAddress(final int port, final String address) {
		this.port = port;
		this.address = address;
		return this;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public final Ajp13ConfigurationListener setPort(final int port) {
		this.port = port;
		return this;
	}

	/**
	 * @param address
	 *            the address to set
	 */
	public final Ajp13ConfigurationListener setAddress(final String address) {
		this.address = address;
		return this;
	}
	
	
	public AddressConfiguration Ajp13Configuration() {
		return new AddressConfiguration(port, address);
	}
}
