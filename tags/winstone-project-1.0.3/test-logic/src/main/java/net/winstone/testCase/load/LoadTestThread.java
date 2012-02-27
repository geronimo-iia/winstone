/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.testCase.load;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

/**
 * A single worked thread in the load testing program
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: LoadTestThread.java,v 1.2 2006/02/28 07:32:49 rickknowles Exp $
 */
public class LoadTestThread implements Runnable {

	protected static org.slf4j.Logger logger = LoggerFactory.getLogger(LoadTestThread.class);
	private final String url;
	private final long delayBeforeStarting;
	private final LoadTest loadTest;
	private WebConversation webConv;
	private final Thread thread;
	private boolean interrupted;
	private LoadTestThread next;

	public LoadTestThread(final String url, final LoadTest loadTest, final WebConversation webConv, final int delayedThreads) {
		this.url = url;
		this.loadTest = loadTest;
		this.webConv = webConv;
		delayBeforeStarting = 1000 * delayedThreads;
		interrupted = false;
		thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();

		// Launch the next second's getter
		if (delayedThreads > 0) {
			next = new LoadTestThread(url, loadTest, webConv, delayedThreads - 1);
		}
	}

	@Override
	public void run() {
		if (delayBeforeStarting > 0) {
			try {
				Thread.sleep(delayBeforeStarting);
			} catch (final InterruptedException err) {
			}
		}

		final long startTime = System.currentTimeMillis();

		try {
			if (webConv == null) {
				webConv = new WebConversation();
			}

			// Access the URL
			final WebRequest wreq = new GetMethodWebRequest(url);
			final WebResponse wresp = webConv.getResponse(wreq);
			final int responseCode = wresp.getResponseCode();
			if (responseCode >= 400) {
				throw new IOException("Failed with status " + responseCode);
			}
			final InputStream inContent = wresp.getInputStream();
			final int contentLength = wresp.getContentLength();
			final byte content[] = new byte[contentLength == -1 ? 100 * 1024 : contentLength];
			int position = 0;
			int value = inContent.read();
			while ((value != -1) && (((contentLength >= 0) && (position < contentLength)) || (contentLength < 0))) {
				content[position++] = (byte) value;
				value = inContent.read();
			}
			inContent.close();

			// Confirm the result is the same size the content-length said it
			// was
			if ((position == contentLength) || (contentLength == -1)) {
				if (interrupted) {
					return;
				}
				loadTest.incTimeTotal(System.currentTimeMillis() - startTime);
				loadTest.incSuccessCount();
			} else {
				throw new IOException("Only downloaded " + position + " of " + contentLength + " bytes");
			}
		} catch (final IOException err) {
			LoadTestThread.logger.debug("Error in response", err);
		} catch (final SAXException err) {
			LoadTestThread.logger.debug("Error in response", err);
		}
	}

	public void destroy() {
		interrupted = true;
		thread.interrupt();
		if (next != null) {
			next.destroy();
		}
	}
}
