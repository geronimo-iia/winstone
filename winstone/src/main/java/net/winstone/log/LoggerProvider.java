package net.winstone.log;

/**
 * LoggerProvider interface.
 * 
 * @author Jerome Guibert
 */
public interface LoggerProvider {
    
    public Logger getLogger(final Class<?> className);
}
