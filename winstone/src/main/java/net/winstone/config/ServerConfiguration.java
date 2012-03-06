/**
 * 
 */
package net.winstone.config;

import java.io.File;
import java.io.Serializable;

/**
 * ServerConfiguration.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class ServerConfiguration implements Serializable {

	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = -5540953351305251467L;
	/**
	 * ControlConfiguration instance.
	 */
	protected ControlConfiguration controlConfiguration;
	/**
	 * HttpListenerConfiguration instance.
	 */
	protected HttpListenerConfiguration httpListenerConfiguration;
	/**
	 * HttpsListenerConfiguration instance.
	 */
	protected HttpsListenerConfiguration httpsListenerConfiguration;
	/**
	 * Ajp13ListenerConfiguration instance.
	 */
	protected Ajp13ListenerConfiguration ajp13ListenerConfiguration;
	/**
	 * ClusterConfiguration instance.
	 */
	protected ClusterConfiguration clusterConfiguration;
	/**
	 * HandlerConfiguration instance.
	 */
	protected HandlerConfiguration handlerConfiguration;
	/**
	 * AccessLoggerConfiguration instance.
	 */
	protected AccessLoggerConfiguration accessLoggerConfiguration;
	/**
	 * SimpleAccessLoggerConfiguration instance.
	 */
	protected SimpleAccessLoggerConfiguration simpleAccessLoggerConfiguration;

	/**
	 * enable servlet reloading (true/false). Default is false
	 */
	protected Boolean useServletReloading;
	/**
	 * enable directory lists (true/false). Default is true
	 */
	protected Boolean directoryListings;
	/**
	 * enable the servlet invoker (true/false) Default is true
	 */
	protected Boolean useInvoker;
	/**
	 * set the invoker prefix. Default is /servlet/
	 */
	protected String invokerPrefix;
	/**
	 * simulate the apache mod_unique_id function. Default is false
	 */
	protected Boolean simulateModUniqueId;
	/**
	 * enables session persistence (true/false). Default is false
	 */
	protected Boolean useSavedSessions;
	/**
	 * set the max number of parameters allowed in a form submission to
	 * protect\n\ against hash DoS attack (oCERT #2011-003). Default is 10000.
	 */
	protected int maxParamAllowed;

	/**
	 * override the preferred webapp class loader.
	 */
	protected String preferredClassLoader;
	/**
	 * RealmConfiguration instance.
	 */
	protected RealmConfiguration realmConfiguration;
	/**
	 * RealmMemoryConfiguration instance.
	 */
	protected RealmMemoryConfiguration realmMemoryConfiguration;
	/**
	 * RealmFileConfiguration instance.
	 */
	protected RealmFileConfiguration realmFileConfiguration;
	/**
	 * RealmJDBCConfiguration instance.
	 */
	protected RealmJDBCConfiguration realmJDBCConfiguration;

	/**
	 * specify a default temporary directory.
	 */
	protected File tempDirectory;

	/**
	 * Build a new instance of ServerConfiguration.
	 */
	public ServerConfiguration() {
		super();
	}

	/**
	 * @return the controlConfiguration
	 */
	public ControlConfiguration getControlConfiguration() {
		return controlConfiguration;
	}

	/**
	 * @return the httpListenerConfiguration
	 */
	public HttpListenerConfiguration getHttpListenerConfiguration() {
		return httpListenerConfiguration;
	}

	/**
	 * @return the httpsListenerConfiguration
	 */
	public HttpsListenerConfiguration getHttpsListenerConfiguration() {
		return httpsListenerConfiguration;
	}

	/**
	 * @return the ajp13ListenerConfiguration
	 */
	public Ajp13ListenerConfiguration getAjp13ListenerConfiguration() {
		return ajp13ListenerConfiguration;
	}

	/**
	 * @return the clusterConfiguration
	 */
	public ClusterConfiguration getClusterConfiguration() {
		return clusterConfiguration;
	}

	/**
	 * @return the handlerConfiguration
	 */
	public HandlerConfiguration getHandlerConfiguration() {
		return handlerConfiguration;
	}

	/**
	 * @return the accessLoggerConfiguration
	 */
	public AccessLoggerConfiguration getAccessLoggerConfiguration() {
		return accessLoggerConfiguration;
	}

	/**
	 * @return the simpleAccessLoggerConfiguration
	 */
	public SimpleAccessLoggerConfiguration getSimpleAccessLoggerConfiguration() {
		return simpleAccessLoggerConfiguration;
	}

	/**
	 * @return the useServletReloading
	 */
	public Boolean getUseServletReloading() {
		return useServletReloading;
	}

	/**
	 * @return the directoryListings
	 */
	public Boolean getDirectoryListings() {
		return directoryListings;
	}

	/**
	 * @return the useInvoker
	 */
	public Boolean getUseInvoker() {
		return useInvoker;
	}

	/**
	 * @return the invokerPrefix
	 */
	public String getInvokerPrefix() {
		return invokerPrefix;
	}

	/**
	 * @return the simulateModUniqueId
	 */
	public Boolean getSimulateModUniqueId() {
		return simulateModUniqueId;
	}

	/**
	 * @return the useSavedSessions
	 */
	public Boolean getUseSavedSessions() {
		return useSavedSessions;
	}

	/**
	 * @return the maxParamAllowed
	 */
	public int getMaxParamAllowed() {
		return maxParamAllowed;
	}

	/**
	 * @return the preferredClassLoader
	 */
	public String getPreferredClassLoader() {
		return preferredClassLoader;
	}

	/**
	 * @return the realmConfiguration
	 */
	public RealmConfiguration getRealmConfiguration() {
		return realmConfiguration;
	}

	/**
	 * @return the realmMemoryConfiguration
	 */
	public RealmMemoryConfiguration getRealmMemoryConfiguration() {
		return realmMemoryConfiguration;
	}

	/**
	 * @return the realmFileConfiguration
	 */
	public RealmFileConfiguration getRealmFileConfiguration() {
		return realmFileConfiguration;
	}

	/**
	 * @return the realmJDBCConfiguration
	 */
	public RealmJDBCConfiguration getRealmJDBCConfiguration() {
		return realmJDBCConfiguration;
	}

	/**
	 * @return the tempDirectory
	 */
	public File getTempDirectory() {
		return tempDirectory;
	}

}
