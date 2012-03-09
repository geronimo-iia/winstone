/**
 * 
 */
package net.winstone.config.builder;

import net.winstone.config.BootStrapConfiguration;

/**
 * BootStrapConfigurationBuilder.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class BootStrapConfigurationBuilder {
	private String config = "winstone.properties";

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
	 * @param useJasper
	 *            the useJasper to set
	 */
	public BootStrapConfigurationBuilder setUseJasper(Boolean useJasper) {
		this.useJasper = useJasper;
		return this;
	}

	public BootStrapConfiguration build() {
		return new BootStrapConfiguration(config, useJasper);
	}

}
