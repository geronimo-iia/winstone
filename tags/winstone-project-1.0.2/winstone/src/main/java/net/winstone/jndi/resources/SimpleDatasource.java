package net.winstone.jndi.resources;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;

import net.winstone.pool.ResourceFactory;
import net.winstone.pool.SimplePool;
import net.winstone.util.Function;

/**
 * DataSource Implementation.
 * 
 * @author Jerome Guibert
 */
public class SimpleDatasource implements DataSource, ResourceFactory<Connection> {
    
    private final SimplePool<Connection> pool;
    private final String name;
    private final String url;
    private final Driver driver;
    private final Properties connectionProperties;
    private PrintWriter logWriter;
    private final String validationQuery;
    private final int validationTimeOut;
    private final boolean validated;
    private final String keepAliveSQL;
    private final int keepAliveTimeOut;
    
    /**
     * @param config
     * @param loader
     * @throws IllegalArgumentException if config url parameter is null or empty, or validationTimeOut<=0,or keepAliveTimeOut<=0
     * @throws IllegalStateException if database driver cannot be loaded
     */
    public SimpleDatasource(final DataSourceConfig config, final ClassLoader loader) throws IllegalArgumentException, IllegalStateException {
        super();
        logWriter = null;
        // Set datasource name
        this.name = (config.getName() != null) ? config.getName() : config.getUrl();
        // init connection properties
        connectionProperties = new Properties();
        if (config.getUsername() != null) {
            connectionProperties.setProperty("user", config.getUsername());
        }
        if (config.getPassword() != null) {
            connectionProperties.setProperty("password", config.getPassword());
        }
        // check url
        url = config.getUrl();
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException(String.format("Can't create database %s connection : url not provided", config.getName()));
        }
        // set validation query
        validationQuery = config.getValidationQuery();
        validated = (config.getValidationQuery() != null && config.getValidationQuery().length() > 0);
        validationTimeOut = config.getValidationTimeOut();
        if (validationTimeOut <= 0) {
            throw new IllegalArgumentException(String.format("Database %s: validation TimeOut must be > 0", config.getName()));
        }
        // set keep alive
        keepAliveSQL = config.getKeepAliveSQL();
        keepAliveTimeOut = config.getKeepAliveTimeOut();
        if (keepAliveTimeOut <= 0) {
            throw new IllegalArgumentException(String.format("Database %s: Keep Alive TimeOut must be > 0", config.getName()));
        }
        // try to init driver
        try {
            if (config.getDriverClassName() != null) {
                Class<?> driverClass = Class.forName(config.getDriverClassName().trim(), true, loader);
                driver = (Driver)driverClass.newInstance();
            } else {
                driver = DriverManager.getDriver(config.getUrl());
            }
        } catch (Throwable err) {
            throw new IllegalStateException(String.format("Can't load database driver for %s connection", config.getName()), err);
        }
        // pool initialisation
        pool = new SimplePool<Connection>(this, config.getMaxActive(), config.getMaxWait(), config.getMinIdle());
        log("Database %s Initialized: %s", null, name, pool.toString());
    }
    
    /**
     * Close this datasource.
     */
    public void close() {
        log("Closing %s", null, name);
        pool.close();
    }
    
    /**
     * Attempts to establish a connection with the data source that this <code>DataSource</code> object represents.
     * <p>
     * If validation is active, in case of failure, retry a maximum of pool capacity before raising an IllegalStateException.
     * </p>
     * 
     * @return a connection to the data source
     * @exception SQLException if a database access error occurs
     * @throws IllegalStateException if no validated connection can be acquire
     */
    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = null;
        if (!validated) {
            connection = pool.acquire();
        } else {
            // try to obtain a validated connection
            int retry = 0;
            do {
                final Connection result = pool.acquire();
                if (validate(result)) {
                    connection = result;
                }
                retry++;
                pool.invalidate(result);
            } while (retry <= pool.getCapacity());
            if (connection == null) {
                throw new IllegalStateException("Unable to obtain a validated connection %s");
            }
        }
        if (connection == null) {
            throw new SQLException("Connection unavailable - pool limit exceeded : " + pool.toString());
        }
        return (Connection)Proxy.newProxyInstance(connection.getClass().getClassLoader(), new Class[] {
            Connection.class
        }, new ConnectionWrapperInvocationHandler(connection));
    }
    
    @Override
    public Connection getConnection(final String username, final String password) throws SQLException {
        connectionProperties.setProperty("user", username);
        connectionProperties.setProperty("password", password);
        return getConnection();
    }
    
    @Override
    public int getLoginTimeout() throws SQLException {
        return pool.getTimeout() / 1000;
    }
    
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return logWriter;
    }
    
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        logWriter = out;
    }
    
    /**
     * Unmodifiable.
     */
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        log("LoginTimeout cannot be modified at runtime for %s%n", null, name);
    }
    
    /**
     * @since 1.6
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return DataSource.class.equals(iface);
    }
    
    /**
     * @since 1.6
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isWrapperFor(iface))
            return (T)this;
        return null;
    }
    
    @Override
    public Connection create() {
        try {
            return this.driver.connect(this.url, this.connectionProperties);
        } catch (SQLException e) {
            log("Can't create connection %s for %s%n", e, name, url);
            throw new IllegalStateException("Can't create connection, check connection parameters and class path for JDBC driver", e);
        }
    }
    
    @Override
    public void destroy(Connection resource) {
        try {
            if (resource != null) {
                resource.close();
            }
        } catch (SQLException err) {
            log("Error when closing %s connection ", err, name);
        }
    }
    
    /**
     * Kills excess unused connections.
     * 
     * @return the number of drained resources.
     */
    public int drain() {
        try {
            return pool.drain();
        } catch (Throwable e) {
            log("Drain error on Datasource %s:", e, name);
        }
        return -1;
    }
    
    /**
     * Execute keep alive operation on all idle connection.
     */
    public void keepAlive() {
        if (keepAliveSQL != null && !keepAliveSQL.isEmpty()) {
            try {
                pool.applyOnIdle(new Function<Void, Connection>() {
                    
                    @Override
                    public Void apply(Connection connection) {
                        PreparedStatement keepAliveQuery = null;
                        try {
                            keepAliveQuery = connection.prepareStatement(keepAliveSQL);
                            keepAliveQuery.execute();
                        } catch (SQLException err) {
                            log("Keep alive failed for %s, invalidate connection", err, name);
                            pool.invalidate(connection);
                        } finally {
                            if (keepAliveQuery != null) {
                                try {
                                    keepAliveQuery.close();
                                } catch (SQLException e) {
                                }
                            }
                        }
                        return null;
                    }
                });
            } catch (Throwable e) {
                log("KeepAlive error on Datasource %s:", e, name);
            }
        }
    }
    
    /**
     * @return the datasource name.
     */
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return "SimpleDatasource [name=" + name + "]";
    }
    
    /**
     * Validate the specified connection.
     * 
     * @param connection the specified connection to validate.
     * @return true if connection is valid.
     */
    private boolean validate(final Connection connection) {
        try {
            if ("isValid".equals(validationQuery)) {
                try {
                    return ((Boolean)connection.getClass().getMethod("isValid", int.class).invoke(10));
                } catch (Exception e) {
                }
            } else if ("isClosed".equals(validationQuery)) {
                return (connection.isClosed() == false);
            } else {
                // validation task
                FutureTask<Boolean> futureTask = new FutureTask<Boolean>(new Runnable() {
                    @Override
                    public void run() {
                        Statement statement = null;
                        try {
                            statement = connection.createStatement();
                            statement.execute(validationQuery);
                        } catch (SQLException e) {
                            throw new IllegalStateException("ValidationFailure");
                        } finally {
                            if (statement != null)
                                try {
                                    statement.close();
                                } catch (SQLException e) {
                                }
                        }
                    }
                }, Boolean.TRUE);
                Boolean valid = Boolean.FALSE;
                try {
                    valid = futureTask.get(validationTimeOut, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                } catch (ExecutionException e) {
                } catch (TimeoutException e) {
                }
                return valid;
            }
        } catch (SQLException e) {
            log("Discarding connection %s because %s%n", null, connection, e);
        }
        return false;
    }
    
    /**
     * Utility to log a message.
     * 
     * @param message
     * @param err
     * @param args
     */
    private void log(final String message, final Throwable err, final Object... args) {
        if (logWriter != null) {
            if (args == null || args.length == 0) {
                logWriter.println(message);
            } else {
                logWriter.write(String.format(message, args));
            }
            if (err != null) {
                err.printStackTrace(logWriter);
            }
            logWriter.flush();
        }
    }
    
    /**
     * A Simple Connection Invocation Wrapper.
     * 
     * @author Jerome Guibert
     */
    private class ConnectionWrapperInvocationHandler implements InvocationHandler {
        private final Connection connection;
        
        protected ConnectionWrapperInvocationHandler(final Connection connection) {
            super();
            this.connection = connection;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (connection == null) {
                throw new SQLException("The connection is closed");
            }
            // close wrap
            if (method.getName().equals("close")) {
                if (connection.getAutoCommit() == false) {
                    try {
                        connection.rollback();
                    } catch (SQLException se) {
                        
                    }
                }
                pool.release(connection);
                return null;
            } else if (method.getName().equals("unwrap")) {
                // unwrap call
                return connection;
            } else if (method.getName().equals("isWrapperFor")) {
                // is wrapper
                return ((Class<?>)args[0]).isInstance(connection);
            } else if (method.getName().equals("equals")) {
                // equals test
                return proxy == args[0];
            } else {
                try {
                    final Object realStmt = method.invoke(connection, args);
                    if (realStmt instanceof Statement == false) {
                        return realStmt;
                    }
                } catch (InvocationTargetException exception) {
                    Throwable cause = exception.getCause();
                    try {
                        if (cause instanceof SQLException) {
                            pool.invalidate(connection);
                        }
                    } catch (Throwable e) {
                    }
                    throw exception.getCause();
                }
            }
            return null;
        }
        
    }
}
