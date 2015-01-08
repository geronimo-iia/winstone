/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.test.load;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.winstone.util.StringUtils;

import org.slf4j.LoggerFactory;

import com.meterware.httpunit.WebConversation;

/**
 * This class is an attempt to benchmark performance under load for winstone. It
 * works by hitting a supplied URL with parallel threads (with keep-alives or
 * without) at an escalating rate, and counting the no of failures. It uses
 * HttpUnit's WebConversation class for the connection.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: LoadTest.java,v 1.2 2006/02/28 07:32:49 rickknowles Exp $
 */
public class LoadTest {

	protected static org.slf4j.Logger logger = LoggerFactory.getLogger(LoadTest.class);
	private final String url;
	private final boolean useKeepAlives;
	private final int startThreads;
	private final int endThreads;
	private final int stepSize;
	private final long stepPeriod;
	private final long gracePeriod;
	private long successTimeTotal;
	private int successCount;

	public LoadTest(final String url, final boolean useKeepAlives, final int startThreads, final int endThreads, final int stepSize, final long stepPeriod, final long gracePeriod) {

		this.url = url;
		this.useKeepAlives = useKeepAlives;
		this.startThreads = startThreads;
		this.endThreads = endThreads;
		this.stepSize = stepSize;
		this.stepPeriod = stepPeriod;
		this.gracePeriod = gracePeriod;

		LoadTest.logger.info("Load test initialised with properties: URL={}, KeepAlives={}, StartThreads={}, EndThreads={}, StepSize={}, StepPeriod={}, GracePeriod={}", new Object[] { this.url, this.useKeepAlives, this.startThreads, this.endThreads,
				this.stepSize, this.stepPeriod, this.gracePeriod });
	}

	public void test() throws InterruptedException {
		WebConversation wc = null;

		// Loop through in steps
		for (int n = startThreads; n <= endThreads; n += stepSize) {
			if (useKeepAlives) {
				wc = new WebConversation();
			}

			// Spawn the threads
			final int noOfSeconds = (int) stepPeriod / 1000;
			final List<LoadTestThread> threads = new ArrayList<LoadTestThread>();
			for (int m = 0; m < n; m++) {
				threads.add(new LoadTestThread(url, this, wc, noOfSeconds - 1));
			}

			// Sleep for step period
			Thread.sleep(stepPeriod + gracePeriod);

			// int errorCount = (noOfSeconds * n) - this.successCount;
			final Long averageSuccessTime = successCount == 0 ? null : new Long(successTimeTotal / successCount);

			// Write out results
			LoadTest.logger.info("n={}, success={}, error={}, averageTime={}ms", new Object[] { n, successCount, ((noOfSeconds * n) - successCount), averageSuccessTime });

			// Close threads
			for (final Iterator<LoadTestThread> i = threads.iterator(); i.hasNext();) {
				i.next().destroy();
			}

			successTimeTotal = 0;
			successCount = 0;

		}
	}

	public void incTimeTotal(final long amount) {
		successTimeTotal += amount;
	}

	public void incSuccessCount() {
		successCount++;
	}

	public static void main(final String args[]) throws Exception {

		// Loop for args
		final Map<String, String> options = new HashMap<String, String>();
		// String operation = "";
		for (int n = 0; n < args.length; n++) {
			final String option = args[n];
			if (option.startsWith("--")) {
				final int equalPos = option.indexOf('=');
				final String paramName = option.substring(2, equalPos == -1 ? option.length() : equalPos);
				final String paramValue = (equalPos == -1 ? "true" : option.substring(equalPos + 1));
				options.put(paramName, paramValue);
			}
		}

		if (options.isEmpty()) {
			LoadTest.printUsage();
			return;
		}

		final String url = StringUtils.stringArg(options, "url", "http://localhost:8080/");
		final boolean keepAlive = StringUtils.booleanArg(options, "keepAlive", true);
		final String startThreads = StringUtils.stringArg(options, "startThreads", "20");
		final String endThreads = StringUtils.stringArg(options, "endThreads", "1000");
		final String stepSize = StringUtils.stringArg(options, "stepSize", "20");
		final String stepPeriod = StringUtils.stringArg(options, "stepPeriod", "5000");
		final String gracePeriod = StringUtils.stringArg(options, "gracePeriod", "5000");

		final LoadTest lt = new LoadTest(url, keepAlive, Integer.parseInt(startThreads), Integer.parseInt(endThreads), Integer.parseInt(stepSize), Integer.parseInt(stepPeriod), Integer.parseInt(gracePeriod));

		lt.test();
	}

	/**
	 * Displays the usage message
	 */
	private static void printUsage() throws IOException {
		System.out.println("Winstone Command Line Load Tester\n" + "Usage: java winstone.testCase.load.LoadTest " + "--url=<url> " + "[--keepAlive=<default true>] " + "[--startThreads=<default 20>] " + "[--endThreads=<default 1000>] "
				+ "[--stepSize=<default 20>] " + "[--stepPeriod=<default 5000ms>] " + "[--gracePeriod=<default 5000ms>]");
	}
}
