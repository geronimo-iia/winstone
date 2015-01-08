/**
 * 
 */
package net.winstone.config;

import java.io.Serializable;

/**
 * AccessLoggerConfiguration.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class AccessLoggerConfiguration implements Serializable {
	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = 635193612842569504L;
	/**
	 * Set the access logger class to use for user authentication. Defaults to
	 * disabled;
	 */
	protected String className;

	/**
	 * Build a new instance of AccessLoggerConfiguration.
	 */
	public AccessLoggerConfiguration() {
		this(null);
	}

	/**
	 * Build a new instance of AccessLoggerConfiguration.
	 * 
	 * @param className
	 */
	public AccessLoggerConfiguration(String className) {
		super();
		this.className = className;
	}

	public Boolean enabled() {
		return className != null;
	}

	/**
	 * @return the className
	 */
	public final String getClassName() {
		return className;
	}

}
