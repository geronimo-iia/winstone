/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.IOException;

/**
 * Implements the main launcher daemon thread. This is the class that gets launched by the command line, and owns the server socket, etc.
 *
 * TODO add jndi parameter analyse
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: Launcher.java,v 1.29 2007/04/23 02:55:35 rickknowles Exp $
 */
public class Launcher {

    public static void main(String argv[]) throws IOException {
        net.winstone.Launcher.main(argv);
    }
}
