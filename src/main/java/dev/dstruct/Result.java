package dev.dstruct;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Result of a DStruct command execution.
 * <p>
 * Types: {@link EmptyResult#OK}, {@link EmptyResult#NOTHING}, {@link Ok}, {@link Results}, {@link Error}
 * <pre>{@code
 * // Pattern matching
 * Result result = dstruct.executeAsync(command).get();
 * switch (result) {
 *     case EmptyResult.OK -> // Success, no value
 *     case EmptyResult.NOTHING -> // Not found
 *     case Ok ok -> processValue(ok.value())
 *     case Results r -> processBatch(r.results())
 *     case Error e -> handleError(e.message())
 * }
 * }</pre>
 */
public sealed interface Result {
	
	/**
	 * Command result without a value.
	 * <p>
	 * {@link #OK} - Command succeeded (MPUT, VSET, DEL, etc.)<br>
	 * {@link #NOTHING} - Command succeeded but found nothing (MGET on missing key, LPOP on empty list)
	 */
	enum EmptyResult implements Result {
		/** Command executed successfully. */
		OK,
		/** Command executed but no data found. */
		NOTHING
	}
	
	/**
	 * Command result with a byte array value.
	 * <p>
	 * Returned by MGET, LPOP, RPOP, LINDEX, etc.
	 * <pre>{@code
	 * if (result instanceof Result.Ok ok) {
	 *     byte[] value = ok.value();
	 *     String str = ok.toString(); // UTF-8 conversion
	 * }
	 * }</pre>
	 * 
	 * @param value the byte array value, may be null
	 */
	record Ok(byte[] value) implements Result {
		
		/** Creates an Ok result from a UTF-8 string. */
		public Ok(String str) {
			this(str.getBytes(StandardCharsets.UTF_8));
		}

		/** Returns UTF-8 string representation, or "null" if value is null. */
		@Override
		public String toString() {
			return value == null ? "null" : new String(value);
		}

		/** Compares byte array content using {@link Arrays#equals}. */
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Ok(byte[] thatValue))) return false;
			return Arrays.equals(value, thatValue);
		}
	}
	
	/**
	 * Result of a batch command containing multiple results.
	 * <p>
	 * Results are in the same order as commands in the batch.
	 * <pre>{@code
	 * if (result instanceof Result.Results batch) {
	 *     for (Result r : batch.results()) {
	 *         // Process each result
	 *     }
	 * }
	 * }</pre>
	 * 
	 * @param results list of results from batch commands
	 */
	record Results(List<Result> results) implements Result {}
	
	/**
	 * Command execution error.
	 * <p>
	 * Common causes: invalid syntax, type mismatch, WAL failure, internal errors.
	 * 
	 * @param message error description
	 */
	record Error(String message) implements Result {}
}