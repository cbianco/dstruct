package dev.dstruct;

public class Main {
	static void main(String[] args) throws Exception {
		Options options = Options.fromEnv();
		DStruct dStruct = new DStruct(options);
		dStruct.start();
	}
}
