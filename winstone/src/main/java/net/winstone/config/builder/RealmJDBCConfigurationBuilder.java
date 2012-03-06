/**
 * 
 */
package net.winstone.config.builder;

import net.winstone.config.RealmJDBCConfiguration;
import net.winstone.core.authentication.realm.JDBCRealm;

/**
 * RealmJDBCConfigurationBuilder.
 * 
 * @author JGT
 * 
 */
public class RealmJDBCConfigurationBuilder {
	private RealmConfigurationBuilder builder;
	private String driver;
	private String url;
	private String user;
	private String password;
	private String userRel = "web_users";
	private String userNameColunm = "username";
	private String userCredColumn = "credential";
	private String userRoleRel = "web_user_roles";
	private String roleNameCol = "rolename";

	/**
	 * Build a new instance of RealmJDBCConfigurationBuilder.
	 * 
	 * @param builder
	 */
	public RealmJDBCConfigurationBuilder(RealmConfigurationBuilder builder) {
		super();
		this.builder = builder;
	}

	public ServerConfigurationBuilder build() {	
		return builder.setRealmConfiguration(new RealmJDBCConfiguration(JDBCRealm.class.getName(), driver, url, user, password, userRel, userNameColunm, userCredColumn, userRoleRel, roleNameCol)).build();
	}

	/**
	 * @param driver
	 *            the driver to set
	 */
	public RealmJDBCConfigurationBuilder setDriver(String driver) {
		this.driver = driver;
		return this;
	}

	/**
	 * @param url
	 *            the url to set
	 */
	public RealmJDBCConfigurationBuilder setUrl(String url) {
		this.url = url;
		return this;
	}

	/**
	 * @param user
	 *            the user to set
	 */
	public RealmJDBCConfigurationBuilder setUser(String user) {
		this.user = user;
		return this;
	}

	/**
	 * @param password
	 *            the password to set
	 */
	public RealmJDBCConfigurationBuilder setPassword(String password) {
		this.password = password;
		return this;
	}

	/**
	 * @param userRel
	 *            the userRel to set
	 */
	public RealmJDBCConfigurationBuilder setUserRel(String userRel) {
		this.userRel = userRel;
		return this;
	}

	/**
	 * @param userNameColunm
	 *            the userNameColunm to set
	 */
	public RealmJDBCConfigurationBuilder setUserNameColunm(String userNameColunm) {
		this.userNameColunm = userNameColunm;
		return this;
	}

	/**
	 * @param userCredColumn
	 *            the userCredColumn to set
	 */
	public RealmJDBCConfigurationBuilder setUserCredColumn(String userCredColumn) {
		this.userCredColumn = userCredColumn;
		return this;
	}

	/**
	 * @param userRoleRel
	 *            the userRoleRel to set
	 */
	public RealmJDBCConfigurationBuilder setUserRoleRel(String userRoleRel) {
		this.userRoleRel = userRoleRel;
		return this;
	}

	/**
	 * @param roleNameCol
	 *            the roleNameCol to set
	 */
	public RealmJDBCConfigurationBuilder setRoleNameCol(String roleNameCol) {
		this.roleNameCol = roleNameCol;
		return this;
	}

}
