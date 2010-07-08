package net.winstone;

import junit.framework.TestCase;
import net.winstone.domain.Person;
import net.winstone.pool.BasicPool;
import net.winstone.pool.Pool;
import net.winstone.pool.ResourceFactory;
import net.winstone.pool.SimplePool;

/**
 * Pool Test Case.
 * 
 * @author Jerome Guibert
 */
public class PoolTest extends TestCase {
    
    public class FactoryPerson implements ResourceFactory<Person> {
        private int i = 0;
        
        public Person create() {
            return new Person("n-" + i++, "l-" + i);
        }
        
        public void destroy(Person resource) {
        }
        
    }
    
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
