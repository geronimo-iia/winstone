package net.winstone.log;

/**
 * Minimal Logger facade interface.
 * 
 * @author Jerome Guibert
 */
public interface Logger {

    /**
     * Log Information message.
     * @param msg the message
     */
    public void info(final String msg);

    /**
     * Log Information from specified key message which will be formatted with parameters.
     * @param key the specified key
     * @param parameters additional parameters
     */
    public void info(final String key, final String... parameters);

    /**
     * Log Information message.
     * @param msg the message
     * @param t the exception (throwable) to log
     */
    public void info(final String msg, final Throwable t);

    /**
     *  Log Warn message.
     * @param msg the message
     */
    public void warn(final String msg);

    /**
     *  Log Warn from specified key message which will be formatted with parameters.
     * @param key the specified key
     * @param parameters additional parameters
     */
    public void warn(final String key, final String... parameters);

    /**
     *  Log Warn message.
     * @param msg the message
     * @param t the exception (throwable) to log
     */
    public void warn(final String msg, final Throwable t);

    /**
     *  Log Error message.
     * @param msg the message
     */
    public void error(final String msg);

    /**
     *  Log Error message.
     * @param msg the message
     * @param  t the exception (throwable) to log
     */
    public void error(final String msg, final Throwable t);

    /**
     * Log Error from specified key message which will be formatted with parameters.
     * @param t the exception (throwable) to log
     * @param key the specified key
     * @param parameters additional parametersers
     */
    public void error(final Throwable t, final String key, final String... parameters);

    /**
     * Log Debug message.
     * @param msg the message
     */
    public void debug(final String msg);

    /**
     * Log Debug from specified key message which will be formatted with parameters.
     * @param key the specified key
     * @param parameters additional parameters
     */
    public void debug(final String key, final String... parameters);

    /**
     * Log Debug message.
     * @param msg the message
     * @param t the exception (throwable) to log
     */
    public void debug(final String msg, final Throwable t);

    /**
     * Log Trace message.
     * @param msg  the message 
     */
    public void trace(final String msg);

    /**
     * Log Trace from specified key message which will be formatted with parameters.
     * @param key the specified key
     * @param parameters additional parameters
     */
    public void trace(final String key, final String... parameters);

    public boolean isInfoEnabled();

    public boolean isWarnEnabled();

    public boolean isErrorEnabled();

    public boolean isDebugEnabled();

    public boolean isTraceEnabled();
}
