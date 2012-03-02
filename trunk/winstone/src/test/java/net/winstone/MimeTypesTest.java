package net.winstone;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * 
 * MimeTypesTest. 
 *
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 *
 */
public class MimeTypesTest extends TestCase {

	/**
	 * MIME type must exist.
	 */
	public void testMimeTypes() {
		final MimeTypes result = MimeTypes.getInstance();
		Assert.assertNotNull("MimeTypes must exists", result);
		Assert.assertNull("getContentTypeFor without extension must return a null", result.getContentTypeFor("aaa"));
		Assert.assertNotNull("getContentTypeFor for txt must exists", result.getContentTypeFor("aaa.txt"));
	}
}
