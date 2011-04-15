package net.winstone.boot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.winstone.WinstoneResourceBundle;
import net.winstone.util.FileUtils;
import net.winstone.util.StringUtils;
import org.slf4j.LoggerFactory;

/**
 * Bootloader prepare load of server.
 * 
 * @author Jerome Guibert
 */
public class BootStrap {

    protected static org.slf4j.Logger logger = LoggerFactory.getLogger(BootStrap.class);
    // embedded war file
    private static final String EMBEDDED_WAR = "/embedded.war";
    // inner folder
    private static final String WS_EMBEDDED_WAR = "winstoneEmbeddedWebroot";
    // embedded propeties file
    private static final String EMBEDDED_PROPERTIES = "/embedded.properties";
    // winstone prooperties files
    private static final String WINSTONE_PROPERTIES = "winstone.properties";
    // arguments from command line
    private final String[] argv;
    // arguments from other java code
    private final Map<String, String> arguments;

    /**
     * Build a new instance of BootStrap with command line argument.
     * @param argv arguments
     */
    public BootStrap(final String[] argv) {
        super();
        this.argv = argv == null ? new String[0] : argv;
        arguments = null;
    }

    /**
     * Build a new instance of BootStrap with the specified map of argument
     * @param arguments
     */
    public BootStrap(final Map<String, String> arguments) {
        super();
        this.argv = null;
        this.arguments = arguments;
    }

    public Server boot() {
        Server server = null;
        /**  Load argument for server initialization.*/
        logger.info("stage 1/3: Loading arguments...");
        Map<String, String> args = arguments;
        if (args == null) {
            // load from command line
            args = loadArgs("nonSwitch");
            // Small hack to allow re-use of the command line parsing inside the control tool
            String firstNonSwitchArgument = (String) args.get("nonSwitch");
            args.remove("nonSwitch");
            // Check if the non-switch arg is a file or folder, and overwrite the config
            if (firstNonSwitchArgument != null) {
                File webapp = new File(firstNonSwitchArgument);
                if (webapp.exists()) {
                    if (webapp.isDirectory()) {
                        args.put("webroot", firstNonSwitchArgument);
                    } else if (webapp.isFile()) {
                        args.put("warfile", firstNonSwitchArgument);
                    }
                }
            }
        }
        /** is usage request ? */
        if (args.containsKey("usage") || args.containsKey("help")) {
            printUsage();
            return server;
        }
        /** check embeded */
        logger.info("stage 2/3: check embedded file...");
        if (!deployEmbeddedWarfile(args)) {
            logger.info("no embedded file was found");
        }
        /** check parameter validity */
        if (!args.containsKey("webroot") && !args.containsKey("warfile") && !args.containsKey("webappsDir") && !args.containsKey("hostsDir")) {
            printUsage();
            return server;
        }
        /** compute lib path */
        logger.info("stage 3/3: compute JSP classpath...");
        computeJSPClassPath(args);
        server = new Server(args, getClass().getClassLoader());
        return server;
    }

    /**
     * Compute jsp class path value.
     * @param args
     * @throws IOException
     */
    protected void computeJSPClassPath(final Map<String, String> args) {
        try {
            List<File> jspClasspaths = new ArrayList<File>();

            // Check for java home
            String javaHome = StringUtils.stringArg(args, "javaHome", System.getProperty("java.home"));
            logger.debug("Using JAVA_HOME={}", javaHome);
            String toolsJarLocation = StringUtils.stringArg(args, "toolsJar", null);
            File toolsJar = null;
            if (toolsJarLocation == null) {
                toolsJar = new File(javaHome, "lib/tools.jar");

                // first try - if it doesn't exist, try up one dir since we might have
                // the JRE home by mistake
                if (!toolsJar.exists()) {
                    File javaHome2 = new File(javaHome).getParentFile();
                    File toolsJar2 = new File(javaHome2, "lib/tools.jar");
                    if (toolsJar2.exists()) {
                        javaHome = javaHome2.getCanonicalPath();
                        toolsJar = toolsJar2;
                    }
                }
            } else {
                toolsJar = new File(toolsJarLocation);
            }

            // Add tools jar to classloader path
            if (toolsJar.exists()) { 
                jspClasspaths.add(toolsJar);
                logger.debug("Adding {} to common classpath", toolsJar.getName());
            } else if (StringUtils.booleanArg(args, "useJasper", false)) {
                logger.warn("WARNING: Tools.jar was not found - jsp compilation will cause errors. Maybe you should set JAVA_HOME using --javaHome");
            }

            // Set up common lib class loader
            String commonLibCLFolder = StringUtils.stringArg(args, "commonLibFolder", "lib");
            File libFolder = new File(commonLibCLFolder);
            if (libFolder.exists() && libFolder.isDirectory()) {
                logger.debug("Using common lib folder: {}", libFolder.getCanonicalPath());
                File children[] = libFolder.listFiles();
                for (int n = 0; n < children.length; n++) {
                    if (children[n].getName().endsWith(".jar") || children[n].getName().endsWith(".zip")) {
                        jspClasspaths.add(children[n]);
                        logger.debug("Adding {} to common classpath", children[n].getName());
                    }
                }
            } else {
                logger.debug("No common lib folder found");
            }


            logger.debug("Initializing JSP Common Lib classloader: {}", jspClasspaths.toString());
            /** calcule de m'attribut pour les jsp */
            StringBuilder cp = new StringBuilder();
            File[] fa = (File[]) jspClasspaths.toArray(new File[0]);
            for (int n = 0; n < fa.length; n++) {
                cp.append(fa[n].getCanonicalPath()).append(File.pathSeparatorChar);
            }
            String jspClasspath = cp.length() > 0 ? cp.substring(0, cp.length() - 1) : "";
            if (!args.containsKey("jspClasspath")) {
                args.put("jspClasspath", jspClasspath);
            }

        } catch (IOException ex) {
            logger.error("computeClassPath", ex);
        } 
    }

