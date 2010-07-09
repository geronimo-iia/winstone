package net.winstone.util;

import java.util.Map;

/**
 * String utility<br />
 * 
 * @author Jerome Guibert
 */
public class StringUtils {
    
    public static final boolean booleanArg(final Map<String, String> args, final String name, final boolean defaultTrue) {
        String value = args.get(name);
        if (defaultTrue)
            return (value == null) || (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
        else
            return (value != null) && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
    }
    
    public static final String stringArg(final Map<String, String> args, final String name, final String defaultValue) {
        return (args.get(name) == null ? defaultValue : args.get(name));
    }
    
    public static final int intArg(final Map<String, String> args, final String name, final int defaultValue) {
        return Integer.parseInt(stringArg(args, name, Integer.toString(defaultValue)));
    }
    
    /**
     * This function extract meaningful path or query
     * 
     * @param path path to extract from
     * @param query true if extract query
     * @return extraction or null
     */
    public static String extractQueryAnchor(final String path, final boolean query) {
        int qp = path.indexOf('?');
        if (query) {
            if (qp >= 0) {
                return path.substring(qp + 1);
            }
            return null;
        }
        int hp = path.indexOf('#');
        if (qp >= 0) {
            if (hp >= 0 && hp < qp) {
                return path.substring(0, hp);
            }
            return path.substring(0, qp);
        } else if (hp >= 0) {
            return path.substring(0, hp);
        }
        return path;
    }
    
}
