/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.test.load;

import java.util.Map;

import net.winstone.Server;
import net.winstone.boot.BootStrap;

import org.slf4j.LoggerFactory;

/**
 * This is the class that gets launched by the command line. Load argument from
 * command line, process them and try to launch server.
 * 
 *@author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public class Launcher {

	protected static org.slf4j.Logger logger = LoggerFactory.getLogger(Launcher.class);
	// server attribut member
	private Server server;

	/**
	 * Main method. This basically just accepts a few args, then initialises the
	 * listener thread. For now, just shut it down with a control-C.
	 */
	public static void main(final String argv[]) {
		final Launcher launcher = new Launcher(argv);
		launcher.launch();
	}

	/**
	 * Build a new Launcher from java code.
	 * 
	 * @param args
	 */
	public Launcher(final Map<String, String> args) {
		server = new BootStrap(args).boot();
	}

	/**
	 * Build a new Launcher from command line.
	 * 
	 * @param argv
	 */
	public Launcher(final String argv[]) {
		super();
		server = new BootStrap(argv).boot();
	}

	/**
	 * Launch server.
	 */
	public final void launch() {
		try {
			if (server != null) {
				server.start();
			}
		} catch (final Throwable err) {
			System.err.println("Container startup failed");
			err.printStackTrace(System.err);
		}
	}

	/**
	 * Shutdown server.
	 */
	public void shutdown() {
		if (server != null) {
			server.shutdown();
			server = null;
		}
	}
}
