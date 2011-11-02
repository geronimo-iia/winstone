package net.winstone.util;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import net.winstone.jndi.resources.DataSourceConfig;

/**
 * Testing of MapConverter.
 * @author Jerome Guibert
 */
public class MapConverterTest extends TestCase {

    public void testInjectionOfProperties() {
        Map<String, String> args = new HashMap<String, String>();
        args.put("name", "test");
        args.put("driverClassName", "com.nosql.is.comming");
        args.put("maxActive", "25");
        args.put("didnotexixts", "novalue");
 
        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        MapConverter.apply(args, dataSourceConfig);

        assertEquals(args.get("name"), dataSourceConfig.getName());
        assertEquals(args.get("driverClassName"), dataSourceConfig.getDriverClassName());
        assertEquals(Integer.parseInt(args.get("maxActive")), dataSourceConfig.getMaxActive());

    }
}
