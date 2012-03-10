package net.winstone.relocate;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TreePathTest extends TestCase {
	private TreePath tree;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tree = new TreePath();
	}

	public void testBasic() {
		tree.put("/a/b/c", 1L);
		tree.put("/a/b/c/d", 2L);

		Assert.assertNull(get("/a/b")[0]);
		Assert.assertEquals(get("/a/b/c")[0], 1L);
		get("/a/b/c/d");
		Assert.assertEquals(get("/a/b/c/d/e")[0], 2L);

	}

	private Object[] get(final String path) {
		final Object[] res = tree.get(path);
		System.err.println(path + " => res [ " + res[0] + ", " + res[1] + "]");
		return res;
	}
}
