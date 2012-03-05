/**
 * 
 */
package net.winstone.config.builder;

/**
 * AddressConfigurationBuilder.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class AddressConfigurationBuilder {
	protected int port = -1;
	protected String address = null;

	/**
	 * Build a new instance of AddressConfigurationBuilder.
	 */
	public AddressConfigurationBuilder() {
		super();
	}

	public AddressConfigurationBuilder setAddress(final int port, final String address) {
		this.port = port;
		this.address = address;
		return this;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public final AddressConfigurationBuilder setPort(final int port) {
		this.port = port;
		return this;
	}

	/**
	 * @param address
	 *            the address to set
	 */
	public final AddressConfigurationBuilder setAddress(final String address) {
		this.address = address;
		return this;
	}

}
