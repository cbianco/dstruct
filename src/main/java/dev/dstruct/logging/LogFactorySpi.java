package dev.dstruct.logging;

public interface LogFactorySpi {

	default boolean active() {
		return true;
	}

	Log create(Class<?> type);

}
