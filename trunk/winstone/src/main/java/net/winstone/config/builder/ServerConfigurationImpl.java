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
 * ServerConfigurationImpl.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class ServerConfigurationImpl extends ServerConfiguration {

	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = -7771130165048536871L;

	/**
	 * Build a new instance of ServerConfigurationImpl.
	 */
	public ServerConfigurationImpl() {
		super();
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
