/**
 * 
 */
package net.winstone.config.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.winstone.config.RealmMemoryConfiguration;
import net.winstone.config.RealmUserConfiguration;
import net.winstone.core.authentication.realm.ArgumentsRealm;

/**
 * RealmMemoryConfigurationBuilder.
 * 
 * @author JGT
 * 
 */
public class RealmMemoryConfigurationBuilder {
	private RealmConfigurationBuilder builder;

	private List<RealmUserConfiguration> users = new ArrayList<RealmUserConfiguration>();

	/**
	 * Build a new instance of RealmMemoryConfigurationBuilder.
	 * 
	 * @param builder
	 */
	public RealmMemoryConfigurationBuilder(RealmConfigurationBuilder builder) {
		super();
		this.builder = builder;
	}

	public ServerConfigurationBuilder build() {
		return builder.setRealmConfiguration(new RealmMemoryConfiguration(ArgumentsRealm.class.getName(), users)).build();
	}

	public RealmMemoryConfigurationBuilder add(String name, String password, String... roles) {
		users.add(new RealmUserConfiguration(name, password, Arrays.asList(roles)));
		return this;
	}
}
