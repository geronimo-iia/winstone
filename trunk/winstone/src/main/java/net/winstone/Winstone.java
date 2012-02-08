package net.winstone;

import net.winstone.boot.BootStrap;
import net.winstone.boot.Server;

/**
 * 
 * @author Jerome Guibert
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
		BootStrap bootStrap = new BootStrap(args);
		Server server = bootStrap.boot();
		server.start();
	}
}
