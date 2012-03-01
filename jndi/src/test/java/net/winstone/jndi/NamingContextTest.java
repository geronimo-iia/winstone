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

import junit.framework.Assert;

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

			Assert.assertEquals("binding_a", jndiManager.getInitialContext().lookup("a"));
			Assert.assertEquals("binding_b", jndiManager.getInitialContext().lookup("b"));
			Assert.assertEquals("binding_c", jndiManager.getInitialContext().lookup("c"));

			// make some changes
			jndiManager.getInitialContext().unbind("b");
			jndiManager.getInitialContext().rename("a", "aa");
			jndiManager.getInitialContext().bind("d", "binding_d");
			jndiManager.getInitialContext().rebind("c", "new_binding");

			Assert.assertEquals("binding_a", jndiManager.getInitialContext().lookup("aa"));
			Assert.assertEquals("binding_d", jndiManager.getInitialContext().lookup("d"));
			Assert.assertEquals("new_binding", jndiManager.getInitialContext().lookup("c"));

			final Context a = jndiManager.getInitialContext().createSubcontext("a");
			final Context b = a.createSubcontext("b");
			final Context c = b.createSubcontext("c");

			jndiManager.getInitialContext().createSubcontext("x");
			jndiManager.getInitialContext().createSubcontext("y");

			Assert.assertEquals("java:/comp/env/a/b/c", c.getNameInNamespace());
			Assert.assertEquals("env", jndiManager.getInitialContext().lookup("").toString());
			Assert.assertEquals("a", jndiManager.getInitialContext().lookup("a").toString());
			Assert.assertEquals("b", jndiManager.getInitialContext().lookup("a/b").toString());
			Assert.assertEquals("c", a.lookup("b/c").toString());

		} catch (final NamingException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	public void testLinkRefContextLookup() throws NamingException {
		jndiManager.getInitialContext().bind("aaaa", "data of a");
		jndiManager.getInitialContext().bind("linka", new LinkRef("aaaa"));
		Assert.assertNotNull("Lookup on java:/comp/env/aaaa must be non-null", jndiManager.getInitialContext().lookup("aaaa"));
		Assert.assertNotNull("Lookup on java:/comp/env/linka must be non-null", jndiManager.getInitialContext().lookup("linka"));
		Assert.assertTrue("Lookup on java:/comp/env/linka must be a LinkRef", jndiManager.getInitialContext().lookup("linka") instanceof LinkRef);
		final Object target = jndiManager.getInitialContext().lookupLink("linka");
		Assert.assertNotNull("LookupLink on java:/comp/env/linka must be non-null", target);
		Assert.assertTrue("LookupLink on java:/comp/env/linka must be a String", target instanceof String);
		Assert.assertEquals("LookupLink on java:/comp/env/linka must be 'data of a' ", target, "data of a");
		jndiManager.getInitialContext().unbind("aaaa");
		boolean raised = Boolean.FALSE;
		try {
			jndiManager.getInitialContext().lookup("aaaa");
		} catch (final NamingException e) {
			raised = Boolean.TRUE;
		}
		Assert.assertTrue("Lookup on java:/comp/env/aaaa must be null", raised);
		raised = Boolean.FALSE;
		try {
			jndiManager.getInitialContext().lookupLink("linka");
		} catch (final NamingException e) {
			raised = Boolean.TRUE;
		}
		Assert.assertTrue("LookupLink on java:/comp/env/linka must be null", raised);
	}

	/**
	 * Performs an absolute context lookup
	 */
	public void testAbsoluteContextLookup() throws NamingException {
		final Object context1 = jndiManager.getInitialContext().lookup("java:/comp/env");
		Assert.assertNotNull("Lookup on java:/comp/env must be non-null", context1);
		Assert.assertTrue("Lookup on java:/comp/env must be a Context", context1 instanceof Context);

		final Object context2 = jndiManager.getInitialContext().lookup("java:/comp/env/");
		Assert.assertNotNull("Lookup on java:/comp/env/ must be non-null", context2);
		Assert.assertTrue("Lookup on java:/comp/env/ must be a Context", context2 instanceof Context);
	}

	/**
	 * Performs an absolute lookup on the context
	 */
	public void testAbsoluteLookup() throws NamingException {
		final Object value = jndiManager.getInitialContext().lookup("java:/comp/env");
		Assert.assertNotNull("Lookup on java:/comp/env must be non-null", value);
	}

	/**
	 * Performs a relative lookup on the context
	 */
	public void testRelativeLookup() throws NamingException {
		final Object value = jndiManager.getInitialContext().lookup("");
		Assert.assertNotNull("Lookup on \"\" must be non-null", value);
	}

	/**
	 * Performs a relative list on the context
	 */
	public void testRelativeList() throws NamingException {
		final NamingEnumeration<NameClassPair> listing = jndiManager.getInitialContext().list("");
		Assert.assertNotNull("Listing of current context must be non-null", listing);
		listing.close();
	}

	/**
	 * Performs an absolute list on the context
	 */
	public void testAbsoluteList() throws NamingException {
		final NamingEnumeration<NameClassPair> listing1 = jndiManager.getInitialContext().list("java:/comp/env");
		Assert.assertNotNull("Listing of java:/comp/env must be non-null", listing1);
		listing1.close();
		final NamingEnumeration<NameClassPair> listing2 = jndiManager.getInitialContext().list("java:/comp/env/");
		Assert.assertNotNull("Listing of java:/comp/env/ must be non-null", listing2);
		listing2.close();
	}

	/**
	 * Performs an absolute list on the context
	 */
	public void testCreateDestroyContexts() throws NamingException {
		final Context child = jndiManager.getInitialContext().createSubcontext("TestChildContext");
		Assert.assertNotNull("Created subcontext TestChildContext must not be null", child);
		final NamingEnumeration<NameClassPair> listing = child.list("");
		Assert.assertTrue("Listing on new child context is empty", !listing.hasMoreElements());
		listing.close();
		jndiManager.getInitialContext().destroySubcontext("java:/comp/env/TestChildContext");
	}

	/**
	 * Attempts a simple bindSmtpSession
	 */
	public void testSimpleBind() throws NamingException {
		final Context child = jndiManager.getInitialContext().createSubcontext("TestBindContext");
		Assert.assertNotNull("Created subcontext TestBindContext must not be null", child);
		child.bind("bindInteger", new Integer(80));
		final Object lookupInt = jndiManager.getInitialContext().lookup("TestBindContext/bindInteger");
		Assert.assertNotNull("java:/comp/env/TestBindContext/bindInteger should be non-null", lookupInt);
		Assert.assertEquals("java:/comp/env/TestBindContext/bindInteger", lookupInt, new Integer(80));
		jndiManager.getInitialContext().destroySubcontext("java:/comp/env/TestBindContext");
	}

	/**
	 * Attempts a rebind
	 */
	public void testSimpleRebind() throws NamingException {
		final Context child = jndiManager.getInitialContext().createSubcontext("TestRebindContext");
		Assert.assertNotNull("Created subcontext TestRebindContext must not be null", child);
		final Context rebindChild = child.createSubcontext("ChildRebind");
		Assert.assertNotNull("Created subcontext rebindChild must not be null", rebindChild);
		rebindChild.rebind("java:/comp/env/TestRebindContext/ChildRebind/integer", new Integer(25));
		rebindChild.close();
		child.close();

		final Object lookupInt = jndiManager.getInitialContext().lookup("java:/comp/env/TestRebindContext/ChildRebind/integer");
		Assert.assertNotNull("java:/comp/env/TestRebindContext/ChildRebind/integer should be non-null", lookupInt);
		Assert.assertEquals("java:/comp/env/TestRebindContext/ChildRebind/integer", lookupInt, new Integer(25));

		jndiManager.getInitialContext().rebind("TestRebindContext/ChildRebind/integer", new Integer(40));
		final Object lookupInt2 = jndiManager.getInitialContext().lookup("TestRebindContext/ChildRebind/integer");
		Assert.assertNotNull("TestRebindContext/ChildRebind/integer should be non-null", lookupInt2);
		Assert.assertEquals("TestRebindContext/ChildRebind/integer", lookupInt2, new Integer(40));
		final Object lookupInt3 = jndiManager.getInitialContext().lookup("java:/comp/env/TestRebindContext/ChildRebind/integer");
		Assert.assertNotNull("java:/comp/env/TestRebindContext/ChildRebind/integer should be non-null", lookupInt3);
		Assert.assertEquals("java:/comp/env/TestRebindContext/ChildRebind/integer", lookupInt3, new Integer(40));

		jndiManager.getInitialContext().destroySubcontext("java:/comp/env/TestRebindContext/ChildRebind");
		jndiManager.getInitialContext().destroySubcontext("java:/comp/env/TestRebindContext");
	}
}
