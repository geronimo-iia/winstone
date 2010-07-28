package net.winstone.log;

import java.util.Map;
import java.util.ResourceBundle;
import net.winstone.util.MapLoader;

/**
 * SimpleLoggerProvider implement LoggerProvider.
 * @author Jerome Guibert
 */
public class SimpleLoggerProvider implements LoggerProvider {

    private final Map<String, String> bundle;

    public SimpleLoggerProvider() {
        super();
        bundle =  MapLoader.load(ResourceBundle.getBundle("net.winstone.log.message"));
    }

    @Override
    public Logger getLogger(Class<?> className) {
        return className != null ? new SimpleLogger(bundle, className.getName()) : new SimpleLogger(bundle);
    }
}
