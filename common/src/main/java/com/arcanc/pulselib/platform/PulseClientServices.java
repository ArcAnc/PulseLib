/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.platform;

/**
 * Client-only service bindings resolved from the active loader implementation.
 */
public final class PulseClientServices
{
	/**
	 * Loader-provided camera rotation service.
	 */
	public static final IPulseCameraService CAMERA = PulseServices.load(IPulseCameraService.class);

	private PulseClientServices()
	{
	}
}
