/**
 * 
 */
package net.winstone.config;

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
	private ControlConfiguration controlConfiguration;
	/**
	 * HttpListenerConfiguration instance.
	 */
	private HttpListenerConfiguration httpListenerConfiguration;
	/**
	 * HttpsListenerConfiguration instance.
	 */
	private HttpsListenerConfiguration httpsListenerConfiguration;
	/**
	 * Ajp13ListenerConfiguration instance.
	 */
	private Ajp13ListenerConfiguration ajp13ListenerConfiguration;
	/**
	 * ClusterConfiguration instance.
	 */
	private ClusterConfiguration clusterConfiguration;
	/**
	 * HandlerConfiguration instance.
	 */
	private HandlerConfiguration handlerConfiguration;
	/**
	 * AccessLoggerConfiguration instance.
	 */
	private AccessLoggerConfiguration accessLoggerConfiguration;
	/**
	 * SimpleAccessLoggerConfiguration instance.
	 */
	private SimpleAccessLoggerConfiguration simpleAccessLoggerConfiguration;

	/**
	 * enable servlet reloading (true/false). Default is false
	 */
	private Boolean useServletReloading;
	/**
	 * enable directory lists (true/false). Default is true
	 */
	private Boolean directoryListings;
	/**
	 * enable the servlet invoker (true/false) Default is true
	 */
	private Boolean useInvoker;
	/**
	 * set the invoker prefix. Default is /servlet/
	 */
	private String invokerPrefix;
	/**
	 * simulate the apache mod_unique_id function. Default is false
	 */
	private Boolean simulateModUniqueId;
	/**
	 * enables session persistence (true/false). Default is false
	 */
	private Boolean useSavedSessions;
	/**
	 * set the max number of parameters allowed in a form submission to
	 * protect\n\ against hash DoS attack (oCERT #2011-003). Default is 10000.
	 */
	private int maxParamAllowed;

	/**
	 * override the preferred webapp class loader.
	 */
	private String preferredClassLoader;
	/**
	 * RealmConfiguration instance.
	 */
	private RealmConfiguration realmConfiguration;
	/**
	 * RealmMemoryConfiguration instance.
	 */
	private RealmMemoryConfiguration realmMemoryConfiguration;
	/**
	 * RealmFileConfiguration instance.
	 */
	private RealmFileConfiguration realmFileConfiguration;
	/**
	 * RealmJDBCConfiguration instance.
	 */
	private RealmJDBCConfiguration realmJDBCConfiguration;

	/**
	 * Override the JAVA_HOME variable
	 */
	private String javaHome;
	/**
	 * 
	 */
	private String toolsJar;// = The location of tools.jar. Default is
							// JAVA_HOME/lib/tools.jar\n\

	/**
	 * 
	 */
	private String tempDirectory;// = specify a default temporary
									// directory.\n\n\
	/**
	 * 
	 */
	private String commonLibFolder;// = folder for additional jar files. Default
									// is ./lib\n\

	/**
	 * 
	 */
	private Boolean useJasper;// = enable jasper JSP handling (true/false).
								// Default is false\n\

	/**
	 * Build a new instance of ServerConfiguration.
	 */
	public ServerConfiguration() {
		super();
	}
}
