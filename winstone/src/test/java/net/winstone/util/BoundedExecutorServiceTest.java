package net.winstone.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

/**
 * @author Kohsuke Kawaguchi
 */
public class BoundedExecutorServiceTest extends TestCase {

	public void testToTranslate() {
		// nothing to do now
	}

	public void bounded() throws Exception {
		ExecutorService es = new ThreadPoolExecutor(2, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS, // idle
																								// thread
																								// will
																								// only
																								// hang
																								// around
																								// for
																								// 60
																								// secs
				new SynchronousQueue<Runnable>(), new ThreadFactory() {
					private int threadIndex;

					public synchronized Thread newThread(Runnable r) {
						// allocate a thread to run on this object
						Thread thread = new Thread(r, "winstone-" + (++threadIndex));
						thread.setDaemon(true);
						return thread;
					}
				});
		BoundedExecutorService bes = new BoundedExecutorService(es, 5);

		for (int i = 0; i < 20; i++) {
			final int n = i;
			bes.submit(new Runnable() {
				public void run() {
					try {
						System.out.println("#" + n + " started");
						Thread.sleep(1000);
						System.out.println("#" + n + " stopped");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
			Thread.sleep(100);
		}

		Thread.sleep(100000);
	}
}