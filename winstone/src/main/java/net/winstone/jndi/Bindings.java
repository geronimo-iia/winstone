package net.winstone.jndi;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;

public class Bindings implements NamingEnumeration<Binding> {
    
    private Iterator<Entry<String, Object>> iterator;
    private Context context;
    private Hashtable<String, Object> environnement;
    
    public Bindings(final Context context, final Hashtable<String, Object> environnement, final Map<String, Object> bindings) {
        super();
        this.context = context;
        this.environnement = environnement;
        this.iterator = bindings.entrySet().iterator();
    }
    
    public void close() throws NamingException {
        this.context = null;
        this.environnement = null;
        this.iterator = null;
    }
    
    public boolean hasMore() throws NamingException {
        if (iterator == null)
            throw new NamingException("Enumeration has already been closed");
        return iterator.hasNext();
    }
    
    public Binding next() throws NamingException {
        if (hasMore()) {
            Entry<String, Object> entry = iterator.next();
            
            String name = entry.getKey();
            Object value = entry.getValue();
            try {
                value = NamingManager.getObjectInstance(value, new CompositeName().add(name), this.context, this.environnement);
            } catch (Throwable err) {
                NamingException errNaming = new NamingException("Failed To Get Instance ");
                errNaming.setRootCause(err);
                throw errNaming;
            }
            return new Binding(name, value);
        }
        throw new NoSuchElementException();
    }
    
    public boolean hasMoreElements() {
        try {
            return hasMore();
        } catch (NamingException err) {
            return false;
        }
    }
    
    public Binding nextElement() {
        try {
            return next();
        } catch (NamingException namingException) {
            throw new NoSuchElementException();
        }
    }
    
}
