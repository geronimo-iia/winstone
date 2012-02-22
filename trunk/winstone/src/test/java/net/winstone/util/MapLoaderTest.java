package net.winstone.util;

import java.util.Map;
import java.util.ResourceBundle;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * MapLoaderTest test unit for MapLoader class.
 * 
 * @author Jerome Guibert
 */
public class MapLoaderTest extends TestCase {

	public void testLoadMapTest() {
		Assert.assertNotNull("Ressource Bundle 'maptest' must exists", ResourceBundle.getBundle("maptest"));
		final Map<String, String> result = MapLoader.load(ResourceBundle.getBundle("maptest"));
		Assert.assertNotNull("Result of MapLoader.load must be not null", result);
		Assert.assertEquals("Result must have a size of 3", result.size(), 3);
		Assert.assertEquals("Key 'abs' must have value 'audio/x-mpeg'", result.get("abs"), "audio/x-mpeg");
	}

	public void testLoadOnNullRessource() {
		final Map<String, String> result = MapLoader.load(null);
		Assert.assertNotNull("Result of MapLoader.load must be not null", result);
		Assert.assertEquals("Result must be empty", result.isEmpty(), true);
	}

}
