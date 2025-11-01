package dev.dstruct;

import dev.dstruct.logging.Log;
import dev.dstruct.logging.LogFactory;

public class Main {

	static void main(String[] args) throws Exception {
		Log log = LogFactory.create(Main.class);
		Options options = Options.fromEnv();
		if (log.isDebugEnabled()) {
			log.debug(options.toString());
		}
		DStruct dStruct = new DStruct(options);
		dStruct.start();
	}
}
