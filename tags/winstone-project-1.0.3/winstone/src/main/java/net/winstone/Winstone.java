package net.winstone;

import java.io.IOException;

import net.winstone.boot.BootStrap;

/**
 * Utility to start winstone.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 */
public class Winstone {

	/**
	 * Main methods.
	 * 
	 * @param args
	 * @throws IOException
	 *             if something is wrong when reading properties files.
	 */
	public static void main(final String[] args) {
		final BootStrap bootStrap = new BootStrap(args);
		final Server server = bootStrap.boot();
		server.start();
	}
}
