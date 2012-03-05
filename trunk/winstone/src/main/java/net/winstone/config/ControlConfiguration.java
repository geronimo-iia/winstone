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
public class ControlConfiguration {

	/**
	 * set the shutdown/control port. -1 to disable, Default disabled
	 */
	private final int controlPort;

	/**
	 * set the ip address which the control listener should bind to, Default is
	 * local host.
	 */
	private final String controlAddress;

	/**
	 * Build a new instance of ControlConfiguration.
	 * @param controlPort
	 * @param controlAddress
	 */
	public ControlConfiguration(int controlPort, String controlAddress) {
		super();
		this.controlPort = controlPort;
		this.controlAddress = controlAddress;
	}

	/**
	 * @return the controlPort
	 */
	public final int getControlPort() {
		return controlPort;
	}

	/**
	 * @return the controlAddress
	 */
	public final String getControlAddress() {
		return controlAddress;
	}
	
	
}
