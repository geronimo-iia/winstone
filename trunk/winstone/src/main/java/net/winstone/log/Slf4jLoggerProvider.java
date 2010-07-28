package net.winstone.log;

import java.util.Map;
import java.util.ResourceBundle;
import net.winstone.util.MapLoader;

/**
 *
 * @author Jerome Guibert
 */
public class Slf4jLoggerProvider implements LoggerProvider {

    private final Map<String, String> bundle;

    public Slf4jLoggerProvider() {
        super();
        bundle =  MapLoader.load(ResourceBundle.getBundle("net.winstone.log.message"));
    }

    @Override
    public Logger getLogger(Class<?> className) {
        return className != null ? new Slf4jLogger(bundle, className.getName()) : new Slf4jLogger(bundle);
    }
}
