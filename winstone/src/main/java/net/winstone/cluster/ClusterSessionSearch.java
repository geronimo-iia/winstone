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
import net.winstone.log.Logger;
import net.winstone.log.LoggerFactory;

import winstone.WinstoneSession;

/**
 * Contains all the logic for reading in sessions
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ClusterSessionSearch.java,v 1.6 2006/03/24 17:24:18 rickknowles Exp $
 */
public class ClusterSessionSearch implements Runnable {

    protected Logger logger = LoggerFactory.getLogger(getClass());
    final int TIMEOUT = 2000;
    public static final byte SESSION_CHECK_TYPE = (byte) '1';
    public static final String SESSION_NOT_FOUND = "NOTFOUND";
    public static final String SESSION_FOUND = "FOUND";
    public static final String SESSION_RECEIVED = "OK";
    private boolean started;
    private boolean isFinished;
    private WinstoneSession result;
    private String searchWebAppHostname;
    private String searchWebAppPrefix;
    private String searchId;
    private String searchAddressPort;
    private int controlPort;

    /**
     * Sets up for a threaded search
     */
    public ClusterSessionSearch(String webAppPrefix, String hostName, String sessionId, String ipPort, int controlPort) {
        this.isFinished = false;
        this.searchWebAppHostname = hostName;
        this.searchWebAppPrefix = webAppPrefix;
        this.searchId = sessionId;
        this.searchAddressPort = ipPort;
        this.result = null;
        this.controlPort = controlPort;
        started = false;
    }

    public void start() {
        if (!started) {
            started = true;
            Thread searchThread = new Thread(this);
            searchThread.setDaemon(true);
            searchThread.start();
        }
    }

    /**
     * Actually implements the search
     */
    @Override
    public void run() {
        try {
            int colonPos = this.searchAddressPort.indexOf(':');
            String ipAddress = this.searchAddressPort.substring(0, colonPos);
            String port = this.searchAddressPort.substring(colonPos + 1);

            Socket controlConnection = new Socket(ipAddress, Integer.parseInt(port));
            controlConnection.setSoTimeout(TIMEOUT);
            OutputStream out = controlConnection.getOutputStream();
            out.write(SESSION_CHECK_TYPE);
            out.flush();

            ObjectOutputStream outControl = new ObjectOutputStream(out);
            outControl.writeInt(this.controlPort);
            outControl.writeUTF(this.searchId);
            outControl.writeUTF(this.searchWebAppHostname);
            outControl.writeUTF(this.searchWebAppPrefix);
            outControl.flush();
            InputStream in = controlConnection.getInputStream();
            ObjectInputStream inSession = new ObjectInputStream(in);
            String reply = inSession.readUTF();
            if ((reply != null) && reply.equals(SESSION_FOUND)) {
                WinstoneSession session = (WinstoneSession) inSession.readObject();
                outControl.writeUTF(SESSION_RECEIVED);
                this.result = session;
            }
            outControl.close();
            inSession.close();
            out.close();
            in.close();
            controlConnection.close();
        } catch (Throwable err) {
            logger.warn("Error during cluster session search", err);
        }
        this.isFinished = true;
        started = false;
    }

    public boolean isFinished() {
        return this.isFinished;
    }

    public boolean isStarted() {
        return started;
    }

    public WinstoneSession getResult() {
        return this.result;
    }

    public void destroy() { 
    }

    public String getAddressPort() {
        return this.searchAddressPort;
    }
}
