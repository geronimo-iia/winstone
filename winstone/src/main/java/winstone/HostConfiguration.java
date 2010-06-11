/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Manages the references to individual webapps within the container. This object handles the mapping of url-prefixes to webapps, and init
 * and shutdown of any webapps it manages.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HostConfiguration.java,v 1.8 2007/08/02 06:16:00 rickknowles Exp $
 */
public class HostConfiguration implements Runnable {
    
    private static final long FLUSH_PERIOD = 60000L;
    
    private static final String WEB_INF = "WEB-INF";
    private static final String WEB_XML = "web.xml";
    
    private String hostname;
    private Map<String, String> args;
    private Map<String, WebAppConfiguration> webapps;
    private Cluster cluster;
    private ObjectPool objectPool;
    private ClassLoader commonLibCL;
    private File commonLibCLPaths[];
    
    private Thread thread;
    
    /**
     * Build a new instance of HostConfiguration.
     * 
     * @param hostname
     * @param commonLibCL
     * @param commonLibCLPaths
     * @param args
     * @param webappsDirName
     * @throws IOException
     */
    public HostConfiguration(String hostname, ClassLoader commonLibCL, File commonLibCLPaths[], Map<String, String> args, String webappsDirName) throws IOException {
        this(hostname, null, null, commonLibCL, commonLibCLPaths, args, webappsDirName);
    }
    
    public HostConfiguration(String hostname, Cluster cluster, ObjectPool objectPool, ClassLoader commonLibCL, File commonLibCLPaths[], Map<String, String> args, String webappsDirName) throws IOException {
        this.hostname = hostname;
        this.args = args;
        this.webapps = new Hashtable<String, WebAppConfiguration>();
        this.cluster = cluster;
        this.objectPool = objectPool;
        this.commonLibCL = commonLibCL;
        this.commonLibCLPaths = commonLibCLPaths;
        
        // Is this the single or multiple configuration ? Check args
        String warfile = (String)args.get("warfile");
        String webroot = (String)args.get("webroot");
        
        // If single-webapp mode
        if ((webappsDirName == null) && ((warfile != null) || (webroot != null))) {
            String prefix = (String)args.get("prefix");
            if (prefix == null) {
                prefix = "";
            }
            try {
                WebAppConfiguration webAppConfiguration = initWebApp(prefix, getWebRoot(webroot, warfile), "webapp");
                this.webapps.put(webAppConfiguration.getContextPath(), webAppConfiguration);
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES, "HostConfig.WebappInitError", prefix, err);
            }
        }
        // Otherwise multi-webapp mode
        else {
            initMultiWebappDir(webappsDirName);
        }
        Logger.log(Logger.DEBUG, Launcher.RESOURCES, "HostConfig.InitComplete", new String[] {
            this.webapps.size() + "", this.webapps.keySet() + ""
        });
        
