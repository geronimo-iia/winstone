/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core;

import net.winstone.core.WebAppConfiguration;
import net.winstone.core.WinstoneRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.slf4j.LoggerFactory;

/**
 * Http session implementation for Winstone.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneSession.java,v 1.10 2006/08/27 07:19:47 rickknowles Exp $
 */
public class WinstoneSession implements HttpSession, Serializable {

    protected static org.slf4j.Logger logger = LoggerFactory.getLogger(WinstoneSession.class);
    /** Generated serial Version UID */
    private static final long serialVersionUID = 6106594480472753553L;
    public static final String SESSION_COOKIE_NAME = "JSESSIONID";
    private String sessionId;
    private WebAppConfiguration webAppConfig;
    private Map<String, Object> sessionData = new HashMap<String, Object>();
    private Set<WinstoneRequest> requestsUsingMe = new HashSet<WinstoneRequest>();
    private long createTime = System.currentTimeMillis();
    private long lastAccessedTime;
    private int maxInactivePeriod;
    private boolean isNew = Boolean.TRUE;
    private boolean isInvalidated = Boolean.FALSE;
    private HttpSessionAttributeListener sessionAttributeListeners[];
    private HttpSessionListener sessionListeners[];
    private HttpSessionActivationListener sessionActivationListeners[];
    private boolean distributable;
    private Object sessionMonitor = Boolean.TRUE;

    /**
     * Constructor
     */
    public WinstoneSession(final String sessionId) {
        super();
        this.sessionId = sessionId;
    }

    public void setWebAppConfiguration(WebAppConfiguration webAppConfig) {
        this.webAppConfig = webAppConfig;
        this.distributable = webAppConfig.isDistributable();
    }

