package winstone;

import java.util.Enumeration;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.common.collect.Maps;

/**
 * Load a resourceBundle and build a map with all keys/values.
 * 
 * @author Jerome Guibert
 */
public final class MapLoader {
    
    /**
     * Load the specified resource bundle and build a map with all keys finded.
     * 
     * @param resourceBundle the specified resource bundle
     * @return a <code>Map</code> instance representing key/value found in the specified resource bundle.
     */
    public final static Map<String, String> load(final ResourceBundle resourceBundle) {
        Enumeration<String> keys = resourceBundle.getKeys();
        Map<String, String> resources = Maps.newHashMap();
        String key = null;
        while (keys.hasMoreElements()) {
            key = (String)keys.nextElement();
            String value = resourceBundle.getString(key);
            resources.put(key, value);
        }
        return resources;
    }
}
