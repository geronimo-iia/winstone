package net.winstone;

import junit.framework.TestCase;

public class MimeTypesTest extends TestCase {
    
    public void testMimeTypes() {
        MimeTypes result = MimeTypes.getInstance();
        assertNotNull("MimeTypes must exists", result);
        assertNull("getContentTypeFor without extension must return a null", result.getContentTypeFor("aaa"));
        assertNotNull("getContentTypeFor for txt must exists", result.getContentTypeFor("aaa.txt"));
    }
}
