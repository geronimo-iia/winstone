/**
 * 
 */
package net.winstone.config;

import java.io.Serializable;

/**
 * HandlerConfiguration.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class HandlerConfiguration implements Serializable {

	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = 616183244738078779L;
	/**
	 * set the no of worker threads to spawn at startup. Default is 5
	 */
	private int countStartup;
	/**
	 * set the max no of worker threads to allow. Default is 300
	 */
	private int countMax;
	/**
	 * set the max no of idle worker threads to allow. Default is 50
	 */
	private int countMaxIdle;

	/**
	 * Build a new instance of HandlerConfiguration.
	 * 
	 * @param countStartup
	 * @param countMax
	 * @param countMaxIdle
	 */
	public HandlerConfiguration(int countStartup, int countMax, int countMaxIdle) {
		super();
		this.countStartup = countStartup;
		this.countMax = countMax;
		this.countMaxIdle = countMaxIdle;
	}

	/**
	 * @return the countStartup
	 */
	final int getCountStartup() {
		return countStartup;
	}

	/**
	 * @return the countMax
	 */
	final int getCountMax() {
		return countMax;
	}

	/**
	 * @return the countMaxIdle
	 */
	final int getCountMaxIdle() {
		return countMaxIdle;
	}

}
