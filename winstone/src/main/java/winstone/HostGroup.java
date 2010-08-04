/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.winstone.cluster.Cluster;

import net.winstone.WinstoneException;

/**
 * Manages the references to individual hosts within the container. This object handles the mapping of ip addresses and hostnames to groups
 * of webapps, and init and shutdown of any hosts it manages.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HostGroup.java,v 1.4 2006/03/24 17:24:21 rickknowles Exp $
 */
public class HostGroup {

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
    public HostGroup(ClassLoader commonLibCL, String jspClasspath, Map<String, String> args) throws IOException {
        this(null, null, commonLibCL, jspClasspath, args);
    }

    public HostGroup(Cluster cluster, ObjectPool objectPool, ClassLoader commonLibCL, String jspClasspath, Map<String, String> args) throws IOException {
        super();
        this.hostConfigs = new HashMap<String, HostConfiguration>();

        // Is this the single or multiple configuration ? Check args
        String hostDirName = (String) args.get("hostsDir");
        String webappsDirName = (String) args.get("webappsDir");

        // If host mode
        if (hostDirName == null) {
            addHostConfiguration(webappsDirName, DEFAULT_HOSTNAME, cluster, objectPool, commonLibCL, jspClasspath, args);
            this.defaultHostName = DEFAULT_HOSTNAME;
            Logger.log(Logger.DEBUG, Launcher.RESOURCES, "HostGroup.InitSingleComplete", new String[]{
                        this.hostConfigs.size() + "", this.hostConfigs.keySet() + ""
                    });
        } // Otherwise multi-host mode
        else {
            initMultiHostDir(hostDirName, cluster, objectPool, commonLibCL, jspClasspath, args);
            Logger.log(Logger.DEBUG, Launcher.RESOURCES, "HostGroup.InitMultiComplete", new String[]{
                        this.hostConfigs.size() + "", this.hostConfigs.keySet() + ""
                    });
        }
    }

    public HostConfiguration getHostByName(String hostname) {
        HostConfiguration host = (HostConfiguration) this.hostConfigs.get(hostname);
        return host != null ? host : this.hostConfigs.get(this.defaultHostName);
    }

    public void destroy() {
        Set<String> hostnames = new HashSet<String>(this.hostConfigs.keySet());
        for (Iterator<String> i = hostnames.iterator(); i.hasNext();) {
            String hostname = i.next();
            HostConfiguration host = (HostConfiguration) this.hostConfigs.get(hostname);
            host.destroy();
            this.hostConfigs.remove(hostname);
        }
        this.hostConfigs.clear();
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
            ClassLoader commonLibCL, String jspClasspath, Map<String, String> args) throws IOException {
        Logger.log(Logger.DEBUG, Launcher.RESOURCES, "HostGroup.DeployingHost", hostname);
        HostConfiguration config = new HostConfiguration(hostname, cluster, objectPool, commonLibCL, jspClasspath, args, webappsDirName);
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
    protected final void initMultiHostDir(String hostsDirName, Cluster cluster, ObjectPool objectPool, ClassLoader commonLibCL, String jspClasspath, Map<String, String> args) throws IOException {
        if (hostsDirName == null) {
            // never reach in this implementation
            hostsDirName = "hosts";
        }
        File hostsDir = new File(hostsDirName);
        if (!hostsDir.exists()) {
            throw new WinstoneException(Launcher.RESOURCES.getString("HostGroup.HostsDirNotFound", hostsDirName));
        } else if (!hostsDir.isDirectory()) {
            throw new WinstoneException(Launcher.RESOURCES.getString("HostGroup.HostsDirIsNotDirectory", hostsDirName));
        } else {
            File children[] = hostsDir.listFiles();
            if ((children == null) || (children.length == 0)) {
                throw new WinstoneException(Launcher.RESOURCES.getString("HostGroup.HostsDirIsEmpty", hostsDirName));
            }
            for (int n = 0; n < children.length; n++) {
                String childName = children[n].getName();

                // Mount directories as host dirs
                if (children[n].isDirectory()) {
                    if (!this.hostConfigs.containsKey(childName)) {
                        addHostConfiguration(children[n].getCanonicalPath(), childName, cluster, objectPool, commonLibCL, jspClasspath, args);
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
