package net.winstone.log;

/**
 * SimpleLoggerProvider implement LoggerProvider.
 * @author Jerome Guibert
 */
public final class SimpleLoggerProvider implements LoggerProvider {

    public SimpleLoggerProvider() {
        super();
    }

    @Override
    public Logger getLogger(Class<?> className) {
        return className != null ? new SimpleLogger(null, className.getName()) : new SimpleLogger(null);
    }
}
