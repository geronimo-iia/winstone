package net.winstone.pool;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Pool Test Case.
 * 
 * @author Jerome Guibert
 */
public class SimplePoolTest extends TestCase {

	public void testLimit() {
		final Pool<Person> pool = new SimplePool<Person>(new FactoryPerson(), 5, 1, 0);
		for (int i = 1; i < 8; i++) {
			if (pool.acquire() == null) {
				Assert.assertTrue(i > 5);
			} else {
				Assert.assertTrue(i <= 5);
			}
		}
	}

	public void testLimitWithstartIdle() {
		final Pool<Person> pool = new SimplePool<Person>(new FactoryPerson(), 5, 1, 3);
		for (int i = 1; i < 8; i++) {
			if (pool.acquire() == null) {
				Assert.assertTrue(i > 5);
			} else {
				Assert.assertTrue(i <= 5);
			}
		}
	}
}
