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
	private Boolean directoryListings;// = enable directory lists (true/false).
										// Default is true\n\
	/**
	 * 
	 */
	private Boolean useJasper;// = enable jasper JSP handling (true/false).
								// Default is false\n\
	/**
	 * 
	 */
	private Boolean useServletReloading;// = enable servlet reloading
										// (true/false). Default is false\n\
	/**
	 * 
	 */
	private String preferredClassLoader;// = override the preferred webapp class
										// loader.\n\
	/**
	 * 
	 */
	private Boolean useInvoker;// = enable the servlet invoker (true/false).
								// Default is true\n\
	/**
	 * 
	 */
	private String invokerPrefix;// = set the invoker prefix. Default is
									// /servlet/\n\
	/**
	 * 
	 */
	private Boolean simulateModUniqueId;// = simulate the apache mod_unique_id
										// function. Default is false\n\
	/**
	 * 
	 */
	private Boolean useSavedSessions;// = enables session persistence
										// (true/false). Default is false\n\
	/**
	 * 
	 */
	private int maxParamAllowed;// = set the max number of parameters allowed in
								// a form submission to protect\n\ against hash
								// DoS attack (oCERT #2011-003). Default is
								// 10000.\n\
	/**
	 * 
	 */
	private Boolean useCluster;// = enable cluster support (true/false). Default
								// is false\n\
	/**
	 * 
	 */
	private String clusterClassName;// = Set the cluster class to use. Defaults
									// to SimpleCluster class\n\
	/**
	 * 
	 */
	private String clusterNodes;// = a comma separated list of node addresses
								// (IP:ControlPort,IP:ControlPort,etc)\n\

	/**
	 * 
	 */
	private String realmClassName;// = Set the realm class to use for user
									// authentication. Defaults to
									// ArgumentsRealm class\n\n\
	// private StringargumentsRealm.passwd.<user> = Password for user <user>.
	// Only valid for the ArgumentsRealm realm class\n\
	// private StringargumentsRealm.roles.<user> = Roles for user <user> (comma
	// separated). Only valid for the ArgumentsRealm realm class\n\n\
	/**
	 * 
	 */
	private String fileRealmConfigFile;// = File containing users/passwds/roles.
										// Only valid for the FileRealm realm
										// class\n\

	/**
	 * 
	 */
	private String accessLoggerClassName;// = Set the access logger class to use
											// for user authentication. Defaults
											// to disabled\n\
	/**
	 * 
	 */
	private String simpleAccessLoggerFormat;// = The log format to use. Supports
											// combined/common/resin/custom
											// (SimpleAccessLogger only)\n\
	/**
	 * 
	 */
	private String simpleAccessLoggerFile;// = The location pattern for the log
											// file(SimpleAccessLogger only)\n\

	/**
	 * Build a new instance of ServerConfiguration.
	 */
	public ServerConfiguration() {
		super();
	}
}
