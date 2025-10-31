package dev.dstruct.util;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.Consumer;

public interface Sink<T> {

	void next(T complete);

	void complete();

	void error(Throwable throwable);

	default Sink<T> then(Sink<T> then) {
		return then(this, then);
	}

	static <T> Sink<T> then(Sink<T> a, Sink<T> b) {
		return new Sink<T>() {
			@Override
			public void next(T complete) {
				a.next(complete);
				b.next(complete);
			}

			@Override
			public void complete() {
				a.complete();
				b.complete();
			}

			@Override
			public void error(Throwable throwable) {
				a.error(throwable);
				b.error(throwable);
			}
		};
	}

	static <T> Sink<T> callback(Consumer<T> consumer) {
		return callback(consumer, null);
	}

	static <T> Sink<T> callback(Consumer<T> consumer, Consumer<Throwable> throwableConsumer) {
		Objects.requireNonNull(consumer, "consumer is null");
		return new Sink<T>() {
			@Override
			public void next(T complete) {
				consumer.accept(complete);
			}

			@Override
			public void complete() {
				consumer.accept(null);
			}

			@Override
			public void error(Throwable throwable) {
				if (throwableConsumer != null) {
					throwableConsumer.accept(throwable);
				}
				else {
					throwable.printStackTrace();
				}
			}
		};
	}

	static <T> Sink<T> multi(Subscriber<T> subscriber) {
		return new Sink<>() {
			@Override
			public void next(T complete) {
				subscriber.onNext(complete);
			}

			@Override
			public void complete() {
				subscriber.onComplete();
			}

			@Override
			public void error(Throwable throwable) {
				subscriber.onError(throwable);
			}
		};
	}

	static <T> Sink<T> single(CompletableFuture<T> cf) {
		Objects.requireNonNull(cf, "cf is null");
		return new Sink<>() {
			@Override
			public void next(T complete) {
				cf.complete(complete);
			}

			@Override
			public void complete() {
				cf.complete(null);
			}

			@Override
			public void error(Throwable throwable) {
				cf.completeExceptionally(throwable);
			}
		};


	}

}
