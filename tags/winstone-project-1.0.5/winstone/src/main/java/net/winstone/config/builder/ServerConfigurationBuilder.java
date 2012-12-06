/**
 * 
 */
package net.winstone.config.builder;

import java.io.File;

import net.winstone.config.AccessLoggerConfiguration;
import net.winstone.config.Ajp13ListenerConfiguration;
import net.winstone.config.ClusterConfiguration;
import net.winstone.config.ControlConfiguration;
import net.winstone.config.DefaultServerConfiguration;
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

	private final DefaultServerConfiguration configuration;

	/**
	 * Build a new instance of ServerConfigurationBuilder.
	 */
	public ServerConfigurationBuilder() {
		super();
		configuration = new DefaultServerConfiguration();

		configuration.setControlConfiguration(null);
		configuration.setHttpListenerConfiguration(null);
		configuration.setHttpsListenerConfiguration(null);
		configuration.setAjp13ListenerConfiguration(null);
		configuration.setClusterConfiguration(null);
		configuration.setHandlerConfiguration(null);
		configuration.setAccessLoggerConfiguration(new AccessLoggerConfiguration());
		configuration.setSimpleAccessLoggerConfiguration(null);

		configuration.setUseServletReloading( Boolean.FALSE);
		configuration.setDirectoryListings( Boolean.TRUE);
		configuration.setUseInvoker( Boolean.TRUE);
		configuration.setInvokerPrefix("/servlet/");
		configuration.setSimulateModUniqueId(Boolean.FALSE);
		configuration.setUseSavedSessions(Boolean.FALSE);
		configuration.setMaxParamAllowed(10000);

		configuration.setPreferredClassLoader(null);

		configuration.setTempDirectory(null);

	}

	/**
	 * 
	 * @return a fresh ServerConfiguration intance.
	 */
	public ServerConfiguration build() {
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
		configuration.setUseServletReloading(useServletReloading);
		return this;
	}

	/**
	 * @param directoryListings
	 *            the directoryListings to set
	 */
	public ServerConfigurationBuilder setDirectoryListings(Boolean directoryListings) {
		configuration.setDirectoryListings(directoryListings);
		return this;
	}

	/**
	 * @param useInvoker
	 *            the useInvoker to set
	 */
	public ServerConfigurationBuilder setUseInvoker(Boolean useInvoker) {
		configuration.setUseInvoker(useInvoker);
		return this;
	}

	/**
	 * @param invokerPrefix
	 *            the invokerPrefix to set
	 */
	public ServerConfigurationBuilder setInvokerPrefix(String invokerPrefix) {
		configuration.setInvokerPrefix(invokerPrefix);
		return this;
	}

	/**
	 * @param simulateModUniqueId
	 *            the simulateModUniqueId to set
	 */
	public ServerConfigurationBuilder setSimulateModUniqueId(Boolean simulateModUniqueId) {
		configuration.setSimulateModUniqueId(simulateModUniqueId);
		return this;
	}

	/**
	 * @param useSavedSessions
	 *            the useSavedSessions to set
	 */
	public ServerConfigurationBuilder setUseSavedSessions(Boolean useSavedSessions) {
		configuration.setUseSavedSessions(useSavedSessions);
		return this;
	}

	/**
	 * @param maxParamAllowed
	 *            the maxParamAllowed to set
	 */
	public ServerConfigurationBuilder setMaxParamAllowed(int maxParamAllowed) {
		configuration.setMaxParamAllowed(maxParamAllowed);
		return this;
	}

	/**
	 * @param preferredClassLoader
	 *            the preferredClassLoader to set
	 */
	public ServerConfigurationBuilder setPreferredClassLoader(String preferredClassLoader) {
		configuration.setPreferredClassLoader(preferredClassLoader);
		return this;
	}

	public RealmConfigurationBuilder setRealmConfiguration() {
		return new RealmConfigurationBuilder(this);
	}

	ServerConfigurationBuilder setControlConfiguration(ControlConfiguration controlConfiguration) {
		configuration.setControlConfiguration(controlConfiguration);
		return this;
	}

	ServerConfigurationBuilder setHttpListenerConfiguration(HttpListenerConfiguration httpListenerConfiguration) {
		configuration.setHttpListenerConfiguration(httpListenerConfiguration);
		return this;
	}

	ServerConfigurationBuilder setHttpsListenerConfiguration(HttpsListenerConfiguration httpsListenerConfiguration) {
		configuration.setHttpsListenerConfiguration(httpsListenerConfiguration);
		return this;
	}

	ServerConfigurationBuilder setAjp13ListenerConfiguration(Ajp13ListenerConfiguration ajp13ListenerConfiguration) {
		configuration.setAjp13ListenerConfiguration(ajp13ListenerConfiguration);
		return this;
	}

	ServerConfigurationBuilder setClusterConfiguration(ClusterConfiguration clusterConfiguration) {
		configuration.setClusterConfiguration(clusterConfiguration);
		return this;
	}

	/**
	 * @param handlerConfiguration
	 *            the handlerConfiguration to set
	 */
	ServerConfigurationBuilder setHandlerConfiguration(HandlerConfiguration handlerConfiguration) {
		configuration.setHandlerConfiguration(handlerConfiguration);
		return this;
	}

	/**
	 * @param accessLoggerConfiguration
	 *            the accessLoggerConfiguration to set
	 */
	ServerConfigurationBuilder setAccessLoggerConfiguration(AccessLoggerConfiguration accessLoggerConfiguration) {
		configuration.setAccessLoggerConfiguration(accessLoggerConfiguration);
		return this;
	}

	/**
	 * @param simpleAccessLoggerConfiguration
	 *            the simpleAccessLoggerConfiguration to set
	 */
	ServerConfigurationBuilder setSimpleAccessLoggerConfiguration(SimpleAccessLoggerConfiguration simpleAccessLoggerConfiguration) {
		configuration.setSimpleAccessLoggerConfiguration(simpleAccessLoggerConfiguration);
		return this;
	}

	/**
	 * @param realmConfiguration
	 *            the realmConfiguration to set
	 */
	ServerConfigurationBuilder setRealmConfiguration(RealmConfiguration realmConfiguration) {
		configuration.setRealmConfiguration(realmConfiguration);
		return this;
	}

	/**
	 * @param realmMemoryConfiguration
	 *            the realmMemoryConfiguration to set
	 */
	ServerConfigurationBuilder setRealmMemoryConfiguration(RealmMemoryConfiguration realmMemoryConfiguration) {
		configuration.setRealmMemoryConfiguration(realmMemoryConfiguration);
		return this;
	}

	/**
	 * @param realmFileConfiguration
	 *            the realmFileConfiguration to set
	 */
	ServerConfigurationBuilder setRealmFileConfiguration(RealmFileConfiguration realmFileConfiguration) {
		configuration.setRealmFileConfiguration(realmFileConfiguration);
		return this;
	}

	/**
	 * @param realmJDBCConfiguration
	 *            the realmJDBCConfiguration to set
	 */
	ServerConfigurationBuilder setRealmJDBCConfiguration(RealmJDBCConfiguration realmJDBCConfiguration) {
		configuration.setRealmJDBCConfiguration(realmJDBCConfiguration);
		return this;
	}

	ServerConfigurationBuilder setTempDirectory(File tempDirectory) {
		configuration.setTempDirectory(tempDirectory);
		return this;
	}

}
