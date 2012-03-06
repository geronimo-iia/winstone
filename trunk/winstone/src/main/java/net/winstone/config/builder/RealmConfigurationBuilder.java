/**
 * 
 */
package net.winstone.config.builder;

import java.util.ArrayList;

import net.winstone.config.RealmConfiguration;
import net.winstone.config.RealmFileConfiguration;
import net.winstone.config.RealmJDBCConfiguration;
import net.winstone.config.RealmMemoryConfiguration;
import net.winstone.config.RealmUserConfiguration;
import net.winstone.core.authentication.realm.ArgumentsRealm;

/**
 * RealmConfigurationBuilder.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class RealmConfigurationBuilder extends CompositeBuilder {

	protected RealmConfiguration realmConfiguration;

	protected RealmMemoryConfiguration realmMemoryConfiguration;

	protected RealmFileConfiguration realmFileConfiguration;

	protected RealmJDBCConfiguration realmJDBCConfiguration;

	/**
	 * Build a new instance of RealmConfigurationBuilder.
	 * 
	 * @param builder
	 */
	public RealmConfigurationBuilder(ServerConfigurationBuilder builder) {
		super(builder);
		realmMemoryConfiguration = new RealmMemoryConfiguration(ArgumentsRealm.class.getName(), new ArrayList<RealmUserConfiguration>());
		realmConfiguration = realmMemoryConfiguration;
	}

	public RealmFileConfigurationBuilder setRealmFileConfiguration() {
		return new RealmFileConfigurationBuilder(this);
	}

	public RealmJDBCConfigurationBuilder setRealmJDBCConfiguration() {
		return new RealmJDBCConfigurationBuilder(this);
	}

	public RealmMemoryConfigurationBuilder setRealmMemoryConfiguration() {
		return new RealmMemoryConfigurationBuilder(this);
	}

	/**
	 * @see net.winstone.config.builder.CompositeBuilder#build()
	 */
	@Override
	public ServerConfigurationBuilder build() {
		return builder.setRealmConfiguration(realmConfiguration).setRealmMemoryConfiguration(realmMemoryConfiguration).setRealmFileConfiguration(realmFileConfiguration).setRealmJDBCConfiguration(realmJDBCConfiguration);
	}

	/**
	 * @param realmMemoryConfiguration
	 *            the realmMemoryConfiguration to set
	 */
	RealmConfigurationBuilder setRealmConfiguration(RealmMemoryConfiguration configuration) {
		this.realmMemoryConfiguration = configuration;
		realmConfiguration = configuration;
		return this;
	}

	/**
	 * @param realmFileConfiguration
	 *            the realmFileConfiguration to set
	 */
	RealmConfigurationBuilder setRealmConfiguration(RealmFileConfiguration configuration) {
		this.realmFileConfiguration = configuration;
		realmConfiguration = configuration;
		return this;
	}

	/**
	 * @param realmJDBCConfiguration
	 *            the realmJDBCConfiguration to set
	 */
	RealmConfigurationBuilder setRealmConfiguration(RealmJDBCConfiguration configuration) {
		this.realmJDBCConfiguration = configuration;
		realmConfiguration = configuration;
		return this;
	}

}
