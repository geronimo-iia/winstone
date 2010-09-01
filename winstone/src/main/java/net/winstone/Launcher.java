package net.winstone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import net.winstone.boot.Server;
import org.slf4j.LoggerFactory;

/**
 * This is the class that gets launched by the command line.
 * Load argument from command line, process them and try to launch server.
 *
 * @author Jerome Guibert
 */
public class Launcher {

    public static final byte SHUTDOWN_TYPE = (byte) '0';
    public static final byte RELOAD_TYPE = (byte) '4';
    private static final String EMBEDDED_WAR = "/embedded.war";
    private static final String WS_EMBEDDED_WAR = "winstoneEmbeddedWebroot";
    private static final String EMBEDDED_PROPERTIES = "/embedded.properties";
    private static final String WINSTONE_PROPERTIES = "winstone.properties";
    protected static org.slf4j.Logger logger = LoggerFactory.getLogger(Launcher.class);
    // server attribut member
    private Server server;

    public Launcher() {
        super();
        server = null;
    }

    /**
     * Main method. This basically just accepts a few args, then initialises the listener thread. For now, just shut it down with a
     * control-C.
     */
    public static void main(String argv[]) throws IOException {
        Launcher launcher = new Launcher();
        launcher.launch(argv);
    }

    public Launcher(final Map<String, String> args) throws IOException {
        this();
        launch(args);
    }

    public final void launch(String argv[]) throws IOException {
        launch(getArgsFromCommandLine(argv));
    }

    public final void launch(Map<String, String> args) throws IOException {


        if (args.containsKey("usage") || args.containsKey("help")) {
            printUsage();
            return;
        }

        // Check for embedded war
        deployEmbeddedWarfile(args);

        // Check for embedded warfile
        if (!args.containsKey("webroot") && !args.containsKey("warfile") && !args.containsKey("webappsDir") && !args.containsKey("hostsDir")) {
            printUsage();
            return;
        }
        // Launch
        try {
            server = new Server(args);
            server.start();
            server.shutdown();
            server = null;
        } catch (Throwable err) {
            System.err.println("Container startup failed");
            err.printStackTrace(System.err);
        }
    }

    public void shutdown() {
        if (server != null) {
            server.shutdown();
            server = null;
        }
    }

    protected static void deployEmbeddedWarfile(Map<String, String> args) throws IOException {
        String embeddedWarfileName = EMBEDDED_WAR;
        InputStream embeddedWarfile = Launcher.class.getResourceAsStream(embeddedWarfileName);
        if (embeddedWarfile != null) {
            File tempWarfile = File.createTempFile("embedded", ".war").getAbsoluteFile();
            tempWarfile.getParentFile().mkdirs();
            tempWarfile.deleteOnExit();

            String embeddedWebroot = WS_EMBEDDED_WAR;
            File tempWebroot = new File(tempWarfile.getParentFile(), embeddedWebroot);
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

            args.put("warfile", tempWarfile.getAbsolutePath());
            args.put("webroot", tempWebroot.getAbsolutePath());
            args.remove("webappsDir");
            args.remove("hostsDir");
        }
    }

    protected static Map<String, String> getArgsFromCommandLine(String argv[]) throws IOException {
        Map<String, String> args = loadArgsFromCommandLineAndConfig(argv, "nonSwitch");

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
        return args;
    }

    public static Map<String, String> loadArgsFromCommandLineAndConfig(String argv[], String nonSwitchArgName) throws IOException {
        Map<String, String> args = new HashMap<String, String>();

        // Load embedded properties file
        String embeddedPropertiesFilename = EMBEDDED_PROPERTIES;

        InputStream embeddedPropsStream = Launcher.class.getResourceAsStream(embeddedPropertiesFilename);
        if (embeddedPropsStream != null) {
            loadPropsFromStream(embeddedPropsStream, args);
            embeddedPropsStream.close();
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
            InputStream inConfig = new FileInputStream(configFile);
            loadPropsFromStream(inConfig, args);
            inConfig.close();
            logger.debug("Property file found ({}) - loading", configFilename);
        }
        return args;
    }

    protected static void loadPropsFromStream(InputStream inConfig, Map<String, String> args) throws IOException {
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

    protected static void printUsage() {
        System.out.println(WinstoneResourceBundle.getInstance().getString("UsageInstructions", WinstoneResourceBundle.getInstance().getString("ServerVersion")));
    }
}
