package net.winstone.servlet;

import java.io.File;
import java.net.MalformedURLException;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Simple file test.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 */
public class FileTest extends TestCase {

	/**
	 * Test deprecated method replacement.
	 * 
	 * @throws MalformedURLException
	 */
	@SuppressWarnings("deprecation")
	public void testDeprecatedReplacement() throws MalformedURLException {
		final File webXml = new File("src/main/resources/web.xml");
		Assert.assertTrue(webXml.toURI().toURL().equals(webXml.toURL()));
	}
}
