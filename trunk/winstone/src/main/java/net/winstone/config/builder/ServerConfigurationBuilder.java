/**
 * 
 */
package net.winstone.config.builder;

import java.io.File;

import net.winstone.config.AccessLoggerConfiguration;
import net.winstone.config.Ajp13ListenerConfiguration;
import net.winstone.config.ClusterConfiguration;
import net.winstone.config.ControlConfiguration;
import net.winstone.config.HandlerConfiguration;
import net.winstone.config.HttpListenerConfiguration;
import net.winstone.config.HttpsListenerConfiguration;
import net.winstone.config.RealmConfiguration;
import net.winstone.config.RealmFileConfiguration;
import net.winstone.config.RealmJDBCConfiguration;
import net.winstone.config.RealmMemoryConfiguration;
import net.winstone.config.ServerConfiguration;
import net.winstone.config.SimpleAccessLoggerConfiguration;

/**
 * ServerConfigurationBuilder.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class ServerConfigurationBuilder {

	/**
	 * Control
	 */
	protected ControlConfiguration controlConfiguration = null;
	/**
	 * Listeners
	 */
	protected HttpListenerConfiguration httpListenerConfiguration = null;
	protected HttpsListenerConfiguration httpsListenerConfiguration = null;
	protected Ajp13ListenerConfiguration ajp13ListenerConfiguration = null;
	/**
	 * Cluster
	 */
	protected ClusterConfiguration clusterConfiguration = null;
	/**
	 * Handler
	 */
	protected HandlerConfiguration handlerConfiguration = null;
	/**
	 * Access Log.
	 */
	protected AccessLoggerConfiguration accessLoggerConfiguration = new AccessLoggerConfiguration();
	/**
	 * SimpleAccessLoggerConfiguration instance.
	 */
	protected SimpleAccessLoggerConfiguration simpleAccessLoggerConfiguration = null;

	private Boolean useServletReloading = Boolean.FALSE;
	private Boolean directoryListings = Boolean.TRUE;
	private Boolean useInvoker = Boolean.TRUE;
	private String invokerPrefix = "/servlet/";
	private Boolean simulateModUniqueId = Boolean.FALSE;
	private Boolean useSavedSessions = Boolean.FALSE;
	private int maxParamAllowed = 10000;

	private String preferredClassLoader = null;

	private RealmConfiguration realmConfiguration;
	private RealmMemoryConfiguration realmMemoryConfiguration;
	private RealmFileConfiguration realmFileConfiguration;
	private RealmJDBCConfiguration realmJDBCConfiguration;

	private File tempDirectory = null;

	/**
	 * Build a new instance of ServerConfigurationBuilder.
	 */
	public ServerConfigurationBuilder() {
		super();
	}

	/**
	 * 
	 * @return a fresh ServerConfiguration intance.
	 */
	public ServerConfiguration build() {

		ServerConfigurationImpl configuration = new ServerConfigurationImpl();
		configuration.setControlConfiguration(controlConfiguration);
		configuration.setHttpListenerConfiguration(httpListenerConfiguration);
		configuration.setHttpsListenerConfiguration(httpsListenerConfiguration);
		configuration.setAjp13ListenerConfiguration(ajp13ListenerConfiguration);
		configuration.setClusterConfiguration(clusterConfiguration);
		configuration.setHandlerConfiguration(handlerConfiguration);
		configuration.setAccessLoggerConfiguration(accessLoggerConfiguration);
		configuration.setSimpleAccessLoggerConfiguration(simpleAccessLoggerConfiguration);

		configuration.setUseServletReloading(useServletReloading);
		configuration.setDirectoryListings(directoryListings);
		configuration.setUseInvoker(useInvoker);
		configuration.setInvokerPrefix(invokerPrefix);
		configuration.setSimulateModUniqueId(simulateModUniqueId);
		configuration.setUseSavedSessions(useSavedSessions);
		configuration.setMaxParamAllowed(maxParamAllowed);

		configuration.setPreferredClassLoader(preferredClassLoader);

		configuration.setRealmConfiguration(realmConfiguration);
		configuration.setRealmMemoryConfiguration(realmMemoryConfiguration);
		configuration.setRealmFileConfiguration(realmFileConfiguration);
		configuration.setRealmJDBCConfiguration(realmJDBCConfiguration);

		configuration.setTempDirectory(tempDirectory);
		return configuration;
	}

	public ControlConfigurationBuilder addControlConfiguration() {
		return new ControlConfigurationBuilder(this);
	}

	public HttpListenerConfigurationBuilder addHttpListenerConfiguration() {
		return new HttpListenerConfigurationBuilder(this);
	}

	public HttpsListenerConfigurationBuilder addHttpsListenerConfiguration() {
		return new HttpsListenerConfigurationBuilder(this);
	}

	public Ajp13ListenerConfigurationBuilder addAjp13ListenerConfiguration() {
		return new Ajp13ListenerConfigurationBuilder(this);
	}

	public ClusterConfigurationBuilder addClusterConfiguration() {
		return new ClusterConfigurationBuilder(this);
	}

	/**
	 * @param useServletReloading
	 *            the useServletReloading to set
	 */
	public ServerConfigurationBuilder setUseServletReloading(Boolean useServletReloading) {
		this.useServletReloading = useServletReloading;
		return this;
	}

	/**
	 * @param directoryListings
	 *            the directoryListings to set
	 */
	public ServerConfigurationBuilder setDirectoryListings(Boolean directoryListings) {
		this.directoryListings = directoryListings;
		return this;
	}

	/**
	 * @param useInvoker
	 *            the useInvoker to set
	 */
	public ServerConfigurationBuilder setUseInvoker(Boolean useInvoker) {
		this.useInvoker = useInvoker;
		return this;
	}

	/**
	 * @param invokerPrefix
	 *            the invokerPrefix to set
	 */
	public ServerConfigurationBuilder setInvokerPrefix(String invokerPrefix) {
		this.invokerPrefix = invokerPrefix;
		return this;
	}

	/**
	 * @param simulateModUniqueId
	 *            the simulateModUniqueId to set
	 */
	public ServerConfigurationBuilder setSimulateModUniqueId(Boolean simulateModUniqueId) {
		this.simulateModUniqueId = simulateModUniqueId;
		return this;
	}

	/**
	 * @param useSavedSessions
	 *            the useSavedSessions to set
	 */
	public ServerConfigurationBuilder setUseSavedSessions(Boolean useSavedSessions) {
		this.useSavedSessions = useSavedSessions;
		return this;
	}

	/**
	 * @param maxParamAllowed
	 *            the maxParamAllowed to set
	 */
	public ServerConfigurationBuilder setMaxParamAllowed(int maxParamAllowed) {
		this.maxParamAllowed = maxParamAllowed;
		return this;
	}

	/**
	 * @param preferredClassLoader
	 *            the preferredClassLoader to set
	 */
	public ServerConfigurationBuilder setPreferredClassLoader(String preferredClassLoader) {
		this.preferredClassLoader = preferredClassLoader;
		return this;
	}

	public RealmConfigurationBuilder setRealmConfiguration() {
		return new RealmConfigurationBuilder(this);
	}

	ServerConfigurationBuilder setControlConfiguration(ControlConfiguration controlConfiguration) {
		this.controlConfiguration = controlConfiguration;
		return this;
	}

	ServerConfigurationBuilder setHttpListenerConfiguration(HttpListenerConfiguration httpListenerConfiguration) {
		this.httpListenerConfiguration = httpListenerConfiguration;
		return this;
	}

	ServerConfigurationBuilder setHttpsListenerConfiguration(HttpsListenerConfiguration httpsListenerConfiguration) {
		this.httpsListenerConfiguration = httpsListenerConfiguration;
		return this;
	}

	ServerConfigurationBuilder setAjp13ListenerConfiguration(Ajp13ListenerConfiguration ajp13ListenerConfiguration) {
		this.ajp13ListenerConfiguration = ajp13ListenerConfiguration;
		return this;
	}

	ServerConfigurationBuilder setClusterConfiguration(ClusterConfiguration clusterConfiguration) {
		this.clusterConfiguration = clusterConfiguration;
		return this;
	}

	/**
	 * @param handlerConfiguration
	 *            the handlerConfiguration to set
	 */
	ServerConfigurationBuilder setHandlerConfiguration(HandlerConfiguration handlerConfiguration) {
		this.handlerConfiguration = handlerConfiguration;
		return this;
	}

	/**
	 * @param accessLoggerConfiguration
	 *            the accessLoggerConfiguration to set
	 */
	ServerConfigurationBuilder setAccessLoggerConfiguration(AccessLoggerConfiguration accessLoggerConfiguration) {
		this.accessLoggerConfiguration = accessLoggerConfiguration;
		return this;
	}

	/**
	 * @param simpleAccessLoggerConfiguration
	 *            the simpleAccessLoggerConfiguration to set
	 */
	ServerConfigurationBuilder setSimpleAccessLoggerConfiguration(SimpleAccessLoggerConfiguration simpleAccessLoggerConfiguration) {
		this.simpleAccessLoggerConfiguration = simpleAccessLoggerConfiguration;
		return this;
	}

	/**
	 * @param realmConfiguration
	 *            the realmConfiguration to set
	 */
	ServerConfigurationBuilder setRealmConfiguration(RealmConfiguration realmConfiguration) {
		this.realmConfiguration = realmConfiguration;
		return this;
	}

	/**
	 * @param realmMemoryConfiguration
	 *            the realmMemoryConfiguration to set
	 */
	ServerConfigurationBuilder setRealmMemoryConfiguration(RealmMemoryConfiguration realmMemoryConfiguration) {
		this.realmMemoryConfiguration = realmMemoryConfiguration;
		return this;
	}

	/**
	 * @param realmFileConfiguration
	 *            the realmFileConfiguration to set
	 */
	ServerConfigurationBuilder setRealmFileConfiguration(RealmFileConfiguration realmFileConfiguration) {
		this.realmFileConfiguration = realmFileConfiguration;
		return this;
	}

	/**
	 * @param realmJDBCConfiguration
	 *            the realmJDBCConfiguration to set
	 */
	ServerConfigurationBuilder setRealmJDBCConfiguration(RealmJDBCConfiguration realmJDBCConfiguration) {
		this.realmJDBCConfiguration = realmJDBCConfiguration;
		return this;
	}

}
