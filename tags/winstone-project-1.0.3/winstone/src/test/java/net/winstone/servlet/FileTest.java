package net.winstone.servlet;

import java.io.File;
import java.net.MalformedURLException;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Simple file test.
 * 
 * @author jguibert
 */
public class FileTest extends TestCase {

	@SuppressWarnings("deprecation")
	public void testDeprecatedReplacement() throws MalformedURLException {
		final File webXml = new File("src/main/resources/web.xml");

		Assert.assertTrue(webXml.toURI().toURL().equals(webXml.toURL()));
	}
}