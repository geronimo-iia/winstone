package net.winstone.util;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Simple number test.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class NumberTest extends TestCase {

	public void testInt() {
		try {
			Integer.parseInt("2516506624");
			Assert.fail();
		} catch (final NumberFormatException e) {
		}
		Long.parseLong("2516506624");
	}
}
