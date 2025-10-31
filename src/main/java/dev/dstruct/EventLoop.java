package dev.dstruct;

import dev.dstruct.command.Command;
import dev.dstruct.util.Process;
import dev.dstruct.util.Sink;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Basic implementation of event loop with a single thread.
 */
class EventLoop implements Process {

	record Event(
		Command command,
		Sink<Command> callback
	) {}

	private final AtomicBoolean running = new AtomicBoolean(true);
	private final ExecutorService executor;
	private final LinkedBlockingDeque<Event> events;

	EventLoop(String name) {
		this(name, Integer.MAX_VALUE);
	}

	EventLoop(String name, int capacity) {
		this.executor = Executors.newSingleThreadExecutor(
			Thread
				.ofPlatform()
				.name(name)
				.factory()
		);
		this.events = new LinkedBlockingDeque<>(capacity);
	}

	public void offer(Command command, Sink<Command> sink) {
		events.offer(new Event(command, sink));
	}

	public void start() {
		this.executor.execute(() -> {
			while (running.get()) {
				try {
					Event event = events.take();

					if (event.callback == null) {
						continue;
					}
					else {
						event.callback.next(event.command());
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}

			}
		});
	}

	@Override
	public void close() {
		running.set(false);
		executor.shutdown();
	}

}
