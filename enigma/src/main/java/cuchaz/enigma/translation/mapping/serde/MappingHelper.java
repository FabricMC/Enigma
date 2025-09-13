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

	private MappingHelper() {
	}
}
