package net.winstone;

import java.util.Set;

import junit.framework.TestCase;
import net.winstone.core.WebAppConfiguration;

public class WebApplicationContextTestCase extends TestCase {

    public void testResourcePaths() {
//        WebAppConfiguration context = new WebAppConfiguration(null, null, "test", "/", getClass().getClassLoader().getResource("webapp").getPath());
//
//        Set<String> root = context.getResourcePaths("/");
//        assertNotNull(root);
//        assertTrue(checkEquals(root, new String[]{
//                    "/catalog/", "/customer/", "/WEB-INF/", "/welcome.html"
//                }));
//
//        root = context.getResourcePaths("/catalog/");
//        assertNotNull(root);
//        assertTrue(checkEquals(root, new String[]{
//                    "/catalog/index.html", "/catalog/products.html", "/catalog/offers/"
//                }));
    }

    protected boolean checkEquals(Set<String> result, String[] valid) {
        for (int i = 0; i < valid.length; i++) {
            if (!result.contains(valid[i])) {
                return false;
            }
        }
        return true;
    }
}
