package net.winstone.boot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.winstone.Launcher;
import net.winstone.WinstoneException;
import net.winstone.WinstoneResourceBundle;
import net.winstone.cluster.Cluster;
import net.winstone.cluster.SimpleCluster;
import net.winstone.core.HostConfiguration;
import net.winstone.core.HostGroup;
import net.winstone.core.ObjectPool;
import net.winstone.core.WebAppConfiguration;
import net.winstone.core.listener.Ajp13Listener;
import net.winstone.core.listener.HttpListener;
import net.winstone.core.listener.HttpsListener;
import net.winstone.core.listener.Listener;
import net.winstone.jndi.JndiManager;
import net.winstone.util.FileUtils;
import net.winstone.util.LifeCycle;
import org.slf4j.LoggerFactory;

/**
 * Implements the main launcher daemon thread.
 * 
 * @author Jerome Guibert
 */
public class Server implements LifeCycle, Runnable {

    protected static org.slf4j.Logger logger = LoggerFactory.getLogger(Server.class);
    private int CONTROL_TIMEOUT = 2000; // wait 2s for control connection
    private int DEFAULT_CONTROL_PORT = -1;
    // parameter
    private final Map<String, String> args;
    // control
    private Thread controlThread;
    private int controlPort;
    private HostGroup hostGroup;
    private ObjectPool objectPool;
    private List<Listener> listeners;
    private Cluster cluster;
    private JndiManager globalJndiManager;

    public Server(final Map<String, String> args) throws IllegalArgumentException {
        super();
        if (args == null) {
            throw new IllegalArgumentException("arg can not be null or empty");
        }
        this.args = args;
        controlThread = null;
        controlPort = DEFAULT_CONTROL_PORT;
        hostGroup = null;
        objectPool = null;
        listeners = null;
        cluster = null;
        globalJndiManager = null;
    }

    public void start() {
        initialize();
        if (controlThread != null) {
            controlThread.start();
        }

    }

