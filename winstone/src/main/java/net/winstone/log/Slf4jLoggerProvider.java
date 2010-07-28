package net.winstone.log;

import net.winstone.WinstoneResourceBundle;

/**
 *
 * @author Jerome Guibert
 */
public class Slf4jLoggerProvider implements LoggerProvider {

    private final WinstoneResourceBundle bundle;

    public Slf4jLoggerProvider() {
        super();
        bundle = new WinstoneResourceBundle("net.winstone.log.message");
    }

    @Override
    public Logger getLogger(Class<?> className) {
        return className != null ? new Slf4jLogger(bundle, className.getName()) : new Slf4jLogger(bundle);
    }
}
