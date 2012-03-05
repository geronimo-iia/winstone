/**
 * 
 */
package net.winstone.config;

/**
 * ControlConfiguration.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class ControlConfiguration extends AddressConfiguration {
	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = -7265773222974448024L;

	/**
	 * Build a new instance of ControlConfiguration.
	 * 
	 * @param port
	 * @param address
	 */
	public ControlConfiguration(int port, String address) {
		super(port, address);
	}

}
