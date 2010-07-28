package net.winstone.log;

import java.util.Map;
import net.winstone.util.StringUtils;

/**
 *AbstractLogger implement log message associated with a resource bundle.
 * @author Jerome guibert
 */
public abstract class AbstractLogger implements Logger {

    Map<String, String> bundle;

    public AbstractLogger(final Map<String, String> bundle) {
        this.bundle = bundle;
    }

    @Override
    public final void info(final String key, final String... parameters) {
        if (isInfoEnabled()) {
            info(StringUtils.replaceToken(bundle.get(key), parameters));
        }
    }

    @Override
    public final void debug(final String key, final String... parameters) {
        if (isDebugEnabled()) {
            debug(StringUtils.replaceToken(bundle.get(key), parameters));
        }
    }

    @Override
    public final void error(final Throwable t, final String key, final String... parameters) {
        if (isErrorEnabled()) {
            error(StringUtils.replaceToken(bundle.get(key), parameters), t);
        }
    }

    @Override
    public final void trace(final String key, final String... parameters) {
        if (isDebugEnabled()) {
            trace(StringUtils.replaceToken(bundle.get(key), parameters));
        }
    }

    @Override
    public void warn(final String key, final String... parameters) {
        if (isWarnEnabled()) {
            warn(StringUtils.replaceToken(bundle.get(key), parameters));
        }
    }
}
