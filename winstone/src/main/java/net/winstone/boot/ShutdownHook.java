/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.boot;

import net.winstone.Server;

/**
 * A jvm hook to force the calling of the web-app destroy before the process
 * terminates
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ShutdownHook.java,v 1.3 2006/02/28 07:32:47 rickknowles Exp $
 */
public class ShutdownHook extends Thread {

	/**
	 * Server instance.
	 */
	private Server server;

	/**
	 * 
	 * Build a new instance of ShutdownHook.
	 * 
	 * @param server
	 *            server instance to manage.
	 */
	public ShutdownHook(final Server server) {
		this.server = server;
	}

	@Override
	public void run() {
		if (server != null) {
			// add shutdown log
			server.info("JVM is terminating. Shutting down Winstone");
			// process
			server.shutdown();
			server = null;
		}
	}
}
