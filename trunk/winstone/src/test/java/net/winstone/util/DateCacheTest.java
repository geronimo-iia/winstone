package net.winstone.util;

import java.text.SimpleDateFormat;
import java.util.Locale;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * DateCache Test Unit
 * 
 * @author Jerome Guibert
 */
public class DateCacheTest extends TestCase {

	public void testSimple() {
		final DateCache cache = new DateCache(new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.US));
		Assert.assertNotNull(cache);
		Assert.assertNotNull(cache.now());

		final long time = 1279534106123L;
		Assert.assertEquals("Mon, 19 Jul 2010 12:08:26", cache.format(time));

	}
}