    public void sendCreatedNotifies() {
        // Notify session listeners of new session
        for (int n = 0; n < this.sessionListeners.length; n++) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            this.sessionListeners[n].sessionCreated(new HttpSessionEvent(this));
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    public void setSessionActivationListeners(HttpSessionActivationListener listeners[]) {
        this.sessionActivationListeners = listeners;
    }

    public void setSessionAttributeListeners(HttpSessionAttributeListener listeners[]) {
        this.sessionAttributeListeners = listeners;
    }

    public void setSessionListeners(HttpSessionListener listeners[]) {
        this.sessionListeners = listeners;
    }

    public void setLastAccessedDate(long time) {
        this.lastAccessedTime = time;
    }

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    public void addUsed(WinstoneRequest request) {
        this.requestsUsingMe.add(request);
    }

    public void removeUsed(WinstoneRequest request) {
        this.requestsUsingMe.remove(request);
    }

    public boolean isUnusedByRequests() {
        return this.requestsUsingMe.isEmpty();
    }

    public boolean isExpired() {
        // check if it's expired yet
        long nowDate = System.currentTimeMillis();
        long maxInactive = getMaxInactiveInterval() * 1000;
        return ((maxInactive > 0) && (nowDate - this.lastAccessedTime > maxInactive));
    }

    // Implementation methods
    @Override
    public Object getAttribute(String name) {
        if (this.isInvalidated) {
            throw new IllegalStateException("Session has been invalidated");
        }
        Object att = null;
        synchronized (this.sessionMonitor) {
            att = this.sessionData.get(name);
        }
        return att;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        if (this.isInvalidated) {
            throw new IllegalStateException("Session has been invalidated");
        }
        Enumeration<String> names = null;
        synchronized (this.sessionMonitor) {
            names = Collections.enumeration(this.sessionData.keySet());
        }
        return names;
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (this.isInvalidated) {
            throw new IllegalStateException("Session has been invalidated");
        }
        // Check for serializability if distributable
        if (this.distributable && (value != null) && !(value instanceof java.io.Serializable)) {
            throw new IllegalArgumentException("Web application is marked distributable, but session object " + name + " (class " + value.getClass().getName() + ") does not extend java.io.Serializable - this variable will be ignored if the session is tranferred");
        }

        // valueBound must be before binding
        if (value instanceof HttpSessionBindingListener) {
            HttpSessionBindingListener hsbl = (HttpSessionBindingListener) value;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            hsbl.valueBound(new HttpSessionBindingEvent(this, name, value));
            Thread.currentThread().setContextClassLoader(cl);
        }

        Object oldValue = null;
        synchronized (this.sessionMonitor) {
            oldValue = this.sessionData.get(name);
            if (value == null) {
                this.sessionData.remove(name);
            } else {
                this.sessionData.put(name, value);
            }
        }

        // valueUnbound must be after unbinding
        if (oldValue instanceof HttpSessionBindingListener) {
            HttpSessionBindingListener hsbl = (HttpSessionBindingListener) oldValue;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            hsbl.valueUnbound(new HttpSessionBindingEvent(this, name, oldValue));
            Thread.currentThread().setContextClassLoader(cl);
        }

        // Notify other listeners
        if (oldValue != null) {
            for (int n = 0; n < this.sessionAttributeListeners.length; n++) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
                this.sessionAttributeListeners[n].attributeReplaced(new HttpSessionBindingEvent(this, name, oldValue));
                Thread.currentThread().setContextClassLoader(cl);
            }
        } else {
            for (int n = 0; n < this.sessionAttributeListeners.length; n++) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
                this.sessionAttributeListeners[n].attributeAdded(new HttpSessionBindingEvent(this, name, value));
                Thread.currentThread().setContextClassLoader(cl);

            }
        }
    }

    @Override
    public void removeAttribute(String name) {
        if (this.isInvalidated) {
            throw new IllegalStateException("Session has been invalidated");
        }
        Object value = null;
        synchronized (this.sessionMonitor) {
            value = this.sessionData.get(name);
            this.sessionData.remove(name);
        }

        // Notify listeners
        if (value instanceof HttpSessionBindingListener) {
            HttpSessionBindingListener hsbl = (HttpSessionBindingListener) value;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            hsbl.valueUnbound(new HttpSessionBindingEvent(this, name));
            Thread.currentThread().setContextClassLoader(cl);
        }
        if (value != null) {
            for (int n = 0; n < this.sessionAttributeListeners.length; n++) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
                this.sessionAttributeListeners[n].attributeRemoved(new HttpSessionBindingEvent(this, name, value));
                Thread.currentThread().setContextClassLoader(cl);
            }
        }
    }

    @Override
    public long getCreationTime() {
        if (this.isInvalidated) {
            throw new IllegalStateException("Session has been invalidated");
        }
        return this.createTime;
    }

    @Override
    public long getLastAccessedTime() {
        if (this.isInvalidated) {
            throw new IllegalStateException("Session has been invalidated");
        }
        return this.lastAccessedTime;
    }

    @Override
    public String getId() {
        return this.sessionId;
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.maxInactivePeriod;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactivePeriod = interval;
    }

    @Override
    public boolean isNew() {
        if (this.isInvalidated) {
            throw new IllegalStateException("Session has been invalidated");
        }
        return this.isNew;
    }

    @Override
    public ServletContext getServletContext() {
        return this.webAppConfig;
    }

    @Override
    public void invalidate() {
        if (this.isInvalidated) {
            throw new IllegalStateException("Session has been invalidated");
        }
        // Notify session listeners of invalidated session -- backwards
        for (int n = this.sessionListeners.length - 1; n >= 0; n--) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            this.sessionListeners[n].sessionDestroyed(new HttpSessionEvent(this));
            Thread.currentThread().setContextClassLoader(cl);
        }

        List<String> keys = new ArrayList<String>(this.sessionData.keySet());
        for (Iterator<String> i = keys.iterator(); i.hasNext();) {
            removeAttribute(i.next());
        }
        synchronized (this.sessionMonitor) {
            this.sessionData.clear();
        }
        this.isInvalidated = true;
        this.webAppConfig.removeSessionById(this.sessionId);
    }

    /**
     * Called after the session has been serialized to another server.
     */
    public void passivate() {
        // Notify session listeners of invalidated session
        for (int n = 0; n < this.sessionActivationListeners.length; n++) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            this.sessionActivationListeners[n].sessionWillPassivate(new HttpSessionEvent(this));
            Thread.currentThread().setContextClassLoader(cl);
        }

        // Question: Is passivation equivalent to invalidation ? Should all
        // entries be removed ?
        // List keys = new ArrayList(this.sessionData.keySet());
        // for (Iterator i = keys.iterator(); i.hasNext(); )
        // removeAttribute((String) i.next());
        synchronized (this.sessionMonitor) {
            this.sessionData.clear();
        }
        this.webAppConfig.removeSessionById(this.sessionId);
    }

    /**
     * Called after the session has been deserialized from another server.
     */
    public void activate(WebAppConfiguration webAppConfig) {
        this.webAppConfig = webAppConfig;
        webAppConfig.setSessionListeners(this);

        // Notify session listeners of invalidated session
        for (int n = 0; n < this.sessionActivationListeners.length; n++) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            this.sessionActivationListeners[n].sessionDidActivate(new HttpSessionEvent(this));
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    /**
     * Save this session to the temp dir defined for this webapp
     */
    public void saveToTemp() {
        File toDir = getSessionTempDir(this.webAppConfig);
        synchronized (this.sessionMonitor) {
            OutputStream out = null;
            ObjectOutputStream objOut = null;
            try {
                File toFile = new File(toDir, this.sessionId + ".ser");
                out = new FileOutputStream(toFile, false);
                objOut = new ObjectOutputStream(out);
                objOut.writeObject(this);
            } catch (IOException err) {
                logger.error("Error saving the session to temp space. Error:", err);
            } finally {
                if (objOut != null) {
                    try {
                        objOut.close();
                    } catch (IOException err) {
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException err) {
                    }
                }
            }
        }
    }

    public static File getSessionTempDir(WebAppConfiguration webAppConfig) {
        File tmpDir = (File) webAppConfig.getAttribute("javax.servlet.context.tempdir");
        File sessionsDir = new File(tmpDir, "WEB-INF" + File.separator + "winstoneSessions");
        if (!sessionsDir.exists()) {
            sessionsDir.mkdirs();
        }
        return sessionsDir;
    }

    public static void loadSessions(WebAppConfiguration webAppConfig) {
        int expiredCount = 0;
        // Iterate through the files in the dir, instantiate and then add to the sessions set
        File tempDir = getSessionTempDir(webAppConfig);
        File possibleSessionFiles[] = tempDir.listFiles();
        for (int n = 0; n < possibleSessionFiles.length; n++) {
            if (possibleSessionFiles[n].getName().endsWith(".ser")) {
                InputStream in = null;
                ObjectInputStream objIn = null;
                try {
                    in = new FileInputStream(possibleSessionFiles[n]);
                    objIn = new ObjectInputStream(in);
                    WinstoneSession session = (WinstoneSession) objIn.readObject();
                    session.setWebAppConfiguration(webAppConfig);
                    webAppConfig.setSessionListeners(session);
                    if (session.isExpired()) {
                        session.invalidate();
                        expiredCount++;
                    } else {
                        webAppConfig.addSession(session.getId(), session);
                        logger.debug("Successfully restored session id {} from temp space", session.getId());
                    }
                } catch (Throwable err) {
                    logger.error("Error loading session from temp space - skipping. Error:", err);
                } finally {
                    if (objIn != null) {
                        try {
                            objIn.close();
                        } catch (IOException err) {
                        }
                    }
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException err) {
                        }
                    }
                    possibleSessionFiles[n].delete();
                }
            }
        }
        if (expiredCount > 0) {
            logger.debug(expiredCount + " Session(s) has been invalidated");
        }
    }

    /**
     * Serialization implementation. This makes sure to only serialize the parts we want to send to another server.
     * 
     * @param out The stream to write the contents to
     * @throws IOException
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeUTF(sessionId);
        out.writeLong(createTime);
        out.writeLong(lastAccessedTime);
        out.writeInt(maxInactivePeriod);
        out.writeBoolean(isNew);
        out.writeBoolean(distributable);

        // Write the map, but first remove non-serializables
        Map<String, Object> copy = new HashMap<String, Object>(sessionData);
        Set<String> keys = new HashSet<String>(copy.keySet());
        for (Iterator<String> i = keys.iterator(); i.hasNext();) {
            String key = i.next();
            if (!(copy.get(key) instanceof Serializable)) {
                logger.warn("Web application is marked distributable, but session object {} (class {}) does not extend java.io.Serializable - this variable is being ignored by session transfer", key, copy.get(key).getClass().getName());
            }
            copy.remove(key);
        }
        out.writeInt(copy.size());
        for (Iterator<String> i = copy.keySet().iterator(); i.hasNext();) {
            String key = i.next();
            out.writeUTF(key);
            out.writeObject(copy.get(key));
        }
    }

    /**
     * Deserialization implementation
     * 
     * @param in The source of stream data
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.sessionId = in.readUTF();
        this.createTime = in.readLong();
        this.lastAccessedTime = in.readLong();
        this.maxInactivePeriod = in.readInt();
        this.isNew = in.readBoolean();
        this.distributable = in.readBoolean();

        // Read the map
        this.sessionData = new HashMap<String, Object>();
        this.requestsUsingMe = new HashSet<WinstoneRequest>();
        int entryCount = in.readInt();
        for (int n = 0; n < entryCount; n++) {
            String key = in.readUTF();
            Object variable = in.readObject();
            this.sessionData.put(key, variable);
        }
        this.sessionMonitor = Boolean.TRUE;
    }

    /**
     * @deprecated
     */
    @Override
    public Object getValue(String name) {
        return getAttribute(name);
    }

    /**
     * @deprecated
     */
    @Override
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    /**
     * @deprecated
     */
    @Override
    public void removeValue(String name) {
        removeAttribute(name);
    }

    /**
     * @deprecated
     */
    @Override
    public String[] getValueNames() {
        return (String[]) this.sessionData.keySet().toArray(new String[0]);
    }

    /**
     * @deprecated
     */
    @Override
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        return null;
    } // deprecated

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        WinstoneSession other = (WinstoneSession) obj;
        if (sessionId == null) {
            if (other.sessionId != null) {
                return false;
            }
        } else if (!sessionId.equals(other.sessionId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "WinstoneSession [sessionId=" + sessionId + "]";
    }
}