/**
 * 
 */
package net.winstone.config.builder;

import java.util.ArrayList;
import java.util.List;

import net.winstone.cluster.SimpleCluster;
import net.winstone.config.AddressConfiguration;
import net.winstone.config.ClusterConfiguration;

/**
 * ClusterConfigurationBuilder.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class ClusterConfigurationBuilder extends CompositeBuilder {
	private Boolean useCluster = Boolean.FALSE;

	private String className = SimpleCluster.class.getName();

	private List<AddressConfiguration> nodes = new ArrayList<AddressConfiguration>();

	/**
	 * Build a new instance of ClusterConfigurationBuilder.
	 * 
	 * @param builder
	 */
	public ClusterConfigurationBuilder(ServerConfigurationBuilder builder) {
		super(builder);
	}

	/**
	 * @see net.winstone.config.builder.CompositeBuilder#build()
	 */
	@Override
	public ServerConfigurationBuilder build() {
		return builder.setClusterConfiguration(new ClusterConfiguration(useCluster, className, nodes));
	}

	/**
	 * @param className
	 *            the className to set
	 */
	public ClusterConfigurationBuilder setClassName(String className) {
		this.className = className;
		return this;
	}

	public ClusterNodeConfigurationBuilder addNode() {
		return new ClusterNodeConfigurationBuilder(this);
	}

	public ClusterConfigurationBuilder addNodeAddress(final int port, final String address) {
		ClusterNodeConfigurationBuilder node = new ClusterNodeConfigurationBuilder(this);
		return node.build();
	}

	/**
	 * @param addressConfiguration
	 * @return
	 */
	ClusterConfigurationBuilder addAddressConfiguration(AddressConfiguration addressConfiguration) {
		this.nodes.add(addressConfiguration);
		return this;
	}
}
