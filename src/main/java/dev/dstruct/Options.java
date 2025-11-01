package dev.dstruct;

import dev.dstruct.util.Config;
import dev.dstruct.wal.SyncPolicy;
import java.nio.file.Path;
import java.nio.file.Paths;

class Options {

	int port = 4242;
	String eventLoopThreadName = "dstruct-event-loop";
	boolean writeAHeadLogging = true;
	String dataDirectory;
	SyncPolicy syncPolicy = SyncPolicy.BATCHED;
	int batchSize = 100;
	long syncIntervalMs = 1000;

	@Override
	public String toString() {
		return "Options{" +
			"tcpPort=" + port +
			", eventLoopThreadName='" + eventLoopThreadName + '\'' +
			", writeAHeadLogging=" + writeAHeadLogging +
			", dataDirectory='" + dataDirectory + '\'' +
			", syncPolicy=" + syncPolicy +
			", batchSize=" + batchSize +
			", syncIntervalMs=" + syncIntervalMs +
			'}';
	}

	Path getDataDirectory() {
		if (dataDirectory != null) {
			return Paths.get(dataDirectory);
		}
		String home = Config.resolve("user.home", null);
		return Paths.get(home, ".dstruct", "data");
	}

	static Options fromEnv() {
		Options options = new Options();

		options.port =
			Config.resolveInt(
				"dstruct.port",
				options.port
			);

		options.eventLoopThreadName =
			Config.resolve(
				"dstruct.event.loop.thread.name",
				options.eventLoopThreadName
			);

		options.writeAHeadLogging =
			Config.resolveBoolean(
				"dstruct.wal.enabled",
				options.writeAHeadLogging
			);

		options.dataDirectory =
			Config.resolve(
				"dstruct.data.dir",
				options.dataDirectory
			);

		String syncPolicy =
			Config.resolve(
				"dstruct.sync.policy",
				options.syncPolicy.name()
			);

		options.syncPolicy = SyncPolicy.valueOf(syncPolicy.toUpperCase());

		options.batchSize =
			Config.resolveInt(
				"dstruct.batch.size",
				options.batchSize
			);

		options.syncIntervalMs =
			Config.resolveLong(
				"dstruct.sync.interval.ms",
				options.syncIntervalMs
			);

		return options;
	}

}
