package dev.dstruct.inmemory;

import dev.dstruct.Result;
import dev.dstruct.Result.EmptyResult;
import dev.dstruct.Result.Error;
import dev.dstruct.Result.Ok;
import dev.dstruct.Result.Results;
import dev.dstruct.command.Command;
import dev.dstruct.command.Command.Batch;
import dev.dstruct.command.Command.Cast;
import dev.dstruct.command.Command.Del;
import dev.dstruct.command.Command.LIndex;
import dev.dstruct.command.Command.LLen;
import dev.dstruct.command.Command.LPop;
import dev.dstruct.command.Command.LPush;
import dev.dstruct.command.Command.MDelete;
import dev.dstruct.command.Command.MGet;
import dev.dstruct.command.Command.MPut;
import dev.dstruct.command.Command.Ping;
import dev.dstruct.command.Command.RPop;
import dev.dstruct.command.Command.RPush;
import dev.dstruct.command.Command.SAdd;
import dev.dstruct.command.Command.SMembers;
import dev.dstruct.command.Command.SRem;
import dev.dstruct.command.Command.Type;
import dev.dstruct.command.Command.VDelete;
import dev.dstruct.command.Command.VGet;
import dev.dstruct.command.Command.VSet;
import dev.dstruct.command.Command.Visitor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static dev.dstruct.inmemory.KeyFactory.newKey;
import static dev.dstruct.util.Binaries.fromBytesToBoolean;
import static dev.dstruct.util.Binaries.fromBytesToDouble;
import static dev.dstruct.util.Binaries.fromBytesToInt;
import static dev.dstruct.util.Binaries.toBytes;

class DataStructureVisitor implements Visitor<Result> {

	enum KeyType {
		MAP, SET, DEQUE, VALUE, NOTHING
	}

	private final Map<String, Set<Key>> setStore = new HashMap<>();
	private final Map<String, Map<Key, byte[]>> mapStore = new HashMap<>();
	private final Map<String, ArrayDeque<byte[]>> dequeStore = new HashMap<>();
	private final Map<String, byte[]> valueStore = new HashMap<>();
	private final Map<String, KeyType> keyTypeMap = new HashMap<>();

	private boolean notValidType(String name, KeyType keyType) {
		KeyType kt = keyTypeMap.getOrDefault(name, KeyType.NOTHING);
		return kt != keyType && kt != KeyType.NOTHING;
	}

	@Override
	public Result visitMPutCommand(MPut command) {
		Objects.requireNonNull(command, "command is null");
		Objects.requireNonNull(command.name(), "command.name is null");
		Objects.requireNonNull(command.key(), "command.key is null");
		Objects.requireNonNull(command.value(), "command.value is null");

		if (notValidType(command.name(), KeyType.MAP)) return new Error("type key mismatch");

		mapStore
			.computeIfAbsent(command.name(), this::newMapStore)
			.put(newKey(command.key()), command.value());

		return EmptyResult.OK;
	}

	@Override
	public Result visitMDeleteCommand(MDelete command) {
		Objects.requireNonNull(command, "command is null");
		Objects.requireNonNull(command.name(), "command.name is null");
		Objects.requireNonNull(command.key(), "command.key is null");

		if (notValidType(command.name(), KeyType.MAP)) return new Error("type key mismatch");

		Map<Key, byte[]> map = mapStore.get(command.name());
		if (map == null || map.remove(newKey(command.key())) == null) return EmptyResult.NOTHING;
		return EmptyResult.OK;
	}

	@Override
	public Result visitMGetCommand(MGet command) {
		Objects.requireNonNull(command, "command is null");
		Objects.requireNonNull(command.name(), "command.name is null");
		Objects.requireNonNull(command.key(), "command.key is null");

		if (notValidType(command.name(), KeyType.MAP)) return new Error("type key mismatch");

		Map<Key, byte[]> map = mapStore.get(command.name());
		if (map != null) {
			byte[] bytes = map.get(newKey(command.key()));
			if (bytes == null) return EmptyResult.NOTHING;
			return new Ok(bytes);
		}
		return EmptyResult.NOTHING;
	}

	@Override
	public Result visitVSetCommand(VSet command) {
		Objects.requireNonNull(command, "command is null");
		Objects.requireNonNull(command.name(), "command.name is null");
		Objects.requireNonNull(command.value(), "command.value is null");

		if (notValidType(command.name(), KeyType.VALUE)) return new Error("type key mismatch");

		if (valueStore.put(command.name(), command.value()) == null) {
			keyTypeMap.put(command.name(), KeyType.VALUE);
		}

		return EmptyResult.OK;
	}

