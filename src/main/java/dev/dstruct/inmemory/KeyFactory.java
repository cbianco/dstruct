package dev.dstruct.inmemory;

import dev.dstruct.inmemory.Key.ByteKey;
import dev.dstruct.inmemory.Key.Bytes;
import dev.dstruct.inmemory.Key.IntKey;
import dev.dstruct.inmemory.Key.LongKey;
import dev.dstruct.inmemory.Key.ShortKey;

import static dev.dstruct.util.Binaries.fromBytesToShort;

/**
 * @see Key
 */
final class KeyFactory {

	private static final ByteKey[] BYTE_CACHE;

	static {
		BYTE_CACHE = new ByteKey[256];
		for (int i = 0; i < 256; i++) {
			BYTE_CACHE[i] = new ByteKey((byte)i);
		}
	}

	static Key newKey(byte[] key) {
		if (key.length == 1) {
			return BYTE_CACHE[key[0] & 0xFF];
		}
		if (key.length == 2) {
			return new ShortKey(fromBytesToShort(key));
		}
		if (key.length <= 4) {
			int result = 0;
			for (byte b : key) {
				result = (result << 8) | (b & 0xFF);
			}
			return new IntKey(result);
		}
		if (key.length <= 8) {
			long result = 0;
			for (byte b : key) {
				result = (result << 8) | (b & 0xFF);
			}
			return new LongKey(result);
		}
		return new Bytes(key);
	}

}
