/**
 * 
 */
package net.winstone.config;

/**
 * HandlerConfiguration. 
 *
 * @author JGT
 *
 */
public class HandlerConfiguration {


	/**
	 * 
	 */
	private int handlerCountStartup;// = set the no of worker threads to spawn
									// at startup. Default is 5\n\
	/**
	 * 
	 */
	private int handlerCountMax;// = set the max no of worker threads to allow.
								// Default is 300\n\
	/**
	 * 
	 */
	private int handlerCountMaxIdle;// = set the max no of idle worker threads
									// to allow. Default is 50\n\
}
