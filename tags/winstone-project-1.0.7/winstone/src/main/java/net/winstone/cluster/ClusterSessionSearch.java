/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.cluster;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import net.winstone.core.WinstoneSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains all the logic for reading in sessions
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ClusterSessionSearch.java,v 1.6 2006/03/24 17:24:18 rickknowles
 *          Exp $
 */
public final class ClusterSessionSearch implements Runnable {

	protected Logger logger = LoggerFactory.getLogger(getClass());
	final int TIMEOUT = 2000;
	public static final byte SESSION_CHECK_TYPE = (byte) '1';
	public static final String SESSION_NOT_FOUND = "NOTFOUND";
	public static final String SESSION_FOUND = "FOUND";
	public static final String SESSION_RECEIVED = "OK";
	private boolean started;
	private boolean isFinished;
	private WinstoneSession result;
	private final String searchWebAppHostname;
	private final String searchWebAppPrefix;
	private final String searchId;
	private final String searchAddressPort;
	private final int controlPort;

	/**
	 * Sets up for a threaded search
	 */
	public ClusterSessionSearch(final String webAppPrefix, final String hostName, final String sessionId, final String ipPort, final int controlPort) {
		isFinished = Boolean.FALSE;
		searchWebAppHostname = hostName;
		searchWebAppPrefix = webAppPrefix;
		searchId = sessionId;
		searchAddressPort = ipPort;
		result = null;
		this.controlPort = controlPort;
		started = Boolean.FALSE;
	}

	public void start() {
		if (!started) {
			started = Boolean.TRUE;
			final Thread searchThread = new Thread(this);
			searchThread.setDaemon(Boolean.TRUE);
			searchThread.start();
		}
	}

	/**
	 * Actually implements the search
	 */
	@Override
	public void run() {
		try {
			final int colonPos = searchAddressPort.indexOf(':');
			final String ipAddress = searchAddressPort.substring(0, colonPos);
			final String port = searchAddressPort.substring(colonPos + 1);

			final Socket controlConnection = new Socket(ipAddress, Integer.parseInt(port));
			controlConnection.setSoTimeout(TIMEOUT);
			final OutputStream out = controlConnection.getOutputStream();
			out.write(ClusterSessionSearch.SESSION_CHECK_TYPE);
			out.flush();

			final ObjectOutputStream outControl = new ObjectOutputStream(out);
			outControl.writeInt(controlPort);
			outControl.writeUTF(searchId);
			outControl.writeUTF(searchWebAppHostname);
			outControl.writeUTF(searchWebAppPrefix);
			outControl.flush();
			final InputStream in = controlConnection.getInputStream();
			final ObjectInputStream inSession = new ObjectInputStream(in);
			final String reply = inSession.readUTF();
			if ((reply != null) && reply.equals(ClusterSessionSearch.SESSION_FOUND)) {
				final WinstoneSession session = (WinstoneSession) inSession.readObject();
				outControl.writeUTF(ClusterSessionSearch.SESSION_RECEIVED);
				result = session;
			}
			outControl.close();
			inSession.close();
			out.close();
			in.close();
			controlConnection.close();
		} catch (final Throwable err) {
			logger.warn("Error during cluster session search", err);
		}
		isFinished = Boolean.TRUE;
		started = Boolean.FALSE;
	}

	public boolean isFinished() {
		return isFinished;
	}

	public boolean isStarted() {
		return started;
	}

	public WinstoneSession getResult() {
		return result;
	}

	public void destroy() {
	}

	public String getAddressPort() {
		return searchAddressPort;
	}
}
