package dev.dstruct.inmemory;

import dev.dstruct.Result;
import dev.dstruct.command.Command;

public class InMemoryStore {

	private final DataStructureVisitor dataStructureVisitor = new DataStructureVisitor();

	public Result manageCommand(Command command) {
		return command.accept(dataStructureVisitor);
	}

}
