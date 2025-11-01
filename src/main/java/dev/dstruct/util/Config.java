package dev.dstruct.util;

import java.util.Objects;

public final class Config {

	public static String resolve(String path, String defaultValue) {
		Objects.requireNonNull(path, "path is null");
		String value = System.getProperty(path);
		if (value != null) return value;
		value = System.getenv(toSnakeUpperCase(path));
		if (value != null) return value;
		return defaultValue;
	}

	public static long resolveLong(String path, long defaultValue) {
		String resolve = resolve(path, null);
		return resolve != null ? Long.parseLong(resolve) : defaultValue;
	}

	public static int resolveInt(String path, int defaultValue) {
		String resolve = resolve(path, null);
		return resolve != null ? Integer.parseInt(resolve) : defaultValue;
	}

	public static boolean resolveBoolean(String path, boolean defaultValue) {
		String resolve = resolve(path, null);
		return resolve != null ? Boolean.parseBoolean(resolve) : defaultValue;
	}

	private static String toSnakeUpperCase(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}

		StringBuilder sb = new StringBuilder(str.length() + 10);

		for (int i = 0; i < str.length(); i++) {
			char current = str.charAt(i);

			if (Character.isUpperCase(current)) {
				if (i > 0 && !sb.isEmpty() && sb.charAt(sb.length() - 1) != '_') {
					char prev = str.charAt(i - 1);
					if (Character.isLowerCase(prev) || Character.isDigit(prev)) {
						sb.append('_');
					}
				}
				sb.append(current);
			}
			else if (current == '.' || current == '-' || current == ' ') {
				if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '_') {
					sb.append('_');
				}
			}
			else if (Character.isLetterOrDigit(current)) {
				sb.append(Character.toUpperCase(current));
			}
		}

		if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '_') {
			sb.setLength(sb.length() - 1);
		}

		return sb.toString();
	}

}
