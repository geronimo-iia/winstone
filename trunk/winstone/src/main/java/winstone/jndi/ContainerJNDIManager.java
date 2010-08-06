/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.jndi;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;

import net.winstone.WinstoneResourceBundle;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Node;

/**
 * Implements a simple web.xml + command line arguments style jndi manager
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ContainerJNDIManager.java,v 1.3 2006/02/28 07:32:48 rickknowles Exp $
 */
public class ContainerJNDIManager implements JNDIManager {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(ContainerJNDIManager.class);
    protected Map<String, Object> objectsToCreate;

    /**
     * Gets the relevant list of objects from the args, validating against the web.xml nodes supplied. All node addresses are assumed to be
     * relative to the java:/comp/env context
     */
    public ContainerJNDIManager(Map<String, String> args, List<Node> webXmlNodes, ClassLoader loader) {
        // Build all the objects we wanted
        this.objectsToCreate = new HashMap<String, Object>();

        Collection<String> keys = new ArrayList<String>(args != null ? args.keySet() : (Collection<String>) new ArrayList<String>());
        for (Iterator<String> i = keys.iterator(); i.hasNext();) {
            String key = (String) i.next();

            if (key.startsWith("jndi.resource.")) {
                String resName = key.substring(14);
                String className = (String) args.get(key);
                String value = (String) args.get("jndi.param." + resName + ".value");
                logger.debug("Creating object: {} from startup arguments", resName);
                Object obj = createObject(resName.trim(), className.trim(), value, args, loader);
                if (obj != null) {
                    this.objectsToCreate.put(resName, obj);
                }
            }
        }
    }

    /**
     * Add the objects passed to the constructor to the JNDI Context addresses specified
     */
    @Override
    public void setup() {

        try {
            InitialContext ic = new InitialContext();
            for (Iterator<String> i = this.objectsToCreate.keySet().iterator(); i.hasNext();) {
                String name = (String) i.next();
                try {
                    Name fullName = new CompositeName(name);
                    Context currentContext = ic;
                    while (fullName.size() > 1) {
                        // Make contexts that are not already present
                        try {
                            currentContext = currentContext.createSubcontext(fullName.get(0));
                        } catch (NamingException err) {
                            currentContext = (Context) currentContext.lookup(fullName.get(0));
                        }
                        fullName = fullName.getSuffix(1);
                    }
                    ic.bind(name, this.objectsToCreate.get(name));
                    logger.debug("Bound object {} to JNDI Context", name);
                } catch (NamingException err) {
                    logger.error("Error binding object " + name + " to JNDI Context", err);
                }
            }
            logger.debug("JNDI initialization complete - " + this.objectsToCreate.size() + " items pre-bound");
        } catch (NamingException err) {
            logger.error("Could not get an initial JNDI Context", err);
        }
    }

    /**
     * Remove the objects under administration from the JNDI Context, and then destroy the objects
     */
    @Override
    public void tearDown() {
        try {
            InitialContext ic = new InitialContext();
            for (Iterator<String> i = this.objectsToCreate.keySet().iterator(); i.hasNext();) {
                String name = (String) i.next();
                try {
                    ic.unbind(name);
                } catch (NamingException err) {
                    logger.error("Error unbinding object " + name + " from JNDI Context", err);
                }
                Object unboundObject = this.objectsToCreate.get(name);
//                if (unboundObject instanceof WinstoneDataSource) {
//                    ((WinstoneDataSource) unboundObject).destroy();
//                }
                logger.debug("Unbound object {} from JNDI Context", name);
            }
            logger.debug("JNDI shutdown complete - " + this.objectsToCreate.size() + " items declared at startup unbound");
        } catch (NamingException err) {
            logger.error("Could not get an initial JNDI Context", err);
        }
    }

    /**
     * Build an object to insert into the jndi space
     */
    protected final Object createObject(String name, String className, String value, Map<String, String> args, ClassLoader loader) {

        if ((className == null) || (name == null)) {
            return null;
        }

        // Set context class loader
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);

        try {
            // If we are working with a datasource
            if (className.equals("javax.sql.DataSource")) {
                try {
                    return null;//new WinstoneDataSource(name, extractRelevantArgs(args, name), loader);
                } catch (Throwable err) {
                    logger.error("Error building JDBC Datasource object " + name, err);
                }
            } // If we are working with a mail session
            else if (className.equals("javax.mail.Session")) {
                try {
                    Class<?> smtpClass = Class.forName(className, true, loader);
                    Method smtpMethod = smtpClass.getMethod("getInstance", new Class[]{
                                Properties.class, Class.forName("javax.mail.Authenticator")
                            });
                    return smtpMethod.invoke(null, new Object[]{
                                extractRelevantArgs(args, name), null
                            });
                    // return Session.getInstance(extractRelevantArgs(args, name), null);
                } catch (Throwable err) {
                    logger.error("Error building JavaMail session " + name, err);
                }
            } // If unknown type, try to instantiate with the string constructor
            else if (value != null) {
                try {
                    Class<?> objClass = Class.forName(className.trim(), true, loader);
                    Constructor<?> objConstr = objClass.getConstructor(new Class[]{
                                String.class
                            });
                    return objConstr.newInstance(new Object[]{
                                value
                            });
                } catch (Throwable err) {
                    logger.error("Error building JNDI object " + name + " (class: " + className + ")", err);
                }
            }

            return null;

        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    /**
     * Rips the parameters relevant to a particular resource from the command args
     */
    private Map<String, String> extractRelevantArgs(Map<String, String> input, String name) {
        Map<String, String> relevantArgs = new HashMap<String, String>();
        for (Iterator<String> i = input.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            if (key.startsWith("jndi.param." + name + ".")) {
                relevantArgs.put(key.substring(12 + name.length()), input.get(key));
            }
        }
        return relevantArgs;
    }
}
