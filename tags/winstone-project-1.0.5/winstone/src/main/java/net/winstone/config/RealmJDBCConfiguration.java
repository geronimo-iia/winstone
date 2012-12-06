/**
 * 
 */
package net.winstone.config;

/**
 * RealmJDBCConfiguration.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class RealmJDBCConfiguration extends RealmConfiguration {

	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = 9209624014220154269L;

	private String driver;
	private String url;
	private String user;
	private String password;
	private String userRel;
	private String userNameColunm;
	private String userCredColumn;
	private String userRoleRel;
	private String roleNameCol;

	/**
	 * Build a new instance of RealmJDBCConfiguration.
	 * 
	 * @param realmClassName
	 * @param driver
	 * @param url
	 * @param user
	 * @param password
	 * @param userRel
	 * @param userNameColunm
	 * @param userCredColumn
	 * @param userRoleRel
	 * @param roleNameCol
	 */
	public RealmJDBCConfiguration(String realmClassName, String driver, String url, String user, String password, String userRel, String userNameColunm, String userCredColumn, String userRoleRel, String roleNameCol) {
		super(realmClassName);
		this.driver = driver;
		this.url = url;
		this.user = user;
		this.password = password;
		this.userRel = userRel;
		this.userNameColunm = userNameColunm;
		this.userCredColumn = userCredColumn;
		this.userRoleRel = userRoleRel;
		this.roleNameCol = roleNameCol;
	}

	/**
	 * @return the driver
	 */
	public String getDriver() {
		return driver;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @return the userRel
	 */
	public String getUserRel() {
		return userRel;
	}

	/**
	 * @return the userNameColunm
	 */
	public String getUserNameColunm() {
		return userNameColunm;
	}

	/**
	 * @return the userCredColumn
	 */
	public String getUserCredColumn() {
		return userCredColumn;
	}

	/**
	 * @return the userRoleRel
	 */
	public String getUserRoleRel() {
		return userRoleRel;
	}

	/**
	 * @return the roleNameCol
	 */
	public String getRoleNameCol() {
		return roleNameCol;
	}

}
