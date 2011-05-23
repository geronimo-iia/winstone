package net.winstone.jndi.resources;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.sql.DataSource;
import net.winstone.jndi.AbstractJndiTestCase;

public class SimpleDatasourceTest extends AbstractJndiTestCase {

    private static String jndiName = "jdbc/test";

    @Override
    public void setUp() throws NamingException {
        super.setUp();

        DataSourceConfig config = new DataSourceConfig();
        config.setName(jndiName);
        config.setUrl("jdbc:h2:~/test");
        config.setDriverClassName("org.h2.Driver");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaxActive(2);
        config.setMaxWait(100);
        jndiManager.bind(config, Thread.currentThread().getContextClassLoader());
    }

    @SuppressWarnings("CallToThreadDumpStack")
    public void testConnection() throws NamingException, SQLException {
        DataSource source = (DataSource) jndiManager.getInitialContext().lookup(jndiName);
        assertNotNull(source);
        Connection connection = null;
        try {
            connection = source.getConnection();
            assertNotNull(connection);
            assertTrue(connection.isValid(1));
        } catch (SQLException e) {
            fail(e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    public void testLimitConnectionPool() throws NamingException {
        DataSource source = (DataSource) jndiManager.getInitialContext().lookup(jndiName);
        assertNotNull(source);
        List<Connection> connections = new ArrayList<Connection>();
        for (int i = 0; i < 4; i++) {
            try {
                connections.add(source.getConnection());
            } catch (SQLException e) {
                if (i < 2) {
                    fail("limit pool failure");
                }
            }
        }
        for (Connection connection : connections) {
            try {
                connection.close();
            } catch (SQLException e) {
            }
        }
    }
}
