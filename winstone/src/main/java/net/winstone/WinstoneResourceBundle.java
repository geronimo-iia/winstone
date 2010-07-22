package net.winstone;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import net.winstone.util.MapLoader;
import net.winstone.util.StringUtils;

/**
 * A ResourceBundle that includes the ability to do string replacement on the resources it retrieves (based on Rick Knowles), and where all
 * properties are loaded in memory.
 * 
 * @author Jérôme Guibert
 */
public class WinstoneResourceBundle {
    
    protected final Map<String, String> resources;
    
    private static class WinstoneResourceBundleHolder {
        private static WinstoneResourceBundle bundle = new WinstoneResourceBundle("net.winstone.winstone-message");
    }
    
    public static WinstoneResourceBundle getInstance() {
        return WinstoneResourceBundleHolder.bundle;
    }
    
    /**
     * Constructor
     */
    public WinstoneResourceBundle(final String baseName) {
        this(ResourceBundle.getBundle(baseName));
    }
    
    public WinstoneResourceBundle(final String baseName, final Locale locale) {
        this(ResourceBundle.getBundle(baseName, locale));
    }
    
    public WinstoneResourceBundle(final String baseName, final Locale locale, final ClassLoader classLoader) {
        this(ResourceBundle.getBundle(baseName, locale, classLoader));
    }
    
    public WinstoneResourceBundle(final ResourceBundle resourceBundle) {
        super();
        resources = MapLoader.load(resourceBundle);
    }
    
    public Iterable<String> getKeys() {
        return this.resources.keySet();
    }
    
    /**
     * Default getString method
     */
    public String getString(final String key) {
        return this.resources.get(key);
    }
    
    /**
     * Perform a string replace for a single from/to pair.
     */
    public String getString(final String key, final String parameter) {
        return StringUtils.replace(this.resources.get(key), "[#0]", parameter);
    }
    
    /**
     * Perform a string replace for a set of from/to pairs.
     */
    public String getString(final String key, final String... parameters) {
        return StringUtils.replaceToken(this.resources.get(key), parameters);
    }
    
    /**
     * Perform a format with parameters on keyed resources.
     * 
     * @param key
     * @param parameters
     * @return a formatted message.
     */
    public String format(final String key, final Object... parameters) {
        return String.format(this.resources.get(key), parameters);
    }
    
}
