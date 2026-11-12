/**
 * @author ArcAnc
 * Created at: 12.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable;

import org.jetbrains.annotations.ApiStatus;

/** Identity transfers shared by ItemStack callbacks, without resolving animation managers or client classes. */
@ApiStatus.Internal
public final class PItemStackAnimationIdentity
{
	private PItemStackAnimationIdentity()
	{
	}

	/** Copies the identity to a nonempty result. The ItemStack callback owns the empty-result check. */
	public static void copy(PItemStackAnimationData source, PItemStackAnimationData result)
	{
		long id = source.pulselib$getOrCreateAnimationId();
		if (id != 0)
			result.pulselib$setAnimationId(id);
	}

	/** Separates a partial split or transfers an exhausted source's identity to its nonempty result. */
	public static void split(PItemStackAnimationData source, PItemStackAnimationData result, boolean sourceEmpty)
	{
		if (result.pulselib$getAnimationId() == 0)
			return;
		if (sourceEmpty)
			source.pulselib$setAnimationId(0);
		else
			result.pulselib$setAnimationId(PItemStackAnimationIds.nextId());
	}
}
