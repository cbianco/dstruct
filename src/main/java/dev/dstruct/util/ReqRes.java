package dev.dstruct.util;

import dev.dstruct.Result;
import dev.dstruct.command.Command;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface ReqRes extends Function<Command, CompletableFuture<Result>> {
}
