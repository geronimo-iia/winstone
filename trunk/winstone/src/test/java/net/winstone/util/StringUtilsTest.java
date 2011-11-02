package net.winstone.util;

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
        assertEquals("One token", "Foo = bar squared", StringUtils.replace("Foo = [#0] squared", "[#0]", "bar"));
        assertEquals("Repeated token", "Foo = bar bar squared", StringUtils.replace("Foo = [#0] [#0] squared", "[#0]", "bar"));
        assertEquals("Two tokens", "Foo = blah bar squared", StringUtils.replace("Foo = [#1] [#0] squared", new String[][] {
            {
                "[#0]", "bar"
            }, {
                "[#1]", "blah"
            }
        }));
        
        assertEquals(StringUtils.replace("testing sentence", "not", "do"), "testing sentence");
        
    }
    
    /**
     * No regression test.
     */
    @SuppressWarnings("deprecation")
    public static void testNoRegression() {
        assertEquals(StringUtils.replace("Foo = [#0] squared", "[#0]", "bar"), StringUtils.globalReplace("Foo = [#0] squared", "[#0]", "bar"));
        assertEquals(StringUtils.replace("Foo = [#0] [#0] squared", "[#0]", "bar"), StringUtils.globalReplace("Foo = [#0] [#0] squared", "[#0]", "bar"));
        assertEquals(StringUtils.replace("Foo = [#1] [#0] squared", new String[][] {
            {
                "[#0]", "bar"
            }, {
                "[#1]", "blah"
            }
        }), StringUtils.globalReplace("Foo = [#1] [#0] squared", new String[][] {
            {
                "[#0]", "bar"
            }, {
                "[#1]", "blah"
            }
        }));
    }
}