        this.thread = new Thread(this, "WinstoneHostConfigurationMgmt:" + this.hostname);
        this.thread.setDaemon(true);
        this.thread.start();
    }
    
    public WebAppConfiguration getWebAppByURI(String uri) {
        if (uri == null) {
            return null;
        } else if (uri.equals("/") || uri.equals("")) {
            return (WebAppConfiguration)this.webapps.get("");
        } else if (uri.startsWith("/")) {
            String decoded = WinstoneRequest.decodeURLToken(uri);
            String noLeadingSlash = decoded.substring(1);
            int slashPos = noLeadingSlash.indexOf("/");
            if (slashPos == -1) {
                return (WebAppConfiguration)this.webapps.get(decoded);
            } else {
                return (WebAppConfiguration)this.webapps.get(decoded.substring(0, slashPos + 1));
            }
        } else {
            return null;
        }
    }
    
    protected WebAppConfiguration initWebApp(String prefix, File webRoot, String contextName) throws IOException {
        Node webXMLParentNode = null;
        File webInfFolder = new File(webRoot, WEB_INF);
        if (webInfFolder.exists()) {
            File webXmlFile = new File(webInfFolder, WEB_XML);
            if (webXmlFile.exists()) {
                Logger.log(Logger.DEBUG, Launcher.RESOURCES, "HostConfig.ParsingWebXml");
                Document webXMLDoc = new WebXmlParser(this.commonLibCL).parseStreamToXML(webXmlFile);
                if (webXMLDoc != null) {
                    webXMLParentNode = webXMLDoc.getDocumentElement();
                    Logger.log(Logger.DEBUG, Launcher.RESOURCES, "HostConfig.WebXmlParseComplete");
                } else {
                    Logger.log(Logger.DEBUG, Launcher.RESOURCES, "HostConfig.WebXmlParseFailed");
                }
            }
        }
        
        // Instantiate the webAppConfig
        return new WebAppConfiguration(this, this.cluster, webRoot.getCanonicalPath(), prefix, this.objectPool, this.args, webXMLParentNode, this.commonLibCL, this.commonLibCLPaths, contextName);
    }
    
    public String getHostname() {
        return this.hostname;
    }
    
    /**
     * Destroy this webapp instance. Kills the webapps, plus any servlets, attributes, etc
     * 
     * @param webApp The webapp to destroy
     */
    private void destroyWebApp(String prefix) {
        WebAppConfiguration webAppConfig = (WebAppConfiguration)this.webapps.get(prefix);
        if (webAppConfig != null) {
            webAppConfig.destroy();
            this.webapps.remove(prefix);
        }
    }
    
    public void destroy() {
        Set<String> prefixes = new HashSet<String>(this.webapps.keySet());
        for (Iterator<String> i = prefixes.iterator(); i.hasNext();) {
            destroyWebApp((String)i.next());
        }
        if (this.thread != null) {
            this.thread.interrupt();
        }
    }
    
    public void invalidateExpiredSessions() {
        Set<WebAppConfiguration> webapps = new HashSet<WebAppConfiguration>(this.webapps.values());
        for (Iterator<WebAppConfiguration> i = webapps.iterator(); i.hasNext();) {
            ((WebAppConfiguration)i.next()).invalidateExpiredSessions();
        }
    }
    
    public void run() {
        boolean interrupted = false;
        while (!interrupted) {
            try {
                Thread.sleep(FLUSH_PERIOD);
                invalidateExpiredSessions();
            } catch (InterruptedException err) {
                interrupted = true;
            }
        }
        this.thread = null;
    }
    
    public void reloadWebApp(String prefix) throws IOException {
        WebAppConfiguration webAppConfig = (WebAppConfiguration)this.webapps.get(prefix);
        if (webAppConfig != null) {
            String webRoot = webAppConfig.getWebroot();
            String contextName = webAppConfig.getContextName();
            destroyWebApp(prefix);
            try {
                WebAppConfiguration webAppConfiguration = initWebApp(prefix, new File(webRoot), contextName);
                this.webapps.put(webAppConfiguration.getContextPath(), webAppConfiguration);
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES, "HostConfig.WebappInitError", prefix, err);
            }
        } else {
            throw new WinstoneException(Launcher.RESOURCES.getString("HostConfig.PrefixUnknown", prefix));
        }
    }
    
    /**
     * Setup the webroot. If a warfile is supplied, extract any files that the war file is newer than. If none is supplied, use the default
     * temp directory.
     */
    protected File getWebRoot(String requestedWebroot, String warfileName) throws IOException {
        if (warfileName != null) {
            Logger.log(Logger.INFO, Launcher.RESOURCES, "HostConfig.BeginningWarExtraction");
            
            // open the war file
            File warfileRef = new File(warfileName);
            if (!warfileRef.exists() || !warfileRef.isFile())
                throw new WinstoneException(Launcher.RESOURCES.getString("HostConfig.WarFileInvalid", warfileName));
            
            // Get the webroot folder (or a temp dir if none supplied)
            File unzippedDir = null;
            if (requestedWebroot != null) {
                unzippedDir = new File(requestedWebroot);
            } else {
                // compute which temp directory to use
                String tempDirectory = WebAppConfiguration.stringArg(args, "tempDirectory", null);
                String child = "winstone" + File.pathSeparator;
                if (tempDirectory == null) {
                    // find default temp directory
                    // System.getProperty("");
                    File tempFile = File.createTempFile("dummy", "dummy");
                    tempDirectory = tempFile.getParent();
                    tempFile.delete();
                    String userName = System.getProperty("user.name");
                    if (userName != null) {
                        child += WinstoneResourceBundle.globalReplace(userName, new String[][] {
                            {
                                "/", ""
                            }, {
                                "\\", ""
                            }, {
                                ",", ""
                            }
                        }) + File.pathSeparator;
                    }
                }
                if (hostname != null) {
                    child += hostname + File.pathSeparator;
                }
                child += warfileRef.getName();
                unzippedDir = new File(tempDirectory, child);
            }
            if (unzippedDir.exists()) {
                if (!unzippedDir.isDirectory()) {
                    throw new WinstoneException(Launcher.RESOURCES.getString("HostConfig.WebRootNotDirectory", unzippedDir.getPath()));
                } else {
                    Logger.log(Logger.DEBUG, Launcher.RESOURCES, "HostConfig.WebRootExists", unzippedDir.getCanonicalPath());
                }
            } else {
                unzippedDir.mkdirs();
            }
            
            // Iterate through the files
            JarFile warArchive = new JarFile(warfileRef);
            for (Enumeration<JarEntry> e = warArchive.entries(); e.hasMoreElements();) {
                JarEntry element = e.nextElement();
                if (element.isDirectory()) {
                    continue;
                }
                String elemName = element.getName();
                
                // If archive date is newer than unzipped file, overwrite
                File outFile = new File(unzippedDir, elemName);
                if (outFile.exists() && (outFile.lastModified() > warfileRef.lastModified())) {
                    continue;
                }
                outFile.getParentFile().mkdirs();
                byte buffer[] = new byte[8192];
                
                // Copy out the extracted file
                InputStream inContent = warArchive.getInputStream(element);
                OutputStream outStream = new FileOutputStream(outFile);
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
    
    protected void initMultiWebappDir(String webappsDirName) throws IOException {
        if (webappsDirName == null) {
            webappsDirName = "webapps";
        }
        File webappsDir = new File(webappsDirName);
        if (!webappsDir.exists()) {
            throw new WinstoneException(Launcher.RESOURCES.getString("HostConfig.WebAppDirNotFound", webappsDirName));
        } else if (!webappsDir.isDirectory()) {
            throw new WinstoneException(Launcher.RESOURCES.getString("HostConfig.WebAppDirIsNotDirectory", webappsDirName));
        } else {
            File children[] = webappsDir.listFiles();
            for (int n = 0; n < children.length; n++) {
                String childName = children[n].getName();
                
                // Check any directories for warfiles that match, and skip: only deploy the war file
                if (children[n].isDirectory()) {
                    File matchingWarFile = new File(webappsDir, children[n].getName() + ".war");
                    if (matchingWarFile.exists() && matchingWarFile.isFile()) {
                        Logger.log(Logger.DEBUG, Launcher.RESOURCES, "HostConfig.SkippingWarfileDir", childName);
                    } else {
                        String prefix = childName.equalsIgnoreCase("ROOT") ? "" : "/" + childName;
                        if (!this.webapps.containsKey(prefix)) {
                            try {
                                WebAppConfiguration webAppConfig = initWebApp(prefix, children[n], childName);
                                this.webapps.put(webAppConfig.getContextPath(), webAppConfig);
                                Logger.log(Logger.INFO, Launcher.RESOURCES, "HostConfig.DeployingWebapp", childName);
                            } catch (Throwable err) {
                                Logger.log(Logger.ERROR, Launcher.RESOURCES, "HostConfig.WebappInitError", prefix, err);
                            }
                        }
                    }
                } else if (childName.endsWith(".war")) {
                    String outputName = childName.substring(0, childName.lastIndexOf(".war"));
                    String prefix = outputName.equalsIgnoreCase("ROOT") ? "" : "/" + outputName;
                    
                    if (!this.webapps.containsKey(prefix)) {
                        File outputDir = new File(webappsDir, outputName);
                        outputDir.mkdirs();
                        try {
                            WebAppConfiguration webAppConfig = initWebApp(prefix, getWebRoot(new File(webappsDir, outputName).getCanonicalPath(), children[n].getCanonicalPath()), outputName);
                            this.webapps.put(webAppConfig.getContextPath(), webAppConfig);
                            Logger.log(Logger.INFO, Launcher.RESOURCES, "HostConfig.DeployingWebapp", childName);
                        } catch (Throwable err) {
                            Logger.log(Logger.ERROR, Launcher.RESOURCES, "HostConfig.WebappInitError", prefix, err);
                        }
                    }
                }
            }
        }
    }
    
    public WebAppConfiguration getWebAppBySessionKey(String sessionKey) {
        List<WebAppConfiguration> allwebapps = new ArrayList<WebAppConfiguration>(this.webapps.values());
        for (Iterator<WebAppConfiguration> i = allwebapps.iterator(); i.hasNext();) {
            WebAppConfiguration webapp = i.next();
            WinstoneSession session = webapp.getSessionById(sessionKey, false);
            if (session != null) {
                return webapp;
            }
        }
        return null;
    }
}
