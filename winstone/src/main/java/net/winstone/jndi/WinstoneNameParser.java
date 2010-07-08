/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.jndi;

import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

/**
 * The name parser for winstone jndi names.<br />
 * The NameParser contains knowledge of the syntactic information (like left-to-right orientation, name separator, etc.) needed to parse
 * names. <br />
 * The equals() method, when used to compare two NameParsers, returns true if and only if they serve the same namespace.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @author Jerome Guibert
 */
public class WinstoneNameParser implements NameParser {
    
    private static final transient Properties syntax = new Properties();
    
    static {
        syntax.put("jndi.syntax.direction", "left_to_right");
        syntax.put("jndi.syntax.separator", "/");
        syntax.put("jndi.syntax.ignorecase", "false");
        syntax.put("jndi.syntax.escape", "\\");
        syntax.put("jndi.syntax.beginquote", "'");
    }
    
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
    
}
