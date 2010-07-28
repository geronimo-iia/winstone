package net.winstone.log;

import net.winstone.WinstoneResourceBundle;
import org.slf4j.LoggerFactory;

public class Slf4jLogger implements Logger {

    private final WinstoneResourceBundle bundle;
    private final org.slf4j.Logger logger;

    public Slf4jLogger(final WinstoneResourceBundle bundle) {
        this(bundle, "net.winstone.log");
    }

    public Slf4jLogger(final WinstoneResourceBundle bundle, final String name) {
        super();
        logger = LoggerFactory.getLogger(name);
        this.bundle = bundle;
    }

    @Override
    public void error(String msg, Throwable t) {
        logger.error(msg, t);
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
    }

    @Override
    public void info(String msg, Throwable t) {
        logger.info(msg, t);
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void warn(String msg, Throwable t) {
        logger.warn(msg, t);
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void debug(String msg, Throwable t) {
        logger.debug(msg, t);
    }

    @Override
    public void trace(String msg) {
        logger.trace(msg);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void info(String key, String... parameters) {
        if (logger.isInfoEnabled()) {
            logger.info(bundle.getString(key, parameters));
        }
    }

    @Override
    public void debug(String key, String... parameters) {
        if (logger.isDebugEnabled()) {
            logger.debug(bundle.getString(key, parameters));
        }
    }
}
