/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.jndi;

import javax.naming.Context;
import javax.naming.LinkRef;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * Automated tests for the JNDI provider component of Winstone.
 * 
 * @author Jerome Guibert
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: NamingTest.java,v 1.2 2006/02/28 07:32:49 rickknowles Exp $
 */
public class NamingContextTest extends AbstractJndiTestCase {
    
    public void testBasicInitialContext() {
        try {
            jndiManager.getInitialContext().bind("a", "binding_a");
            jndiManager.getInitialContext().bind("b", "binding_b");
            jndiManager.getInitialContext().bind("c", "binding_c");
            
            assertEquals("binding_a", jndiManager.getInitialContext().lookup("a"));
            assertEquals("binding_b", jndiManager.getInitialContext().lookup("b"));
            assertEquals("binding_c", jndiManager.getInitialContext().lookup("c"));
            
            // make some changes
            jndiManager.getInitialContext().unbind("b");
            jndiManager.getInitialContext().rename("a", "aa");
            jndiManager.getInitialContext().bind("d", "binding_d");
            jndiManager.getInitialContext().rebind("c", "new_binding");
            
            assertEquals("binding_a", jndiManager.getInitialContext().lookup("aa"));
            assertEquals("binding_d", jndiManager.getInitialContext().lookup("d"));
            assertEquals("new_binding", jndiManager.getInitialContext().lookup("c"));
            
            Context a = jndiManager.getInitialContext().createSubcontext("a");
            Context b = a.createSubcontext("b");
            Context c = b.createSubcontext("c");
            
            jndiManager.getInitialContext().createSubcontext("x");
            jndiManager.getInitialContext().createSubcontext("y");
            
            assertEquals("java:/comp/env/a/b/c", c.getNameInNamespace());
            assertEquals("env", jndiManager.getInitialContext().lookup("").toString());
            assertEquals("a", jndiManager.getInitialContext().lookup("a").toString());
            assertEquals("b", jndiManager.getInitialContext().lookup("a/b").toString());
            assertEquals("c", a.lookup("b/c").toString());
            
        } catch (NamingException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    public void testLinkRefContextLookup() throws NamingException {
        this.jndiManager.getInitialContext().bind("aaaa", "data of a");
        this.jndiManager.getInitialContext().bind("linka", new LinkRef("aaaa"));
        assertNotNull("Lookup on java:/comp/env/aaaa must be non-null", this.jndiManager.getInitialContext().lookup("aaaa"));
        assertNotNull("Lookup on java:/comp/env/linka must be non-null", this.jndiManager.getInitialContext().lookup("linka"));
        assertTrue("Lookup on java:/comp/env/linka must be a LinkRef", this.jndiManager.getInitialContext().lookup("linka") instanceof LinkRef);
        Object target = this.jndiManager.getInitialContext().lookupLink("linka");
        assertNotNull("LookupLink on java:/comp/env/linka must be non-null", target);
        assertTrue("LookupLink on java:/comp/env/linka must be a String", target instanceof String);
        assertEquals("LookupLink on java:/comp/env/linka must be 'data of a' ", target, "data of a");
        this.jndiManager.getInitialContext().unbind("aaaa");
        boolean raised = false;
        try {
            this.jndiManager.getInitialContext().lookup("aaaa");
        } catch (NamingException e) {
            raised = true;
        }
        assertTrue("Lookup on java:/comp/env/aaaa must be null", raised);
        raised = false;
        try {
            this.jndiManager.getInitialContext().lookupLink("linka");
        } catch (NamingException e) {
            raised = true;
        }
        assertTrue("LookupLink on java:/comp/env/linka must be null", raised);
    }
    
    /**
     * Performs an absolute context lookup
     */
    public void testAbsoluteContextLookup() throws NamingException {
        Object context1 = this.jndiManager.getInitialContext().lookup("java:/comp/env");
        assertNotNull("Lookup on java:/comp/env must be non-null", context1);
        assertTrue("Lookup on java:/comp/env must be a Context", context1 instanceof Context);
        
        Object context2 = this.jndiManager.getInitialContext().lookup("java:/comp/env/");
        assertNotNull("Lookup on java:/comp/env/ must be non-null", context2);
        assertTrue("Lookup on java:/comp/env/ must be a Context", context2 instanceof Context);
    }
    
    /**
     * Performs an absolute lookup on the context
     */
    public void testAbsoluteLookup() throws NamingException {
        Object value = this.jndiManager.getInitialContext().lookup("java:/comp/env");
        assertNotNull("Lookup on java:/comp/env must be non-null", value);
    }
    
    /**
     * Performs a relative lookup on the context
     */
    public void testRelativeLookup() throws NamingException {
        Object value = this.jndiManager.getInitialContext().lookup("");
        assertNotNull("Lookup on \"\" must be non-null", value);
    }
    
    /**
     * Performs a relative list on the context
     */
    public void testRelativeList() throws NamingException {
        NamingEnumeration<NameClassPair> listing = this.jndiManager.getInitialContext().list("");
        assertNotNull("Listing of current context must be non-null", listing);
        listing.close();
    }
    
    /**
     * Performs an absolute list on the context
     */
    public void testAbsoluteList() throws NamingException {
        NamingEnumeration<NameClassPair> listing1 = this.jndiManager.getInitialContext().list("java:/comp/env");
        assertNotNull("Listing of java:/comp/env must be non-null", listing1);
        listing1.close();
        NamingEnumeration<NameClassPair> listing2 = this.jndiManager.getInitialContext().list("java:/comp/env/");
        assertNotNull("Listing of java:/comp/env/ must be non-null", listing2);
        listing2.close();
    }
    
    /**
     * Performs an absolute list on the context
     */
    public void testCreateDestroyContexts() throws NamingException {
        Context child = this.jndiManager.getInitialContext().createSubcontext("TestChildContext");
        assertNotNull("Created subcontext TestChildContext must not be null", child);
        NamingEnumeration<NameClassPair> listing = child.list("");
        assertTrue("Listing on new child context is empty", !listing.hasMoreElements());
        listing.close();
        this.jndiManager.getInitialContext().destroySubcontext("java:/comp/env/TestChildContext");
    }
    
    /**
     * Attempts a simple bind
     */
    public void testSimpleBind() throws NamingException {
        Context child = this.jndiManager.getInitialContext().createSubcontext("TestBindContext");
        assertNotNull("Created subcontext TestBindContext must not be null", child);
        child.bind("bindInteger", new Integer(80));
        Object lookupInt = this.jndiManager.getInitialContext().lookup("TestBindContext/bindInteger");
        assertNotNull("java:/comp/env/TestBindContext/bindInteger should be non-null", lookupInt);
        assertEquals("java:/comp/env/TestBindContext/bindInteger", lookupInt, new Integer(80));
        this.jndiManager.getInitialContext().destroySubcontext("java:/comp/env/TestBindContext");
    }
    
    /**
     * Attempts a rebind
     */
    public void testSimpleRebind() throws NamingException {
        Context child = this.jndiManager.getInitialContext().createSubcontext("TestRebindContext");
        assertNotNull("Created subcontext TestRebindContext must not be null", child);
        Context rebindChild = child.createSubcontext("ChildRebind");
        assertNotNull("Created subcontext rebindChild must not be null", rebindChild);
        rebindChild.rebind("java:/comp/env/TestRebindContext/ChildRebind/integer", new Integer(25));
        rebindChild.close();
        child.close();
        
        Object lookupInt = this.jndiManager.getInitialContext().lookup("java:/comp/env/TestRebindContext/ChildRebind/integer");
        assertNotNull("java:/comp/env/TestRebindContext/ChildRebind/integer should be non-null", lookupInt);
        assertEquals("java:/comp/env/TestRebindContext/ChildRebind/integer", lookupInt, new Integer(25));
        
        this.jndiManager.getInitialContext().rebind("TestRebindContext/ChildRebind/integer", new Integer(40));
        Object lookupInt2 = this.jndiManager.getInitialContext().lookup("TestRebindContext/ChildRebind/integer");
        assertNotNull("TestRebindContext/ChildRebind/integer should be non-null", lookupInt2);
        assertEquals("TestRebindContext/ChildRebind/integer", lookupInt2, new Integer(40));
        Object lookupInt3 = this.jndiManager.getInitialContext().lookup("java:/comp/env/TestRebindContext/ChildRebind/integer");
        assertNotNull("java:/comp/env/TestRebindContext/ChildRebind/integer should be non-null", lookupInt3);
        assertEquals("java:/comp/env/TestRebindContext/ChildRebind/integer", lookupInt3, new Integer(40));
        
        this.jndiManager.getInitialContext().destroySubcontext("java:/comp/env/TestRebindContext/ChildRebind");
        this.jndiManager.getInitialContext().destroySubcontext("java:/comp/env/TestRebindContext");
    }
}
