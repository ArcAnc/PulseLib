/**
 * @author ArcAnc
 * Created at: 14.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable;

/**
 * Client-only ItemStack Mixin access. Zero denotes an unassigned runtime identity.
 * Assigning an identity never creates an animation manager or controllers.
 */
public interface PItemStackAnimationData
{
	long pulselib$getAnimationId();

	/** Copies an existing identity, or clears it with zero; intended for stack transfers. */
	void pulselib$setAnimationId(long id);

	/** Returns zero for empty or nonanimated stacks; otherwise assigns an ID once. */
	long pulselib$getOrCreateAnimationId();
}
