package net.winstone;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * 
 * WinstoneResourceBundleTest.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
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
