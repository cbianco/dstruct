package dev.dstruct.addons.logging;

import dev.dstruct.logging.Log;
import dev.dstruct.logging.LogFactorySpi;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLoggerFactory;

public class Slf4jLogFactorySpi implements LogFactorySpi {

	static boolean active;

	{
		ILoggerFactory factory = LoggerFactory.getILoggerFactory();
		active = !(factory instanceof NOPLoggerFactory);
	}

	@Override
	public boolean active() {
		return active;
	}

	@Override
    public Log create(Class<?> clazz) {
        Logger logger = LoggerFactory.getLogger(clazz);
        
        return new Log() {
            @Override
            public void debug(String message) {
                logger.debug(message);
            }

            @Override
            public void debug(Throwable throwable) {
                logger.debug("", throwable);
            }

            @Override
            public void debugf(String message, Object... args) {
                logger.debug(message, args);
            }

            @Override
            public boolean isDebugEnabled() {
                return logger.isDebugEnabled();
            }

            @Override
            public void info(String message) {
                logger.info(message);
            }

            @Override
            public void info(Throwable throwable) {
                logger.info("", throwable);
            }

            @Override
            public void infof(String message, Object... args) {
                logger.info(message, args);
            }

            @Override
            public boolean isInfoEnabled() {
                return logger.isInfoEnabled();
            }

            @Override
            public void warn(String message) {
                logger.warn(message);
            }

            @Override
            public void warn(Throwable throwable) {
                logger.warn("", throwable);
            }

            @Override
            public void warnf(String message, Object... args) {
                logger.warn(message, args);
            }

            @Override
            public boolean isWarnEnabled() {
                return logger.isWarnEnabled();
            }

            @Override
            public void error(String message) {
                logger.error(message);
            }

            @Override
            public void error(Throwable throwable) {
                logger.error("", throwable);
            }

            @Override
            public void errorf(String message, Object... args) {
                logger.error(message, args);
            }

            @Override
            public boolean isErrorEnabled() {
                return logger.isErrorEnabled();
            }
        };
    }
}