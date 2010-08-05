package net.winstone.log;

/**
 *
 * @author Jerome Guibert
 */
public final class Slf4jLoggerProvider implements LoggerProvider {

    public Slf4jLoggerProvider() {
        super();
    }

    @Override
    public Logger getLogger(Class<?> className) {
        return className != null ? new Slf4jLogger(null, className.getName()) : new Slf4jLogger(null);
    }
}
