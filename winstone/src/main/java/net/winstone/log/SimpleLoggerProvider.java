package net.winstone.log;

import net.winstone.WinstoneResourceBundle;

/**
 * SimpleLoggerProvider implement LoggerProvider.
 * @author Jerome Guibert
 */
public class SimpleLoggerProvider implements LoggerProvider {

    private final WinstoneResourceBundle bundle;

    public SimpleLoggerProvider() {
        super();
        bundle = new WinstoneResourceBundle("net.winstone.log.message");
    }

    @Override
    public Logger getLogger(Class<?> className) {
        return className != null ? new SimpleLogger(bundle, className.getName()) : new SimpleLogger(bundle);
    }
}
