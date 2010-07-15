/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.testCase;

import junit.framework.TestCase;
import net.winstone.util.StringUtils;

/**
 * Simple tests for the string replacer
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneResourceBundleTest.java,v 1.1 2006/11/09 05:39:43 rickknowles Exp $
 */
public class WinstoneResourceBundleTest extends TestCase {
    
    public static void testGlobalReplaceFromWinstoneResourceBundle() throws Exception {
        assertEquals("One token", "Foo = bar squared", StringUtils.replace("Foo = [#0] squared", "[#0]", "bar"));
        assertEquals("Repeated token", "Foo = bar bar squared", StringUtils.replace("Foo = [#0] [#0] squared", "[#0]", "bar"));
        assertEquals("Two tokens", "Foo = blah bar squared", StringUtils.replace("Foo = [#1] [#0] squared", new String[][] {
            {
                "[#0]", "bar"
            }, {
                "[#1]", "blah"
            }
        }));
        
        
//        System.err.println("One token: " + StringUtils.globalReplace("Foo = [#0] squared", "[#0]", "bar"));
//        System.err.println("One token: " + String.format("Foo = %s squared", "bar"));
//        System.err.println("One token: " + StringUtils.replace("Foo = [#0] squared", "[#0]", "bar"));
    }
    
}
