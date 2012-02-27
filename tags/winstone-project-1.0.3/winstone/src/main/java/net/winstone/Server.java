package net.winstone;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.winstone.boot.Command;
import net.winstone.boot.ShutdownHook;
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
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 */
public class Server implements LifeCycle {

	protected static org.slf4j.Logger logger = LoggerFactory.getLogger(Server.class);
	/**
	 * Default control port.
	 */
	private final int DEFAULT_CONTROL_PORT = -1;
	/**
	 * parameter
	 */
	private final Map<String, String> args;
	/**
	 * common libraries class loader
	 */
	private final ClassLoader commonLibClassLoader;
	/**
	 * control
	 */
	private Thread controlThread = null;
	/**
	 * control Port
	 */
	private int controlPort = DEFAULT_CONTROL_PORT;
	/**
	 * hostGroup instance.
	 */
	private HostGroup hostGroup = null;
	/**
	 * cluster instance.
	 */
	private Cluster cluster = null;
	/**
	 * object pool instance.
	 */
	private ObjectPool objectPool = null;
	/**
	 * listener instance.
	 */
	private final List<Listener> listeners = new ArrayList<Listener>();
	/**
	 * jndi manager instance.
	 */
	private JndiManager globalJndiManager = null;

	/**
	 * Build a new instance of server.
	 * 
	 * @param args
	 *            configuration
	 * @param commonLibClassLoader
	 *            class loader of common lib folder
	 * @throws IllegalArgumentException
	 *             if args or commonLibClassLoader is null.
	 */
	public Server(final Map<String, String> args, final ClassLoader commonLibClassLoader) throws IllegalArgumentException {
		super();
		if (args == null) {
			throw new IllegalArgumentException("arg can not be null or empty");
		}
		if (commonLibClassLoader == null) {
			throw new IllegalArgumentException("commonLibClassLoader can not be null");
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
	 * initialises the web app, object pools, control port and the available
	 * protocol listeners.
	 */
	@Override
	public void initialize() {
		try {
			Server.logger.debug("Winstone startup arguments: {}", args.toString());
			initializeJndi();
			objectPool = new ObjectPool(args);
			controlPort = (args.get("controlPort") == null ? DEFAULT_CONTROL_PORT : Integer.parseInt(args.get("controlPort")));
			initializeCluster();
			// Open the web apps
			hostGroup = new HostGroup(cluster, objectPool, globalJndiManager, commonLibClassLoader, args);
			initializeListener();
			if (!listeners.isEmpty()) {
				controlThread = new Thread(new ServerControlThread(), "LauncherControlThread[ControlPort=" + Integer.toString(controlPort) + "]]");
				controlThread.setDaemon(Boolean.FALSE);
				controlThread.start();
			}
			Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
		} catch (final IOException iOException) {
			try {
				destroy();
			} catch (Throwable e) {
				// IGNORE
			}
			throw new WinstoneException("Server.initialize", iOException);
		}
	}

	@Override
	public void destroy() {
		// Release all listeners/pools/webapps
		if (listeners != null) {
			for (final Listener listener : listeners) {
				listener.destroy();
			}
			listeners.clear();
		}
		if (objectPool != null) {
			objectPool.destroy();
			objectPool = null;
		}
		if (cluster != null) {
			cluster.destroy();
			cluster = null;
		}
		if (hostGroup != null) {
			hostGroup.destroy();
			hostGroup = null;
		}
		if (globalJndiManager != null) {
			globalJndiManager.destroy();
			globalJndiManager = null;
		}
		if (controlThread != null) {
			controlThread.interrupt();
			controlThread = null;
		}
	}

	/**
	 * Shutdown this server.
	 */
	public void shutdown() {
		Server.logger.info("Winstone shutdown...");
		destroy();
		Thread.yield();
		Server.logger.info("Exiting");
	}

	/**
	 * @return Boolean.TRUE is the server is running.
	 */
	public boolean isRunning() {
		return (controlThread != null) && controlThread.isAlive();
	}

	/**
	 * Instantiate listener.
	 * 
	 * @throws IOException
	 */
	private void initializeListener() throws IOException {
		// Create connectors (http, https and ajp)
		spawnListener(HttpListener.class.getName());
		spawnListener(Ajp13Listener.class.getName());
		try {
			Class.forName("javax.net.ServerSocketFactory");
			spawnListener(HttpsListener.class.getName());
		} catch (final ClassNotFoundException err) {
			Server.logger.debug("Listener class {} needs JDK1.4 support. Disabling", HttpsListener.class.getName());
		}
	}

	/**
	 * Instantiate cluster if needed.
	 */
	private void initializeCluster() {
		// Optionally set up clustering if enabled and libraries are available
		final String useCluster = args.get("useCluster");
		final boolean switchOnCluster = (useCluster != null) && (useCluster.equalsIgnoreCase("true") || useCluster.equalsIgnoreCase("yes"));
		if (switchOnCluster) {
			if (controlPort < 0) {
				Server.logger.info("Clustering disabled - control port must be enabled");
			} else {
				final String clusterClassName = StringUtils.stringArg(args, "clusterClassName", SimpleCluster.class.getName()).trim();
				try {
					final Class<?> clusterClass = Class.forName(clusterClassName);
					final Constructor<?> clusterConstructor = clusterClass.getConstructor(new Class[] { Map.class, Integer.class });
					cluster = (Cluster) clusterConstructor.newInstance(new Object[] { args, new Integer(controlPort) });
				} catch (final Throwable err) {
					Server.logger.error("WARNING: Error during startup of cluster implementation - ignoring", err);
				}
			}
		}
	}

	/**
	 * Instantiate Jndi Manager if needed.
	 */
	private void initializeJndi() {
		// If jndi is enabled, run the container wide jndi populator
		if (StringUtils.booleanArg(args, "useJNDI", Boolean.FALSE)) {
			// Set jndi resource handler if not set (workaround for JamVM bug)
			try {
				final Class<?> ctxFactoryClass = Class.forName("net.winstone.jndi.url.java.javaURLContextFactory");
				if (System.getProperty("java.naming.factory.initial") == null) {
					System.setProperty("java.naming.factory.initial", ctxFactoryClass.getName());
				}
				if (System.getProperty("java.naming.factory.url.pkgs") == null) {
					System.setProperty("java.naming.factory.url.pkgs", "net.winstone.jndi");
				}
			} catch (final ClassNotFoundException err) {
				Server.logger.error("JNDI Error ", err);
			}
			// instanciate Jndi Manager
			final String jndiMgrClassName = StringUtils.stringArg(args, "containerJndiClassName", JndiManager.class.getName()).trim();
			try {
				// Build the realm
				final Class<?> jndiMgrClass = Class.forName(jndiMgrClassName, Boolean.TRUE, commonLibClassLoader);
				globalJndiManager = (JndiManager) jndiMgrClass.newInstance();
				globalJndiManager.initialize();
				Server.logger.info("JNDI Started {}", jndiMgrClass.getName());
			} catch (final ClassNotFoundException err) {
				Server.logger.error("JNDI disabled at container level - can't find JNDI Manager class");
			} catch (final Throwable err) {
				Server.logger.error("JNDI disabled at container level - couldn't load JNDI Manager: " + jndiMgrClassName, err);
			}
			// instanciate data
			final Collection<String> keys = new ArrayList<String>(args != null ? args.keySet() : (Collection<String>) new ArrayList<String>());
			for (final Iterator<String> i = keys.iterator(); i.hasNext();) {
				final String key = i.next();
				if (key.startsWith("jndi.resource.")) {
					final String resourceName = key.substring(14);
					final String className = args.get(key);
					final String value = args.get("jndi.param." + resourceName + ".value");
					Server.logger.debug("Creating object: {} from startup arguments", resourceName);
					createObject(resourceName.trim(), className.trim(), value, args, commonLibClassLoader);
				}
			}

		}
	}

	/**
	 * Build an object to insert into the jndi space
	 */
	protected final boolean createObject(final String name, final String className, final String value, final Map<String, String> args, final ClassLoader loader) {
		// basic check
		if ((className == null) || (name == null)) {
			return Boolean.FALSE;
		}

		// If we are working with a datasource
		if (className.equals("javax.sql.DataSource")) {
			try {
				final DataSourceConfig dataSourceConfig = MapConverter.apply(extractRelevantArgs(args, name), new DataSourceConfig());
				globalJndiManager.bind(dataSourceConfig, loader);
				return Boolean.TRUE;
			} catch (final Throwable err) {
				Server.logger.error("Error building JDBC Datasource object " + name, err);
			}
		} // If we are working with a mail session
		else if (className.equals("javax.mail.Session")) {
			try {
				final Properties p = new Properties();
				p.putAll(extractRelevantArgs(args, name));
				globalJndiManager.bindSmtpSession(name, p, loader);
				return Boolean.TRUE;
			} catch (final Throwable err) {
				Server.logger.error("Error building JavaMail session " + name, err);
			}
		} // If unknown type, try to instantiate with the string constructor
		else if (value != null) {
			try {
				globalJndiManager.bind(name, className, value, loader);
				return Boolean.TRUE;
			} catch (final Throwable err) {
				Server.logger.error("Error building JNDI object " + name + " (class: " + className + ")", err);
			}
		}

		return Boolean.FALSE;
	}

	/**
	 * Rips the parameters relevant to a particular resource from the command
	 * args
	 */
	private Map<String, String> extractRelevantArgs(final Map<String, String> input, final String name) {
		final Map<String, String> relevantArgs = new HashMap<String, String>();
		for (final Iterator<String> i = input.keySet().iterator(); i.hasNext();) {
			final String key = i.next();
			if (key.startsWith("jndi.param." + name + ".")) {
				relevantArgs.put(key.substring(12 + name.length()), input.get(key));
			}
		}
		relevantArgs.put("name", name);
		return relevantArgs;
	}

	/**
	 * Instantiates listeners. Note that an exception thrown in the constructor
	 * is interpreted as the listener being disabled, so don't do anything too
	 * adventurous in the constructor, or if you do, catch and log any errors
	 * locally before rethrowing.
	 * 
	 * @throws IOException
	 */
	protected final void spawnListener(final String listenerClassName) throws IOException {
		try {
			final Class<?> listenerClass = Class.forName(listenerClassName);
			final Constructor<?> listenerConstructor = listenerClass.getConstructor(new Class[] { Map.class, ObjectPool.class, HostGroup.class });
			final Listener listener = (Listener) listenerConstructor.newInstance(new Object[] { args, objectPool, hostGroup });
			if (listener.start()) {
				listeners.add(listener);
			}
		} catch (final ClassNotFoundException err) {
			Server.logger.info("Listener {} not found / disabled - ignoring", listenerClassName);
		} catch (final Throwable err) {
			Server.logger.error("Error during listener startup " + listenerClassName, err);
			throw (IOException) new IOException("Failed to start a listener: " + listenerClassName).initCause(err);
		}
	}

	/**
	 * ServerControl Thread.
	 * 
	 * @author Jerome Guibert
	 */
	private final class ServerControlThread implements Runnable {

		private final static transient int CONTROL_TIMEOUT = 2000; // wait 2s
																	// for
																	// control
																	// connection

		/**
		 * The main run method. This handles the normal thread processing.
		 */
		@Override
		public void run() {
			boolean interrupted = Boolean.FALSE;
			try {
				ServerSocket controlSocket = null;

				if (controlPort > 0) {
					// Without password, we limit control port on local
					// interface or from controlAddress in parameter.
					InetAddress inetAddress = null;
					final String controlAddress = StringUtils.stringArg(args, "controlAddress", null);
					if (controlAddress != null) {
						inetAddress = InetAddress.getByName(controlAddress);
					}
					if (inetAddress == null) {
						inetAddress = InetAddress.getLocalHost();
					}
					controlSocket = new ServerSocket(controlPort, 0, inetAddress);
					controlSocket.setSoTimeout(ServerControlThread.CONTROL_TIMEOUT);
				}
				if (Server.logger.isInfoEnabled()) {
					Server.logger.info("{} running: controlPort={}", new Object[] { WinstoneResourceBundle.getInstance().getString("ServerVersion"), (controlPort > 0 ? Integer.toString(controlPort) : "disabled") });
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
							Thread.sleep(ServerControlThread.CONTROL_TIMEOUT);
						}
					} catch (final InterruptedIOException err) {
					} catch (final InterruptedException err) {
						interrupted = Boolean.TRUE;
					} catch (final Throwable err) {
						Server.logger.error("Error during listener init or shutdown", err);
					} finally {
						if (accepted != null) {
							try {
								accepted.close();
							} catch (final IOException err) {
							}
						}
						if (Thread.interrupted()) {
							interrupted = Boolean.TRUE;
						}
					}
				}

				// Close server socket
				if (controlSocket != null) {
					controlSocket.close();
				}
			} catch (final Throwable err) {
				Server.logger.error("Error during listener init or shutdown", err);
			}
			Server.logger.info("Winstone shutdown successfully");
		}

		protected void handleControlRequest(final Socket csAccepted) throws IOException {
			InputStream inSocket = null;
			OutputStream outSocket = null;
			ObjectInputStream inControl = null;
			try {
				inSocket = csAccepted.getInputStream();
				final int reqType = inSocket.read();
				if ((byte) reqType == Command.SHUTDOWN.getCode()) {
					Server.logger.info("Shutdown request received via the controlPort. Commencing Winstone shutdowny");
					shutdown();
				} else if ((byte) reqType == Command.RELOAD.getCode()) {
					inControl = new ObjectInputStream(inSocket);
					final String host = inControl.readUTF();
					final String prefix = inControl.readUTF();
					Server.logger.info("Reload request received via the controlPort. Commencing Winstone reload from {}{}", host, prefix);
					final HostConfiguration hostConfig = hostGroup.getHostByName(host);
					hostConfig.reloadWebApp(prefix);
				} else if (cluster != null) {
					outSocket = csAccepted.getOutputStream();
					cluster.clusterRequest((byte) reqType, inSocket, outSocket, csAccepted, hostGroup);
				}
			} finally {
				if (inControl != null) {
					try {
						inControl.close();
					} catch (final IOException err) {
					}
				}
				if (inSocket != null) {
					try {
						inSocket.close();
					} catch (final IOException err) {
					}
				}
				if (outSocket != null) {
					try {
						outSocket.close();
					} catch (final IOException err) {
					}
				}
			}
		}
	}

