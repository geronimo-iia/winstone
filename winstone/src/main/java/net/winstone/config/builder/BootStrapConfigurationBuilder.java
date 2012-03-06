/**
 * 
 */
package net.winstone.config.builder;

import java.io.File;

import net.winstone.config.BootStrapConfiguration;

/**
 * BootStrapConfigurationBuilder.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class BootStrapConfigurationBuilder {
	private String config = "winstone.properties";

	private String javaHome = null;

	private String toolsJar = null;

	private File commonLibFolder = null;

	private Boolean useJasper = Boolean.FALSE;

	/**
	 * Build a new instance of BootStrapConfigurationBuilder.
	 */
	public BootStrapConfigurationBuilder() {
		super();
	}

	/**
	 * @param config
	 *            the config to set
	 */
	public BootStrapConfigurationBuilder setConfig(String config) {
		this.config = config;
		return this;
	}

	/**
	 * @param javaHome
	 *            the javaHome to set
	 */
	public BootStrapConfigurationBuilder setJavaHome(String javaHome) {
		this.javaHome = javaHome;
		return this;
	}

	/**
	 * @param toolsJar
	 *            the toolsJar to set
	 */
	public BootStrapConfigurationBuilder setToolsJar(String toolsJar) {
		this.toolsJar = toolsJar;
		return this;
	}

	/**
	 * @param commonLibFolder
	 *            the commonLibFolder to set
	 */
	public BootStrapConfigurationBuilder setCommonLibFolder(File commonLibFolder) {
		this.commonLibFolder = commonLibFolder;
		return this;
	}

	/**
	 * @param useJasper
	 *            the useJasper to set
	 */
	public BootStrapConfigurationBuilder setUseJasper(Boolean useJasper) {
		this.useJasper = useJasper;
		return this;
	}

	public BootStrapConfiguration build() {
		return new BootStrapConfiguration(config, javaHome, toolsJar, commonLibFolder, useJasper);
	}

}
