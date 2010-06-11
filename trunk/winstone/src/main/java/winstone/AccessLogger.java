/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

/**
 * Used for logging accesses, eg in Apache access_log style
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: AccessLogger.java,v 1.2 2006/02/28 07:32:47 rickknowles Exp $
 */
public interface AccessLogger {
    public void log(String originalURL, WinstoneRequest request, WinstoneResponse response);
    public void destroy();
}
