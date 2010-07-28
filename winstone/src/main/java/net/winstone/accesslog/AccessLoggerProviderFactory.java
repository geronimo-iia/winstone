package net.winstone.accesslog;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * AccessLoggerProviderFactory instanciate AccessLoggerProvider using ServiceLoader provided by JDK 1.6.
 * 
 * @author Jerome guibert
 */
public class AccessLoggerProviderFactory {
    
    private final AccessLoggerProvider provider;
    
    private static class AccessLoggerProviderFactoryHolder {
        private static AccessLoggerProviderFactory loggerFactory = new AccessLoggerProviderFactory();
    }
    
    public static AccessLogger getAccessLogger(final String host, final String webapp, final PatternType patternType, final String filePattern) {
        return AccessLoggerProviderFactoryHolder.loggerFactory.provider.getAccessLogger(host, webapp, patternType, filePattern);
    }
    
    public static void destroy(AccessLogger accessLogger) {
        AccessLoggerProviderFactoryHolder.loggerFactory.provider.destroy(accessLogger);
    }
    
    private AccessLoggerProviderFactory() {
        ServiceLoader<AccessLoggerProvider> loader = ServiceLoader.load(AccessLoggerProvider.class);
        Iterator<AccessLoggerProvider> iterator = loader.iterator();
        AccessLoggerProvider accessLoggerProvider = null;
        while (accessLoggerProvider == null && iterator.hasNext()) {
            try {
                accessLoggerProvider = iterator.next();
            } catch (Throwable e) { 
            }
        }
        if (accessLoggerProvider == null) {
            throw new Error("No Access Logger Provider registered");
        }
        provider = accessLoggerProvider;
    }
}
