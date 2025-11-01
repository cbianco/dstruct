package dev.dstruct;

import dev.dstruct.wal.SyncPolicy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

class Options {

	int tcpPort = 4242;
	String eventLoopThreadName = "dstruct-event-loop";
	boolean writeAHeadLogging = true;
	String dataDirectory;
	SyncPolicy syncPolicy = SyncPolicy.BATCHED;
	int batchSize = 100;
	long syncIntervalMs = 1000;

	Path getDataDirectory() {
		if (dataDirectory != null) {
			return Paths.get(dataDirectory);
		}
		String home = System.getProperty("user.home");
		return Paths.get(home, ".dstruct", "data");
	}

	static Options fromEnv() {
		Map<String, String> env = System.getenv();
		Options options = new Options();

		options.tcpPort =
			getIntProperty(
				"dstruct.tcp.port",
				"DSTRUCT_TCP_PORT",
				env,
				options.tcpPort
			);

		options.eventLoopThreadName =
			getStringProperty(
				"dstruct.event.loop.thread.name",
				"DSTRUCT_EVENT_LOOP_THREAD_NAME",
				env,
				options.eventLoopThreadName
			);

		options.writeAHeadLogging =
			getBooleanProperty(
				"dstruct.wal.enabled",
				"DSTRUCT_WAL_ENABLED",
				env,
				options.writeAHeadLogging
			);

		options.dataDirectory =
			getStringProperty(
				"dstruct.data.dir",
				"DSTRUCT_DATA_DIR",
				env,
				options.dataDirectory
			);

		String syncPolicy =
			getStringProperty(
				"dstruct.sync.policy",
				"DSTRUCT_SYNC_POLICY",
				env, options.syncPolicy.name()
			);

		options.syncPolicy = SyncPolicy.valueOf(syncPolicy.toUpperCase());

		options.batchSize =
			getIntProperty(
				"dstruct.batch.size",
				"DSTRUCT_BATCH_SIZE",
				env,
				options.batchSize
			);

		options.syncIntervalMs =
			getLongProperty(
				"dstruct.sync.interval.ms",
				"DSTRUCT_SYNC_INTERVAL_MS",
				env,
				options.syncIntervalMs
			);

		return options;
	}

	private static String getStringProperty(
		String propertyKey, String envKey, Map<String, String> env, String defaultValue) {

		String sysProp = System.getProperty(propertyKey);
		if (sysProp != null) {
			return sysProp;
		}
		String envVar = env.get(envKey);
		if (envVar != null) {
			return envVar;
		}
		return defaultValue;
	}

	private static int getIntProperty(
		String propertyKey, String envKey, Map<String, String> env, int defaultValue) {

		String value = getStringProperty(propertyKey, envKey, env, null);
		if (value != null) {
			return Integer.parseInt(value);
		}
		return defaultValue;
	}

	private static long getLongProperty(
		String propertyKey, String envKey, Map<String, String> env, long defaultValue) {

		String value = getStringProperty(propertyKey, envKey, env, null);
		if (value != null) {
			return Long.parseLong(value);
		}
		return defaultValue;
	}

	private static boolean getBooleanProperty(
		String propertyKey, String envKey, Map<String, String> env, boolean defaultValue) {

		String value = getStringProperty(propertyKey, envKey, env, null);
		if (value != null) {
			return Boolean.parseBoolean(value);
		}
		return defaultValue;
	}

}
