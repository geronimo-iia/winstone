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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.winstone.WinstoneException;
import net.winstone.cluster.Cluster;
import net.winstone.jndi.JndiManager;
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
	private static final long FLUSH_PERIOD = 60000L;
	private static final String WEB_INF = "WEB-INF";
	private static final String WEB_XML = "web.xml";
	private final String hostname;
	private final Map<String, String> args;
	private final Map<String, WebAppConfiguration> webapps;
	private final Cluster cluster;
	private final ObjectPool objectPool;
	private final ClassLoader commonLibCL;
	private final JndiManager jndiManager;
	private Thread thread;

	public HostConfiguration(final String hostname, final Cluster cluster, final ObjectPool objectPool, final JndiManager jndiManager, final ClassLoader commonLibCL, final Map<String, String> args, final String webappsDirName) throws IOException {
		this.hostname = hostname;
		this.args = args;
		webapps = new HashMap<String, WebAppConfiguration>();
		this.cluster = cluster;
		this.objectPool = objectPool;
		this.commonLibCL = commonLibCL;
		this.jndiManager = jndiManager;
		// Is this the single or multiple configuration ? Check args
		final String warfile = args.get("warfile");
		final String webroot = args.get("webroot");

		// If single-webapp mode
		if ((webappsDirName == null) && ((warfile != null) || (webroot != null))) {
			String prefix = args.get("prefix");
			if (prefix == null) {
				prefix = "";
			}
			try {
				final WebAppConfiguration webAppConfiguration = initWebApp(prefix, getWebRoot(webroot, warfile), "webapp");
				webapps.put(webAppConfiguration.getContextPath(), webAppConfiguration);
			} catch (final Throwable err) {
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

	public void destroy() {
		final Set<String> prefixes = new HashSet<String>(webapps.keySet());
		for (final Iterator<String> i = prefixes.iterator(); i.hasNext();) {
			destroyWebApp(i.next());
		}
		if (thread != null) {
			thread.interrupt();
		}
	}

	public void invalidateExpiredSessions() {
		final Set<WebAppConfiguration> webappConfiguration = new HashSet<WebAppConfiguration>(webapps.values());
		for (final Iterator<WebAppConfiguration> i = webappConfiguration.iterator(); i.hasNext();) {
			i.next().invalidateExpiredSessions();
		}
	}

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
	 */
	protected final File getWebRoot(final String requestedWebroot, final String warfileName) throws IOException {
		if (warfileName != null) {
			HostConfiguration.logger.info("Beginning extraction from war file");

			// open the war file
			final File warfileRef = new File(warfileName);
			if (!warfileRef.exists() || !warfileRef.isFile()) {
				throw new WinstoneException("The warfile supplied is unavailable or invalid (" + warfileName + ")");
			}

			// Get the webroot folder (or a temp dir if none supplied)
			File unzippedDir = null;
			if (requestedWebroot != null) {
				unzippedDir = new File(requestedWebroot);
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
				child += warfileRef.getName();
				unzippedDir = new File(tempDirectory, child);
			}
			if (unzippedDir.exists()) {
				if (!unzippedDir.isDirectory()) {
					throw new WinstoneException("The webroot supplied is not a valid directory (" + unzippedDir.getPath() + ")");
				} else {
					HostConfiguration.logger.debug("The webroot supplied already exists - overwriting where newer ({})", unzippedDir.getCanonicalPath());
				}
			} else {
				unzippedDir.mkdirs();
			}

			// Iterate through the files
			final JarFile warArchive = new JarFile(warfileRef);
			for (final Enumeration<JarEntry> e = warArchive.entries(); e.hasMoreElements();) {
				final JarEntry element = e.nextElement();
				if (element.isDirectory()) {
					continue;
				}
				final String elemName = element.getName();

				// If archive date is newer than unzipped file, overwrite
				final File outFile = new File(unzippedDir, elemName);
				if (outFile.exists() && (outFile.lastModified() > warfileRef.lastModified())) {
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

			// Return webroot
			return unzippedDir;
		} else {
			return new File(requestedWebroot);
		}
	}

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
			for (int n = 0; n < children.length; n++) {
				final String childName = children[n].getName();

				// Check any directories for warfiles that match, and skip: only
				// deploy the war file
				if (children[n].isDirectory()) {
					final File matchingWarFile = new File(webappsDir, children[n].getName() + ".war");
					if (matchingWarFile.exists() && matchingWarFile.isFile()) {
						HostConfiguration.logger.debug("Webapp dir deployment {} skipped, since there is a war file of the same name to check for re-extraction", childName);
					} else {
						final String prefix = childName.equalsIgnoreCase("ROOT") ? "" : "/" + childName;
						if (!webapps.containsKey(prefix)) {
							try {
								final WebAppConfiguration webAppConfig = initWebApp(prefix, children[n], childName);
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
							final WebAppConfiguration webAppConfig = initWebApp(prefix, getWebRoot(new File(webappsDir, outputName).getCanonicalPath(), children[n].getCanonicalPath()), outputName);
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

	public WebAppConfiguration getWebAppBySessionKey(final String sessionKey) {
		final List<WebAppConfiguration> allwebapps = new ArrayList<WebAppConfiguration>(webapps.values());
		for (final Iterator<WebAppConfiguration> i = allwebapps.iterator(); i.hasNext();) {
			final WebAppConfiguration webapp = i.next();
			final WinstoneSession session = webapp.getSessionById(sessionKey, false);
			if (session != null) {
				return webapp;
			}
		}
		return null;
	}
}
