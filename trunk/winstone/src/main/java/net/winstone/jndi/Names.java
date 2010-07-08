package net.winstone.jndi;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * Enumeration across the names/classes of the bindings in a particular context. Used by the list() method.
 * 
 * @author Jerome Guibert
 */
public class Names implements NamingEnumeration<NameClassPair> {
    
    private Iterator<Entry<String, Object>> iterator;
    
    
    public Names(final Map<String, Object> bindings) {
        super();
        this.iterator =  bindings.entrySet().iterator();
    }
    
    public void close() throws NamingException {
        this.iterator = null;
    }
    
    public boolean hasMore() throws NamingException {
        if (iterator == null)
            throw new NamingException("Enumeration has already been closed");
        return iterator.hasNext();
    }
    
    public NameClassPair next() throws NamingException {
        if (hasMore()) {
            Entry<String, Object> entry = iterator.next();
            return new NameClassPair(entry.getKey(), entry.getValue().getClass().getName());
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
    
    public NameClassPair nextElement() {
        try {
            return next();
        } catch (NamingException namingException) {
            throw new NoSuchElementException();
        }
    }
}
