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

/**
 * ServiceLoader access to loader-specific implementations required by common code.
 */
public final class PulseServices
{
	/**
	 * Loader-provided physical-side and entity behavior service.
	 */
	public static final IPulsePlatform PLATFORM = load(IPulsePlatform.class);

	private PulseServices()
	{
	}

	/**
	 * Loads the required implementation of the supplied platform service interface.
	 *
	 * @param <T> the value type carried by this contract
	 * @param service the platform service interface to resolve
	 * @return the active loader's required service implementation
	 */
	public static <T> T load(Class<T> service)
	{
		var providers = ServiceLoader.load(service).stream().toList();
		if (providers.size() != 1)
			throw new IllegalStateException("Expected one PulseLib provider for " + service.getName() + ", found " + providers.size());
		return providers.getFirst().get();
	}
}
