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

    public void warn(String string, final String... parameters);

    public void warn(final String msg, final Throwable t);

    public void error(final String msg);

    public void error(final String msg, final Throwable t);

    /**
     * Log Error from specified key message which will be formatted with parameters.
     * @param t
     * @param key
     * @param parameters
     */
    public void error(final Throwable t, final String key, final String... parameters);

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

    /**
     * Log Trace message.
     * @param key
     * @param parameters
     */
    public void trace(final String msg);

    /**
     * Log Trace from specified key message which will be formatted with parameters.
     * @param key
     * @param parameters
     */
    public void trace(final String key, final String... parameters);

    public boolean isInfoEnabled();

    public boolean isWarnEnabled();

    public boolean isErrorEnabled();

    public boolean isDebugEnabled();

    public boolean isTraceEnabled();
}
