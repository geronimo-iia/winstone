package net.winstone.log;

/**
 * Minimal Logger facade interface.
 * 
 * @author Jerome Guibert
 */
public interface Logger {
    
    public void info(final String msg);
    
    public void info(final String msg, final Throwable t);
    
    public void warn(final String msg);
    
    public void warn(final String msg, final Throwable t);
    
    public void error(final String msg);
    
    public void error(final String msg, final Throwable t);

    public void debug(final String msg);
    
    public void debug(final String msg, final Throwable t);
}