    /**
     * Load argument from command line and config file.
     * This method is used by winstone control tool. May we should extract argument management in specific class ?
     * @param nonSwitchArgName key for non switch command line argument
     */
    public Map<String, String> loadArgs(final String nonSwitchArgName) {
        Map<String, String> args = new HashMap<String, String>();
        // Load embedded properties file
        String embeddedPropertiesFilename = EMBEDDED_PROPERTIES;
        InputStream embeddedPropsStream = BootStrap.class.getResourceAsStream(embeddedPropertiesFilename);
        if (embeddedPropsStream != null) {
            try {
                loadPropsFromStream(embeddedPropsStream, args);
                embeddedPropsStream.close();
            } catch (IOException ex) {
                logger.error("loadArgs", ex);
            }
        }

        // Get command line args
        String configFilename = WINSTONE_PROPERTIES;
        for (int n = 0; n < argv.length; n++) {
            String option = argv[n];
            if (option.startsWith("--")) {
                int equalPos = option.indexOf('=');
                String paramName = option.substring(2, equalPos == -1 ? option.length() : equalPos);
                if (equalPos != -1) {
                    args.put(paramName, option.substring(equalPos + 1));
                } else {
                    args.put(paramName, "true");
                }
                if (paramName.equals("config")) {
                    configFilename = (String) args.get(paramName);
                }
            } else {
                args.put(nonSwitchArgName, option);
            }
        }

        // Load default props if available
        File configFile = new File(configFilename);
        if (configFile.exists() && configFile.isFile()) {
            logger.debug("Property file found ({}) - loading", configFilename);
            try {
                InputStream inConfig = new FileInputStream(configFile);
                loadPropsFromStream(inConfig, args);
                inConfig.close();
            } catch (IOException ex) {
                logger.error("loadArgs", ex);
            }
        }

        return args;
    }

    /**
     * Load propeties from specified streamand add them if the key is not ever in args.
     * @param inConfig stream to load
     * @param args arguments
     * @throws IOException
     */
    protected void loadPropsFromStream(final InputStream inConfig, Map<String, String> args) throws IOException {
        Properties props = new Properties();
        props.load(inConfig);
        for (Iterator<Object> i = props.keySet().iterator(); i.hasNext();) {
            String key = ((String) i.next()).trim();
            if (!args.containsKey(key)) {
                args.put(key, props.getProperty(key));
            }
        }
        props.clear();
    }

    /**
     * Test and deploy embedded war file if exixts.
     * @param args
     * @return true if one is found, false otherwise. 
     */
    protected boolean deployEmbeddedWarfile(final Map<String, String> args) {
        boolean result = false;
        InputStream embeddedWarfile = BootStrap.class.getResourceAsStream(EMBEDDED_WAR);
        if (embeddedWarfile != null) {
            try {
                // find temp directory
                File tempWarfile = File.createTempFile("embedded", ".war").getAbsoluteFile();
                tempWarfile.getParentFile().mkdirs();
                tempWarfile.deleteOnExit();
                // create temp web root
                File tempWebroot = new File(tempWarfile.getParentFile(), WS_EMBEDDED_WAR);
                tempWebroot.mkdirs();
                logger.debug("Extracting embedded warfile to {}", tempWarfile.getAbsolutePath());
                OutputStream out = new FileOutputStream(tempWarfile, true);
                int read = 0;
                byte buffer[] = new byte[2048];
                while ((read = embeddedWarfile.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.close();
                embeddedWarfile.close();
                // update argument
                args.put("warfile", tempWarfile.getAbsolutePath());
                args.put("webroot", tempWebroot.getAbsolutePath());
                args.remove("webappsDir");
                args.remove("hostsDir");
                result = true;
            } catch (IOException e) {
                logger.error("deployEmbeddedWarfile", e);
            }
        }
        return result;
    }

    /** print usage */
    protected static void printUsage() {
        System.out.println(WinstoneResourceBundle.getInstance().getString("UsageInstructions", WinstoneResourceBundle.getInstance().getString("ServerVersion")));
    }
}
