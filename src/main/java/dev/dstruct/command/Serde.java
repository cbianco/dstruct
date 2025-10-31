package dev.dstruct.command;

import dev.dstruct.wal.BufferedChannel;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import dev.dstruct.command.Command.MPut;
import dev.dstruct.command.Command.MDelete;
import dev.dstruct.command.Command.MGet;
import dev.dstruct.command.Command.VSet;
import dev.dstruct.command.Command.VDelete;
import dev.dstruct.command.Command.LPush;
import dev.dstruct.command.Command.LPop;
import dev.dstruct.command.Command.RPush;
import dev.dstruct.command.Command.RPop;
import dev.dstruct.command.Command.SAdd;
import dev.dstruct.command.Command.SRem;
import dev.dstruct.command.Command.Del;
import dev.dstruct.command.Command.Batch;

/* generated at 2025-10-30T13:52:51.817190Z */
public final class Serde {

	public static short type(Command command) {
		return switch(command) {
			case MPut ignore -> 1;
			case MDelete ignore -> 2;
			case MGet ignore -> 3;
			case VSet ignore -> 4;
			case VDelete ignore -> 5;
			case LPush ignore -> 6;
			case LPop ignore -> 7;
			case RPush ignore -> 8;
			case RPop ignore -> 9;
			case SAdd ignore -> 10;
			case SRem ignore -> 11;
			case Del ignore -> 12;
			case Batch ignore -> 13;
			default -> -1;
		};
	}
	public static Command deserialize(BufferedChannel bufferedChannel) throws IOException {
		if (!bufferedChannel.hasRemaining()) return null;
		short type = bufferedChannel.getShort();
		return switch (type) {
			case 1 -> deserializeMPut(bufferedChannel);
			case 2 -> deserializeMDelete(bufferedChannel);
			case 3 -> deserializeMGet(bufferedChannel);
			case 4 -> deserializeVSet(bufferedChannel);
			case 5 -> deserializeVDelete(bufferedChannel);
			case 6 -> deserializeLPush(bufferedChannel);
			case 7 -> deserializeLPop(bufferedChannel);
			case 8 -> deserializeRPush(bufferedChannel);
			case 9 -> deserializeRPop(bufferedChannel);
			case 10 -> deserializeSAdd(bufferedChannel);
			case 11 -> deserializeSRem(bufferedChannel);
			case 12 -> deserializeDel(bufferedChannel);
			default -> null;
		};
	}
	public static ByteBuffer serialize(Command command) {
		if (command == null) return null;
		return switch (command) {
			case MPut a -> serializeMPut(a);
			case MDelete a -> serializeMDelete(a);
			case MGet a -> serializeMGet(a);
			case VSet a -> serializeVSet(a);
			case VDelete a -> serializeVDelete(a);
			case LPush a -> serializeLPush(a);
			case LPop a -> serializeLPop(a);
			case RPush a -> serializeRPush(a);
			case RPop a -> serializeRPop(a);
			case SAdd a -> serializeSAdd(a);
			case SRem a -> serializeSRem(a);
			case Del a -> serializeDel(a);
			case Batch a -> serializeBatch(a);
			default -> null;
		};
	}
	private static ByteBuffer serializeMPut(MPut c) {
		short type = type(c);
		String name = c.name();
		int nameLength = name == null ? 0 : name.length();
		int nameByteLength = Integer.BYTES + nameLength;
		byte[] key = c.key();
		int keyLength = key == null ? 0 : key.length;
		int keyByteLength = Integer.BYTES + keyLength;
		byte[] value = c.value();
		int valueLength = value == null ? 0 : value.length;
		int valueByteLength = Integer.BYTES + valueLength;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Short.BYTES + nameByteLength + keyByteLength + valueByteLength);
		byteBuffer.putShort(type);
		byteBuffer.putInt(nameLength);
		if (nameLength > 0) byteBuffer.put(name.getBytes(StandardCharsets.UTF_8));
		byteBuffer.putInt(keyLength);
		if (keyLength > 0) byteBuffer.put(key);
		byteBuffer.putInt(valueLength);
		if (valueLength > 0) byteBuffer.put(value);
		return byteBuffer.flip();
	}
	private static MPut deserializeMPut(BufferedChannel bufferedChannel) throws IOException {
		if (bufferedChannel == null) return null;
		byte[] bytes;
		if (!bufferedChannel.hasRemaining()) return null;
		int nameLength = bufferedChannel.getInt();
		String name = null;
		if (nameLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[nameLength];
			bufferedChannel.get(bytes);
			name = new String(bytes, StandardCharsets.UTF_8);
		}
		if (!bufferedChannel.hasRemaining()) return null;
		int keyLength = bufferedChannel.getInt();
		byte[] key = null;
		if (keyLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[keyLength];
			bufferedChannel.get(bytes);
			key = bytes;
		}
		if (!bufferedChannel.hasRemaining()) return null;
		int valueLength = bufferedChannel.getInt();
		byte[] value = null;
		if (valueLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[valueLength];
			bufferedChannel.get(bytes);
			value = bytes;
		}
		return new MPut(name, key, value);
	}
	private static ByteBuffer serializeMDelete(MDelete c) {
		short type = type(c);
		String name = c.name();
		int nameLength = name == null ? 0 : name.length();
		int nameByteLength = Integer.BYTES + nameLength;
		byte[] key = c.key();
		int keyLength = key == null ? 0 : key.length;
		int keyByteLength = Integer.BYTES + keyLength;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Short.BYTES + nameByteLength + keyByteLength);
		byteBuffer.putShort(type);
		byteBuffer.putInt(nameLength);
		if (nameLength > 0) byteBuffer.put(name.getBytes(StandardCharsets.UTF_8));
		byteBuffer.putInt(keyLength);
		if (keyLength > 0) byteBuffer.put(key);
		return byteBuffer.flip();
	}
	private static MDelete deserializeMDelete(BufferedChannel bufferedChannel) throws IOException {
		if (bufferedChannel == null) return null;
		byte[] bytes;
		if (!bufferedChannel.hasRemaining()) return null;
		int nameLength = bufferedChannel.getInt();
		String name = null;
		if (nameLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[nameLength];
			bufferedChannel.get(bytes);
			name = new String(bytes, StandardCharsets.UTF_8);
		}
		if (!bufferedChannel.hasRemaining()) return null;
		int keyLength = bufferedChannel.getInt();
		byte[] key = null;
		if (keyLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[keyLength];
			bufferedChannel.get(bytes);
			key = bytes;
		}
		return new MDelete(name, key);
	}
	private static ByteBuffer serializeMGet(MGet c) {
		short type = type(c);
		String name = c.name();
		int nameLength = name == null ? 0 : name.length();
		int nameByteLength = Integer.BYTES + nameLength;
		byte[] key = c.key();
		int keyLength = key == null ? 0 : key.length;
		int keyByteLength = Integer.BYTES + keyLength;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Short.BYTES + nameByteLength + keyByteLength);
		byteBuffer.putShort(type);
		byteBuffer.putInt(nameLength);
		if (nameLength > 0) byteBuffer.put(name.getBytes(StandardCharsets.UTF_8));
		byteBuffer.putInt(keyLength);
		if (keyLength > 0) byteBuffer.put(key);
		return byteBuffer.flip();
	}
	private static MGet deserializeMGet(BufferedChannel bufferedChannel) throws IOException {
		if (bufferedChannel == null) return null;
		byte[] bytes;
		if (!bufferedChannel.hasRemaining()) return null;
		int nameLength = bufferedChannel.getInt();
		String name = null;
		if (nameLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[nameLength];
			bufferedChannel.get(bytes);
			name = new String(bytes, StandardCharsets.UTF_8);
		}
		if (!bufferedChannel.hasRemaining()) return null;
		int keyLength = bufferedChannel.getInt();
		byte[] key = null;
		if (keyLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[keyLength];
			bufferedChannel.get(bytes);
			key = bytes;
		}
		return new MGet(name, key);
	}
	private static ByteBuffer serializeVSet(VSet c) {
		short type = type(c);
		String name = c.name();
		int nameLength = name == null ? 0 : name.length();
		int nameByteLength = Integer.BYTES + nameLength;
		byte[] value = c.value();
		int valueLength = value == null ? 0 : value.length;
		int valueByteLength = Integer.BYTES + valueLength;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Short.BYTES + nameByteLength + valueByteLength);
		byteBuffer.putShort(type);
		byteBuffer.putInt(nameLength);
		if (nameLength > 0) byteBuffer.put(name.getBytes(StandardCharsets.UTF_8));
		byteBuffer.putInt(valueLength);
		if (valueLength > 0) byteBuffer.put(value);
		return byteBuffer.flip();
	}
	private static VSet deserializeVSet(BufferedChannel bufferedChannel) throws IOException {
		if (bufferedChannel == null) return null;
		byte[] bytes;
		if (!bufferedChannel.hasRemaining()) return null;
		int nameLength = bufferedChannel.getInt();
		String name = null;
		if (nameLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[nameLength];
			bufferedChannel.get(bytes);
			name = new String(bytes, StandardCharsets.UTF_8);
		}
		if (!bufferedChannel.hasRemaining()) return null;
		int valueLength = bufferedChannel.getInt();
		byte[] value = null;
		if (valueLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[valueLength];
			bufferedChannel.get(bytes);
			value = bytes;
		}
		return new VSet(name, value);
	}
	private static ByteBuffer serializeVDelete(VDelete c) {
		short type = type(c);
		String name = c.name();
		int nameLength = name == null ? 0 : name.length();
		int nameByteLength = Integer.BYTES + nameLength;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Short.BYTES + nameByteLength);
		byteBuffer.putShort(type);
		byteBuffer.putInt(nameLength);
		if (nameLength > 0) byteBuffer.put(name.getBytes(StandardCharsets.UTF_8));
		return byteBuffer.flip();
	}
	private static VDelete deserializeVDelete(BufferedChannel bufferedChannel) throws IOException {
		if (bufferedChannel == null) return null;
		byte[] bytes;
		if (!bufferedChannel.hasRemaining()) return null;
		int nameLength = bufferedChannel.getInt();
		String name = null;
		if (nameLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[nameLength];
			bufferedChannel.get(bytes);
			name = new String(bytes, StandardCharsets.UTF_8);
		}
		return new VDelete(name);
	}
	private static ByteBuffer serializeLPush(LPush c) {
		short type = type(c);
		String name = c.name();
		int nameLength = name == null ? 0 : name.length();
		int nameByteLength = Integer.BYTES + nameLength;
		byte[] value = c.value();
		int valueLength = value == null ? 0 : value.length;
		int valueByteLength = Integer.BYTES + valueLength;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Short.BYTES + nameByteLength + valueByteLength);
		byteBuffer.putShort(type);
		byteBuffer.putInt(nameLength);
		if (nameLength > 0) byteBuffer.put(name.getBytes(StandardCharsets.UTF_8));
		byteBuffer.putInt(valueLength);
		if (valueLength > 0) byteBuffer.put(value);
		return byteBuffer.flip();
	}
	private static LPush deserializeLPush(BufferedChannel bufferedChannel) throws IOException {
		if (bufferedChannel == null) return null;
		byte[] bytes;
		if (!bufferedChannel.hasRemaining()) return null;
		int nameLength = bufferedChannel.getInt();
		String name = null;
		if (nameLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[nameLength];
			bufferedChannel.get(bytes);
			name = new String(bytes, StandardCharsets.UTF_8);
		}
		if (!bufferedChannel.hasRemaining()) return null;
		int valueLength = bufferedChannel.getInt();
		byte[] value = null;
		if (valueLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[valueLength];
			bufferedChannel.get(bytes);
			value = bytes;
		}
		return new LPush(name, value);
	}
	private static ByteBuffer serializeLPop(LPop c) {
		short type = type(c);
		String name = c.name();
		int nameLength = name == null ? 0 : name.length();
		int nameByteLength = Integer.BYTES + nameLength;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Short.BYTES + nameByteLength);
		byteBuffer.putShort(type);
		byteBuffer.putInt(nameLength);
		if (nameLength > 0) byteBuffer.put(name.getBytes(StandardCharsets.UTF_8));
		return byteBuffer.flip();
	}
	private static LPop deserializeLPop(BufferedChannel bufferedChannel) throws IOException {
		if (bufferedChannel == null) return null;
		byte[] bytes;
		if (!bufferedChannel.hasRemaining()) return null;
		int nameLength = bufferedChannel.getInt();
		String name = null;
		if (nameLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[nameLength];
			bufferedChannel.get(bytes);
			name = new String(bytes, StandardCharsets.UTF_8);
		}
		return new LPop(name);
	}
	private static ByteBuffer serializeRPush(RPush c) {
		short type = type(c);
		String name = c.name();
		int nameLength = name == null ? 0 : name.length();
		int nameByteLength = Integer.BYTES + nameLength;
		byte[] value = c.value();
		int valueLength = value == null ? 0 : value.length;
		int valueByteLength = Integer.BYTES + valueLength;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Short.BYTES + nameByteLength + valueByteLength);
		byteBuffer.putShort(type);
		byteBuffer.putInt(nameLength);
		if (nameLength > 0) byteBuffer.put(name.getBytes(StandardCharsets.UTF_8));
		byteBuffer.putInt(valueLength);
		if (valueLength > 0) byteBuffer.put(value);
		return byteBuffer.flip();
	}
	private static RPush deserializeRPush(BufferedChannel bufferedChannel) throws IOException {
		if (bufferedChannel == null) return null;
		byte[] bytes;
		if (!bufferedChannel.hasRemaining()) return null;
		int nameLength = bufferedChannel.getInt();
		String name = null;
		if (nameLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[nameLength];
			bufferedChannel.get(bytes);
			name = new String(bytes, StandardCharsets.UTF_8);
		}
		if (!bufferedChannel.hasRemaining()) return null;
		int valueLength = bufferedChannel.getInt();
		byte[] value = null;
		if (valueLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[valueLength];
			bufferedChannel.get(bytes);
			value = bytes;
		}
		return new RPush(name, value);
	}
	private static ByteBuffer serializeRPop(RPop c) {
		short type = type(c);
		String name = c.name();
		int nameLength = name == null ? 0 : name.length();
		int nameByteLength = Integer.BYTES + nameLength;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Short.BYTES + nameByteLength);
		byteBuffer.putShort(type);
		byteBuffer.putInt(nameLength);
		if (nameLength > 0) byteBuffer.put(name.getBytes(StandardCharsets.UTF_8));
		return byteBuffer.flip();
	}
	private static RPop deserializeRPop(BufferedChannel bufferedChannel) throws IOException {
		if (bufferedChannel == null) return null;
		byte[] bytes;
		if (!bufferedChannel.hasRemaining()) return null;
		int nameLength = bufferedChannel.getInt();
		String name = null;
		if (nameLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[nameLength];
			bufferedChannel.get(bytes);
			name = new String(bytes, StandardCharsets.UTF_8);
		}
		return new RPop(name);
	}
	private static ByteBuffer serializeSAdd(SAdd c) {
		short type = type(c);
		String name = c.name();
		int nameLength = name == null ? 0 : name.length();
		int nameByteLength = Integer.BYTES + nameLength;
		byte[] value = c.value();
		int valueLength = value == null ? 0 : value.length;
		int valueByteLength = Integer.BYTES + valueLength;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Short.BYTES + nameByteLength + valueByteLength);
		byteBuffer.putShort(type);
		byteBuffer.putInt(nameLength);
		if (nameLength > 0) byteBuffer.put(name.getBytes(StandardCharsets.UTF_8));
		byteBuffer.putInt(valueLength);
		if (valueLength > 0) byteBuffer.put(value);
		return byteBuffer.flip();
	}
	private static SAdd deserializeSAdd(BufferedChannel bufferedChannel) throws IOException {
		if (bufferedChannel == null) return null;
		byte[] bytes;
		if (!bufferedChannel.hasRemaining()) return null;
		int nameLength = bufferedChannel.getInt();
		String name = null;
		if (nameLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[nameLength];
			bufferedChannel.get(bytes);
			name = new String(bytes, StandardCharsets.UTF_8);
		}
		if (!bufferedChannel.hasRemaining()) return null;
		int valueLength = bufferedChannel.getInt();
		byte[] value = null;
		if (valueLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[valueLength];
			bufferedChannel.get(bytes);
			value = bytes;
		}
		return new SAdd(name, value);
	}
	private static ByteBuffer serializeSRem(SRem c) {
		short type = type(c);
		String name = c.name();
		int nameLength = name == null ? 0 : name.length();
		int nameByteLength = Integer.BYTES + nameLength;
		byte[] value = c.value();
		int valueLength = value == null ? 0 : value.length;
		int valueByteLength = Integer.BYTES + valueLength;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Short.BYTES + nameByteLength + valueByteLength);
		byteBuffer.putShort(type);
		byteBuffer.putInt(nameLength);
		if (nameLength > 0) byteBuffer.put(name.getBytes(StandardCharsets.UTF_8));
		byteBuffer.putInt(valueLength);
		if (valueLength > 0) byteBuffer.put(value);
		return byteBuffer.flip();
	}
	private static SRem deserializeSRem(BufferedChannel bufferedChannel) throws IOException {
		if (bufferedChannel == null) return null;
		byte[] bytes;
		if (!bufferedChannel.hasRemaining()) return null;
		int nameLength = bufferedChannel.getInt();
		String name = null;
		if (nameLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[nameLength];
			bufferedChannel.get(bytes);
			name = new String(bytes, StandardCharsets.UTF_8);
		}
		if (!bufferedChannel.hasRemaining()) return null;
		int valueLength = bufferedChannel.getInt();
		byte[] value = null;
		if (valueLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[valueLength];
			bufferedChannel.get(bytes);
			value = bytes;
		}
		return new SRem(name, value);
	}
	private static ByteBuffer serializeDel(Del c) {
		short type = type(c);
		String name = c.name();
		int nameLength = name == null ? 0 : name.length();
		int nameByteLength = Integer.BYTES + nameLength;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Short.BYTES + nameByteLength);
		byteBuffer.putShort(type);
		byteBuffer.putInt(nameLength);
		if (nameLength > 0) byteBuffer.put(name.getBytes(StandardCharsets.UTF_8));
		return byteBuffer.flip();
	}
	private static Del deserializeDel(BufferedChannel bufferedChannel) throws IOException {
		if (bufferedChannel == null) return null;
		byte[] bytes;
		if (!bufferedChannel.hasRemaining()) return null;
		int nameLength = bufferedChannel.getInt();
		String name = null;
		if (nameLength > 0) {
			if (!bufferedChannel.hasRemaining()) return null;
			bytes = new byte[nameLength];
			bufferedChannel.get(bytes);
			name = new String(bytes, StandardCharsets.UTF_8);
		}
		return new Del(name);
	}
	private static ByteBuffer serializeBatch(Batch batch) {
		if (batch == null) return null;
		List<Command> commands = batch.commands();
		if (commands == null) return null;
		ByteBuffer[] byteBuffers = new ByteBuffer[commands.size()];
		int capacity = 0;
		for (int i = 0; i < commands.size(); i++) {
			ByteBuffer buffer = serialize(commands.get(i));
			byteBuffers[i] = buffer;
			capacity += buffer.capacity();
		}
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(capacity);
		for (ByteBuffer buffer : byteBuffers) {
			byteBuffer.put(buffer);
		}
		return byteBuffer.flip();
	}

}
