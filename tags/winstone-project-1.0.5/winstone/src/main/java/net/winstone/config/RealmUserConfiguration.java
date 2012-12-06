/**
 * 
 */
package net.winstone.config;

import java.io.Serializable;
import java.util.List;

/**
 * RealmUserConfiguration.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class RealmUserConfiguration implements Serializable {

	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = 6024269194202745118L;
	private String name;
	private String password;
	private List<String> roles;

	/**
	 * Build a new instance of RealmUserConfiguration.
	 * 
	 * @param name
	 * @param password
	 * @param roles
	 */
	public RealmUserConfiguration(String name, String password, List<String> roles) {
		super();
		this.name = name;
		this.password = password;
		this.roles = roles;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @return the roles
	 */
	public List<String> getRoles() {
		return roles;
	}

}
