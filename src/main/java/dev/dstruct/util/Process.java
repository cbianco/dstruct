package dev.dstruct.util;

import java.io.IOException;

public interface Process extends AutoCloseable {

	void start() throws IOException;

	void close() throws IOException;

	Process NOOP = new Process() {
		@Override
		public void start() throws IOException {

		}

		@Override
		public void close() throws IOException {

		}
	};

}
