package dev.dstruct;

import dev.dstruct.Result.Error;
import dev.dstruct.command.Command;
import dev.dstruct.inmemory.InMemoryStore;
import dev.dstruct.logging.Log;
import dev.dstruct.logging.LogFactory;
import dev.dstruct.util.Process;
import dev.dstruct.util.Sink;
import dev.dstruct.wal.WALStore;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


/**
 * DStruct is a single threaded in memory database that manages communication through commands.
 * Command is a unique way to communicate. There are persistent command and read only command.
 *
 * Persistent commands are saved in a wal store (WAL write-ahead logging)
 * before being saved in memory.
 * At startup, DStruct reads a WAL store path and restores the in-memory data structures,
 * then the server is started.
 */
public class DStruct {

	private static final Log log = LogFactory.create(DStruct.class);

	private final EventLoop eventLoop;
	private final Process tcpServer;
	private final Options options;
	private final WALStore walStore;
	private final InMemoryStore inMemoryStore = new InMemoryStore();

	public DStruct() {
		this(new Options());
	}

	public DStruct(Options options) {
		this.options = options;
		this.eventLoop = new EventLoop(options.eventLoopThreadName);
		this.tcpServer = options.port == 0 ? Process.NOOP : new TcpServer(options.port, this::executeAsync);
		this.walStore = new WALStore(
			options.getDataDirectory(),
			options.syncPolicy,
			options.batchSize,
			options.syncIntervalMs
		);
	}

	public void execute(Command command) {
		eventLoop.offer(command, Sink.callback(c -> onCommand(c, false)));
	}

	public Result executeSync(Command command) throws Exception {
		return executeAsync(command).get(5, TimeUnit.SECONDS);
	}

	public CompletableFuture<Result> executeAsync(Command dsCommand) {
		CompletableFuture<Result> cf = new CompletableFuture<>();
		eventLoop.offer(
			dsCommand,
			Sink
				.callback(command -> {
					try {
						cf.complete(onCommand(command, false));
					}
					catch (Exception e) {
						cf.completeExceptionally(e);
					}
				}, cf::completeExceptionally)
		);
		return cf;
	}

	private Result onCommand(Command command, boolean startup) {
		try {
			if (options.writeAHeadLogging && !startup && command.isPersisted()) {
				walStore.save(command);
			}
			return inMemoryStore.manageCommand(command);
		}
		catch (Exception e) {
			log.error(e);
			return new Error(e.getMessage());
		}
	}

	public void start() throws Exception {
		Instant now = Instant.now();
		eventLoop.start();
		walStore.start(dsCommand -> onCommand(dsCommand, true));
		tcpServer.start();
		log.info("Server started in: " + Duration.between(now, Instant.now()));
	}

	public void stop() {
		try (
			var a1 = eventLoop;
			var a2 = tcpServer;
			var a3 = walStore
		) {}
		catch (Exception e) {
			log.error(e);
		}
	}

}
