package net.winstone.util;

import java.util.Map;
import java.util.ResourceBundle;

import junit.framework.TestCase;

/**
 * MapLoaderTest test unit for MapLoader class.
 * 
 * @author Jerome Guibert
 */
public class MapLoaderTest extends TestCase {
    
    public void testLoadMapTest() {
        assertNotNull("Ressource Bundle 'maptest' must exists", ResourceBundle.getBundle("maptest"));
        Map<String, String> result = MapLoader.load(ResourceBundle.getBundle("maptest"));
        assertNotNull("Result of MapLoader.load must be not null", result);
        assertEquals("Result must have a size of 3", result.size(), 3);
        assertEquals("Key 'abs' must have value 'audio/x-mpeg'", result.get("abs"), "audio/x-mpeg");
    }
    
    public void testLoadOnNullRessource() {
        Map<String, String> result = MapLoader.load(null);
        assertNotNull("Result of MapLoader.load must be not null", result);
        assertEquals("Result must be empty", result.isEmpty(), true);
    }
    
}
