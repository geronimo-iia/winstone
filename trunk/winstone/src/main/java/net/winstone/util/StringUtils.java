package net.winstone.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * String utility<br />
 * 
 * @author Jerome Guibert
 */
public class StringUtils {

	/**
	 * @param args
	 * @param name
	 * @return a file instance for specified name argument, null if none is
	 *         found.
	 */
	public static File fileArg(final Map<String, String> args, final String name) {
		final String value = args.get(name);
		return value != null ? new File(value) : null;
	}

	public static boolean booleanArg(final Map<String, String> args, final String name, final boolean defaultTrue) {
		final String value = args.get(name);
		if (defaultTrue) {
			return (value == null) || (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
		} else {
			return (value != null) && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
		}
	}

	public static String stringArg(final Map<String, String> args, final String name, final String defaultValue) {
		return (args.get(name) == null ? defaultValue : args.get(name));
	}

	public static int intArg(final Map<String, String> args, final String name, final int defaultValue) {
		return Integer.parseInt(StringUtils.stringArg(args, name, Integer.toString(defaultValue)));
	}

	public static String get(final String value, final String defaultValue) {
		if ((value == null) || "".equals(value)) {
			return defaultValue;
		}
		return value;
	}

	/**
	 * This function extract meaningful path or query
	 * 
	 * @param path
	 *            path to extract from
	 * @param query
	 *            true if extract query
	 * @return extraction or null
	 */
	public static String extractQueryAnchor(final String path, final boolean query) {
		final int qp = path.indexOf('?');
		if (query) {
			if (qp >= 0) {
				return path.substring(qp + 1);
			}
			return null;
		}
		final int hp = path.indexOf('#');
		if (qp >= 0) {
			if ((hp >= 0) && (hp < qp)) {
				return path.substring(0, hp);
			}
			return path.substring(0, qp);
		} else if (hp >= 0) {
			return path.substring(0, hp);
		}
		return path;
	}

	/**
	 * Just does a string swap, replacing occurrences of from with to.
	 */
	@Deprecated
	public static String globalReplace(final String input, final String fromMarker, final String toValue) {
		final StringBuffer out = new StringBuffer(input);
		StringUtils.globalReplace(out, fromMarker, toValue);
		return out.toString();
	}

	@Deprecated
	public static String globalReplace(final String input, final String parameters[][]) {
		if (parameters != null) {
			final StringBuffer out = new StringBuffer(input);
			for (int n = 0; n < parameters.length; n++) {
				StringUtils.globalReplace(out, parameters[n][0], parameters[n][1]);
			}
			return out.toString();
		} else {
			return input;
		}
	}

	@Deprecated
	public static void globalReplace(final StringBuffer input, final String fromMarker, final String toValue) {
		if (input == null) {
			return;
		} else if (fromMarker == null) {
			return;
		}
		final String value = toValue == null ? "(null)" : toValue;
		int index = 0;
		int foundAt = input.indexOf(fromMarker, index);
		while (foundAt != -1) {
			input.replace(foundAt, foundAt + fromMarker.length(), value);
			index = foundAt + toValue.length();
			foundAt = input.indexOf(fromMarker, index);
		}
	}

	/**
	 * replace substrings within string.
	 */
	public static String replace(final String input, final String sub, final String with) {
		int fromIndex = 0;
		int index = input.indexOf(sub, fromIndex);
		if (index == -1) {
			return input;
		}
		final StringBuilder buf = new StringBuilder(input.length() + with.length());
		do {
			buf.append(input.substring(fromIndex, index));
			buf.append(with);
			fromIndex = index + sub.length();
		} while ((index = input.indexOf(sub, fromIndex)) != -1);

		if (fromIndex < input.length()) {
			buf.append(input.substring(fromIndex, input.length()));
		}
		return buf.toString();
	}

	public static String replace(final String input, final String[][] tokens) {
		if ((tokens != null) && (input != null)) {
			String out = input;
			for (int n = 0; n < tokens.length; n++) {
				out = StringUtils.replace(out, tokens[n][0], tokens[n][1]);
			}
			return out;
		} else {
			return input;
		}
	}

	public static String replaceToken(final String input, final String... parameters) {
		if ((input != null) && (parameters != null)) {
			final String tokens[][] = new String[parameters.length][2];
			for (int n = 0; n < parameters.length; n++) {
				tokens[n] = new String[] { "[#" + n + "]", parameters[n] };
			}
			return StringUtils.replace(input, tokens);
		}
		return input;
	}

	/**
	 * Performs necessary escaping to render arbitrary plain text as plain text
	 * without any markup.
	 */
	public static String htmlEscapeBasicMarkup(final String text) {
		final StringBuilder buf = new StringBuilder(text.length() + 64);
		for (int i = 0; i < text.length(); i++) {
			final char ch = text.charAt(i);
			switch (ch) {
			case '<':
				buf.append("&lt;");
				break;
			case '&':
				buf.append("&amp;");
				break;
			case '>':
				buf.append("&gt;");
				break;
			default:
				buf.append(ch);
				break;
			}
		}
		return buf.toString();
	}

	/**
	 * Eliminates "." and ".." in the path. So that this method can be used for
	 * any string that looks like an URI, this method preserves the leading and
	 * trailing '/'.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String canonicalPath(final String path) {
		final List r = new ArrayList(Arrays.asList(path.split("/+")));
		for (int i = 0; i < r.size();) {
			final String cur = (String) r.get(i);
			if ((cur.length() == 0) || cur.equals(".")) {
				// empty token occurs for example, "".split("/+") is [""]
				r.remove(i);
			} else if (cur.equals("..")) {
				// i==0 means this is a broken URI.
				r.remove(i);
				if (i > 0) {
					r.remove(i - 1);
					i--;
				}
			} else {
				i++;
			}
		}

		final StringBuilder buf = new StringBuilder();
		if (path.startsWith("/")) {
			buf.append('/');
		}
		boolean first = true;
		for (final Object aR : r) {
			final String token = (String) aR;
			if (!first) {
				buf.append('/');
			} else {
				first = false;
			}
			buf.append(token);
		}
		// translation: if (path.endsWith("/") && !buf.endsWith("/"))
		if (path.endsWith("/") && ((buf.length() == 0) || (buf.charAt(buf.length() - 1) != '/'))) {
			buf.append('/');
		}
		return buf.toString();
	}
}
