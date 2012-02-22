package net.winstone.util;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Simple tests for the string utility.
 * 
 * @author Jerome Guibert
 */
public class StringUtilsTest extends TestCase {

	/**
	 * Replacement test.
	 */
	public static void testReplace() {
		Assert.assertEquals("One token", "Foo = bar squared", StringUtils.replace("Foo = [#0] squared", "[#0]", "bar"));
		Assert.assertEquals("Repeated token", "Foo = bar bar squared", StringUtils.replace("Foo = [#0] [#0] squared", "[#0]", "bar"));
		Assert.assertEquals("Two tokens", "Foo = blah bar squared", StringUtils.replace("Foo = [#1] [#0] squared", new String[][] { { "[#0]", "bar" }, { "[#1]", "blah" } }));

		Assert.assertEquals(StringUtils.replace("testing sentence", "not", "do"), "testing sentence");

	}

	/**
	 * No regression test.
	 */
	@SuppressWarnings("deprecation")
	public static void testNoRegression() {
		Assert.assertEquals(StringUtils.replace("Foo = [#0] squared", "[#0]", "bar"), StringUtils.globalReplace("Foo = [#0] squared", "[#0]", "bar"));
		Assert.assertEquals(StringUtils.replace("Foo = [#0] [#0] squared", "[#0]", "bar"), StringUtils.globalReplace("Foo = [#0] [#0] squared", "[#0]", "bar"));
		Assert.assertEquals(StringUtils.replace("Foo = [#1] [#0] squared", new String[][] { { "[#0]", "bar" }, { "[#1]", "blah" } }), StringUtils.globalReplace("Foo = [#1] [#0] squared", new String[][] { { "[#0]", "bar" }, { "[#1]", "blah" } }));
	}
}
