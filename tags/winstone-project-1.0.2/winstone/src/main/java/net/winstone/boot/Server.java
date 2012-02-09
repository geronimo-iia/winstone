package net.winstone.boot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.winstone.WinstoneException;
import net.winstone.WinstoneResourceBundle;
import net.winstone.cluster.Cluster;
import net.winstone.cluster.SimpleCluster;
import net.winstone.core.HostConfiguration;
import net.winstone.core.HostGroup;
import net.winstone.core.ObjectPool;
import net.winstone.core.listener.Ajp13Listener;
import net.winstone.core.listener.HttpListener;
import net.winstone.core.listener.HttpsListener;
import net.winstone.core.listener.Listener;
import net.winstone.jndi.JndiManager;
import net.winstone.jndi.resources.DataSourceConfig;
import net.winstone.util.LifeCycle;
import net.winstone.util.MapConverter;
import net.winstone.util.StringUtils;

import org.slf4j.LoggerFactory;

/**
 * Implements the main launcher daemon thread.
 * 
 * @author Jerome Guibert
 */
public class Server implements LifeCycle {

    protected static org.slf4j.Logger logger = LoggerFactory.getLogger(Server.class);
    private int DEFAULT_CONTROL_PORT = -1;
    // parameter
    private final Map<String, String> args;
    private final ClassLoader commonLibClassLoader;
    // control
    private Thread controlThread = null;
    private int controlPort = DEFAULT_CONTROL_PORT;
    // host and cluster
    private HostGroup hostGroup = null;
    private Cluster cluster = null;
    // object pool
    private ObjectPool objectPool = null;
    // listener
    private List<Listener> listeners = null;
    // jndi manager
    private JndiManager globalJndiManager = null;

    /**
     * Build a new instance of server.
     * @param args configuration
     * @param commonLibClassLoader class loader of common lib folder
     * @throws IllegalArgumentException
     */
    public Server(final Map<String, String> args, final ClassLoader commonLibClassLoader) throws IllegalArgumentException {
        super();
        if (args == null) {
            throw new IllegalArgumentException("arg can not be null or empty");
        }
        this.args = args;
        this.commonLibClassLoader = commonLibClassLoader;
    }

    /**
     * Start server
     */
    public void start() {
        initialize();
    }

    /**
     * initialises the web app, object pools, control port and the available protocol listeners.
     */
    @Override
    public void initialize() {
        try {
            logger.debug("Winstone startup arguments: {}", args.toString());
            initializeJndi();
            objectPool = new ObjectPool(args);
            controlPort = (args.get("controlPort") == null ? DEFAULT_CONTROL_PORT : Integer.parseInt((String) args.get("controlPort")));
            initializeCluster();
            // Open the web apps
            hostGroup = new HostGroup(this.cluster, this.objectPool, globalJndiManager, commonLibClassLoader, args);
            initializeListener();
            if (!listeners.isEmpty()) {
                controlThread = new Thread(new ServerControlThread(), "LauncherControlThread[ControlPort=" + Integer.toString(this.controlPort) + "]]");
                controlThread.setDaemon(false);
                controlThread.start(); 
            }
            Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
        } catch (IOException iOException) {
            throw new WinstoneException("Server.initialize", iOException);
        }
    }

    @Override
    public void destroy() {
        // Release all listeners/pools/webapps
        if (listeners != null) {
            for (Iterator<Listener> i = this.listeners.iterator(); i.hasNext();) {
                i.next().destroy();
            }
            listeners.clear();
            listeners = null;
        }
        if (objectPool != null) {
            this.objectPool.destroy();
            this.objectPool = null;
        }
        if (this.cluster != null) {
            this.cluster.destroy();
            cluster = null;
        }
        if (hostGroup != null) {
            this.hostGroup.destroy();
            hostGroup = null;
        }
        if (this.globalJndiManager != null) {
            this.globalJndiManager.destroy();
            globalJndiManager = null;
        }
        if (this.controlThread != null) {
            this.controlThread.interrupt();
            controlThread = null;
        }
    }

