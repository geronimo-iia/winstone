package net.winstone.log;

import org.slf4j.LoggerFactory;

public class Slf4jLogger implements Logger, LoggerProvider {

    private org.slf4j.Logger logger;

    public Slf4jLogger() {
        this("net.winstone.log");
    }

    public Slf4jLogger(final String name) {
        super();
        logger = LoggerFactory.getLogger(name);
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
    public Logger getLogger(Class<?> className) {
        return className != null ? new Slf4jLogger(className.getName()) : new Slf4jLogger();
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }
}
