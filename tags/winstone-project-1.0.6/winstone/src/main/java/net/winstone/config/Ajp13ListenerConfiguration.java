/**
 * 
 */
package net.winstone.config;

/**
 * Ajp13ListenerConfiguration.
 * 
 *  @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class Ajp13ListenerConfiguration extends AddressConfiguration {

	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = -4100806360220081365L;

	/**
	 * Build a new instance of Ajp13ListenerConfiguration.
	 * 
	 * @param port
	 * @param address
	 */
	public Ajp13ListenerConfiguration(int port, String address) {
		super(port, address);
	}

}
