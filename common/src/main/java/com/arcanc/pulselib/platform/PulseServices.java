/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

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
