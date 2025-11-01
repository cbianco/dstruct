package dev.dstruct.logging;

public interface Log {

	void info(String message);

	void info(Throwable throwable);

	void infof(String message, Object...args);

	default boolean isInfoEnabled() {
		return true;
	}

	void warn(String message);

	void warn(Throwable throwable);

	void warnf(String message, Object...args);

	default boolean isWarnEnabled() {
		return true;
	}

	void error(String message);

	void error(Throwable throwable);

	void errorf(String message, Object...args);

	default boolean isErrorEnabled() {
		return true;
	}

	void debug(String message);

	void debug(Throwable throwable);

	void debugf(String message, Object...args);

	default boolean isDebugEnabled() {
		return true;
	}

}
