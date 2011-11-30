/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.testCase.load;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import winstone.Logger;
import winstone.WebAppConfiguration;
import winstone.WinstoneResourceBundle;

import com.meterware.httpunit.WebConversation;

/**
 * This class is an attempt to benchmark performance under load for winstone. It
 * works by hitting a supplied URL with parallel threads (with keep-alives or
 * without) at an escalating rate, and counting the no of failures.
 * 
 * It uses HttpUnit's WebConversation class for the connection.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: LoadTest.java,v 1.2 2006/02/28 07:32:49 rickknowles Exp $
 */
public class LoadTest {
    private String url;
    private boolean useKeepAlives;
    private int startThreads;
    private int endThreads;
    private int stepSize;
    private long stepPeriod;
    private long gracePeriod;
    private long successTimeTotal;
    private int successCount;
    private WinstoneResourceBundle resources;

    private static String LOCAL_RESOURCE_FILE = "winstone.testCase.load.LocalStrings";

    public LoadTest(WinstoneResourceBundle resources, String url,
            boolean useKeepAlives, int startThreads, int endThreads,
            int stepSize, long stepPeriod, long gracePeriod) {
        this.resources = resources;
        this.url = url;
        this.useKeepAlives = useKeepAlives;
        this.startThreads = startThreads;
        this.endThreads = endThreads;
        this.stepSize = stepSize;
        this.stepPeriod = stepPeriod;
        this.gracePeriod = gracePeriod;

        Logger.log(Logger.INFO, resources, "LoadTest.Config", new String[] {
                this.url, this.useKeepAlives + "", this.startThreads + "",
                this.endThreads + "", this.stepSize + "", this.stepPeriod + "",
                this.gracePeriod + "" });
    }

    public void test() throws InterruptedException {
        WebConversation wc = null;

        // Loop through in steps
        for (int n = this.startThreads; n <= this.endThreads; n += this.stepSize) {
            if (this.useKeepAlives)
                wc = new WebConversation();

            // Spawn the threads
            int noOfSeconds = (int) this.stepPeriod / 1000;
            List threads = new ArrayList();
            for (int m = 0; m < n; m++)
                threads.add(new LoadTestThread(this.url, this, this.resources,
                        wc, noOfSeconds - 1));

            // Sleep for step period
            Thread.sleep(this.stepPeriod + gracePeriod);

            // int errorCount = (noOfSeconds * n) - this.successCount;
            Long averageSuccessTime = this.successCount == 0 ? null : new Long(
                    this.successTimeTotal / this.successCount);

            // Write out results
            Logger.log(Logger.INFO, resources, "LoadTest.LineResult",
                    new String[] { n + "", this.successCount + "",
                            ((noOfSeconds * n) - this.successCount) + "",
                            averageSuccessTime + "" });

            // Close threads
            for (Iterator i = threads.iterator(); i.hasNext();)
                ((LoadTestThread) i.next()).destroy();

            this.successTimeTotal = 0;
            this.successCount = 0;

        }
    }

    public void incTimeTotal(long amount) {
        this.successTimeTotal += amount;
    }

    public void incSuccessCount() {
        this.successCount++;
    }

    public static void main(String args[]) throws Exception {
        WinstoneResourceBundle resources = new WinstoneResourceBundle(
                LOCAL_RESOURCE_FILE);

        // Loop for args
        Map options = new HashMap();
        // String operation = "";
        for (int n = 0; n < args.length; n++) {
            String option = args[n];
            if (option.startsWith("--")) {
                int equalPos = option.indexOf('=');
                String paramName = option.substring(2, equalPos == -1 ? option
                        .length() : equalPos);
                String paramValue = (equalPos == -1 ? "true" : option
                        .substring(equalPos + 1));
                options.put(paramName, paramValue);
            }
        }

        if (options.size() == 0) {
            printUsage(resources);
            return;
        }
        Logger.setCurrentDebugLevel(Integer.parseInt(WebAppConfiguration
                .stringArg(options, "debug", "5")));

        String url = WebAppConfiguration.stringArg(options, "url",
                "http://localhost:8080/");
        boolean keepAlive = WebAppConfiguration.booleanArg(options,
                "keepAlive", true);
        String startThreads = WebAppConfiguration.stringArg(options,
                "startThreads", "20");
        String endThreads = WebAppConfiguration.stringArg(options,
                "endThreads", "1000");
        String stepSize = WebAppConfiguration.stringArg(options, "stepSize",
                "20");
        String stepPeriod = WebAppConfiguration.stringArg(options,
                "stepPeriod", "5000");
        String gracePeriod = WebAppConfiguration.stringArg(options,
                "gracePeriod", "5000");

        LoadTest lt = new LoadTest(resources, url, keepAlive, Integer
                .parseInt(startThreads), Integer.parseInt(endThreads), Integer
                .parseInt(stepSize), Integer.parseInt(stepPeriod), Integer
                .parseInt(gracePeriod));

        lt.test();
    }

    /**
     * Displays the usage message
     */
    private static void printUsage(WinstoneResourceBundle resources)
            throws IOException {
        System.out.println(resources.getString("LoadTest.Usage"));
    }

}