	@Override
	public Result visitVDeleteCommand(VDelete command) {
		Objects.requireNonNull(command, "command is null");
		Objects.requireNonNull(command.name(), "command.name is null");
		if (notValidType(command.name(), KeyType.VALUE)) return new Error("type key mismatch");
		if (valueStore.remove(command.name()) == null) return EmptyResult.NOTHING;
		keyTypeMap.remove(command.name());
		return EmptyResult.OK;
	}

	@Override
	public Result visitVGetCommand(VGet command) {
		if (notValidType(command.name(), KeyType.VALUE)) return new Error("type key mismatch");
		byte[] bytes = valueStore.get(command.name());
		if (bytes == null) {
			return EmptyResult.NOTHING;
		}
		else {
			return new Ok(bytes);
		}
	}

	@Override
	public Result visitLPushCommand(LPush command) {
		Objects.requireNonNull(command, "command is null");
		Objects.requireNonNull(command.name(), "command.name is null");
		Objects.requireNonNull(command.value(), "command.value is null");
		if (notValidType(command.name(), KeyType.DEQUE)) return new Error("type key mismatch");
		dequeStore
			.computeIfAbsent(command.name(), this::newDeque)
			.push(command.value());
		return EmptyResult.OK;
	}



	@Override
	public Result visitLPopCommand(LPop command) {
		Objects.requireNonNull(command, "command is null");
		Objects.requireNonNull(command.name(), "command.name is null");
		if (notValidType(command.name(), KeyType.DEQUE)) return new Error("type key mismatch");
		Deque<byte[]> deque = dequeStore.get(command.name());
		if (deque == null || deque.isEmpty()) return EmptyResult.NOTHING;
		else return new Ok(deque.poll());
	}

	@Override
	public Result visitRPushCommand(RPush command) {
		Objects.requireNonNull(command, "command is null");
		Objects.requireNonNull(command.name(), "command.name is null");
		Objects.requireNonNull(command.value(), "command.value is null");
		if (notValidType(command.name(), KeyType.DEQUE)) return new Error("type key mismatch");
		dequeStore
			.computeIfAbsent(command.name(), this::newDeque)
			.offer(command.value());
		return EmptyResult.OK;
	}

	@Override
	public Result visitRPopCommand(RPop command) {
		Objects.requireNonNull(command, "command is null");
		Objects.requireNonNull(command.name(), "command.name is null");
		if (notValidType(command.name(), KeyType.DEQUE)) return new Error("type key mismatch");
		Deque<byte[]> deque = dequeStore.get(command.name());
		if (deque == null || deque.isEmpty()) return EmptyResult.NOTHING;
		else return new Ok(deque.pollLast());
	}

	@Override
	public Result visitLLenCommand(LLen command) {
		Objects.requireNonNull(command, "command is null");
		Objects.requireNonNull(command.name(), "command.name is null");
		if (notValidType(command.name(), KeyType.DEQUE)) return new Error("type key mismatch");
		Deque<byte[]> deque = dequeStore.get(command.name());
		if (deque == null) return EmptyResult.NOTHING;
		else return new Ok(toBytes(deque.size()));
	}

	@Override
	public Result visitLIndexCommand(LIndex command) {
		Objects.requireNonNull(command, "command is null");
		Objects.requireNonNull(command.name(), "command.name is null");
		Objects.requireNonNull(command.index(), "command.index is null");
		if (notValidType(command.name(), KeyType.DEQUE)) return new Error("type key mismatch");
		Deque<byte[]> deque = dequeStore.get(command.name());
		if (deque == null || deque.isEmpty()) return EmptyResult.NOTHING;
		int index = fromBytesToInt(command.index());
		Iterator<byte[]> iterator;
		if (index < 0) {
			index += deque.size();
			iterator = deque.descendingIterator();
		}
		else {
			iterator = deque.iterator();
		}

		while (iterator.hasNext() && (index--) > 0) {
			iterator.next();
		}
		if (index == -1 && iterator.hasNext()) {
			return new Ok(iterator.next());
		}
		else {
			return EmptyResult.NOTHING;
		}
	}

