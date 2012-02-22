package net.winstone.pool;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Pool Test Case.
 * 
 * @author Jerome Guibert
 */
public class BasicPoolTest extends TestCase {

	public void testBasicPoolWithoutLimit() {
		final BasicPool<Person> pool = new BasicPool<Person>(new FactoryPerson(), -1);
		for (int i = 1; i < 8; i++) {
			Assert.assertTrue(pool.acquire() != null);
		}
		Assert.assertTrue(pool.getNumActive() == -1);
	}

	public void testBasicPoolLimited() {
		final BasicPool<Person> pool = new BasicPool<Person>(new FactoryPerson(), 5);
		for (int i = 1; i < 8; i++) {
			if (pool.acquire() == null) {
				Assert.assertTrue(i > 5);
				Assert.assertTrue(pool.getNumActive() == 5);
			} else {
				Assert.assertTrue(i <= 5);
				Assert.assertTrue(pool.getNumActive() == i);
			}
		}
	}
}
