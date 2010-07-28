package net.winstone.log;

import java.text.SimpleDateFormat;
import java.util.Map;
import net.winstone.WinstoneResourceBundle;
import net.winstone.util.DateCache;

/**
 * A utility class for logging event and status messages. SimpleLogger log all message in System.err stream.<br />
 * 
 * @author Jerome Guibert
 */
public class SimpleLogger extends AbstractLogger implements Logger {

    private final static String lineSeparator = System.getProperty("line.separator");
    private final DateCache dateCache = new DateCache(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    private final StringBuilder fullMessage = new StringBuilder(128);
    private final String name; 

    public SimpleLogger(final Map<String, String> bundle) {
        this(bundle, "net.winstone.log");
    }

    public SimpleLogger(final Map<String, String> bundle, final String name) {
        super(bundle);
        this.name = ":" + name + ":";
    }

    private void log(final String level, final String message, final Throwable error) {
        synchronized (fullMessage) {
            fullMessage.append(dateCache.now());
            fullMessage.append(level);
            fullMessage.append(name);
            fullMessage.append(message);
            // add error if necessary
            if (error != null) {
                fullMessage.append(lineSeparator);
                fullMessage.append(error.toString());
                StackTraceElement[] elements = error.getStackTrace();
                for (int i = 0; elements != null && i < elements.length; i++) {
                    fullMessage.append(lineSeparator);
                    fullMessage.append("\tat ");
                    fullMessage.append(elements[i].toString());
                }
            }
            fullMessage.append(lineSeparator);
            // write message on err
            System.err.print(fullMessage.toString());
            System.err.flush();
        }
    }

    @Override
    public void error(String msg) {
        log(":ERROR", msg, null);
    }

    @Override
    public void error(String msg, Throwable t) {
        log(":ERROR", msg, t);
    }

    @Override
    public void info(String msg) {
        log(":INFO", msg, null);
    }

    @Override
    public void info(String msg, Throwable t) {
        log(":INFO", msg, t);
    }

    @Override
    public void warn(String msg) {
        log(":WARN", msg, null);
    }

    @Override
    public void warn(String msg, Throwable t) {
        log(":WARN", msg, t);
    }

    @Override
    public void debug(String msg) {
        log(":DEBUG", msg, null);
    }

    @Override
    public void debug(String msg, Throwable t) {
        log(":DEBUG", msg, t);
    }

    @Override
    public void trace(String msg) {
        log(":TRACE", msg, null);
    }

    @Override
    public boolean isDebugEnabled() {
        return Boolean.TRUE;
    }

    @Override
    public boolean isTraceEnabled() {
        return Boolean.TRUE;
    }

    @Override
    public boolean isWarnEnabled() {
        return Boolean.TRUE;
    }

    @Override
    public boolean isInfoEnabled() {
        return Boolean.TRUE;
    }

    @Override
    public boolean isErrorEnabled() {
        return Boolean.TRUE;
    }
}
