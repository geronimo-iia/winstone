package net.winstone.log;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * LoggerFactory implements mechanism for loading logger provider.
 * 
 * @author Jerome Guibert
 */
public final class LoggerFactory {
    
    private final LoggerProvider provider;
    
    // singleton holder pattern
    private static class LoggerFactoryHolder {
        private static LoggerFactory loggerFactory = new LoggerFactory();
    }
    
    /**
     * Return a logger for the specified class name.
     * 
     * @param className
     * @return a logger instance for the specified class name.
     */
    public static Logger getLogger(final Class<?> className) {
        return LoggerFactoryHolder.loggerFactory.provider.getLogger(className);
    }
    
    private LoggerFactory() {
        ServiceLoader<LoggerProvider> loader = ServiceLoader.load(LoggerProvider.class);
        Iterator<LoggerProvider> iterator = loader.iterator();
        LoggerProvider loggerProvider = null;
        while (loggerProvider == null && iterator.hasNext()) {
            try {
                loggerProvider = iterator.next();
            } catch (Throwable e) {
            }
        }
        if (loggerProvider == null) {
            throw new Error("No Logger Provider registered");
        }
        provider = loggerProvider;
    }
    
}
