/**
 * 
 */
package net.winstone.config.builder;


/**
 * CompositeBuilder.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public abstract class CompositeBuilder {

	protected final ServerConfigurationBuilder builder;

	/**
	 * Build a new instance of CompositeBuilder.
	 * 
	 * @param builder
	 *            ServerConfigurationBuilder instance.
	 */
	public CompositeBuilder(ServerConfigurationBuilder builder) {
		super();
		this.builder = builder;
	}

	/**
	 * @return ServerConfigurationBuilder instance.
	 */
	public abstract ServerConfigurationBuilder build();

}
