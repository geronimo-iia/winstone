package net.winstone.jndi.resources;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;
import net.winstone.jndi.AbstractJndiTestCase;


public class DataSourceConfigTest extends AbstractJndiTestCase {
    
    public void testDataSourceManagement() throws NamingException {
        String jndiName = "jdbc/test";
        DataSourceConfig config = new DataSourceConfig();
        config.setName(jndiName);
        config.setUrl("jdbc:h2:~/test");
        config.setMaxWait(500);
        jndiManager.bind(config, Thread.currentThread().getContextClassLoader());
        
        assertNotNull(jndiName + " not exists", jndiManager.getInitialContext().lookup(jndiName));
        assertTrue(jndiName + " is not an instance of DataSource", jndiManager.getInitialContext().lookup(jndiName) instanceof DataSource);
        jndiManager.getInitialContext().unbind(jndiName);
        try {
            jndiManager.getInitialContext().lookup(jndiName);
        } catch (NameNotFoundException e) {
            return;
        }
        fail(jndiName + " not removed");
    }
    
}