    public void shutdown() {
    	logger.info("Winstone shutdown...");
        destroy();
        Thread.yield();
        logger.info("Exiting");
    }

    /**
     * @return true is the server is running.
     */
    public boolean isRunning() {
        return (this.controlThread != null) && this.controlThread.isAlive();
    }

    /**
     * Instanciate listener.
     */
    private void initializeListener() {
        //TODO create a parameters for this list 
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
    }

    /**
     * Instanciate cluster if needed.
     */
    private void initializeCluster() {
        // Optionally set up clustering if enabled and libraries are available
        String useCluster = (String) args.get("useCluster");
        boolean switchOnCluster = (useCluster != null) && (useCluster.equalsIgnoreCase("true") || useCluster.equalsIgnoreCase("yes"));
        if (switchOnCluster) {
            if (this.controlPort < 0) {
                logger.info("Clustering disabled - control port must be enabled");
            } else {
                String clusterClassName = StringUtils.stringArg(args, "clusterClassName", SimpleCluster.class.getName()).trim();
                try {
                    Class<?> clusterClass = Class.forName(clusterClassName);
                    Constructor<?> clusterConstructor = clusterClass.getConstructor(new Class[]{Map.class, Integer.class});
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
    }

    /**
     * Instanciate Jndi Manaher if needed.
     */
    private void initializeJndi() {
        // If jndi is enabled, run the container wide jndi populator
        if (StringUtils.booleanArg(args, "useJNDI", false)) {
            // Set jndi resource handler if not set (workaround for JamVM bug)
            try {
                Class<?> ctxFactoryClass = Class.forName("net.winstone.jndi.url.java.javaURLContextFactory");
                if (System.getProperty("java.naming.factory.initial") == null) {
                    System.setProperty("java.naming.factory.initial", ctxFactoryClass.getName());
                }
                if (System.getProperty("java.naming.factory.url.pkgs") == null) {
                    System.setProperty("java.naming.factory.url.pkgs", "net.winstone.jndi");
                }
            } catch (ClassNotFoundException err) {
            }
            // instanciate Jndi Manager
            String jndiMgrClassName = StringUtils.stringArg(args, "containerJndiClassName", JndiManager.class.getName()).trim();
            try {
                // Build the realm
                Class<?> jndiMgrClass = Class.forName(jndiMgrClassName, true, commonLibClassLoader);
                this.globalJndiManager = (JndiManager) jndiMgrClass.newInstance();
                this.globalJndiManager.initialize();
                logger.info("JNDI Started {}", jndiMgrClass.getName());
            } catch (ClassNotFoundException err) {
                logger.debug("JNDI disabled at container level - can't find JNDI Manager class");
            } catch (Throwable err) {
                logger.error("JNDI disabled at container level - couldn't load JNDI Manager: " + jndiMgrClassName, err);
            }
            // instanciate data
            Collection<String> keys = new ArrayList<String>(args != null ? args.keySet() : (Collection<String>) new ArrayList<String>());
            for (Iterator<String> i = keys.iterator(); i.hasNext();) {
                String key = (String) i.next();
                if (key.startsWith("jndi.resource.")) {
                    String resourceName = key.substring(14);
                    String className = args.get(key);
                    String value = args.get("jndi.param." + resourceName + ".value");
                    logger.debug("Creating object: {} from startup arguments", resourceName);
                    createObject(resourceName.trim(), className.trim(), value, args, commonLibClassLoader);
                }
            }



        }
    }

    /**
     * Build an object to insert into the jndi space
     */
    protected final boolean createObject(String name, String className, String value, Map<String, String> args, ClassLoader loader) {
        // basic check
        if ((className == null) || (name == null)) {
            return false;
        }

        // If we are working with a datasource
        if (className.equals("javax.sql.DataSource")) {
            try {
                DataSourceConfig dataSourceConfig = MapConverter.apply(extractRelevantArgs(args, name), new DataSourceConfig());
                globalJndiManager.bind(dataSourceConfig, loader);
                return true;
            } catch (Throwable err) {
                logger.error("Error building JDBC Datasource object " + name, err);
            }
        } // If we are working with a mail session
        else if (className.equals("javax.mail.Session")) {
            try {
                Properties p = new Properties();
                p.putAll(extractRelevantArgs(args, name));
                globalJndiManager.bindSmtpSession(name, p, loader);
                return true;
            } catch (Throwable err) {
                logger.error("Error building JavaMail session " + name, err);
            }
        } // If unknown type, try to instantiate with the string constructor
        else if (value != null) {
            try {
                globalJndiManager.bind(name, className, value, loader);
                return true;
            } catch (Throwable err) {
                logger.error("Error building JNDI object " + name + " (class: " + className + ")", err);
            }
        }

        return false;
    }

    /**
     * Rips the parameters relevant to a particular resource from the command args
     */
    private Map<String, String> extractRelevantArgs(final Map<String, String> input, final String name) {
        Map<String, String> relevantArgs = new HashMap<String, String>();
        for (Iterator<String> i = input.keySet().iterator(); i.hasNext();) {
            String key = i.next();
            if (key.startsWith("jndi.param." + name + ".")) {
                relevantArgs.put(key.substring(12 + name.length()), input.get(key));
            }
        }
        relevantArgs.put("name", name);
        return relevantArgs;
    }

    /**
     * Instantiates listeners. Note that an exception thrown in the constructor is interpreted as the listener being disabled, so don't do
     * anything too adventurous in the constructor, or if you do, catch and log any errors locally before rethrowing.
     */
    protected final void spawnListener(final String listenerClassName) {
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

    /**
     * ServerControl Thread.
     *
     * @author Jerome Guibert
     */
    private final class ServerControlThread implements Runnable {

        private final static transient int CONTROL_TIMEOUT = 2000; // wait 2s for control connection

        /**
         * The main run method. This handles the normal thread processing.
         */
        @Override 
        public void run() {
            boolean interrupted = false;
            try {
                ServerSocket controlSocket = null;

                if (controlPort > 0) {
                    // Without password, we limit control port on local interface or from controlAddress in parameter.
                    InetAddress inetAddress = null;
                    String controlAddress = StringUtils.stringArg(args, "controlAddress", null);
                    if (controlAddress != null) {
                        inetAddress = InetAddress.getByName(controlAddress);
                    }
                    if (inetAddress == null) {
                        inetAddress = InetAddress.getLocalHost();
                    }
                    controlSocket = new ServerSocket(controlPort, 0, inetAddress);
                    controlSocket.setSoTimeout(CONTROL_TIMEOUT);
                }
                if (logger.isInfoEnabled()) {
                    logger.info("{} running: controlPort={}", new Object[]{
                                WinstoneResourceBundle.getInstance().getString("ServerVersion"),
                                (controlPort > 0 ? Integer.toString(controlPort) : "disabled")});
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

        protected void handleControlRequest(Socket csAccepted) throws IOException {
            InputStream inSocket = null;
            OutputStream outSocket = null;
            ObjectInputStream inControl = null;
            try {
                inSocket = csAccepted.getInputStream();
                int reqType = inSocket.read();
                if ((byte) reqType == Command.SHUTDOWN.getCode()) {
                    logger.info("Shutdown request received via the controlPort. Commencing Winstone shutdowny");
                    shutdown();
                } else if ((byte) reqType == Command.RELOAD.getCode()) {
                    inControl = new ObjectInputStream(inSocket);
                    String host = inControl.readUTF();
                    String prefix = inControl.readUTF();
                    logger.info("Reload request received via the controlPort. Commencing Winstone reload from {}{}", host, prefix);
                    HostConfiguration hostConfig = hostGroup.getHostByName(host);
                    hostConfig.reloadWebApp(prefix);
                } else if (cluster != null) {
                    outSocket = csAccepted.getOutputStream();
                    cluster.clusterRequest((byte) reqType, inSocket, outSocket, csAccepted, hostGroup);
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
    }
}
