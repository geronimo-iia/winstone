/**
 * 
 */
package net.winstone.config;

import java.io.Serializable;

/**
 * BootStrapConfiguration.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class BootStrapConfiguration implements Serializable {
	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = -1467434771504163391L;

	/**
	 * load configuration properties from here. Default is ./winstone.properties
	 */
	private final String config;

	/**
	 * enable jasper JSP handling (true/false) Default is false.
	 */
	private final Boolean useJasper;

	/**
	 * Build a new instance of BootStrapConfiguration.
	 * 
	 * @param config
	 * @param javaHome
	 * @param toolsJar
	 * @param commonLibFolder
	 * @param useJasper
	 */
	public BootStrapConfiguration(final String config, final Boolean useJasper) {
		super();
		this.config = config;
		this.useJasper = useJasper;
	}

	/**
	 * @return the config
	 */
	public String getConfig() {
		return config;
	}

	/**
	 * @return the useJasper
	 */
	public Boolean getUseJasper() {
		return useJasper;
	}

}
