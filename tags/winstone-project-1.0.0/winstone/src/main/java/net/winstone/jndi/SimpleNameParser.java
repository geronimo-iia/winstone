package net.winstone.jndi;

import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

/**
 * The name parser for jndi names.<br />
 * The NameParser contains knowledge of the syntactic information (like left-to-right orientation, name separator, etc.) needed to parse
 * names. <br />
 * The equals() method, when used to compare two NameParsers, returns true if and only if they serve the same namespace.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @author Jerome Guibert
 */
public class SimpleNameParser implements NameParser {
    
    private static final transient Properties syntax = new Properties();
    
    static {
        syntax.put("jndi.syntax.direction", "left_to_right");
        syntax.put("jndi.syntax.separator", "/");
        syntax.put("jndi.syntax.ignorecase", "false");
        syntax.put("jndi.syntax.escape", "\\");
        syntax.put("jndi.syntax.beginquote", "'");
    }
    
    @Override
    public Name parse(final String name) throws NamingException {
        return new CompoundName(name != null ? name : "", syntax);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        return hash;
    }
    
}
