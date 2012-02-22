package net.winstone;

import junit.framework.Assert;
import junit.framework.TestCase;

public class WinstoneResourceBundleTest extends TestCase {

	public void testKey() {
		final WinstoneResourceBundle bundle = WinstoneResourceBundle.getInstance();
		Assert.assertNotNull(bundle);
		final String[] keys = { "ServerVersion", "PoweredByHeader", "WinstoneResponse.ErrorPage", "StaticResourceServlet.Row", "StaticResourceServlet.Body", "UsageInstructions" };
		for (final String key : keys) {
			Assert.assertNotNull(bundle.getString(key));
		}

	}
}
