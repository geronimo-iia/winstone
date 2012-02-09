package net.winstone.util;

import java.util.Map;

/**
 * String utility<br />
 * 
 * @author Jerome Guibert
 */
public class StringUtils {

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
		return Integer.parseInt(stringArg(args, name, Integer.toString(defaultValue)));
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
				globalReplace(out, parameters[n][0], parameters[n][1]);
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
				out = replace(out, tokens[n][0], tokens[n][1]);
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

	public static String get(final String value, final String defaultValue) {
		if ((value == null) || "".equals(value)) {
			return defaultValue;
		}
		return value;
	}
}
