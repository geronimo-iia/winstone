/**
 * 
 */
package net.winstone.config;

import java.io.File;
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
	private String config;

	/**
	 * Override the JAVA_HOME variable
	 */
	private String javaHome;
	/**
	 * The location of tools.jar. Default is JAVA_HOME/lib/tools.jar
	 */
	private String toolsJar;

	/**
	 * folder for additional jar files. Default is ./lib
	 */
	private File commonLibFolder;

	/**
	 * enable jasper JSP handling (true/false) Default is false.
	 */
	private Boolean useJasper;

	/**
	 * Build a new instance of BootStrapConfiguration.
	 * 
	 * @param config
	 * @param javaHome
	 * @param toolsJar
	 * @param commonLibFolder
	 * @param useJasper
	 */
	public BootStrapConfiguration(String config, String javaHome, String toolsJar, File commonLibFolder, Boolean useJasper) {
		super();
		this.config = config;
		this.javaHome = javaHome;
		this.toolsJar = toolsJar;
		this.commonLibFolder = commonLibFolder;
		this.useJasper = useJasper;
	}

	/**
	 * @return the config
	 */
	public String getConfig() {
		return config;
	}

	/**
	 * @return the javaHome
	 */
	public String getJavaHome() {
		return javaHome;
	}

	/**
	 * @return the toolsJar
	 */
	public String getToolsJar() {
		return toolsJar;
	}

	/**
	 * @return the commonLibFolder
	 */
	public File getCommonLibFolder() {
		return commonLibFolder;
	}

	/**
	 * @return the useJasper
	 */
	public Boolean getUseJasper() {
		return useJasper;
	}

}
