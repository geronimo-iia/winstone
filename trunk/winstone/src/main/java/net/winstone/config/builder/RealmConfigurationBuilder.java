/**
 * 
 */
package net.winstone.config.builder;

/**
 * RealmConfigurationBuilder. 
 *
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 *
 */
public class RealmConfigurationBuilder extends CompositeBuilder{

	/**
	 * Build a new instance of RealmConfigurationBuilder.
	 * @param builder
	 */
	public RealmConfigurationBuilder(ServerConfigurationBuilder builder) {
		super(builder);
	}

	/**
	 * @see net.winstone.config.builder.CompositeBuilder#build()
	 */
	@Override
	public ServerConfigurationBuilder build() {
		return builder;
	}

}
