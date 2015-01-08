package net.winstone.util;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.winstone.jndi.resources.DataSourceConfig;

/**
 * Testing of MapConverter.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 */
public class MapConverterTest extends TestCase {

	public void testInjectionOfProperties() {
		final Map<String, String> args = new HashMap<String, String>();
		args.put("name", "test");
		args.put("driverClassName", "com.nosql.is.comming");
		args.put("maxActive", "25");
		args.put("didnotexixts", "novalue");

		final DataSourceConfig dataSourceConfig = new DataSourceConfig();
		MapConverter.apply(args, dataSourceConfig);

		Assert.assertEquals(args.get("name"), dataSourceConfig.getName());
		Assert.assertEquals(args.get("driverClassName"), dataSourceConfig.getDriverClassName());
		Assert.assertEquals(Integer.parseInt(args.get("maxActive")), dataSourceConfig.getMaxActive());

	}
}
