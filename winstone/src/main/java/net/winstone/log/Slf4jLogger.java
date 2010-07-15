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
    
    public void error(String msg, Throwable t) {
        logger.error(msg, t);
    }
    
    public void error(String msg) {
        logger.error(msg);
    }
    
    public void info(String msg, Throwable t) {
        logger.info(msg, t);
    }
    
    public void info(String msg) {
        logger.info(msg);
    }
    
    public void warn(String msg, Throwable t) {
        logger.warn(msg, t);
    }
    
    public void warn(String msg) {
        logger.warn(msg);
    }
    
    @Override
    public Logger getLogger(Class<?> className) {
        return className != null ? new Slf4jLogger(className.getName()) : new Slf4jLogger();
    }
    
}
