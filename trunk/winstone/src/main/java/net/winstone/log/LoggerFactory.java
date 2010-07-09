package net.winstone.log;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
//TODO finalize it
public class LoggerFactory {
    // used for inner logger
    private static Writer stream;
    // our invocation handler
    private InvocationHandler handler;
    // slf4j factory
    private Object factory;
    private Class<?> sl4jFactory;
    
    // singleton holder pattern
    private static class LoggerFactoryHolder {
        private static LoggerFactory loggerFactory = new LoggerFactory();
    }
    
    public static Logger getLogger(final Class<?> className) {
        return LoggerFactoryHolder.loggerFactory.instanciateLogger(className);
    }
    
    /**
     * Build a new instance of Logger Factory.
     */
    private LoggerFactory() {
        super();
        handler = null;
        sl4jFactory = null;
        factory = null;
        // try loading SL4J factory
        try {
            sl4jFactory = Class.forName("org.slf4j.LoggerFactory", true, getClass().getClassLoader());
            // Method methodFactory = sl4jFactory.getMethod("getILoggerFactory");
            // factory = methodFactory.invoke(sl4jFactory);
            
        } catch (Throwable e) {
            e.printStackTrace();
        }
        // use local implementation
        if (sl4jFactory == null) {
            stream = new PrintWriter(System.err);
            handler = new InvocationHandler() {
                private String lineSeparator = System.getProperty("line.separator");
                private DateFormat logFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                
                // use innner implementation
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (stream != null) {
                        final String level = method.getName();
                        final String message = (String)args[0];
                        final Throwable error = (args.length > 1) ? (Throwable)args[1] : null;
                        
                        Writer fullMessage = new StringWriter();
                        String date = null;
                        synchronized (logFormat) {
                            date = logFormat.format(new Date());
                        }
                        try {
                            fullMessage.write("[");
                            fullMessage.write(level);
                            fullMessage.write(" ");
                            fullMessage.write(date);
                            fullMessage.write("] - ");
                            fullMessage.write(message);
                            if (error != null) {
                                fullMessage.write(lineSeparator);
                                PrintWriter pw = new PrintWriter(fullMessage);
                                error.printStackTrace(pw);
                                pw.flush();
                            }
                            fullMessage.write(lineSeparator);
                            
                            stream.write(fullMessage.toString());
                            stream.flush();
                        } catch (IOException err) {
                            System.err.println("Error writing log message: " + message);
                            err.printStackTrace(System.err);
                        }
                    }
                    
                    return null;
                }
            };
        }
    }
    
    /**
     * Instantiate a logger
     * 
     * @param className
     * @return
     */
    private Logger instanciateLogger(final Class<?> className) {
        InvocationHandler invocationHandler = handler;
        if (invocationHandler == null) {
            try {
                Method method = sl4jFactory.getMethod("getLogger", Class.class);
                final Object logger = method.invoke(sl4jFactory, className);
                invocationHandler = new InvocationHandler() {
                    // use SL4J implementation
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return method.invoke(logger, args);
                    }
                };
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return (Logger)Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {
            Logger.class
        }, invocationHandler);
    }
}
