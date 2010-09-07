package net.winstone.jndi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import net.winstone.jndi.resources.DataSourceConfig;
import net.winstone.jndi.resources.SimpleDatasource;

import net.winstone.util.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jndi Manager
 * 
 * @author Jerome Guibert
 */
public class JndiManager implements LifeCycle {

    protected Logger logger = LoggerFactory.getLogger(getClass());
    protected InitialContext initialContext;
    protected ScheduledExecutorService scheduler;

    public JndiManager() {
        super();
    }

    /**
     * Initialize context factory.
     * 
     * @throws NamingException
     */
    @Override
    public void initialize() {
        // Instantiate scheduler with a initial pool size of one thread.
        scheduler = Executors.newScheduledThreadPool(1);
        // initiate context factory
        @SuppressWarnings("UseOfObsoleteCollectionType")
        Hashtable<Object, Object> env = new Hashtable<Object, Object>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "net.winstone.jndi.url.java.javaURLContextFactory");
        env.put(Context.URL_PKG_PREFIXES, "net.winstone.jndi.url");
        try {
            initialContext = new InitialContext(env);
        } catch (NamingException e) {
            throw new IllegalStateException(e);
        }
        logger.info("jndi context initialized");
    }

    /**
     * Clean destroy.
     * 
     * @throws NamingException
     */
    @Override
    public void destroy() {
        if (initialContext != null) {
            // close datasource
            try {
                Context jdbc = (Context) initialContext.lookup("java:/comp/env/jdbc");
                NamingEnumeration<NameClassPair> names = jdbc.list("");
                while (names.hasMore()) {
                    try {
                        NameClassPair pair = names.next();
                        Object object = jdbc.lookup(pair.getName());
                        // is a winstone datasource ?
                        if (object instanceof SimpleDatasource) {
                            // close it
                            ((SimpleDatasource) object).close();
                        }
                        // unbind datasource
                        jdbc.unbind(pair.getName());
                    } catch (NamingException e) {
                    }
                }
            } catch (NamingException e) {
                throw new IllegalStateException(e);
            }
            // close initial context
            try {
                initialContext.close();
                initialContext = null;
            } catch (NamingException e) {
            }
        }
        // stop scheduler
        if (!scheduler.isShutdown()) {
            // stop scheduled
            scheduler.shutdownNow();
        }
        logger.info("jndi context destroyed");
    }

    /**
     * Create and bindSmtpSession a datasource in Naming context.
     * 
     * @param dataSourceConfig the datasource configuration to bindSmtpSession.
     * @param loader the classloader to use
     * @throws IllegalStateException If Jndi manager is closed
     * @throws NamingException If binding already exists.
     */
    public void bind(final DataSourceConfig dataSourceConfig, final ClassLoader loader) throws IllegalStateException, NamingException {
        final SimpleDatasource dataSource = new SimpleDatasource(dataSourceConfig, loader);
        String jndiName = dataSource.getName();
        if (jndiName.startsWith("jdbc/")) {
            jndiName = "java:/comp/env/" + jndiName;
        }
        if (!jndiName.startsWith("java:/comp/env/")) {
            jndiName = "java:/comp/env/" + jndiName;
        }
        bind(jndiName, dataSource);
        if (dataSourceConfig.getKeepAlivePeriod() > 0) {
            scheduler.scheduleWithFixedDelay(new Runnable() {

                @Override
                public void run() {
                    dataSource.keepAlive();
                }
            }, dataSourceConfig.getKeepAlivePeriod(), dataSourceConfig.getKeepAlivePeriod(), TimeUnit.MINUTES);
        }
        if (dataSourceConfig.getKillInactivePeriod() > 0) {
            scheduler.scheduleWithFixedDelay(new Runnable() {

                @Override
                public void run() {
                    dataSource.drain();
                }
            }, dataSourceConfig.getKillInactivePeriod(), dataSourceConfig.getKillInactivePeriod(), TimeUnit.MINUTES);
        }
    }

    /**
     * Create and bind a mail session under java:comp/env/mail.
     * 
     * @param name
     * @param properties
     * @param loader
     * @throws IllegalStateException
     * @throws NamingException
     */
    public void bindSmtpSession(final String name, final Properties properties, final ClassLoader loader) throws IllegalStateException, NamingException {
        try {
            Class<?> smtpClass = Class.forName("javax.mail.Session", true, loader);
            Method smtpMethod = smtpClass.getMethod("getInstance", new Class[]{
                        Properties.class, Class.forName("javax.mail.Authenticator")
                    });
            // create object
            Object object = smtpMethod.invoke(null, new Object[]{
                        properties, null
                    });
            String jndiName = name;
            if (name.startsWith("mail/")) {
                jndiName = "java:comp/env/" + name;
            }
            // bind it
            bind(jndiName, object);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (SecurityException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Create and bind an simple object under "java:/comp/env/"
     * 
     * @param name name of binding
     * @param className class name
     * @param value object value
     * @param loader class loader to use
     * @throws IllegalStateException
     * @throws NamingException
     */
    public void bind(final String name, final String className, final String value, final ClassLoader loader) throws IllegalStateException, NamingException {
        if (value != null) {
            try {
                // load class
                Class<?> objClass = Class.forName(className.trim(), true, loader);
                // find constructor
                Constructor<?> objConstr = objClass.getConstructor(new Class[]{
                            String.class
                        });
                // create object
                Object object = objConstr.newInstance(new Object[]{
                            value
                        });
                // bind it
                bind(name, object);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(e);
            } catch (InstantiationException e) {
                throw new IllegalStateException(e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException(e);
            } catch (SecurityException e) {
                throw new IllegalStateException(e);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public InitialContext getInitialContext() {
        return initialContext;
    }

    /**
     * Utility method to bind an object under "java:/comp/env/": we build all needed sub context.
     * 
     * @param name the name
     * @param object object to bindSmtpSession
     * @throws IllegalStateException If Jndi manager is closed
     * @throws NamingException
     */
    private void bind(final String name, Object object) throws IllegalStateException, NamingException {
        if (initialContext == null) {
            throw new IllegalStateException("Initial Context is closed");
        }
        String jndiName = name;
        if (!jndiName.startsWith("java:/comp/env/")) {
            jndiName = "java:/comp/env/" + jndiName;
        }
        Name fullName = new CompositeName(jndiName);
        Context currentContext = initialContext;
        while (fullName.size() > 1) {
            // Make contexts that are not already present
            try {
                currentContext = currentContext.createSubcontext(fullName.get(0));
            } catch (NamingException err) {
                currentContext = (Context) currentContext.lookup(fullName.get(0));
            }
            fullName = fullName.getSuffix(1);
        }
        initialContext.bind(name, object);
    }
}
