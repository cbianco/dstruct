package dev.dstruct.logging;

import java.util.Iterator;
import java.util.ServiceLoader;

public final class LogFactory {

	static final LogFactorySpi LOG_FACTORY_SPI;

	static {
		ServiceLoader<LogFactorySpi> serviceLoader =
			ServiceLoader.load(LogFactorySpi.class);

		Iterator<LogFactorySpi> iterator = serviceLoader.iterator();

		LogFactorySpi selectedSpi = null;

		while (iterator.hasNext()) {
			LogFactorySpi candidate = iterator.next();
			if (candidate.active()) {
				selectedSpi = candidate;
				break;
			} else {
				System.err.println(
					"LogFactorySpi implementation " +
						candidate.getClass().getSimpleName() +
						" is not active, skipping"
				);
			}
		}

		if (selectedSpi == null) {
			System.err.println(
				"No active LogFactorySpi found, using DefaultLogFactorySpi"
			);
			selectedSpi = new DefaultLogFactorySpi();
		} else {
			System.out.println(
				"Using LogFactorySpi: " + selectedSpi.getClass().getSimpleName()
			);
		}

		LOG_FACTORY_SPI = selectedSpi;

	}

	public static Log create(Class<?> type) {
		return LOG_FACTORY_SPI.create(type);
	}

}
