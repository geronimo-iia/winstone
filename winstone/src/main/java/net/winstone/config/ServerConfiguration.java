/**
 * 
 */
package net.winstone.config;

import java.io.File;


/**
 * ServerConfiguration. 
 *
 * @author JGT
 *
 */
public interface ServerConfiguration {

	public abstract File getTempDirectory();

	public abstract RealmJDBCConfiguration getRealmJDBCConfiguration();

	public abstract RealmFileConfiguration getRealmFileConfiguration();

	public abstract RealmMemoryConfiguration getRealmMemoryConfiguration();

	public abstract RealmConfiguration getRealmConfiguration();

	public abstract String getPreferredClassLoader();

	public abstract int getMaxParamAllowed();

	public abstract Boolean getUseSavedSessions();

	public abstract Boolean getSimulateModUniqueId();

	public abstract String getInvokerPrefix();

	public abstract Boolean getUseInvoker();

	public abstract Boolean getDirectoryListings();

	public abstract Boolean getUseServletReloading();

	public abstract SimpleAccessLoggerConfiguration getSimpleAccessLoggerConfiguration();

	public abstract AccessLoggerConfiguration getAccessLoggerConfiguration();

	public abstract HandlerConfiguration getHandlerConfiguration();

	public abstract ClusterConfiguration getClusterConfiguration();

	public abstract Ajp13ListenerConfiguration getAjp13ListenerConfiguration();

	public abstract HttpsListenerConfiguration getHttpsListenerConfiguration();

	public abstract HttpListenerConfiguration getHttpListenerConfiguration();

	public abstract ControlConfiguration getControlConfiguration();

}
