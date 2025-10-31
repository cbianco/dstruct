package dev.dstruct.wal;

import dev.dstruct.command.Command;
import dev.dstruct.command.Serde;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.function.Consumer;

public final class WALStore implements AutoCloseable {

	private static final int MAGIC = 0x44535452;  // "DSTR" in hex
	private static final int VERSION = 1;

	private final Path dstructPath;
	private final SyncPolicy syncPolicy;
	private final int batchSize;
	private final long syncIntervalMs;
	private FileChannel appendChannel;
	private FileChannel readChannel;

	private int writesSinceLastSync = 0;
	private long lastSyncTime = System.currentTimeMillis();

	public WALStore(Path dstructPath, SyncPolicy syncPolicy) {
		this(dstructPath, syncPolicy, 0, 0);
	}

	public WALStore(Path dstructPath, SyncPolicy syncPolicy, int batchSize, long syncIntervalMs) {
		this.dstructPath = dstructPath;
		this.syncPolicy = syncPolicy;
		this.batchSize = batchSize;
		this.syncIntervalMs = syncIntervalMs;
	}

	public void save(Command command) {
		try {
			ByteBuffer byteBuffer = Serde.serialize(command);
			if (byteBuffer != null) {
				appendChannel.write(byteBuffer);
				if (syncPolicy == SyncPolicy.ALWAYS) {
					appendChannel.force(true);
				}
				else if (syncPolicy == SyncPolicy.BATCHED) {
					if (++writesSinceLastSync >= batchSize) {
						appendChannel.force(true);
						writesSinceLastSync = 0;
					}
					else {
						long now = System.currentTimeMillis();
						if (now - lastSyncTime >= syncIntervalMs) {
							appendChannel.force(true);
							lastSyncTime = now;
							writesSinceLastSync = 0;
						}
					}

				}
			}
		}
		catch (Exception e) {
			throw new WalException(e);
		}
	}

	public void start() throws IOException {

		Path walfile = dstructPath.resolve("commands");

		if (Files.notExists(walfile)) {
			Files.createDirectories(walfile.getParent());
			Files.createFile(walfile);

			this.readChannel = FileChannel.open(walfile, Set.of(StandardOpenOption.READ));
			this.appendChannel = FileChannel.open(walfile, Set.of(StandardOpenOption.CREATE, StandardOpenOption.APPEND));

			ByteBuffer header = ByteBuffer.allocateDirect(8);
			header.putInt(MAGIC);
			header.putInt(VERSION);
			header.flip();
			appendChannel.write(header);
			appendChannel.force(true);
			readChannel.position(8);
		}
		else {

			this.readChannel = FileChannel.open(walfile, Set.of(StandardOpenOption.READ));
			this.appendChannel = FileChannel.open(walfile, Set.of(StandardOpenOption.CREATE, StandardOpenOption.APPEND));

			ByteBuffer header = ByteBuffer.allocateDirect(8);
			readChannel.read(header);
			header.flip();

			int magic = header.getInt();
			int version = header.getInt();

			if (magic != MAGIC) {
				throw new IOException("Invalid WAL file format");
			}
			if (version != VERSION) {
				throw new IOException("Unsupported WAL version: " + version);
			}
		}
	}

	public void start(Consumer<Command> commandConsumer) throws IOException {
		start();
		Command command;
		BufferedChannel bufferedChannel = new BufferedChannel(readChannel, 8192);
		while ((command = Serde.deserialize(bufferedChannel)) != null) {
			commandConsumer.accept(command);
		}
	}

	@Override
	public void close() throws IOException {
		if (syncPolicy != SyncPolicy.ALWAYS) {
			flush();
		}
		try (
			var a1 = appendChannel;
			var a2 = readChannel
		) {}
	}

	public void flush() {
		if (appendChannel == null) return;
		try {
			appendChannel.force(true);
			writesSinceLastSync = 0;
		} catch (Exception e) {
			throw new WalException("WAL flush failed", e);
		}
	}


}
