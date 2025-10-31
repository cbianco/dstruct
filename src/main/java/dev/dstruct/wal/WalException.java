package dev.dstruct.wal;

public class WalException extends RuntimeException {

	public WalException(String message) {
		super(message);
	}

	public WalException(Throwable exception) {
		super(exception);
	}

	public WalException(String message, Throwable exception) {
		super(message, exception);
	}
}
