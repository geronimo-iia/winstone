/**
 * 
 */
package net.winstone.config.builder;

import net.winstone.config.HandlerConfiguration;

/**
 * HandlerConfigurationBuilder.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class HandlerConfigurationBuilder extends CompositeBuilder {
	private int countStartup = 5;

	private int countMax = 300;

	private int countMaxIdle = 50;

	/**
	 * Build a new instance of HandlerConfigurationBuilder.
	 * 
	 * @param builder
	 */
	public HandlerConfigurationBuilder(ServerConfigurationBuilder builder) {
		super(builder);
	}

	/**
	 * @param countStartup
	 *            the countStartup to set
	 */
	public final HandlerConfigurationBuilder setCountStartup(int countStartup) {
		this.countStartup = countStartup;
		return this;
	}

	/**
	 * @param countMax
	 *            the countMax to set
	 */
	public final HandlerConfigurationBuilder setCountMax(int countMax) {
		this.countMax = countMax;
		return this;
	}

	/**
	 * @param countMaxIdle
	 *            the countMaxIdle to set
	 */
	public final HandlerConfigurationBuilder setCountMaxIdle(int countMaxIdle) {
		this.countMaxIdle = countMaxIdle;
		return this;
	}

	/**
	 * @see net.winstone.config.builder.CompositeBuilder#build()
	 */
	@Override
	public ServerConfigurationBuilder build() {
		return builder.setHandlerConfiguration(new HandlerConfiguration(countStartup, countMax, countMaxIdle));
	}

}