    /**
     * initialises the web app, object pools, control port and the available protocol listeners.
     */
    @Override
    public void initialize() {
        try {
            boolean useJNDI = WebAppConfiguration.booleanArg(args, "useJNDI", false);

            // Set jndi resource handler if not set (workaround for JamVM bug)
            if (useJNDI) {
                try {
                    Class<?> ctxFactoryClass = Class.forName("net.winstone.jndi.java.javaURLContextFactory");
                    if (System.getProperty("java.naming.factory.initial") == null) {
                        System.setProperty("java.naming.factory.initial", ctxFactoryClass.getName());
                    }
                    if (System.getProperty("java.naming.factory.url.pkgs") == null) {
                        System.setProperty("java.naming.factory.url.pkgs", "net.winstone.jndi");
                    }
                } catch (ClassNotFoundException err) {
                }
            }

            logger.debug("Winstone startup arguments: {}", args.toString());

            this.controlPort = (args.get("controlPort") == null ? DEFAULT_CONTROL_PORT : Integer.parseInt((String) args.get("controlPort")));

            List<URL> jars = new ArrayList<URL>();
            List<File> jspClasspaths = new ArrayList<File>();

            // Check for java home
            String defaultJavaHome = System.getProperty("java.home");
            String javaHome = WebAppConfiguration.stringArg(args, "javaHome", defaultJavaHome);
            logger.debug("Using JAVA_HOME={}", javaHome);
            String toolsJarLocation = WebAppConfiguration.stringArg(args, "toolsJar", null);
            File toolsJar = null;
            if (toolsJarLocation == null) {
                toolsJar = new File(javaHome, "lib/tools.jar");

                // first try - if it doesn't exist, try up one dir since we might have
                // the JRE home by mistake
                if (!toolsJar.exists()) {
                    File javaHome2 = new File(javaHome).getParentFile();
                    File toolsJar2 = new File(javaHome2, "lib/tools.jar");
                    if (toolsJar2.exists()) {
                        javaHome = javaHome2.getCanonicalPath();
                        toolsJar = toolsJar2;
                    }
                }
            } else {
                toolsJar = new File(toolsJarLocation);
            }

            // Add tools jar to classloader path
            if (toolsJar.exists()) {
                jars.add(toolsJar.toURI().toURL());
                jspClasspaths.add(toolsJar);
                logger.debug("Adding {} to common classpath", toolsJar.getName());
            } else if (WebAppConfiguration.booleanArg(args, "useJasper", false)) {
                logger.warn("WARNING: Tools.jar was not found - jsp compilation will cause errors. Maybe you should set JAVA_HOME using --javaHome");
            }

            // Set up common lib class loader
            String commonLibCLFolder = WebAppConfiguration.stringArg(args, "commonLibFolder", "lib");
            File libFolder = new File(commonLibCLFolder);
            if (libFolder.exists() && libFolder.isDirectory()) {
                logger.debug("Using common lib folder: {}", libFolder.getCanonicalPath());
                File children[] = libFolder.listFiles();
                for (int n = 0; n < children.length; n++) {
                    if (children[n].getName().endsWith(".jar") || children[n].getName().endsWith(".zip")) {
                        jars.add(children[n].toURI().toURL());
                        //jars.add(children[n].toURL());
                        jspClasspaths.add(children[n]);
                        logger.debug("Adding {} to common classpath", children[n].getName());
                    }
                }
            } else {
                logger.debug("No common lib folder found");
            }
            try {
                // try to find in META-INF/lib
                //TODO explode them in a temp folder?
                String[] childrenName = FileUtils.getResourceListing(getClass(), "META-INF/lib");
                for (int n = 0; n < childrenName.length; n++) {
                    if (childrenName[n].endsWith(".jar") || childrenName[n].endsWith(".zip")) {
                        URL url = getClass().getClassLoader().getResource("META-INF/lib/" + childrenName[n]);
                        jars.add(url);
                        // add for jsp classpath in webapp!
                        //commonLibCLPaths.add(children[n]);
                        logger.debug("Adding {} to common classpath", childrenName[n]);
                    }
                }
            } catch (URISyntaxException ex) {
                logger.error("Reading META-INF/lib", ex);
            }



            ClassLoader commonLibCL = new URLClassLoader((URL[]) jars.toArray(new URL[jars.size()]), getClass().getClassLoader());
            logger.debug("Initializing Common Lib classloader: {}", commonLibCL.toString());
            logger.debug("Initializing JSP Common Lib classloader: {}", jspClasspaths.toString());
            /** calcule de m'attribut pour les jsp */
            StringBuilder cp = new StringBuilder();
            File[] fa = (File[]) jspClasspaths.toArray(new File[0]);
            for (int n = 0; n < fa.length; n++) {
                cp.append(fa[n].getCanonicalPath()).append(File.pathSeparatorChar);
            }
            String jspClasspath = cp.length() > 0 ? cp.substring(0, cp.length() - 1) : "";

            this.objectPool = new ObjectPool(args);

            // Optionally set up clustering if enabled and libraries are available
            String useCluster = (String) args.get("useCluster");
            boolean switchOnCluster = (useCluster != null) && (useCluster.equalsIgnoreCase("true") || useCluster.equalsIgnoreCase("yes"));
            if (switchOnCluster) {
                if (this.controlPort < 0) {
                    logger.info("Clustering disabled - control port must be enabled");
                } else {
                    String clusterClassName = WebAppConfiguration.stringArg(args, "clusterClassName", SimpleCluster.class.getName()).trim();
                    try {
                        Class<?> clusterClass = Class.forName(clusterClassName);
                        Constructor<?> clusterConstructor = clusterClass.getConstructor(new Class[]{
                                    Map.class, Integer.class
                                });
                        this.cluster = (Cluster) clusterConstructor.newInstance(new Object[]{
                                    args, new Integer(this.controlPort)
                                });
                    } catch (ClassNotFoundException err) {
                        logger.debug("Clustering disabled - cluster implementation class not found");
                    } catch (Throwable err) {
                        logger.error("WARNING: Error during startup of cluster implementation - ignoring", err);
                    }
                }
            }

            // If jndi is enabled, run the container wide jndi populator
            if (useJNDI) {
                String jndiMgrClassName = WebAppConfiguration.stringArg(args, "containerJndiClassName", JndiManager.class.getName()).trim();
                try {
                    // Build the realm
                    Class<?> jndiMgrClass = Class.forName(jndiMgrClassName, true, commonLibCL);
                    this.globalJndiManager = (JndiManager) jndiMgrClass.newInstance();
                    this.globalJndiManager.initialize();
                } catch (ClassNotFoundException err) {
                    logger.debug("JNDI disabled at container level - can't find JNDI Manager class");
                } catch (Throwable err) {
                    logger.error("JNDI disabled at container level - couldn't load JNDI Manager: " + jndiMgrClassName, err);
                }
            }

            // Open the web apps
            this.hostGroup = new HostGroup(this.cluster, this.objectPool, commonLibCL, jspClasspath, args);

            // Create connectors (http, https and ajp)
            this.listeners = new ArrayList<Listener>();
            spawnListener(HttpListener.class.getName());
            spawnListener(Ajp13Listener.class.getName());
            try {
                Class.forName("javax.net.ServerSocketFactory");
                spawnListener(HttpsListener.class.getName());
            } catch (ClassNotFoundException err) {
                logger.debug("Listener class {} needs JDK1.4 support. Disabling", HttpsListener.class.getName());
            }

            this.controlThread = new Thread(this, "LauncherControlThread[ControlPort=" + Integer.toString(this.controlPort) + "]]");
            this.controlThread.setDaemon(false);
            this.controlThread.start();

            Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));

        } catch (IOException iOException) {
            throw new WinstoneException("Server.initialize", iOException);
        }
    }

    @Override
    public void destroy() {
        // Release all listeners/pools/webapps
        for (Iterator<Listener> i = this.listeners.iterator(); i.hasNext();) {
            i.next().destroy();
        }
        listeners.clear();
        listeners = null;
        this.objectPool.destroy();
        this.objectPool = null;
        if (this.cluster != null) {
            this.cluster.destroy();
            cluster = null;
        }
        this.hostGroup.destroy();
        hostGroup = null;
        if (this.globalJndiManager != null) {
            this.globalJndiManager.destroy();
            globalJndiManager = null;
        }

        if (this.controlThread != null) {
            this.controlThread.interrupt();
            controlThread = null;
        }
    }

    /**
     * The main run method. This handles the normal thread processing.
     */
    @Override
    public void run() {
        boolean interrupted = false;
        try {
            ServerSocket controlSocket = null;

            if (this.controlPort > 0) {
                // Without password, we limit control port on local interface or from controlAddress in parameter.
                InetAddress inetAddress = null;
                String controlAddress = WebAppConfiguration.stringArg(args, "controlAddress", null);
                if (controlAddress != null) {
                    inetAddress = InetAddress.getByName(controlAddress);
                }
                if (inetAddress == null) {
                    inetAddress = InetAddress.getLocalHost();
                }
                controlSocket = new ServerSocket(this.controlPort, 0, inetAddress);
                controlSocket.setSoTimeout(CONTROL_TIMEOUT);
            }
            if (logger.isInfoEnabled()) {
                logger.info("{} running: controlPort={}", new Object[]{
                            WinstoneResourceBundle.getInstance().getString("ServerVersion"),
                            (this.controlPort > 0 ? Integer.toString(this.controlPort) : "disabled")});
            }

            // Enter the main loop
            while (!interrupted) {
                // this.objectPool.removeUnusedRequestHandlers();
                // this.hostGroup.invalidateExpiredSessions();

                // Check for control request
                Socket accepted = null;
                try {
                    if (controlSocket != null) {
                        accepted = controlSocket.accept();
                        if (accepted != null) {
                            handleControlRequest(accepted);
                        }
                    } else {
                        Thread.sleep(CONTROL_TIMEOUT);
                    }
                } catch (InterruptedIOException err) {
                } catch (InterruptedException err) {
                    interrupted = true;
                } catch (Throwable err) {
                    logger.error("Error during listener init or shutdown", err);
                } finally {
                    if (accepted != null) {
                        try {
                            accepted.close();
                        } catch (IOException err) {
                        }
                    }
                    if (Thread.interrupted()) {
                        interrupted = true;
                    }
                }
            }

            // Close server socket
            if (controlSocket != null) {
                controlSocket.close();
            }
        } catch (Throwable err) {
            logger.error("Error during listener init or shutdown", err);
        }
        logger.info("Winstone shutdown successfully");
    }

    @SuppressWarnings("CallToThreadYield")
    public void shutdown() {
        destroy();
        Thread.yield();
        logger.info("Winstone shutdown successfully");
    }

    public boolean isRunning() {
        return (this.controlThread != null) && this.controlThread.isAlive();
    }

    protected void handleControlRequest(Socket csAccepted) throws IOException {
        InputStream inSocket = null;
        OutputStream outSocket = null;
        ObjectInputStream inControl = null;
        try {
            inSocket = csAccepted.getInputStream();
            int reqType = inSocket.read();
            if ((byte) reqType == Launcher.SHUTDOWN_TYPE) {
                logger.info("Shutdown request received via the controlPort. Commencing Winstone shutdowny");
                shutdown();
            } else if ((byte) reqType == Launcher.RELOAD_TYPE) {
                inControl = new ObjectInputStream(inSocket);
                String host = inControl.readUTF();
                String prefix = inControl.readUTF();
                logger.info("Reload request received via the controlPort. Commencing Winstone reload from {}{}", host, prefix);
                HostConfiguration hostConfig = this.hostGroup.getHostByName(host);
                hostConfig.reloadWebApp(prefix);
            } else if (this.cluster != null) {
                outSocket = csAccepted.getOutputStream();
                this.cluster.clusterRequest((byte) reqType, inSocket, outSocket, csAccepted, this.hostGroup);
            }
        } finally {
            if (inControl != null) {
                try {
                    inControl.close();
                } catch (IOException err) {
                }
            }
            if (inSocket != null) {
                try {
                    inSocket.close();
                } catch (IOException err) {
                }
            }
            if (outSocket != null) {
                try {
                    outSocket.close();
                } catch (IOException err) {
                }
            }
        }
    }

    /**
     * Instantiates listeners. Note that an exception thrown in the constructor is interpreted as the listener being disabled, so don't do
     * anything too adventurous in the constructor, or if you do, catch and log any errors locally before rethrowing.
     */
    protected final void spawnListener(String listenerClassName) {
        try {
            Class<?> listenerClass = Class.forName(listenerClassName);
            Constructor<?> listenerConstructor = listenerClass.getConstructor(new Class[]{
                        Map.class, ObjectPool.class, HostGroup.class
                    });
            Listener listener = (Listener) listenerConstructor.newInstance(new Object[]{
                        args, this.objectPool, this.hostGroup
                    });
            if (listener.start()) {
                this.listeners.add(listener);
            }
        } catch (ClassNotFoundException err) {
            logger.info("Listener {} not found / disabled - ignoring", listenerClassName);
        } catch (Throwable err) {
            logger.error("Error during listener startup " + listenerClassName, err);
        }
    }
}
