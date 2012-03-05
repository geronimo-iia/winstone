/**
 * 
 */
package net.winstone.config;

import java.io.Serializable;
import java.util.List;

import net.winstone.cluster.SimpleCluster;

/**
 * ClusterConfiguration.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 * 
 */
public class ClusterConfiguration implements Serializable {

	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = 7101861040958821946L;

	private Boolean useCluster = Boolean.FALSE;

	private String className = SimpleCluster.class.getName();

	private List<AddressConfiguration> nodes;

	/**
	 * Build a new instance of ClusterConfiguration.
	 * 
	 * @param useCluster
	 * @param className
	 * @param nodes
	 */
	public ClusterConfiguration(Boolean useCluster, String className, List<AddressConfiguration> nodes) {
		super();
		this.useCluster = useCluster;
		this.className = className;
		this.nodes = nodes;
	}

	/**
	 * @return the useCluster
	 */
	final Boolean getUseCluster() {
		return useCluster;
	}

	/**
	 * @return the className
	 */
	final String getClassName() {
		return className;
	}

	/**
	 * @return the nodes
	 */
	final List<AddressConfiguration> getNodes() {
		return nodes;
	}

}
