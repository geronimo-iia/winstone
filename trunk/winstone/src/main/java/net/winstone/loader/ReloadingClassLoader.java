/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.loader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.winstone.util.StringUtils;
import net.winstone.core.WebAppConfiguration;

/**
 * This subclass of WinstoneClassLoader is the reloading version. It runs a monitoring thread in the background that checks for updates to
 * any files in the class path.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ReloadingClassLoader.java,v 1.11 2007/02/17 01:55:12 rickknowles Exp $
 */
public class ReloadingClassLoader extends WebappClassLoader implements ServletContextListener, Runnable {

    protected Logger logger = LoggerFactory.getLogger(getClass());
    private static final int RELOAD_SEARCH_SLEEP = 10;
    private boolean interrupted;
    private WebAppConfiguration webAppConfig;
    private final Set<String> loadedClasses;
    private File classPaths[];
    private int classPathsLength;

    public ReloadingClassLoader(final URL urls[], final ClassLoader parent) {
        super(urls, parent);
        this.loadedClasses = new HashSet<String>();
        if (urls != null) {
            this.classPaths = new File[urls.length];
            for (int n = 0; n < urls.length; n++) {
                this.classPaths[this.classPathsLength++] = new File(urls[n].getFile());
            }
        }
    }

    @Override
    protected void addURL(final URL url) {
        super.addURL(url);
        synchronized (this.loadedClasses) {
            if (this.classPaths == null) {
                this.classPaths = new File[10];
                this.classPathsLength = 0;
            } else if (this.classPathsLength == (this.classPaths.length - 1)) {
                File temp[] = this.classPaths;
                this.classPaths = new File[(int) (this.classPathsLength * 1.75)];
                System.arraycopy(temp, 0, this.classPaths, 0, this.classPathsLength);
            }
            this.classPaths[this.classPathsLength++] = new File(url.getFile());
        }
    }

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        this.webAppConfig = (WebAppConfiguration) sce.getServletContext();
        this.interrupted = false;
        synchronized (this) {
            this.loadedClasses.clear();
        }
        Thread thread = new Thread(this, "WinstoneClassLoader Reloading Monitor Thread");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        this.interrupted = true;
        this.webAppConfig = null;
        synchronized (this) {
            this.loadedClasses.clear();
        }
    }

    /**
     * The maintenance thread. This makes sure that any changes in the files in the classpath trigger a classLoader self destruct and
     * recreate.
     */
    @Override
    public void run() {
        logger.info("WinstoneClassLoader reloading monitor thread started");

        Map<String, Long> classDateTable = new HashMap<String, Long>();
        Map<String, File> classLocationTable = new HashMap<String, File>();
        Set<String> lostClasses = new HashSet<String>();
        while (!interrupted) {
            try {
                String loadedClassesCopy[] = null;
                synchronized (this) {
                    loadedClassesCopy = (String[]) this.loadedClasses.toArray(new String[0]);
                }

                for (int n = 0; (n < loadedClassesCopy.length) && !interrupted; n++) {
                    Thread.sleep(RELOAD_SEARCH_SLEEP);
                    String className = transformToFileFormat(loadedClassesCopy[n]);
                    File location = (File) classLocationTable.get(className);
                    Long classDate = null;
                    if ((location == null) || !location.exists()) {
                        for (int j = 0; (j < this.classPaths.length) && (classDate == null); j++) {
                            File path = this.classPaths[j];
                            if (!path.exists()) {
                                continue;
                            } else if (path.isDirectory()) {
                                File classLocation = new File(path, className);
                                if (classLocation.exists()) {
                                    classDate = new Long(classLocation.lastModified());
                                    classLocationTable.put(className, classLocation);
                                }
                            } else if (path.isFile()) {
                                classDate = searchJarPath(className, path);
                                if (classDate != null) {
                                    classLocationTable.put(className, path);
                                }
                            }
                        }
                    } else if (location.exists()) {
                        classDate = new Long(location.lastModified());
                    }

                    // Has class vanished ? Leave a note and skip over it
                    if (classDate == null) {
                        if (!lostClasses.contains(className)) {
                            lostClasses.add(className);
                            logger.debug("WARNING: Maintenance thread can't find class {} - Lost ? Ignoring", className);
                        }
                        continue;
                    }
                    if ((classDate != null) && lostClasses.contains(className)) {
                        lostClasses.remove(className);
                    }

                    // Stash date of loaded files, and compare with last
                    // iteration
                    Long oldClassDate = (Long) classDateTable.get(className);
                    if (oldClassDate == null) {
                        classDateTable.put(className, classDate);
                    } else if (oldClassDate.compareTo(classDate) != 0) {
                        // Trigger reset of webAppConfig
                        logger.info("Class {} changed at {} (old date {}) - reloading",
                                new Object[]{
                                    className, new Date(classDate.longValue()).toString(), new Date(oldClassDate.longValue()).toString()});
                        this.webAppConfig.resetClassLoader();
                    }
                }
            } catch (Throwable err) {
                logger.error("Error in WinstoneClassLoader reloading monitor thread", err);
            }
        }
        logger.info("WinstoneClassLoader reloading monitor thread finished");
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        synchronized (this) {
            this.loadedClasses.add("Class:" + name);
        }
        return super.findClass(name);
    }

    @Override
    public URL findResource(final String name) {
        synchronized (this) {
            this.loadedClasses.add(name);
        }
        return super.findResource(name);
    }

    /**
     * Iterates through a jar file searching for a class. If found, it returns that classes date
     */
    private Long searchJarPath(final String classResourceName, final File path) throws IOException, InterruptedException {
        JarFile jar = new JarFile(path);
        for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements() && !interrupted;) {
            JarEntry entry = (JarEntry) e.nextElement();
            if (entry.getName().equals(classResourceName)) {
                return new Long(path.lastModified());
            }
        }
        return null;
    }

    private static String transformToFileFormat(final String name) {
        if (!name.startsWith("Class:")) {
            return name;
        }
        return StringUtils.replace(name.substring(6), ".", "/") + ".class";
    }
}
