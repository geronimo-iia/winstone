package net.winstone.jndi.resources;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.sql.DataSource;

import junit.framework.Assert;
import net.winstone.jndi.AbstractJndiTestCase;

public class SimpleDatasourceTest extends AbstractJndiTestCase {

	private static String jndiName = "jdbc/test";

	@Override
	public void setUp() throws NamingException {
		super.setUp();

		final DataSourceConfig config = new DataSourceConfig();
		config.setName(SimpleDatasourceTest.jndiName);
		config.setUrl("jdbc:h2:~/test");
		// config.setDriverClassName("org.h2.Driver");
		// config.setUsername("sa");
		// config.setPassword("");
		config.setMaxActive(2);
		config.setMaxWait(100);
		jndiManager.bind(config, Thread.currentThread().getContextClassLoader());
	}

	public void testConnection() throws NamingException, SQLException {
		final DataSource source = (DataSource) jndiManager.getInitialContext().lookup(SimpleDatasourceTest.jndiName);
		Assert.assertNotNull(source);
		Connection connection = null;
		try {
			connection = source.getConnection();
			Assert.assertNotNull(connection);
			Assert.assertTrue(connection.isValid(1));
		} catch (final SQLException e) {
			Assert.fail(e.getMessage());
			e.printStackTrace();
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

	public void testLimitConnectionPool() throws NamingException {
		final DataSource source = (DataSource) jndiManager.getInitialContext().lookup(SimpleDatasourceTest.jndiName);
		Assert.assertNotNull(source);
		final List<Connection> connections = new ArrayList<Connection>();
		for (int i = 0; i < 4; i++) {
			try {
				connections.add(source.getConnection());
			} catch (final SQLException e) {
				if (i < 2) {
					Assert.fail("limit pool failure");
				}
			}
		}
		for (final Connection connection : connections) {
			try {
				connection.close();
			} catch (final SQLException e) {
			}
		}
	}
}
