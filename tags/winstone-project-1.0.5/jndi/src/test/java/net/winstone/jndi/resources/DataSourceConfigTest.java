package net.winstone.jndi.resources;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;

import junit.framework.Assert;
import net.winstone.jndi.AbstractJndiTestCase;

public class DataSourceConfigTest extends AbstractJndiTestCase {

	public void testDataSourceManagement() throws NamingException {
		final String jndiName = "jdbc/test";
		final DataSourceConfig config = new DataSourceConfig();
		config.setName(jndiName);
		config.setUrl("jdbc:h2:~/test");
		// config.setUsername("sa");
		// config.setPassword("");
		// config.setDriverClassName("org.h2.Driver");
		config.setMaxWait(500);
		jndiManager.bind(config);

		Assert.assertNotNull(jndiName + " not exists", jndiManager.getInitialContext().lookup(jndiName));
		Assert.assertTrue(jndiName + " is not an instance of DataSource", jndiManager.getInitialContext().lookup(jndiName) instanceof DataSource);
		jndiManager.getInitialContext().unbind(jndiName);
		try {
			jndiManager.getInitialContext().lookup(jndiName);
		} catch (final NameNotFoundException e) {
			return;
		}
		Assert.fail(jndiName + " not removed");
	}

}
