/**
 * 
 */
package net.winstone.config;

import java.io.File;

/**
 * RealmFileConfiguration.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class RealmFileConfiguration extends RealmConfiguration {

	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = -7536778581583446266L;
	/**
	 * Configuration file. File containing users/passwds/roles. Only valid for
	 * the FileRealm realm class.
	 */
	private File configFile;

	/**
	 * Build a new instance of RealmFileConfiguration.
	 * 
	 * @param realmClassName
	 * @param configFile
	 */
	public RealmFileConfiguration(String realmClassName, File configFile) {
		super(realmClassName);
		this.configFile = configFile;
	}

	/**
	 * @return the configFile
	 */
	public File getConfigFile() {
		return configFile;
	}

}
