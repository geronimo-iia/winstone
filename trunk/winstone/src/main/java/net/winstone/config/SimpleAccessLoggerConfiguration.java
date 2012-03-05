/**
 * 
 */
package net.winstone.config;

import net.winstone.accesslog.SimpleAccessLogger;

/**
 * SimpleAccessLoggerConfiguration.
 * 
 *  @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class SimpleAccessLoggerConfiguration extends AccessLoggerConfiguration {
	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = 491570258594643467L;
	/**
	 * The log format to use. Supports combined/common/resin/custom.
	 */
	private String loggerFormat;
	/**
	 * The location pattern for the log file.
	 */
	private String loggerFilePattern;

	/**
	 * Build a new instance of SimpleAccessLoggerConfiguration.
	 * 
	 * @param className
	 * @param loggerFormat
	 * @param loggerFilePattern
	 */
	public SimpleAccessLoggerConfiguration(String loggerFormat, String loggerFilePattern) {
		super(SimpleAccessLogger.class.getName());
		this.loggerFormat = loggerFormat;
		this.loggerFilePattern = loggerFilePattern;
	}

	/**
	 * @return the loggerFormat
	 */
	final String getLoggerFormat() {
		return loggerFormat;
	}

	/**
	 * @return the loggerFilePattern
	 */
	final String getLoggerFilePattern() {
		return loggerFilePattern;
	}

}
