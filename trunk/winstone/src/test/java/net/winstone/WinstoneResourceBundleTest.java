package net.winstone;

import junit.framework.TestCase;

public class WinstoneResourceBundleTest extends TestCase {

    public void testKey() {
        WinstoneResourceBundle bundle = WinstoneResourceBundle.getInstance();
        assertNotNull(bundle);
        String[] keys = {
            "ServerVersion", "WinstoneResponse.ErrorPage", "StaticResourceServlet.Row", "StaticResourceServlet.Body", "UsageInstructions"
        };
        for (String key : keys) {
            assertNotNull(bundle.getString(key));
        }

    }
}
