package net.winstone.core;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import junit.framework.Assert;
import junit.framework.TestCase;

public class WinstoneRequestTest extends TestCase {

    public void testMethodPathDecoding() throws UnsupportedEncodingException {
        String in = "e%C4%9Flenceli%20k%C3%B6pek";
        System.out.println(WinstoneRequest.decodeURLToken(in, "UTF-8"));
        Assert.assertEquals(WinstoneRequest.decodeURLToken(in, "UTF-8"), URLDecoder.decode(in, "UTF-8"));
        in = "e%C4%9Flenceli%20k%C3%B6pe+k";
        System.out.println(WinstoneRequest.decodeURLToken(in, "UTF-8"));
        Assert.assertEquals(WinstoneRequest.decodeURLToken(in, "UTF-8"), URLDecoder.decode(in, "UTF-8"));
    }

    
    public void testMethodParamDecoding() throws UnsupportedEncodingException {
        String in = "e%C4%9Flenceli%20k%C3%B6pek";
        System.out.println(WinstoneRequest.decodeURLToken(in, "UTF-8"));
        Assert.assertEquals(WinstoneRequest.decodeURLToken(in, "UTF-8"), URLDecoder.decode(in, "UTF-8"));
        in = "e%C4%9Flenceli%20k%C3%B6pe+k";
        System.out.println(WinstoneRequest.decodeURLToken(in, "UTF-8"));
        Assert.assertEquals(WinstoneRequest.decodeURLToken(in, "UTF-8"), URLDecoder.decode(in, "UTF-8"));
    }
}
