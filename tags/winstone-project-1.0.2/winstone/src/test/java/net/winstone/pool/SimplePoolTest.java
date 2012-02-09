package net.winstone.pool;

import junit.framework.TestCase;

/**
 * Pool Test Case.
 * 
 * @author Jerome Guibert
 */
public class SimplePoolTest extends TestCase {

    public void testLimit() {
        Pool<Person> pool = new SimplePool<Person>(new FactoryPerson(), 5, 1, 0);
        for (int i = 1; i < 8; i++) {
            if (pool.acquire() == null) {
                assertTrue(i > 5);
            } else {
                assertTrue(i <= 5);
            }
        }
    }

    public void testLimitWithstartIdle() {
        Pool<Person> pool = new SimplePool<Person>(new FactoryPerson(), 5, 1, 3);
        for (int i = 1; i < 8; i++) {
            if (pool.acquire() == null) {
                assertTrue(i > 5);
            } else {
                assertTrue(i <= 5);
            }
        }
    }
}
