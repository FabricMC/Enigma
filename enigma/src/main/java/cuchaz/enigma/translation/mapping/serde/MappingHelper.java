package cuchaz.enigma.translation.mapping.serde;

public final class MappingHelper {
	private static final String TO_ESCAPE = "\\\n\r\0\t";
	private static final String ESCAPED = "\\nr0t";

	public static String escape(String raw) {
		StringBuilder builder = new StringBuilder(raw.length() + 1);
		for (int i = 0; i < raw.length(); i++) {
			final char c = raw.charAt(i);
			final int r = TO_ESCAPE.indexOf(c);
			if (r < 0) {
				builder.append(c);
			} else {
				builder.append('\\').append(ESCAPED.charAt(r));
			}
		}
		return builder.toString();
	}

	public static String unescape(String str) {
		int pos = str.indexOf('\\');
		if (pos < 0) return str;

		StringBuilder ret = new StringBuilder(str.length() - 1);
		int start = 0;

		do {
			ret.append(str, start, pos);
			pos++;
			int type;

			if (pos >= str.length()) {
				throw new RuntimeException("incomplete escape sequence at the end");
			} else if ((type = ESCAPED.indexOf(str.charAt(pos))) < 0) {
				throw new RuntimeException("invalid escape character: \\" + str.charAt(pos));
			} else {
				ret.append(TO_ESCAPE.charAt(type));
			}

			start = pos + 1;
		} while ((pos = str.indexOf('\\', start)) >= 0);

		ret.append(str, start, str.length());

		return ret.toString();
	}

	private MappingHelper() {
	}
}
