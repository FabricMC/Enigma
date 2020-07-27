package cuchaz.enigma.config;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ConfigSerializer {

	private static final Pattern FULL_RGB_COLOR = Pattern.compile("#[0-9A-Fa-f]{6}");
	private static final Pattern MIN_RGB_COLOR = Pattern.compile("#[0-9A-Fa-f]{3}");

	private static final int UNEXPECTED_TOKEN = -1;
	private static final int NO_MATCH = -2;

	public static void parse(String v, ConfigStructureVisitor visitor) {
		String[] lines = v.split("\n");

		// join escaped newlines
		int len = lines.length;
		for (int i = len - 2; i >= 0; i--) {
			if (lines[i].endsWith("\\")) {
				lines[i] = String.format("%s\n%s", lines[i], lines[i + 1]);
				len -= 1;
			}
		}

		// parse for real
		for (int i = 0; i < len; i++) {
			String line = lines[i];

			// skip empty lines and comment lines
			if (line.trim().isEmpty() || line.trim().startsWith(";")) continue;

			int r;
			boolean fail = (r = parseSectionLine(line, 0, visitor)) == NO_MATCH &&
					(r = parseKeyValue(line, 0, visitor)) == NO_MATCH;
		}
	}

	private static int parseSectionLine(String v, int idx, ConfigStructureVisitor visitor) {
		if (v.startsWith("[")) {
			visitor.jumpToRootSection();

			while (idx < v.length() && v.charAt(idx) == '[') {
				idx = parseSection(v, idx, visitor);
				if (idx == UNEXPECTED_TOKEN) return UNEXPECTED_TOKEN;
			}
			return v.length();
		} else {
			return NO_MATCH;
		}
	}

	private static int parseSection(String v, int idx, ConfigStructureVisitor visitor) {
		idx += 1; // skip leading [
		StringBuilder sb = new StringBuilder();
		while (idx < v.length()) {
			int nextCloseBracket = v.indexOf(']', idx);
			int nextEscape = v.indexOf('\\', idx);
			int next = optMin(nextCloseBracket, nextEscape);
			if (next == nextCloseBracket) {
				sb.append(v, idx, nextCloseBracket);
				visitor.visitSection(sb.toString());
				return nextCloseBracket + 1;
			} else if (next == nextEscape) {
				sb.append(v, idx, nextEscape);
				if (nextEscape + 1 < v.length()) {
					sb.append(v.charAt(nextEscape + 1));
				}
				idx = nextEscape + 2;
			} else {
				// unexpected
				return UNEXPECTED_TOKEN;
			}
		}
		return idx;
	}

	private static int parseKeyValue(String v, int idx, ConfigStructureVisitor visitor) {
		StringBuilder sb = new StringBuilder();
		String k = null;
		while (idx < v.length()) {
			int nextEq = v.indexOf('=', idx);
			int nextEscape = v.indexOf('\\', idx);
			int next = optMin(nextEq, nextEscape);
			if (next == nextEq) {
				sb.append(v, idx, nextEq);
				k = sb.toString();
				sb.delete(0, sb.length());
				idx = nextEq + 1;
				break;
			} else if (next == nextEscape) {
				sb.append(v, idx, nextEscape);
				if (nextEscape + 1 < v.length()) {
					sb.append(v.charAt(nextEscape + 1));
				}
				idx = nextEscape + 2;
			} else {
				break;
			}
		}
		while (idx < v.length()) {
			int nextEscape = v.indexOf('\\', idx);
			if (nextEscape != -1) {
				sb.append(v, idx, nextEscape);
				if (nextEscape + 1 < v.length()) {
					sb.append(v.charAt(nextEscape + 1));
				}
				idx = nextEscape + 2;
			} else {
				break;
			}
		}
		sb.append(v, idx, v.length());
		if (k == null) return NO_MATCH;
		visitor.visitKeyValue(k, sb.toString());
		return idx;
	}

	public static String structureToString(ConfigSection section) {
		StringBuilder sb = new StringBuilder();
		structureToString(section, sb, new ArrayList<>());
		return sb.toString();
	}

	private static void structureToString(ConfigSection section, StringBuilder sb, List<String> pathStack) {
		if (!section.values().isEmpty()) {
			if (sb.length() > 0) sb.append('\n');
			pathStack.forEach(n -> sb.append('[').append(escapeSection(n)).append(']'));
			sb.append('\n');
			section.values().entrySet().stream()
					.sorted(Entry.comparingByKey())
					.forEach(e -> sb.append(escapeKey(e.getKey())).append('=').append(escapeValue(e.getValue())).append('\n'));
		}

		section.sections().entrySet().stream().sorted(Entry.comparingByKey()).forEach(e -> {
			pathStack.add(e.getKey());
			structureToString(e.getValue(), sb, pathStack);
			pathStack.remove(pathStack.size() - 1);
		});
	}

	private static String escapeSection(String s) {
		return s.replace("\\", "\\\\").replace("]", "\\]");
	}

	private static String unescapeSection(String s) {
		return s.replace("\\]", "]").replace("\\\\", "\\");
	}

	private static String escapeKey(String s) {
		return s.replace("\\", "\\\\").replace("[", "\\[").replace("\n", "\\n").replace("=", "\\=");
	}

	private static String unescapeKey(String s) {
		return s.replace("\\=", "=").replace("\\n", "\n").replace("\\[", "[").replace("\\\\", "\\");
	}

	private static String escapeValue(String s) {
		return s.replace("\\", "\\\\").replace("\n", "\\n");
	}

	private static String unescapeValue(String s) {
		return s.replace("\\n", "\n").replace("\\\\", "\\");
	}

	public static Optional<Boolean> parseBool(String v) {
		if (v == null) return Optional.empty();
		switch (v) {
			case "true":
				return Optional.of(true);
			case "false":
				return Optional.of(false);
			default:
				return Optional.empty();
		}
	}

	public static OptionalInt parseInt(String v) {
		if (v == null) return OptionalInt.empty();
		try {
			return OptionalInt.of(Integer.parseInt(v));
		} catch (NumberFormatException e) {
			return OptionalInt.empty();
		}
	}

	public static OptionalDouble parseDouble(String v) {
		if (v == null) return OptionalDouble.empty();
		try {
			return OptionalDouble.of(Double.parseDouble(v));
		} catch (NumberFormatException e) {
			return OptionalDouble.empty();
		}
	}

	public static OptionalInt parseRgbColor(String v) {
		if (v == null) return OptionalInt.empty();
		try {
			if (FULL_RGB_COLOR.matcher(v).matches()) {
				return OptionalInt.of(Integer.parseUnsignedInt(v.substring(1), 16));
			} else if (MIN_RGB_COLOR.matcher(v).matches()) {
				int result = Integer.parseUnsignedInt(v.substring(1), 16);
				// change 0xABC to 0xAABBCC
				result = (result & 0x00F) | (result & 0x0F0) << 4 | (result & 0xF00) << 8;
				result = result | result << 4;
				return OptionalInt.of(result);
			} else {
				return OptionalInt.empty();
			}
		} catch (NumberFormatException e) {
			return OptionalInt.empty();
		}
	}

	public static String rgbColorToString(int color) {
		color = color & 0xFFFFFF;
		boolean isShort = ((color & 0xF0F0F0) >> 4 ^ color & 0x0F0F0F) == 0;
		if (isShort) {
			int packed = color & 0x0F0F0F;
			packed = packed & 0xF | packed >> 4;
			packed = packed & 0xFF | (packed & ~0xFF) >> 4;
			return String.format("#%03x", packed);
		} else {
			return String.format("#%06x", color);
		}
	}

	public static Optional<String[]> parseArray(String v) {
		if (v == null) return Optional.empty();
		List<String> l = new ArrayList<>();
		int idx = 0;
		StringBuilder cur = new StringBuilder();
		while (true) {
			int nextSep = v.indexOf(',', idx);
			int nextEsc = v.indexOf('\\', idx);
			int next = optMin(nextSep, nextEsc);
			if (next == -1) {
				cur.append(v, idx, v.length());
				l.add(cur.toString());
				return Optional.of(l.toArray(new String[0]));
			} else if (next == nextSep) {
				cur.append(v, idx, nextSep);
				l.add(cur.toString());
				cur.delete(0, cur.length());
				idx = nextSep + 1;
			} else if (next == nextEsc) {
				cur.append(v, idx, nextEsc);
				if (nextEsc + 1 < v.length()) {
					cur.append(v.charAt(nextEsc + 1));
				}
				idx = nextEsc + 2;
			}
		}
	}

	public static String arrayToString(String[] values) {
		return Arrays.stream(values)
				.map(s -> s.replace("\\", "\\\\").replace(",", "\\,"))
				.collect(Collectors.joining(","));
	}

	public static <T extends Enum<T>> Optional<T> parseEnum(Function<String, T> byName, String v) {
		if (v == null) return Optional.empty();
		try {
			return Optional.of(byName.apply(v));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private static int optMin(int v1, int v2) {
		if (v1 == -1) return v2;
		if (v2 == -1) return v1;
		return Math.min(v1, v2);
	}

}
