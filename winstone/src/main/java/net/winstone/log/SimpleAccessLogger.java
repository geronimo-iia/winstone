/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import net.winstone.util.StringUtils;
import winstone.WebAppConfiguration;
import winstone.WinstoneRequest;
import winstone.WinstoneResponse;

/**
 * Simulates an apache "combined" style logger, which logs User-Agent, Referer, etc
 * 
 * @author Jerome Guibert
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: SimpleAccessLogger.java,v 1.5 2006/03/24 17:24:19 rickknowles Exp $
 */
public class SimpleAccessLogger implements AccessLogger {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    private static final DateFormat accessFormat = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");
    private static final String COMMON = "###ip### - ###user### ###time### \"###uriLine###\" ###status### ###size###";
    private static final String COMBINED = COMMON + " \"###referer###\" \"###userAgent###\"";
    private static final String RESIN = COMMON + " \"###userAgent###\"";
    
    private PrintWriter outWriter;
    private String pattern;
    
    public SimpleAccessLogger(WebAppConfiguration webAppConfig, Map<String, String> startupArgs) throws IOException {
        
        // Get pattern
        String patternType = WebAppConfiguration.stringArg(startupArgs, "simpleAccessLogger.format", "combined");
        if (patternType.equalsIgnoreCase("combined")) {
            this.pattern = COMBINED;
        } else if (patternType.equalsIgnoreCase("common")) {
            this.pattern = COMMON;
        } else if (patternType.equalsIgnoreCase("resin")) {
            this.pattern = RESIN;
        } else {
            this.pattern = patternType;
        }
        
        // Get filename
        String filePattern = WebAppConfiguration.stringArg(startupArgs, "simpleAccessLogger.file", "logs/###host###/###webapp###_access.log");
        String fileName = StringUtils.replace(filePattern, new String[][] {
            {
                "###host###", webAppConfig.getOwnerHostname()
            }, {
                "###webapp###", webAppConfig.getContextName()
            }
        });
        
        File file = new File(fileName);
        file.getParentFile().mkdirs();
        this.outWriter = new PrintWriter(new FileOutputStream(file, true), true);
        
        logger.info(String.format("Initialized access log at %s (format: %s)", fileName, patternType));
    }
    
    public void log(final String originalURL, final WinstoneRequest request, final WinstoneResponse response) {
        String uriLine = request.getMethod() + " " + originalURL + " " + request.getProtocol();
        int status = response.getErrorStatusCode() == null ? response.getStatus() : response.getErrorStatusCode().intValue();
        int size = response.getWinstoneOutputStream().getBytesCommitted();
        String date = getCurrentAccessDate();
        String logLine = StringUtils.replace(this.pattern, new String[][] {
            {
                "###ip###", request.getRemoteHost()
            }, {
                "###user###", nvl(request.getRemoteUser())
            }, {
                "###time###", "[" + date + "]"
            }, {
                "###uriLine###", uriLine
            }, {
                "###status###", "" + status
            }, {
                "###size###", "" + size
            }, {
                "###referer###", nvl(request.getHeader("Referer"))
            }, {
                "###userAgent###", nvl(request.getHeader("User-Agent"))
            }
        });
        this.outWriter.println(logLine);
    }
    
    private static String nvl(final String input) {
        return input == null ? "-" : input;
    }
    
    public void destroy() {
        logger.info("Closed access log");
        if (this.outWriter != null) {
            this.outWriter.flush();
            this.outWriter.close();
            this.outWriter = null;
        }
    }
    
    public static String getCurrentAccessDate() {
        String result = null;
        synchronized (accessFormat) {
            result = accessFormat.format(new Date());
        }
        return result;
    }
}
