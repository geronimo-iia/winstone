package net.winstone.relocate;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * TreePath map object with a path like name1/name2/name3[/*.ext] and get match
 * to a pattern and a unmatched tail.<br />
 * Based on Acme.Serve.PathTreeDictionary. <br />
 * May we change return type with sub path which matched ?
 * 
 * @author Jerome Guibert
 */
public class TreePath {

	private final TreeNode root;

	public TreePath() {
		super();
		root = new TreeNode();
	}

	public Set<String> keys() {
		final Set<String> result = new HashSet<String>();
		addSiblingNames(root, result, "");
		return result;
	}

	protected void addSiblingNames(final TreeNode node, final Set<String> result, final String path) {
		for (String pc : node.keys()) {
			final TreeNode childNode = node.get(pc);
			pc = path + '/' + pc;
			if (childNode.value != null) {
				result.add(pc);
			}
			addSiblingNames(childNode, result, pc);
		}
	}

	public Set<Object> elements() {
		final Set<Object> result = new HashSet<Object>();
		addSiblingObjects(root, result);
		return result;
	}

	protected void addSiblingObjects(final TreeNode node, final Set<Object> result) {
		for (final String pc : node.keys()) {
			final TreeNode childNode = node.get(pc);
			if (childNode.value != null) {
				result.add(childNode.value);
			}
			addSiblingObjects(childNode, result);
		}
	}

	public void put(final String path, final Object value) {
		final StringTokenizer st = new StringTokenizer(path, "\\/");
		TreeNode cur_node = root;
		while (st.hasMoreTokens()) {
			final String nodename = st.nextToken();
			TreeNode node = cur_node.get(nodename);
			if (node == null) {
				node = new TreeNode();
				cur_node.put(nodename, node);
			}
			cur_node = node;
		}
		cur_node.value = value;
	}

	public Object[] remove(final Object value) {
		return remove(root, value);
	}

	public Object[] remove(final String path) {
		final Object[] result = get(path);
		if (result[1] != null) {
			return remove(result[1]);
		}
		return result;
	}

	public Object[] remove(final TreeNode node, final Object value) {
		// TODO make full path, not only last element
		for (final String path : node.keys()) {
			final TreeNode childNode = node.get(path);
			if (childNode.value == value) {// it's safe because the same
											// instance can't be shared for
											// several paths in this design
				childNode.value = null;
				return new Object[] { value, new Integer(0) };
			}
			final Object[] result = remove(childNode, value);
			if (result[0] != null) {
				return result;
			}
		}

		return new Object[] { null, null };
	}

	/**
	 * This function looks up in the directory to find the perfect match and
	 * remove matching part from path, so if you need to keep original path,
	 * save it somewhere.
	 */
	public Object[] get(final String path) {
		final Object[] result = new Object[2];
		if (path == null) {
			return result;
		}
		final char[] ps = path.toCharArray();
		TreeNode cur_node = root;
		int p0 = 0, lm = 0; // last match
		result[0] = cur_node.value;
		boolean div_state = true;
		for (int i = 0; i < ps.length; i++) {
			if ((ps[i] == '/') || (ps[i] == '\\')) {
				if (div_state) {
					continue;
				}
				final TreeNode node = cur_node.get(new String(ps, p0, i - p0));
				if (node == null) {
					result[1] = new Integer(lm);
					return result;
				}
				if (node.value != null) {
					result[0] = node.value;
					lm = i;
				}
				cur_node = node;
				div_state = true;
			} else {
				if (div_state) {
					p0 = i;
					div_state = false;
				}
			}
		}
		cur_node = cur_node.get(new String(ps, p0, ps.length - p0));
		if ((cur_node != null) && (cur_node.value != null)) {
			result[0] = cur_node.value;
			lm = ps.length;
		}
		result[1] = new Integer(lm);
		return result;
	}

	/**
	 * Inner class.
	 * 
	 * @author jguibert
	 */
	private final class TreeNode {
		Map<String, TreeNode> children;
		Object value;

		@SuppressWarnings("unchecked")
		public Set<String> keys() {
			return children == null ? Collections.EMPTY_SET : children.keySet();
		}

		public void put(final String name, final TreeNode node) {
			if (children == null) {
				children = new HashMap<String, TreeNode>();
			}
			children.put(name, node);
		}

		public TreeNode get(final String pc) {
			return children != null ? children.get(pc) : null;
		}

	}
}
