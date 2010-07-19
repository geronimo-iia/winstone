package winstone;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

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
        return globalReplace(this.resources.get(key), "[#0]", parameter);
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
            myCopy = globalReplace(myCopy, tokens);
        }
        return myCopy;
    }
    
    /**
     * Just does a string swap, replacing occurrences of from with to.
     */
    public static String globalReplace(final String input, final String fromMarker, final String toValue) {
        StringBuffer out = new StringBuffer(input);
        globalReplace(out, fromMarker, toValue);
        return new String(out.toString());
    }
    
    private static void globalReplace(StringBuffer input, final String fromMarker, final String toValue) {
        if (input == null) {
            return;
        } else if (fromMarker == null) {
            return;
        }
        String value = toValue == null ? "(null)" : toValue;
        int index = 0;
        int foundAt = input.indexOf(fromMarker, index);
        while (foundAt != -1) {
            input.replace(foundAt, foundAt + fromMarker.length(), value);
            index = foundAt + toValue.length();
            foundAt = input.indexOf(fromMarker, index);
        }
    }
    
    public static String globalReplace(final String input, final String parameters[][]) {
        if (parameters != null) {
            StringBuffer out = new StringBuffer(input);
            for (int n = 0; n < parameters.length; n++) {
                globalReplace(out, parameters[n][0], parameters[n][1]);
            }
            return out.toString();
        } else {
            return input;
        }
    }
}