	/**
	 * @return the controlPort
	 */
	public int getControlPort() {
		return controlPort;
	}

	/**
	 * @return the commonLibClassLoader
	 */
	public ClassLoader getCommonLibClassLoader() {
		return commonLibClassLoader;
	}

	/**
	 * @return the args
	 */
	public Map<String, String> getArgs() {
		return args;
	}

	/**
	 * @return the objectPool
	 * @throws IllegalStateException
	 *             if server is not started.
	 */
	public ObjectPool getObjectPool() throws IllegalStateException {
		if (!isRunning()) {
			throw new IllegalStateException("Server is not started");
		}
		return objectPool;
	}

	/**
	 * @return the HostGroup instance
	 * @throws IllegalStateException
	 *             if server is not started.
	 */
	public HostGroup getHostGroup() throws IllegalStateException {
		if (hostGroup == null) {
			throw new IllegalStateException("Server is not started");
		}
		return hostGroup;
	}

	/**
	 * @return the JndiManager instance, null if none is used.
	 * @throws IllegalStateException
	 *             if server is not started.
	 */
	public JndiManager getJndiManager() throws IllegalStateException {
		if (!isRunning()) {
			throw new IllegalStateException("Server is not started");
		}
		return globalJndiManager;
	}

	/**
	 * @return the cluster, null if not used.
	 * @throws IllegalStateException
	 *             if server is not started.
	 */
	public Cluster getCluster() throws IllegalStateException {
		if (!isRunning()) {
			throw new IllegalStateException("Server is not started");
		}
		return cluster;
	}

	/**
	 * @return a unmodifiable list of listener.
	 * @throws IllegalStateException
	 *             if server is not started.
	 */
	public List<Listener> getListeners() throws IllegalStateException {
		if (!isRunning()) {
			throw new IllegalStateException("Server is not started");
		}
		return Collections.unmodifiableList(listeners);
	}

	/**
	 * Log an information message.
	 * 
	 * @param message
	 */
	public void info(final String message) {
		logger.info(message);
	}
}
