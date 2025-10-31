package dev.dstruct.inmemory;

import dev.dstruct.util.Binaries;
import java.util.Arrays;

/**
 * DStruct Key is an optimization of simple Key(byte[]).
 * <p>
 * In DStruct everything is a byte[], so to enable byte[] to be used as a key of and hashmaps,
 * a wrapper was created.
 * The optimization is in a primitive type conversion from byte[] to byte, short, int, long.
 * Why? In java without header optimization: enabled by following instruction: `-XX:+UseCompactObjectHeaders` (jdk 25).
 * <p>
 * the memory layout of record Key(byte[] bytes) {} is:
 *  - Object header: 12 bytes (8-byte mark word + 4-byte class pointer with compressed oops)
 *  - byte[] reference: 4 bytes (compressed oops)
 *  - Padding: 4 bytes (to align to 8-byte boundary)
 *  - Total: 24 bytes per Key instance
 * <p>
 * Plus the actual byte array object:
 * - Array object header: 16 bytes (12-byte object header + 4-byte array length)
 * - Array data: N bytes (actual byte content)
 * - Array padding: 0-7 bytes (to align to 8-byte boundary)
 * <p>
 * Total memory per Key = 24 + 16 + N + padding bytes
 *
 * <p><b>Memory comparison for different key types (without compact headers):</b>
 * <ul>
 *   <li><b>ByteKey(byte):</b> 16 bytes (12-byte header + 1 byte + 3 padding)
 *   <li><b>ShortKey(short):</b> 16 bytes (12-byte header + 2 bytes + 2 padding)
 *   <li><b>IntKey(int):</b> 16 bytes (12-byte header + 4 bytes)
 *   <li><b>LongKey(long):</b> 24 bytes (12-byte header + 8 bytes + 4 padding)
 *   <li><b>Bytes(byte[]):</b> 24 + 16 + N + padding bytes
 * </ul>
 *
 * <p><b>Memory comparison with `-XX:+UseCompactObjectHeaders` (JDK 25+):</b>
 * <ul>
 *   <li><b>ByteKey(byte):</b> 16 bytes (8-byte compact header + 1 byte + 7 padding)
 *   <li><b>ShortKey(short):</b> 16 bytes (8-byte compact header + 2 bytes + 6 padding)
 *   <li><b>IntKey(int):</b> 16 bytes (8-byte compact header + 4 bytes + 4 padding)
 *   <li><b>LongKey(long):</b> 16 bytes (8-byte compact header + 8 bytes)
 *   <li><b>Bytes(byte[]):</b> 16 + 16 + N + padding bytes (8-byte reduction per instance)
 * </ul>
 *
 * <p><b>Memory savings examples (without compact headers):</b>
 * <ul>
 *   <li>1-byte key: IntKey uses 16 bytes vs Bytes uses 40+ bytes
 *   <li>4-byte key: IntKey uses 16 bytes vs Bytes uses 44+ bytes
 *   <li>8-byte key: LongKey uses 24 bytes vs Bytes uses 48+ bytes
 * </ul>
 *
 * <p><b>Memory savings examples (with compact headers):</b>
 * <ul>
 *   <li>1-byte key: IntKey uses 16 bytes vs Bytes uses 32+ bytes
 *   <li>4-byte key: IntKey uses 16 bytes vs Bytes uses 36+ bytes
 *   <li>8-byte key: LongKey uses 16 bytes vs Bytes uses 40+ bytes
 * </ul>
 *
 * <p>This optimization is particularly effective for short ASCII strings with fewer than 8 characters,
 * numeric IDs, counters, and timestamps and so fort, which are common in database workloads. By storing primitive
 * types directly instead of byte arrays, we eliminate both the array object overhead and improve cache locality.
 * Compact object headers further improve memory efficiency across all key types.
 *
 */
sealed interface Key {

	byte[] toBytes();

	record ByteKey(byte key) implements Key {
		@Override
		public byte[] toBytes() {
			return Binaries.toBytes(this.key);
		}
	}

	record ShortKey(short key) implements Key {
		@Override
		public byte[] toBytes() {
			return Binaries.toBytes(this.key);
		}
	}

	record IntKey(int key) implements Key {
		@Override
		public byte[] toBytes() {
			return Binaries.toBytes(this.key);
		}
	}

	record LongKey(long key) implements Key {
		@Override
		public byte[] toBytes() {
			return Binaries.toBytes(this.key);
		}
	}

	record Bytes(byte[] key) implements Key {

		@Override
		public byte[] toBytes() {
			return key;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Bytes(byte[] other))) {
				return false;
			}
			return Arrays.equals(key, other);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(key);
		}
	}
}
