/**
 * 
 */
package net.winstone.config;

import java.io.Serializable;

/**
 * AddressConfiguration.
 * 
* @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class AddressConfiguration implements Serializable {

	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = 3088393476849694363L;
	private final int port;
	private final String address;

	/**
	 * Build a new instance of AddressConfiguration.
	 * 
	 * @param port
	 * @param address
	 */
	public AddressConfiguration(int port, String address) {
		super();
		this.port = port;
		this.address = address;
	}

	/**
	 * @return true id enabled.
	 */
	public final Boolean isEnabled() {
		return port != -1;
	}

	/**
	 * @return the port
	 */
	public final int getPort() {
		return port;
	}

	/**
	 * @return the address
	 */
	public final String getAddress() {
		return address;
	}

}
