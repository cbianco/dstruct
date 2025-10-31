package dev.dstruct.wal;

public enum SyncPolicy {
	/**
	 * fsync after every write - maximum durability, minimum performance
	 * Use for: financial transactions, critical data
	 */
	ALWAYS,

	/**
	 * fsync every N writes or T seconds - balanced approach
	 * Use for: most production workloads
	 */
	BATCHED,

	/**
	 * No explicit fsync - OS decides - maximum performance, minimum durability
	 * Use for: cache, testing, non-critical data
	 */
	ASYNC
}