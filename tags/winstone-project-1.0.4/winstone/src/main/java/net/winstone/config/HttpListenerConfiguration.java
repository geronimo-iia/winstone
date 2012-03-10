/**
 * 
 */
package net.winstone.config;

/**
 * HttpListenerConfiguration.
 * 
* @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class HttpListenerConfiguration extends AddressConfiguration {
	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = 3804815852233764910L;
	/**
	 * Enable or not host name lookup.
	 */
	protected final Boolean hostnameLookup;

	/**
	 * Build a new instance of HttpListenerConfiguration.
	 * 
	 * @param port
	 * @param address
	 * @param enableHostnameLookup
	 */
	public HttpListenerConfiguration(int port, String address, Boolean enableHostnameLookup) {
		super(port, address);
		this.hostnameLookup = enableHostnameLookup;
	}

	/**
	 * @return the doHostnameLookup
	 */
	public final Boolean isHostnameLookupEnabled() {
		return hostnameLookup;
	}

}