	@Override
	public Result visitSAddCommand(SAdd command) {
		Objects.requireNonNull(command, "command is null");
		Objects.requireNonNull(command.name(), "command.name is null");
		Objects.requireNonNull(command.value(), "command.value is null");
		if (notValidType(command.name(), KeyType.SET)) return new Error("type key mismatch");
		setStore
			.computeIfAbsent(command.name(), this::newSet)
			.add(newKey(command.value()));
		return EmptyResult.OK;
	}

	@Override
	public Result visitSRemCommand(SRem command) {
		Objects.requireNonNull(command, "command is null");
		Objects.requireNonNull(command.name(), "command.name is null");
		Objects.requireNonNull(command.value(), "command.value is null");
		if (notValidType(command.name(), KeyType.SET)) return new Error("type key mismatch");
		Set<Key> set = setStore.get(command.name());
		if (set == null || !set.remove(newKey(command.value()))) return EmptyResult.NOTHING;
		return EmptyResult.OK;
	}

	@Override
	public Result visitSMembersCommand(SMembers command) {
		Objects.requireNonNull(command, "command is null");
		Objects.requireNonNull(command.name(), "command.name is null");
		if (notValidType(command.name(), KeyType.SET)) return new Error("type key mismatch");
		Set<Key> set = setStore.get(command.name());
		if (set == null) return EmptyResult.NOTHING;
		List<Result> results = new ArrayList<>(set.size());
		for (Key key : set) {
			results.add(new Ok(key.toBytes()));
		}
		return new Results(results);
	}

	@Override
	public Result visitDelCommand(Del command) {
		Objects.requireNonNull(command, "command is null");
		String name = command.name();
		Objects.requireNonNull(name, "command.name is null");
		KeyType keyType = keyTypeMap.remove(name);
		if (keyType == null) return EmptyResult.NOTHING;
		switch (keyType) {
			case MAP -> mapStore.remove(name);
			case SET -> setStore.remove(name);
			case DEQUE -> dequeStore.remove(name);
			case VALUE -> valueStore.remove(name);
			case NOTHING -> {}
		}
		return EmptyResult.OK;
	}

	@Override
	public Result visitTypeCommand(Type command) {
		Objects.requireNonNull(command, "command is null");
		String name = command.name();
		Objects.requireNonNull(name, "command.name is null");
		KeyType keyType = keyTypeMap.get(name);
		if (keyType == null) return EmptyResult.NOTHING;
		return new Ok(keyType.name());
	}

	@Override
	public Result visitBatchCommand(Batch command) {
		Objects.requireNonNull(command, "command is null");
		List<Command> commands = command.commands();
		Objects.requireNonNull(commands, "command.name is null");
		if (commands.isEmpty()) {
			return EmptyResult.NOTHING;
		}
		List<Result> results = new ArrayList<>(commands.size());
		for (Command c : commands) {
			results.add(c.accept(this));
		}
		return new Results(results);
	}

	@Override
	public Result visitCastCommand(Cast command) {
		Objects.requireNonNull(command, "command is null");
		Objects.requireNonNull(command.command(), "command.command is null");
		if (command.type() < 0 || command.type() > 4) {
			throw new IllegalStateException("type not supported" + command.type());
		}
		Result result = command.command().accept(this);
		if (result instanceof Ok(byte[] value)) {
			return switch (command.type()) {
				case 1 -> new Ok(Integer.toString(fromBytesToInt(value)));
				case 2 -> new Ok(Boolean.toString(fromBytesToBoolean(value)));
				case 3 -> new Ok(Double.toString(fromBytesToDouble(value)));
				default -> new Ok(value);
			};
		}
		return result;
	}

	@Override
	public Result visitPingCommand(Ping command) {
		Objects.requireNonNull(command, "command is null");
		if (command.message() == null) {
			return new Ok("PONG");
		}
		else {
			return new Ok(command.message());
		}
	}

	private Set<Key> newSet(String key) {
		keyTypeMap.put(key, KeyType.SET);
		return HashSet.newHashSet(32);
	}

	private ArrayDeque<byte[]> newDeque(String key) {
		keyTypeMap.put(key, KeyType.DEQUE);
		return new ArrayDeque<>(32);
	}

	private Map<Key, byte[]> newMapStore(String key) {
		keyTypeMap.put(key, KeyType.MAP);
		return HashMap.newHashMap(32);
	}

}
