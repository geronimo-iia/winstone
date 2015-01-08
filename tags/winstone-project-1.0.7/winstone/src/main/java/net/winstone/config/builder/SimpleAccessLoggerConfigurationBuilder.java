/**
 * 
 */
package net.winstone.config.builder;

import net.winstone.config.SimpleAccessLoggerConfiguration;

/**
 * SimpleAccessLoggerConfigurationBuilder.
 * 
 * @author JGT
 * 
 */
public class SimpleAccessLoggerConfigurationBuilder extends CompositeBuilder {
	private String loggerFormat;

	private String loggerFilePattern;

	/**
	 * Build a new instance of SimpleAccessLoggerConfigurationBuilder.
	 * 
	 * @param builder
	 */
	public SimpleAccessLoggerConfigurationBuilder(ServerConfigurationBuilder builder) {
		super(builder);
	}

	/**
	 * @see net.winstone.config.builder.CompositeBuilder#build()
	 */
	@Override
	public ServerConfigurationBuilder build() {
		SimpleAccessLoggerConfiguration configuration = new SimpleAccessLoggerConfiguration(loggerFormat, loggerFilePattern);
		return builder.setAccessLoggerConfiguration(configuration).setSimpleAccessLoggerConfiguration(configuration);
	}

	/**
	 * @param loggerFormat
	 *            the loggerFormat to set
	 */
	public SimpleAccessLoggerConfigurationBuilder setLoggerFormat(String loggerFormat) {
		this.loggerFormat = loggerFormat;
		return this;
	}

	/**
	 * @param loggerFilePattern
	 *            the loggerFilePattern to set
	 */
	public SimpleAccessLoggerConfigurationBuilder setLoggerFilePattern(String loggerFilePattern) {
		this.loggerFilePattern = loggerFilePattern;
		return this;
	}
}
