package dev.dstruct;

import dev.dstruct.wal.SyncPolicy;

public class Main {
	static void main(String[] args) throws Exception {
		Options options = new Options();
		options.writeAHeadLogging = false;
		options.syncPolicy = SyncPolicy.ASYNC;
		DStruct dStruct = new DStruct(options);
		dStruct.start();
	}
}
