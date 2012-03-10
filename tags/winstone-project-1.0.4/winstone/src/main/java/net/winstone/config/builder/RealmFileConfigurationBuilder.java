/**
 * 
 */
package net.winstone.config.builder;

import java.io.File;

import net.winstone.config.RealmFileConfiguration;
import net.winstone.core.authentication.realm.FileRealm;

/**
 * RealmFileConfigurationBuilder.
 * 
 * @author JGT
 * 
 */
public class RealmFileConfigurationBuilder {
	private RealmConfigurationBuilder builder;
	private File configFile = null;

	/**
	 * Build a new instance of RealmFileConfigurationBuilder.
	 * 
	 * @param builder
	 */
	public RealmFileConfigurationBuilder(RealmConfigurationBuilder builder) {
		super();
		this.builder = builder;
	}

	public ServerConfigurationBuilder build() {
		return builder.setRealmConfiguration(new RealmFileConfiguration(FileRealm.class.getName(), configFile)).build();
	}

	/**
	 * @param configFile
	 *            the configFile to set
	 */
	public RealmFileConfigurationBuilder setConfigFile(File configFile) {
		this.configFile = configFile;
		return this;
	}

}
