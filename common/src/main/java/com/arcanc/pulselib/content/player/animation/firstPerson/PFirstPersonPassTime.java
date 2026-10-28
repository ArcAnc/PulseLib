/**
 * @author ArcAnc
 * Created at: 28.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

import org.jetbrains.annotations.ApiStatus;

/** Internal access to timing owned by the existing native hand pass stack. */
@ApiStatus.Internal
public final class PFirstPersonPassTime
{
	private PFirstPersonPassTime()
	{
	}

	public static float partialTick(float fallback)
	{
		return PFirstPersonRenderContexts.partialTick(fallback);
	}
}
