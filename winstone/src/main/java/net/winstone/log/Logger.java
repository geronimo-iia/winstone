package net.winstone.log;

/**
 * Minimal Logger facade interface.
 * 
 * @author Jerome Guibert
 */
public interface Logger {

    /**
     * Log Information message.
     * @param msg
     */
    public void info(final String msg);

    /**
     * Log Information from specified key message which will be formatted with parameters.
     * @param key
     * @param parameters
     */
    public void info(final String key, final String... parameters);

    public void info(final String msg, final Throwable t);

    public void warn(final String msg);

    public void warn(final String msg, final Throwable t);

    public void error(final String msg);

    public void error(final String msg, final Throwable t);

    /**
     * Log Debug message.
     * @param msg
     */
    public void debug(final String msg);

    /**
     * Log Debug from specified key message which will be formatted with parameters.
     * @param key
     * @param parameters
     */
    public void debug(final String key, final String... parameters);

    public void debug(final String msg, final Throwable t);

    public void trace(final String msg);

    public boolean isInfoEnabled();

    public boolean isWarnEnabled();

    public boolean isErrorEnabled();
    
    public boolean isDebugEnabled();

    public boolean isTraceEnabled();
}
