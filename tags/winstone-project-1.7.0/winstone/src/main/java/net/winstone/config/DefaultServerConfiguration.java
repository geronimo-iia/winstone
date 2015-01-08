/**
 * 
 */
package net.winstone.config;

import java.io.File;
import java.io.Serializable;

/**
 * DefaultServerConfiguration.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class DefaultServerConfiguration implements ServerConfiguration, Serializable {

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
	 * Build a new instance of DefaultServerConfiguration.
	 */
	public DefaultServerConfiguration() {
		super();
	}

	/**
	 * @return the controlConfiguration
	 */
	@Override
	public ControlConfiguration getControlConfiguration() {
		return controlConfiguration;
	}

	/**
	 * @return the httpListenerConfiguration
	 */
	@Override
	public HttpListenerConfiguration getHttpListenerConfiguration() {
		return httpListenerConfiguration;
	}

	/**
	 * @return the httpsListenerConfiguration
	 */
	@Override
	public HttpsListenerConfiguration getHttpsListenerConfiguration() {
		return httpsListenerConfiguration;
	}

	/**
	 * @return the ajp13ListenerConfiguration
	 */
	@Override
	public Ajp13ListenerConfiguration getAjp13ListenerConfiguration() {
		return ajp13ListenerConfiguration;
	}

	/**
	 * @return the clusterConfiguration
	 */
	@Override
	public ClusterConfiguration getClusterConfiguration() {
		return clusterConfiguration;
	}

	/**
	 * @return the handlerConfiguration
	 */
	@Override
	public HandlerConfiguration getHandlerConfiguration() {
		return handlerConfiguration;
	}

	/**
	 * @return the accessLoggerConfiguration
	 */
	@Override
	public AccessLoggerConfiguration getAccessLoggerConfiguration() {
		return accessLoggerConfiguration;
	}

	/**
	 * @return the simpleAccessLoggerConfiguration
	 */
	@Override
	public SimpleAccessLoggerConfiguration getSimpleAccessLoggerConfiguration() {
		return simpleAccessLoggerConfiguration;
	}

	/**
	 * @return the useServletReloading
	 */
	@Override
	public Boolean getUseServletReloading() {
		return useServletReloading;
	}

	/**
	 * @return the directoryListings
	 */
	@Override
	public Boolean getDirectoryListings() {
		return directoryListings;
	}

	/**
	 * @return the useInvoker
	 */
	@Override
	public Boolean getUseInvoker() {
		return useInvoker;
	}

	/**
	 * @return the invokerPrefix
	 */
	@Override
	public String getInvokerPrefix() {
		return invokerPrefix;
	}

	/**
	 * @return the simulateModUniqueId
	 */
	@Override
	public Boolean getSimulateModUniqueId() {
		return simulateModUniqueId;
	}

	/**
	 * @return the useSavedSessions
	 */
	@Override
	public Boolean getUseSavedSessions() {
		return useSavedSessions;
	}

	/**
	 * @return the maxParamAllowed
	 */
	@Override
	public int getMaxParamAllowed() {
		return maxParamAllowed;
	}

	/**
	 * @return the preferredClassLoader
	 */
	@Override
	public String getPreferredClassLoader() {
		return preferredClassLoader;
	}

	/**
	 * @return the realmConfiguration
	 */
	@Override
	public RealmConfiguration getRealmConfiguration() {
		return realmConfiguration;
	}

	/**
	 * @return the realmMemoryConfiguration
	 */
	@Override
	public RealmMemoryConfiguration getRealmMemoryConfiguration() {
		return realmMemoryConfiguration;
	}

	/**
	 * @return the realmFileConfiguration
	 */
	@Override
	public RealmFileConfiguration getRealmFileConfiguration() {
		return realmFileConfiguration;
	}

	/**
	 * @return the realmJDBCConfiguration
	 */
	@Override
	public RealmJDBCConfiguration getRealmJDBCConfiguration() {
		return realmJDBCConfiguration;
	}

	/**
	 * @return the tempDirectory
	 */
	@Override
	public File getTempDirectory() {
		return tempDirectory;
	}

	/**
	 * @param controlConfiguration
	 *            the controlConfiguration to set
	 */
	public void setControlConfiguration(ControlConfiguration controlConfiguration) {
		this.controlConfiguration = controlConfiguration;
	}

	/**
	 * @param httpListenerConfiguration
	 *            the httpListenerConfiguration to set
	 */
	public void setHttpListenerConfiguration(HttpListenerConfiguration httpListenerConfiguration) {
		this.httpListenerConfiguration = httpListenerConfiguration;
	}

	/**
	 * @param httpsListenerConfiguration
	 *            the httpsListenerConfiguration to set
	 */
	public void setHttpsListenerConfiguration(HttpsListenerConfiguration httpsListenerConfiguration) {
		this.httpsListenerConfiguration = httpsListenerConfiguration;
	}

	/**
	 * @param ajp13ListenerConfiguration
	 *            the ajp13ListenerConfiguration to set
	 */
	public void setAjp13ListenerConfiguration(Ajp13ListenerConfiguration ajp13ListenerConfiguration) {
		this.ajp13ListenerConfiguration = ajp13ListenerConfiguration;
	}

	/**
	 * @param clusterConfiguration
	 *            the clusterConfiguration to set
	 */
	public void setClusterConfiguration(ClusterConfiguration clusterConfiguration) {
		this.clusterConfiguration = clusterConfiguration;
	}

	/**
	 * @param handlerConfiguration
	 *            the handlerConfiguration to set
	 */
	public void setHandlerConfiguration(HandlerConfiguration handlerConfiguration) {
		this.handlerConfiguration = handlerConfiguration;
	}

	/**
	 * @param accessLoggerConfiguration
	 *            the accessLoggerConfiguration to set
	 */
	public void setAccessLoggerConfiguration(AccessLoggerConfiguration accessLoggerConfiguration) {
		this.accessLoggerConfiguration = accessLoggerConfiguration;
	}

	/**
	 * @param simpleAccessLoggerConfiguration
	 *            the simpleAccessLoggerConfiguration to set
	 */
	public void setSimpleAccessLoggerConfiguration(SimpleAccessLoggerConfiguration simpleAccessLoggerConfiguration) {
		this.simpleAccessLoggerConfiguration = simpleAccessLoggerConfiguration;
	}

	/**
	 * @param useServletReloading
	 *            the useServletReloading to set
	 */
	public void setUseServletReloading(Boolean useServletReloading) {
		this.useServletReloading = useServletReloading;
	}

	/**
	 * @param directoryListings
	 *            the directoryListings to set
	 */
	public void setDirectoryListings(Boolean directoryListings) {
		this.directoryListings = directoryListings;
	}

	/**
	 * @param useInvoker
	 *            the useInvoker to set
	 */
	public void setUseInvoker(Boolean useInvoker) {
		this.useInvoker = useInvoker;
	}

	/**
	 * @param invokerPrefix
	 *            the invokerPrefix to set
	 */
	public void setInvokerPrefix(String invokerPrefix) {
		this.invokerPrefix = invokerPrefix;
	}

	/**
	 * @param simulateModUniqueId
	 *            the simulateModUniqueId to set
	 */
	public void setSimulateModUniqueId(Boolean simulateModUniqueId) {
		this.simulateModUniqueId = simulateModUniqueId;
	}

	/**
	 * @param useSavedSessions
	 *            the useSavedSessions to set
	 */
	public void setUseSavedSessions(Boolean useSavedSessions) {
		this.useSavedSessions = useSavedSessions;
	}

	/**
	 * @param maxParamAllowed
	 *            the maxParamAllowed to set
	 */
	public void setMaxParamAllowed(int maxParamAllowed) {
		this.maxParamAllowed = maxParamAllowed;
	}

	/**
	 * @param preferredClassLoader
	 *            the preferredClassLoader to set
	 */
	public void setPreferredClassLoader(String preferredClassLoader) {
		this.preferredClassLoader = preferredClassLoader;
	}

	/**
	 * @param realmConfiguration
	 *            the realmConfiguration to set
	 */
	public void setRealmConfiguration(RealmConfiguration realmConfiguration) {
		this.realmConfiguration = realmConfiguration;
	}

	/**
	 * @param realmMemoryConfiguration
	 *            the realmMemoryConfiguration to set
	 */
	public void setRealmMemoryConfiguration(RealmMemoryConfiguration realmMemoryConfiguration) {
		this.realmMemoryConfiguration = realmMemoryConfiguration;
	}

	/**
	 * @param realmFileConfiguration
	 *            the realmFileConfiguration to set
	 */
	public void setRealmFileConfiguration(RealmFileConfiguration realmFileConfiguration) {
		this.realmFileConfiguration = realmFileConfiguration;
	}

	/**
	 * @param realmJDBCConfiguration
	 *            the realmJDBCConfiguration to set
	 */
	public void setRealmJDBCConfiguration(RealmJDBCConfiguration realmJDBCConfiguration) {
		this.realmJDBCConfiguration = realmJDBCConfiguration;
	}

	/**
	 * @param tempDirectory
	 *            the tempDirectory to set
	 */
	public void setTempDirectory(File tempDirectory) {
		this.tempDirectory = tempDirectory;
	}

}
