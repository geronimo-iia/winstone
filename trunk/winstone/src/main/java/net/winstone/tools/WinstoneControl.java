/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.tools;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;

import net.winstone.boot.BootStrap;
import net.winstone.boot.Command;
import net.winstone.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Included so that we can control winstone from the command line a little more
 * easily.
 * <p>
 * TODO add a rest like control more human readable.
 * </p>
 * 
 * @author Jerome Guibert
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneControl.java,v 1.6 2006/03/13 15:37:29 rickknowles Exp
 *          $
 */
public class WinstoneControl {

	private final static int TIMEOUT = 10000;
	protected Logger logger = LoggerFactory.getLogger(getClass());

	public WinstoneControl() {
		super();
	}

	/**
	 * Parses command line parameters, and calls the appropriate method for
	 * executing the winstone operation required.
	 */
	public static void main(final String argv[]) {
		new WinstoneControl().call(argv);
	}

	/**
	 * Displays the usage message
	 */
	private static void printUsage() {
		System.out.println("Winstone Command Line Controller\nUsage: java winstone.tools.WinstoneControl <operation> --host=<host> --port=<control port>\n\n<operation> can be \"shutdown\" or \"reload:<prefix>\"");
	}

	public void call(final String[] argv) {

		// Load args from the config file
		final Map<String, String> options = new BootStrap(argv).loadArgs("operation");
		if (options.isEmpty() || options.containsKey("usage") || options.containsKey("help")) {
			WinstoneControl.printUsage();
			return;
		}
		assert options != null;

		final String operation = options.get("operation");
		if (options.containsKey("controlPort") && !options.containsKey("port")) {
			options.put("port", options.get("controlPort"));
		}

		if ((operation == null) || operation.equals("")) {
			WinstoneControl.printUsage();
			return;
		}

		final String host = StringUtils.stringArg(options, "host", "localhost");
		final String port = StringUtils.stringArg(options, "port", "8081");
		logger.info("Connecting to {}:{}", host, port);

		// Check for shutdown
		if (operation.equalsIgnoreCase("shutdown")) {
			execute(host, port, Command.SHUTDOWN, null);
		} // check for reload
		else if (operation.toLowerCase().startsWith("reload:")) {
			execute(host, port, Command.RELOAD, operation.substring("reload:".length()));
			logger.info("Successfully sent webapp reload command to {}:{}", host, port);
		} else {
			WinstoneControl.printUsage();
		}
	}

	/**
	 * Execute a call for the specified command.
	 * 
	 * @param host
	 * @param port
	 * @param command
	 * @param extra
	 */
	private void execute(final String host, final String port, final Command command, final String extra) {
		Socket socket = null;
		try {
			socket = new Socket(host, Integer.parseInt(port));
			socket.setSoTimeout(WinstoneControl.TIMEOUT);
			final OutputStream out = socket.getOutputStream();
			out.write(command.getCode());
			if (extra != null) {
				final ObjectOutputStream objOut = new ObjectOutputStream(out);
				objOut.writeUTF(host);
				objOut.writeUTF(extra);
				objOut.close();
			}
			logger.info("Successfully sent server shutdown command to {}:{}", host, port);

		} catch (final NumberFormatException e) {
			logger.error("execute: " + command, e);
		} catch (final UnknownHostException e) {
			logger.error("execute: " + command, e);
		} catch (final IOException e) {
			logger.error("execute: " + command, e);
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (final IOException e) {
				}
			}
		}
	}
}
