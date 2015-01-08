/**
 * 
 */
package net.winstone.config;

import java.util.List;

/**
 * RealmMemoryConfiguration.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class RealmMemoryConfiguration extends RealmConfiguration {
	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = 6291233631467553040L;
	/**
	 * User list.
	 */
	private List<RealmUserConfiguration> users;

	/**
	 * Build a new instance of RealmMemoryConfiguration.
	 * 
	 * @param realmClassName
	 * @param users
	 */
	public RealmMemoryConfiguration(String realmClassName, List<RealmUserConfiguration> users) {
		super(realmClassName);
		this.users = users;
	}

	/**
	 * @return the users
	 */
	public List<RealmUserConfiguration> getUsers() {
		return users;
	}

}
