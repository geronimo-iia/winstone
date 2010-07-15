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
        return StringUtils.globalReplace(this.resources.get(key), "[#0]", parameter);
    }
    
    /**
     * Perform a string replace for a set of from/to pairs.
     */
    public String getString(final String key, final String... parameters) {
        String myCopy = this.resources.get(key);
        if (parameters != null) {
            String tokens[][] = new String[parameters.length][2];
            for (int n = 0; n < parameters.length; n++) {
                tokens[n] = new String[] {
                    "[#" + n + "]", parameters[n]
                };
            }
            myCopy = StringUtils.globalReplace(myCopy, tokens);
        }
        return myCopy;
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
