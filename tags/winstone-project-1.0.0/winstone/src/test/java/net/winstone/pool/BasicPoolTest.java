package net.winstone.pool;

import junit.framework.TestCase;

/**
 * Pool Test Case.
 * 
 * @author Jerome Guibert
 */
public class BasicPoolTest extends TestCase {

    public void testBasicPoolWithoutLimit() {
        BasicPool<Person> pool = new BasicPool<Person>(new FactoryPerson(), -1);
        for (int i = 1; i < 8; i++) {
            assertTrue(pool.acquire() != null);
        }
        assertTrue(pool.getNumActive() == -1);
    }

    public void testBasicPoolLimited() {
        BasicPool<Person> pool = new BasicPool<Person>(new FactoryPerson(), 5);
        for (int i = 1; i < 8; i++) {
            if (pool.acquire() == null) {
                assertTrue(i > 5);
                assertTrue(pool.getNumActive() == 5);
            } else {
                assertTrue(i <= 5);
                assertTrue(pool.getNumActive() == i);
            }
        }
    }
}
