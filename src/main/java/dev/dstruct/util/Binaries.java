package dev.dstruct.util;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class Binaries {

	public static double fromBytesToDouble(byte[] bytes) {
		if (bytes == null || bytes.length != 8) {
			throw new IllegalArgumentException("Expected 8 bytes for double");
		}
		return Double.longBitsToDouble(fromBytesToLong(bytes));
	}

	public static float fromBytesToFloat(byte[] bytes) {
		if (bytes == null || bytes.length != 4) {
			throw new IllegalArgumentException("Expected 4 bytes for float");
		}
		return Float.intBitsToFloat(fromBytesToInt(bytes));
	}

	public static long fromBytesToLong(byte[] bytes) {
		if (bytes == null || bytes.length != 8) {
			throw new IllegalArgumentException("Expected 8 bytes for long");
		}
		return
			((long)(bytes[0] & 0xFF) << 56) |
			((long)(bytes[1] & 0xFF) << 48) |
			((long)(bytes[2] & 0xFF) << 40) |
			((long)(bytes[3] & 0xFF) << 32) |
			((long)(bytes[4] & 0xFF) << 24) |
			((long)(bytes[5] & 0xFF) << 16) |
			((long)(bytes[6] & 0xFF) << 8)  |
			(bytes[7] & 0xFF);
	}

	public static int fromBytesToInt(byte[] bytes) {
		if (bytes == null || bytes.length != 4) {
			throw new IllegalArgumentException("Expected 4 bytes for int");
		}
		return
			((bytes[0] & 0xFF) << 24) |
			((bytes[1] & 0xFF) << 16) |
			((bytes[2] & 0xFF) << 8)  |
			(bytes[3] & 0xFF);
	}

	public static short fromBytesToShort(byte[] bytes) {
		if (bytes == null || bytes.length != 2) {
			throw new IllegalArgumentException("Expected 2 bytes for short");
		}
		return (short)(((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF));
	}

	public static char fromBytesToChar(byte[] bytes) {
		if (bytes == null || bytes.length != 2) {
			throw new IllegalArgumentException("Expected 2 bytes for char");
		}
		return (char)(((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF));
	}

	public static byte fromBytesToByte(byte[] bytes) {
		if (bytes == null || bytes.length != 1) {
			throw new IllegalArgumentException("Expected 1 byte");
		}
		return bytes[0];
	}

	public static boolean fromBytesToBoolean(byte[] bytes) {
		if (bytes == null || bytes.length != 1) {
			throw new IllegalArgumentException("Expected 1 byte for boolean");
		}
		return bytes[0] != 0;
	}

	public static byte[] toBytes(double d) {
		return toBytes(Double.doubleToLongBits(d));
	}

	public static byte[] toBytes(float f) {
		return toBytes(Float.floatToIntBits(f));
	}

	public static byte[] toBytes(String str) {
		Objects.requireNonNull(str, "str is null");
		return str.getBytes(StandardCharsets.UTF_8);
	}

	public static byte[] toBytes(long l) {
		return new byte[] {
			(byte)(l >>> 56 & 0xFF),
			(byte)(l >>> 48 & 0xFF),
			(byte)(l >>> 40 & 0xFF),
			(byte)(l >>> 32 & 0xFF),
			(byte)(l >>> 24 & 0xFF),
			(byte)(l >>> 16 & 0xFF),
			(byte)(l >>> 8 & 0xFF),
			(byte)(l & 0xFF)
		};
	}

	public static byte[] toBytes(int i) {
		return new byte[] {
			(byte)(i >>> 24 & 0xFF),
			(byte)(i >>> 16 & 0xFF),
			(byte)(i >>> 8 & 0xFF),
			(byte)(i & 0xFF)
		};
	}

	public static byte[] toBytes(char c) {
		return toBytes((short)c);
	}

	public static byte[] toBytes(short s) {
		return new byte[] {
			(byte)(s >>> 8 & 0xFF),
			(byte)(s & 0xFF)
		};
	}

	public static byte[] toBytes(byte b) {
		return new byte[] { b };
	}

	public static byte[] toBytes(boolean bool) {
		return new byte[] { (byte)(bool ? 1 : 0) };
	}

}
