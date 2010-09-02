/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.winstone.cluster.Cluster;

import net.winstone.WinstoneException;
import org.slf4j.LoggerFactory;

/**
 * Manages the references to individual hosts within the container. This object handles the mapping of ip addresses and hostnames to groups
 * of webapps, and init and shutdown of any hosts it manages.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HostGroup.java,v 1.4 2006/03/24 17:24:21 rickknowles Exp $
 */
public class HostGroup {

    protected static org.slf4j.Logger logger = LoggerFactory.getLogger(HostGroup.class);
    private final static transient String DEFAULT_HOSTNAME = "default";
    /** map of host configuration */
    private final Map<String, HostConfiguration> hostConfigs;
    /** default host name if host mode is on */
    private String defaultHostName;

    /**
     * Build a new instance of HostGroup.
     * 
     * @param commonLibCL
     * @param commonLibCLPaths
     * @param args
     * @throws IOException
     */
    public HostGroup(final ClassLoader commonLibCL, final Map<String, String> args) throws IOException {
        this(null, null, commonLibCL, args);
    }

    public HostGroup(final Cluster cluster, final ObjectPool objectPool, final ClassLoader commonLibCL, final Map<String, String> args) throws IOException {
        super();
        this.hostConfigs = new HashMap<String, HostConfiguration>();

        // Is this the single or multiple configuration ? Check args
        String hostDirName = (String) args.get("hostsDir");
        String webappsDirName = (String) args.get("webappsDir");

        // If host mode
        if (hostDirName == null) {
            addHostConfiguration(webappsDirName, DEFAULT_HOSTNAME, cluster, objectPool, commonLibCL, args);
            this.defaultHostName = DEFAULT_HOSTNAME;
            logger.debug("Initialized in non-virtual-host mode");
        } // Otherwise multi-host mode
        else {
            initMultiHostDir(hostDirName, cluster, objectPool, commonLibCL, args);
            logger.debug("Initialized in virtual host mode with {} hosts: hostnames - {}", this.hostConfigs.size() + "", this.hostConfigs.keySet() + "");
        }
    }

    public HostConfiguration getHostByName(final String hostname) {
        HostConfiguration host = this.hostConfigs.get(hostname);
        return host != null ? host : this.hostConfigs.get(this.defaultHostName);
    }

    public void destroy() {
        if (this.hostConfigs != null) {
            // obtain a copy of name
            Set<String> hostnames = new HashSet<String>(this.hostConfigs.keySet());
            for (Iterator<String> i = hostnames.iterator(); i.hasNext();) {
                String hostname = i.next();
                hostConfigs.get(hostname).destroy();
                hostConfigs.remove(hostname);
            }
            this.hostConfigs.clear();
        }
    }

    /**
     * Initialize an host.
     * 
     * @param webappsDirName
     * @param hostname
     * @param cluster
     * @param objectPool
     * @param commonLibCL
     * @param commonLibCLPaths
     * @param args
     * @throws IOException
     */
    protected final void addHostConfiguration(String webappsDirName, String hostname, Cluster cluster, ObjectPool objectPool,
            ClassLoader commonLibCL, Map<String, String> args) throws IOException {
        logger.debug("Deploying host found at {}", hostname);
        HostConfiguration config = new HostConfiguration(hostname, cluster, objectPool, commonLibCL, args, webappsDirName);
        this.hostConfigs.put(hostname, config);
    }

    /**
     * Initialize a group host.
     * 
     * @param hostsDirName
     * @param cluster
     * @param objectPool
     * @param commonLibCL
     * @param commonLibCLPaths
     * @param args
     * @throws IOException
     */
    protected final void initMultiHostDir(String hostsDirName, Cluster cluster, ObjectPool objectPool, ClassLoader commonLibCL, Map<String, String> args) throws IOException {
        if (hostsDirName == null) {
            // never reach in this implementation
            hostsDirName = "hosts";
        }
        File hostsDir = new File(hostsDirName);
        if (!hostsDir.exists()) {
            throw new WinstoneException("Hosts dir " + hostsDirName + " not foundd");
        } else if (!hostsDir.isDirectory()) {
            throw new WinstoneException("Hosts dir " + hostsDirName + " is not a directory");
        } else {
            File children[] = hostsDir.listFiles();
            if ((children == null) || (children.length == 0)) {
                throw new WinstoneException("Hosts dir " + hostsDirName + " is empty (no child webapps found)");
            }
            for (int n = 0; n < children.length; n++) {
                String childName = children[n].getName();

                // Mount directories as host dirs
                if (children[n].isDirectory()) {
                    if (!this.hostConfigs.containsKey(childName)) {
                        addHostConfiguration(children[n].getCanonicalPath(), childName, cluster, objectPool, commonLibCL, args);
                    }
                }
                // set default host name
                if ((defaultHostName == null) || childName.equals(DEFAULT_HOSTNAME)) {
                    this.defaultHostName = childName;
                }
            }
        }
    }

    /**
     * Finalize threads.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            destroy();
        } catch (Throwable e) {
        }
        super.finalize();
    }
}
