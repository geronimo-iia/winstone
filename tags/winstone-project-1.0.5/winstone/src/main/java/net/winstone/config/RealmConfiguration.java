/**
 * 
 */
package net.winstone.config;

import java.io.Serializable;

import net.winstone.core.authentication.realm.ArgumentsRealm;

/**
 * RealmConfiguration.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class RealmConfiguration implements Serializable {

	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = 2498604739801805604L;
	/**
	 * Set the realm class to use for user authentication. Defaults to
	 * ArgumentsRealm class
	 */
	private String realmClassName = ArgumentsRealm.class.getName();

	/**
	 * Build a new instance of RealmConfiguration.
	 * 
	 * @param realmClassName
	 */
	public RealmConfiguration(String realmClassName) {
		super();
		this.realmClassName = realmClassName;
	}

	/**
	 * @return the realmClassName
	 */
	public String getRealmClassName() {
		return realmClassName;
	}

}
