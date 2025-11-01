package dev.dstruct.logging;

import dev.dstruct.util.Config;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

class DefaultLogFactorySpi implements LogFactorySpi {

	enum LogLevel {
		DEBUG, INFO, WARN, ERROR
	}

	private final LogLevel minLevel;
	private final DateTimeFormatter formatter;

	DefaultLogFactorySpi() {
		String level = Config.resolve("dstruct.log.level", "INFO");
		String pattern = Config.resolve("dstruct.log.date.format", "yyyy-MM-dd HH:mm:ss.SSSXXX");
		this.minLevel = LogLevel.valueOf(level.toUpperCase());
		this.formatter =
			DateTimeFormatter
				.ofPattern(pattern)
				.withZone(ZoneOffset.UTC);
	}

	@Override
	public Log create(Class<?> clazz) {
		return new Log() {

			static final String LOG_TEMPLATE = "%-5s [%s][%s][%s] %s\n";

			static final String INFO = "INFO";
			static final String WARN = "WARN";
			static final String ERROR = "ERROR";
			static final String DEBUG = "DEBUG";

			@Override
			public boolean isDebugEnabled() {
				return isLevelEnabled(LogLevel.DEBUG);
			}

			@Override
			public boolean isInfoEnabled() {
				return isLevelEnabled(LogLevel.INFO);
			}

			@Override
			public boolean isWarnEnabled() {
				return isLevelEnabled(LogLevel.WARN);
			}

			@Override
			public boolean isErrorEnabled() {
				return isLevelEnabled(LogLevel.ERROR);
			}

			@Override
			public void info(String message) {
				if (isInfoEnabled()) print(INFO, message);
			}

			@Override
			public void info(Throwable throwable) {
				if (isInfoEnabled()) print(INFO, throwable);
			}

			@Override
			public void infof(String message, Object... args) {
				if (isInfoEnabled()) printf(INFO, message, args);
			}

			@Override
			public void warn(String message) {
				if (isWarnEnabled()) print(WARN, message);
			}

			@Override
			public void warn(Throwable throwable) {
				if (isWarnEnabled()) print(WARN, throwable);
			}

			@Override
			public void warnf(String message, Object... args) {
				if (isWarnEnabled()) printf(WARN, message, args);
			}

			@Override
			public void error(String message) {
				if (isErrorEnabled()) print(ERROR, message);
			}

			@Override
			public void error(Throwable throwable) {
				if (isErrorEnabled()) print(ERROR, throwable);
			}

			@Override
			public void errorf(String message, Object... args) {
				if (isErrorEnabled()) printf(ERROR, message, args);
			}

			@Override
			public void debug(String message) {
				if (isDebugEnabled()) print(DEBUG, message);
			}

			@Override
			public void debug(Throwable throwable) {
				if (isDebugEnabled()) print(DEBUG, throwable);
			}

			@Override
			public void debugf(String message, Object... args) {
				if (isDebugEnabled()) printf(DEBUG, message, args);
			}

			// DEBUG < INFO < WARN < ERROR
			private boolean isLevelEnabled(LogLevel level) {
				return minLevel.ordinal() <= level.ordinal();
			}

			private void print(String level, Throwable throwable) {
				StringWriter out = new StringWriter();
				try (PrintWriter printWriter = new PrintWriter(out)) {
					throwable.printStackTrace(printWriter);
				}
				print(level, out.toString().strip());
			}

			private void printf(String level, String message, Object...args) {
				print(level, String.format(message, args));
			}

			private void print(String level, String message) {
				Thread thread = Thread.currentThread();
				OffsetDateTime now = OffsetDateTime.now();
				System.out.printf(
					LOG_TEMPLATE,
					level,
					formatter.format(now),
					thread.getName(),
					clazz.getSimpleName(),
					message
				);
			}

		};
	}

}
