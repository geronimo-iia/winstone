package net.winstone.testCase;

import java.io.File;
import java.net.MalformedURLException;

import junit.framework.TestCase;

/**
 * Simple file test.
 * 
 * @author jguibert
 */
public class FileTest extends TestCase {
    
    @SuppressWarnings("deprecation")
    public void testDeprecatedReplacement() throws MalformedURLException {
        File webXml = new File("src/testwebapp/WEB-INF/web.xml");
        
        assertTrue(webXml.toURI().toURL().equals(webXml.toURL()));
    }
}
