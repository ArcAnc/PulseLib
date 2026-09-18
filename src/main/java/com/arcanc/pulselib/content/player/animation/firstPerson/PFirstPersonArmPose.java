/**
 * @author ArcAnc
 * Created at: 11.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;


import com.arcanc.pulselib.content.model.animation.PTransform;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/** VIEW-space arm render command produced from the canonical skeleton. */
public record PFirstPersonArmPose(
		PFirstPersonRenderMode mode,
		@Nullable PTransform transform)
{
	/**
	 * Creates an instance of the enclosing type.
	 * @param mode the mode to use.
	 * @param transform the transform to use.
	 */
	public PFirstPersonArmPose
	{
		Objects.requireNonNull(mode);
		if (mode == PFirstPersonRenderMode.ANIMATED && transform == null)
			throw new IllegalArgumentException("An animated first-person arm needs a transform");
		if (mode != PFirstPersonRenderMode.ANIMATED && transform != null)
			throw new IllegalArgumentException("Only an animated first-person arm may have a transform");
	}

	/**
	 * Performs the vanilla operation.
	 * @return the value produced by this operation.
	 */
	public static PFirstPersonArmPose vanilla()
	{
		return new PFirstPersonArmPose(PFirstPersonRenderMode.VANILLA, null);
	}

	/**
	 * Performs the animated operation.
	 * @param transform the transform to use.
	 * @return the value produced by this operation.
	 */
	public static PFirstPersonArmPose animated(PTransform transform)
	{
		return new PFirstPersonArmPose(PFirstPersonRenderMode.ANIMATED, transform);
	}

}
