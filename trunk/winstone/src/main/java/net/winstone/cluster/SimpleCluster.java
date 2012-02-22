/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.cluster;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import net.winstone.core.HostConfiguration;
import net.winstone.core.HostGroup;
import net.winstone.core.WebAppConfiguration;
import net.winstone.core.WinstoneSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a cluster of winstone containers.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: SimpleCluster.java,v 1.8 2006/08/10 06:38:31 rickknowles Exp $
 */
public final class SimpleCluster implements Runnable, Cluster {

	protected Logger logger = LoggerFactory.getLogger(getClass());
	final int SESSION_CHECK_TIMEOUT = 100;
	final int HEARTBEAT_PERIOD = 5000;
	final int MAX_NO_OF_MISSING_HEARTBEATS = 3;
	final byte NODELIST_DOWNLOAD_TYPE = (byte) '2';
	final byte NODE_HEARTBEAT_TYPE = (byte) '3';
	private int controlPort;
	private final String initialClusterNodes;
	private final Map<String, Date> clusterAddresses;
	private boolean interrupted;

	/**
	 * Builds a cluster instance
	 */
	public SimpleCluster(final Map<String, String> args, final Integer controlPort) {
		interrupted = false;
		clusterAddresses = new HashMap<String, Date>();
		if (controlPort != null) {
			this.controlPort = controlPort.intValue();
		}

		// Start cluster init thread
		initialClusterNodes = args.get("clusterNodes");
		final Thread thread = new Thread(this, "Cluster monitor thread");
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

	@Override
	public void destroy() {
		interrupted = true;
	}

	/**
	 * Send a heartbeat every now and then, and remove any nodes that haven't
	 * responded in 3 heartbeats.
	 */
	@Override
	public void run() {
		// Ask each of the known addresses for their cluster lists, and build a
		// set
		if (initialClusterNodes != null) {
			final StringTokenizer st = new StringTokenizer(initialClusterNodes, ",");
			while (st.hasMoreTokens() && !interrupted) {
				askClusterNodeForNodeList(st.nextToken());
			}
		}
		logger.info("Cluster initialised with {} nodes", Integer.toString(clusterAddresses.size()));

		while (!interrupted) {
			try {
				final Set<String> addresses = new HashSet<String>(clusterAddresses.keySet());
				final Date noHeartbeatDate = new Date(System.currentTimeMillis() - (MAX_NO_OF_MISSING_HEARTBEATS * HEARTBEAT_PERIOD));
				for (final Iterator<String> i = addresses.iterator(); i.hasNext();) {
					final String ipPort = i.next();

					final Date lastHeartBeat = clusterAddresses.get(ipPort);
					if (lastHeartBeat.before(noHeartbeatDate)) {
						clusterAddresses.remove(ipPort);
						logger.debug("Removing address from cluster node list: {}", ipPort);
					} // Send heartbeat
					else {
						sendHeartbeat(ipPort);
					}

				}
				Thread.sleep(HEARTBEAT_PERIOD);
			} catch (final Throwable err) {
				logger.error("Error in cluster monitor thread", err);
			}
		}
		logger.info("Cluster monitor thread finished");
	}

	/**
	 * Check if the other nodes in this cluster have a session for this
	 * sessionId.
	 * 
	 * @param sessionId
	 *            The id of the session to check for
	 * @return A valid session instance
	 */
	@Override
	public WinstoneSession askClusterForSession(final String sessionId, final WebAppConfiguration webAppConfig) {
		// Iterate through the cluster members
		final Collection<String> addresses = new ArrayList<String>(clusterAddresses.keySet());
		final Collection<ClusterSessionSearch> searchThreads = new ArrayList<ClusterSessionSearch>();
		for (final Iterator<String> i = addresses.iterator(); i.hasNext();) {
			final String ipPort = i.next();
			final ClusterSessionSearch search = new ClusterSessionSearch(webAppConfig.getContextPath(), webAppConfig.getOwnerHostname(), sessionId, ipPort, controlPort);
			search.start();
			searchThreads.add(search);
		}

		// Wait until we get an answer
		WinstoneSession answer = null;
		String senderThread = null;
		boolean finished = false;
		while (!finished) {
			// Loop through all search threads. If finished, exit, otherwise
			// sleep
			final List<ClusterSessionSearch> finishedThreads = new ArrayList<ClusterSessionSearch>();
			for (final Iterator<ClusterSessionSearch> i = searchThreads.iterator(); i.hasNext();) {
				final ClusterSessionSearch searchThread = i.next();
				if (!searchThread.isFinished()) {
					continue;
				} else if (searchThread.getResult() == null) {
					finishedThreads.add(searchThread);
				} else {
					answer = searchThread.getResult();
					senderThread = searchThread.getAddressPort();
				}
			}

			// Remove finished threads
			for (final Iterator<ClusterSessionSearch> i = finishedThreads.iterator(); i.hasNext();) {
				searchThreads.remove(i.next());
			}

			if (searchThreads.isEmpty() || (answer != null)) {
				finished = true;
			} else {
				try {
					Thread.sleep(100);
				} catch (final InterruptedException err) {
				}
			}
		}

		// Once we have an answer, terminate all search threads
		for (final Iterator<ClusterSessionSearch> i = searchThreads.iterator(); i.hasNext();) {
			final ClusterSessionSearch searchThread = i.next();
			searchThread.destroy();
		}
		if (answer != null) {
			answer.activate(webAppConfig);
			logger.debug("Session transferred from: {}", senderThread);
		}
		return answer;
	}

	/**
	 * Given an address, retrieve the list of cluster nodes and initialise dates
	 * 
	 * @param address
	 *            The address to request a node list from
	 */
	private void askClusterNodeForNodeList(final String address) {
		try {
			final int colonPos = address.indexOf(':');
			final String ipAddress = address.substring(0, colonPos);
			final String port = address.substring(colonPos + 1);
			final Socket clusterListSocket = new Socket(ipAddress, Integer.parseInt(port));
			clusterAddresses.put(clusterListSocket.getInetAddress().getHostAddress() + ":" + port, new Date());
			final InputStream in = clusterListSocket.getInputStream();
			final OutputStream out = clusterListSocket.getOutputStream();
			out.write(NODELIST_DOWNLOAD_TYPE);
			out.flush();

			// Write out the control port
			final ObjectOutputStream outControl = new ObjectOutputStream(out);
			outControl.writeInt(controlPort);
			outControl.flush();

			// For each node, add an entry to cluster nodes
			final ObjectInputStream inData = new ObjectInputStream(in);
			final int nodeCount = inData.readInt();
			for (int n = 0; n < nodeCount; n++) {
				clusterAddresses.put(inData.readUTF(), new Date());
			}

			inData.close();
			outControl.close();
			out.close();
			in.close();
			clusterListSocket.close();
		} catch (final ConnectException err) {
			logger.debug("No cluster node detected at {} - ignoring", address);
		} catch (final Throwable err) {
			logger.error("Error getting nodelist from: " + address, err);
		}
	}

	/**
	 * Given an address, send a heartbeat
	 * 
	 * @param address
	 *            The address to request a node list from
	 */
	private void sendHeartbeat(final String address) {
		try {
			final int colonPos = address.indexOf(':');
			final String ipAddress = address.substring(0, colonPos);
			final String port = address.substring(colonPos + 1);
			final Socket heartbeatSocket = new Socket(ipAddress, Integer.parseInt(port));
			final OutputStream out = heartbeatSocket.getOutputStream();
			out.write(NODE_HEARTBEAT_TYPE);
			out.flush();
			final ObjectOutputStream outData = new ObjectOutputStream(out);
			outData.writeInt(controlPort);
			outData.close();
			heartbeatSocket.close();
			logger.debug("Heartbeat sent to: {}", address);
		} catch (final ConnectException err) {/* ignore - 3 fails, and we remove */

		} catch (final Throwable err) {
			logger.error("Error sending heartbeat to: " + address, err);
		}
	}

	/**
	 * Accept a control socket request related to the cluster functions and
	 * process the request.
	 * 
	 * @param requestType
	 *            A byte indicating the request type
	 * @param in
	 *            Socket input stream
	 * @param outSocket
	 *            output stream
	 * @param webAppConfig
	 *            Instance of the web app
	 * @throws IOException
	 */
	@Override
	public void clusterRequest(final byte requestType, final InputStream in, final OutputStream out, final Socket socket, final HostGroup hostGroup) throws IOException {
		if (requestType == ClusterSessionSearch.SESSION_CHECK_TYPE) {
			handleClusterSessionRequest(socket, in, out, hostGroup);
		} else if (requestType == NODELIST_DOWNLOAD_TYPE) {
			handleNodeListDownloadRequest(socket, in, out);
		} else if (requestType == NODE_HEARTBEAT_TYPE) {
			handleNodeHeartBeatRequest(socket, in);
		} else {
			logger.error("Unknown cluster request type: " + ((char) requestType));
		}
	}

	/**
	 * Handles incoming socket requests for session search
	 */
	public void handleClusterSessionRequest(final Socket socket, final InputStream in, final OutputStream out, final HostGroup hostGroup) throws IOException {
		// Read in a string for the sessionId
		final ObjectInputStream inControl = new ObjectInputStream(in);
		final int port = inControl.readInt();
		final String ipPortSender = socket.getInetAddress().getHostAddress() + ":" + port;
		final String sessionId = inControl.readUTF();
		final String hostname = inControl.readUTF();
		final HostConfiguration hostConfig = hostGroup.getHostByName(hostname);
		final String webAppPrefix = inControl.readUTF();
		final WebAppConfiguration webAppConfig = hostConfig.getWebAppByURI(webAppPrefix);
		final ObjectOutputStream outData = new ObjectOutputStream(out);
		if (webAppConfig == null) {
			outData.writeUTF(ClusterSessionSearch.SESSION_NOT_FOUND);
		} else {
			final WinstoneSession session = webAppConfig.getSessionById(sessionId, true);
			if (session != null) {
				outData.writeUTF(ClusterSessionSearch.SESSION_FOUND);
				outData.writeObject(session);
				outData.flush();
				if (inControl.readUTF().equals(ClusterSessionSearch.SESSION_RECEIVED)) {
					session.passivate();
				}
				logger.debug("Session transferred to: {}", ipPortSender);
			} else {
				outData.writeUTF(ClusterSessionSearch.SESSION_NOT_FOUND);
			}
		}
		outData.close();
		inControl.close();
	}

	/**
	 * Handles incoming socket requests for cluster node lists.
	 */
	public void handleNodeListDownloadRequest(final Socket socket, final InputStream in, final OutputStream out) throws IOException {
		// Get the ip and port of the requester, and make sure we don't send
		// that
		final ObjectInputStream inControl = new ObjectInputStream(in);
		final int port = inControl.readInt();
		final String ipPortSender = socket.getInetAddress().getHostAddress() + ":" + port;
		final List<String> allClusterNodes = new ArrayList<String>(clusterAddresses.keySet());
		final List<String> relevantClusterNodes = new ArrayList<String>();
		for (final Iterator<String> i = allClusterNodes.iterator(); i.hasNext();) {
			final String node = i.next();
			if (!node.equals(ipPortSender)) {
				relevantClusterNodes.add(node);
			}
		}

		final ObjectOutputStream outData = new ObjectOutputStream(out);
		outData.writeInt(relevantClusterNodes.size());
		outData.flush();
		for (final Iterator<String> i = relevantClusterNodes.iterator(); i.hasNext();) {
			final String ipPort = i.next();
			if (!ipPort.equals(ipPortSender)) {
				outData.writeUTF(ipPort);
			}
			outData.flush();
		}
		outData.close();
		inControl.close();
	}

	/**
	 * Handles heartbeats. Just updates the date of this node's last heartbeat
	 */
	public void handleNodeHeartBeatRequest(final Socket socket, final InputStream in) throws IOException {
		final ObjectInputStream inData = new ObjectInputStream(in);
		final int remoteControlPort = inData.readInt();
		inData.close();
		final String ipPort = socket.getInetAddress().getHostAddress() + ":" + remoteControlPort;
		clusterAddresses.put(ipPort, new Date());
		logger.debug("Heartbeat received from: {}", ipPort);
	}
}
