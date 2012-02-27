package net.winstone;

import junit.framework.Assert;
import junit.framework.TestCase;

public class MimeTypesTest extends TestCase {

	public void testMimeTypes() {
		final MimeTypes result = MimeTypes.getInstance();
		Assert.assertNotNull("MimeTypes must exists", result);
		Assert.assertNull("getContentTypeFor without extension must return a null", result.getContentTypeFor("aaa"));
		Assert.assertNotNull("getContentTypeFor for txt must exists", result.getContentTypeFor("aaa.txt"));
	}
}
