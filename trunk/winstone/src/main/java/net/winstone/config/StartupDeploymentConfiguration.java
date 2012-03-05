/**
 * 
 */
package net.winstone.config;

import java.io.File;
import java.util.List;

/**
 * StartupDeploymentConfiguration.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class StartupDeploymentConfiguration {

	/**
	 * set document root folder.
	 */
	private File webroot;
	/**
	 * When webroot deployement option is used. Add this prefix to all URLs (eg
	 * http://localhost:8080/prefix/resource). Default is none\n\
	 */
	private String prefix;
	/**
	 * set location of warfile to extract from.
	 */
	private File warfile;
	/**
	 * set n document root folder (war file or webapplication folder) in a
	 * dot-comma separated list. <code>
	 * Example:
	 * --webroots=../target/sample.war;./sample-2-folder
	 * </code>
	 */
	private List<File> webroots;
	/**
	 * set directory for multiple webapps to be deployed from
	 */
	private File webappsDir;
	/**
	 * set directory for name-based virtual hosts to be deployed from
	 */
	private File hostsDir;
}
