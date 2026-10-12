package com.arcanc.pulselib.platform;

import java.util.ServiceLoader;

public final class PulseServices
{
	public static final IPulsePlatform PLATFORM = load(IPulsePlatform.class);

	private PulseServices()
	{
	}

	public static <T> T load(Class<T> service)
	{
		var providers = ServiceLoader.load(service).stream().toList();
		if (providers.size() != 1)
			throw new IllegalStateException("Expected one PulseLib provider for " + service.getName() + ", found " + providers.size());
		return providers.getFirst().get();
	}
}
