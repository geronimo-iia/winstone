/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.winstone.WinstoneException;
import net.winstone.cluster.Cluster;
import net.winstone.jndi.JndiManager;
import net.winstone.util.FileUtils;
import net.winstone.util.StringUtils;

import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Manages the references to individual webapps within the container. This
 * object handles the mapping of url-prefixes to webapps, and init and shutdown
 * of any webapps it manages.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HostConfiguration.java,v 1.8 2007/08/02 06:16:00 rickknowles
 *          Exp $
 */
public class HostConfiguration implements Runnable {

	protected static org.slf4j.Logger logger = LoggerFactory.getLogger(HostConfiguration.class);
	/**
	 * TIME PERIOD (ms) OF SESSION FLUSHING
	 */
	private static final long FLUSH_PERIOD = 60000L;
	private static final String WEB_INF = "WEB-INF";
	private static final String WEB_XML = "web.xml";
	/**
	 * host name/
	 */
	private final String hostname;
	/**
	 * Arguments map.
	 */
	private final Map<String, String> args;
	/**
	 * Map of WebAppConfiguration, key is context path of web application
	 */
	private final Map<String, WebAppConfiguration> webapps;
	/**
	 * Cluster instance.
	 */
	private final Cluster cluster;
	/**
	 * Object Pool instance.
	 */
	private final ObjectPool objectPool;
	/**
	 * Common libraries class loader instance.
	 */
	private final ClassLoader commonLibCL;
	/**
	 * JNDI Manager instance.
	 */
	private final JndiManager jndiManager;
	/**
	 * Thread instance in order to flush sessions.
	 */
	private Thread thread;

