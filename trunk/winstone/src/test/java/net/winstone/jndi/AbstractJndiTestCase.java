package net.winstone.jndi;

import javax.naming.NamingException;

import junit.framework.TestCase;

public abstract class AbstractJndiTestCase extends TestCase {
    
    protected JndiManager jndiManager;
    
    /**
     * Begins the setup of the test case
     */
    public void setUp() throws NamingException {
        jndiManager = new JndiManager();
        jndiManager.initialize();
    }
    
    /**
     * Undoes any setup work for the test case
     */
    public void tearDown() throws NamingException {
        jndiManager.destroy();
        jndiManager = null;
    }
}