	/**
	 * 
	 * Build a new instance of HostConfiguration.
	 * 
	 * @param hostname
	 * @param cluster
	 * @param objectPool
	 * @param jndiManager
	 * @param commonLibCL
	 * @param args
	 * @param webappsDirName
	 * @throws IOException
	 */
	public HostConfiguration(final String hostname, final Cluster cluster, final ObjectPool objectPool, final JndiManager jndiManager, final ClassLoader commonLibCL, final Map<String, String> args, final String webappsDirName) throws IOException {
		this.hostname = hostname;
		this.args = args;
		webapps = new HashMap<String, WebAppConfiguration>();
		this.cluster = cluster;
		this.objectPool = objectPool;
		this.commonLibCL = commonLibCL;
		this.jndiManager = jndiManager;
		// Is this the single or multiple configuration ? Check args
		File warfile = StringUtils.fileArg(args, "warfile");
		File webroot = StringUtils.fileArg(args, "webroot");
		// If single-webapp mode
		if ((webappsDirName == null) && ((warfile != null) || (webroot != null))) {
			String prefix = args.get("prefix");
			if (prefix == null) {
				prefix = "";
			}
			try {
				final WebAppConfiguration webAppConfiguration = initWebApp(prefix, getWebRoot(webroot, warfile), "webapp");
				webapps.put(webAppConfiguration.getContextPath(), webAppConfiguration);
			} catch (final IOException err) {
				HostConfiguration.logger.error("Error initializing web application: prefix [" + prefix + "]", err);
			}
		} // Otherwise multi-webapp mode
		else {
			initMultiWebappDir(webappsDirName);
		}
		HostConfiguration.logger.debug("Initialized {} webapps: prefixes - {}", webapps.size() + "", webapps.keySet() + "");

		thread = new Thread(this, "WinstoneHostConfigurationMgmt:" + this.hostname);
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * @param uri
	 * @return a WebAppConfiguration for specified uri or null if none is found.
	 */
	public WebAppConfiguration getWebAppByURI(final String uri) {
		if (uri == null) {
			return null;
		} else if (uri.equals("/") || uri.equals("")) {
			return webapps.get("");
		} else if (uri.startsWith("/")) {
			final String decoded = WinstoneRequest.decodeURLToken(uri);
			final String noLeadingSlash = decoded.substring(1);
			final int slashPos = noLeadingSlash.indexOf("/");
			if (slashPos == -1) {
				return webapps.get(decoded);
			} else {
				return webapps.get(decoded.substring(0, slashPos + 1));
			}
		} else {
			return null;
		}
	}

	/**
	 * Initialize specified webapplication.
	 * 
	 * @param prefix
	 *            prefix
	 * @param webRoot
	 *            web root file
	 * @param contextName
	 *            context name
	 * @return a WebAppConfiguration instance.
	 * @throws IOException
	 */
	protected final WebAppConfiguration initWebApp(final String prefix, final File webRoot, final String contextName) throws IOException {
		Node webXMLParentNode = null;
		final File webInfFolder = new File(webRoot, HostConfiguration.WEB_INF);
		if (webInfFolder.exists()) {
			final File webXmlFile = new File(webInfFolder, HostConfiguration.WEB_XML);
			if (webXmlFile.exists()) {
				HostConfiguration.logger.debug("Parsing web.xml");
				final Document webXMLDoc = new WebXmlParser(commonLibCL).parseStreamToXML(webXmlFile);
				if (webXMLDoc != null) {
					webXMLParentNode = webXMLDoc.getDocumentElement();
					HostConfiguration.logger.debug("Finished parsing web.xml");
				} else {
					HostConfiguration.logger.debug("Failure parsing the web.xml file. Ignoring and continuing as if no web.xml was found.");

				}
			}
		}
		// Instantiate the webAppConfig
		return new WebAppConfiguration(this, cluster, jndiManager, webRoot.getCanonicalPath(), prefix, objectPool, args, webXMLParentNode, commonLibCL, contextName);
	}

	/**
	 * @return host name
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * Destroy this webapp instance. Kills the webapps, plus any servlets,
	 * attributes, etc
	 * 
	 * @param webApp
	 *            The webapp to destroy
	 */
	private void destroyWebApp(final String prefix) {
		final WebAppConfiguration webAppConfig = webapps.get(prefix);
		if (webAppConfig != null) {
			webAppConfig.destroy();
			webapps.remove(prefix);
		}
	}

	/**
	 * Destroy all webapplication.
	 */
	public void destroy() {
		for (String prefixe : webapps.keySet()) {
			destroyWebApp(prefixe);
		}
		if (thread != null) {
			thread.interrupt();
		}
	}

	/**
	 * Invalidate all expired sessions.
	 */
	public void invalidateExpiredSessions() {
		for (WebAppConfiguration webapp : webapps.values()) {
			webapp.invalidateExpiredSessions();
		}
	}

	/**
	 * Main method of thread which invalidate Expired Sessions every 60s.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		boolean interrupted = false;
		while (!interrupted) {
			try {
				Thread.sleep(HostConfiguration.FLUSH_PERIOD);
				invalidateExpiredSessions();
			} catch (final InterruptedException err) {
				interrupted = true;
			}
		}
		thread = null;
	}

	/**
	 * Reload Specified Webapplication.
	 * 
	 * @param prefix
	 * @throws IOException
	 */
	public void reloadWebApp(final String prefix) throws IOException {
		final WebAppConfiguration webAppConfig = webapps.get(prefix);
		if (webAppConfig != null) {
			final String webRoot = webAppConfig.getWebroot();
			final String contextName = webAppConfig.getContextName();
			destroyWebApp(prefix);
			try {
				final WebAppConfiguration webAppConfiguration = initWebApp(prefix, new File(webRoot), contextName);
				webapps.put(webAppConfiguration.getContextPath(), webAppConfiguration);
			} catch (final Throwable err) {
				HostConfiguration.logger.error("Error initializing web application: prefix [" + prefix + "]", err);
			}
		} else {
			throw new WinstoneException("Unknown webapp prefix: " + prefix);
		}
	}

	/**
	 * Setup the webroot. If a warfile is supplied, extract any files that the
	 * war file is newer than. If none is supplied, use the default temp
	 * directory.
	 * 
	 * @param requestedWebroot
	 * @param warfile
	 * @return
	 * @throws IOException
	 */
	protected File getWebRoot(File requestedWebroot, File warfile) throws IOException {
		if (warfile != null) {
			HostConfiguration.logger.info("Beginning extraction from war file");
			// open the war file
			if (!warfile.exists() || !warfile.isFile()) {
				throw new WinstoneException("The warfile supplied is unavailable or invalid (" + warfile + ")");
			}

			// Get the webroot folder (or a temp dir if none supplied)
			File unzippedDir = null;
			if (requestedWebroot != null) {
				unzippedDir = requestedWebroot;
			} else {
				// compute which temp directory to use
				String tempDirectory = StringUtils.stringArg(args, "tempDirectory", null);
				String child = "winstone" + File.separator;
				if (tempDirectory == null) {
					// find default temp directory
					// System.getProperty("");
					final File tempFile = File.createTempFile("dummy", "dummy");
					tempDirectory = tempFile.getParent();
					tempFile.delete();
					final String userName = System.getProperty("user.name");
					if (userName != null) {
						child += StringUtils.replace(userName, new String[][] { { "/", "" }, { "\\", "" }, { ",", "" } }) + File.separator;
					}
				}
				if (hostname != null) {
					child += hostname + File.separator;
				}
				child += warfile.getName();
				unzippedDir = new File(tempDirectory, child);
			}
			if (unzippedDir.exists()) {
				if (!unzippedDir.isDirectory()) {
					throw new WinstoneException("The webroot supplied is not a valid directory (" + unzippedDir.getPath() + ")");
				} else {
					HostConfiguration.logger.debug("The webroot supplied already exists - overwriting where newer ({})", unzippedDir.getCanonicalPath());
				}
			}

			// check consistency and if out-of-sync, recreate
			File timestampFile = new File(unzippedDir, ".timestamp");
			if (!timestampFile.exists() || Math.abs(timestampFile.lastModified() - warfile.lastModified()) > 1000) {
				// contents of the target directory is inconsistent from the
				// war.
				FileUtils.delete(unzippedDir);
				unzippedDir.mkdirs();
			} else {
				// files are up to date
				return unzippedDir;
			}

			// Iterate through the files
			final JarFile warArchive = new JarFile(warfile);
			for (final Enumeration<JarEntry> e = warArchive.entries(); e.hasMoreElements();) {
				final JarEntry element = e.nextElement();
				if (element.isDirectory()) {
					continue;
				}
				final String elemName = element.getName();

				// If archive date is newer than unzipped file, overwrite
				final File outFile = new File(unzippedDir, elemName);
				if (outFile.exists() && (outFile.lastModified() > warfile.lastModified())) {
					continue;
				}

				outFile.getParentFile().mkdirs();
				final byte buffer[] = new byte[8192];

				// Copy out the extracted file
				final InputStream inContent = warArchive.getInputStream(element);
				final OutputStream outStream = new FileOutputStream(outFile);
				int readBytes = inContent.read(buffer);
				while (readBytes != -1) {
					outStream.write(buffer, 0, readBytes);
					readBytes = inContent.read(buffer);
				}
				inContent.close();
				outStream.close();
			}
			// extraction completed
			new FileOutputStream(timestampFile).close();
			timestampFile.setLastModified(warfile.lastModified());

			// Return webroot
			return unzippedDir;
		} else {
			return requestedWebroot;
		}
	}

	/**
	 * Initialize host with multiple webapplication from specified directory.
	 * 
	 * @param webappsDirName
	 * @throws IOException
	 */
	protected final void initMultiWebappDir(String webappsDirName) throws IOException {
		if (webappsDirName == null) {
			webappsDirName = "webapps";
		}
		final File webappsDir = new File(webappsDirName);
		if (!webappsDir.exists()) {
			throw new WinstoneException("Webapps dir " + webappsDirName + " not found");
		} else if (!webappsDir.isDirectory()) {
			throw new WinstoneException("Webapps dir " + webappsDirName + " is not a directory");
		} else {
			final File children[] = webappsDir.listFiles();
			for (File aChildren : children) {
				final String childName = aChildren.getName();
				// Check any directories for warfiles that match, and skip: only
				// deploy the war file
				if (aChildren.isDirectory()) {
					final File matchingWarFile = new File(webappsDir, aChildren.getName() + ".war");
					if (matchingWarFile.exists() && matchingWarFile.isFile()) {
						HostConfiguration.logger.debug("Webapp dir deployment {} skipped, since there is a war file of the same name to check for re-extraction", childName);
					} else {
						final String prefix = childName.equalsIgnoreCase("ROOT") ? "" : "/" + childName;
						if (!webapps.containsKey(prefix)) {
							try {
								final WebAppConfiguration webAppConfig = initWebApp(prefix, aChildren, childName);
								webapps.put(webAppConfig.getContextPath(), webAppConfig);
								HostConfiguration.logger.info("Deployed web application found at {}", childName);
							} catch (final Throwable err) {
								HostConfiguration.logger.error("Error initializing web application: prefix [" + prefix + "]", err);
							}
						}
					}
				} else if (childName.endsWith(".war")) {
					final String outputName = childName.substring(0, childName.lastIndexOf(".war"));
					final String prefix = outputName.equalsIgnoreCase("ROOT") ? "" : "/" + outputName;

					if (!webapps.containsKey(prefix)) {
						final File outputDir = new File(webappsDir, outputName);
						outputDir.mkdirs();
						try {
							final WebAppConfiguration webAppConfig = initWebApp(prefix, getWebRoot(new File(webappsDir, outputName), aChildren), outputName);
							webapps.put(webAppConfig.getContextPath(), webAppConfig);
							HostConfiguration.logger.info("Deployed web application found at {}", childName);
						} catch (final Throwable err) {
							HostConfiguration.logger.error("Error initializing web application: prefix [" + prefix + "]", err);
						}
					}
				}
			}
		}
	}

	/**
	 * @param sessionKey
	 * @return a WebAppConfiguration instance for specified session key or null
	 *         if none was found.
	 */
	public WebAppConfiguration getWebAppBySessionKey(final String sessionKey) {
		for (WebAppConfiguration webAppConfiguration : webapps.values()) {
			final WinstoneSession session = webAppConfiguration.getSessionById(sessionKey, false);
			if (session != null) {
				return webAppConfiguration;
			}
		}
		return null;
	}
}
