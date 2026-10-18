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
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * VIEW-space arm render command produced from the canonical skeleton.
 *
 * @param mode the rule used to combine the layer with the accumulated value
 * @param transform the transform; may be {@code null}
 */
public record PFirstPersonArmPose(
		PFirstPersonRenderMode mode,
		@Nullable PTransform transform)
{
	/**
	 * Creates an arm render command with the supplied mode and optional animated transform.
	 *
	 * @param mode the rule used to combine the layer with the accumulated value
	 * @param transform the transform; may be {@code null}
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
	 * Creates an arm presentation that retains vanilla first-person rendering.
	 *
	 * @return an arm command retaining vanilla rendering
	 */
	public static PFirstPersonArmPose vanilla()
	{
		return new PFirstPersonArmPose(PFirstPersonRenderMode.VANILLA, null);
	}

	/**
	 * Creates an arm presentation driven by the supplied animation transform.
	 *
	 * @param transform the transform
	 * @return an arm command using the supplied animation transform
	 */
	public static PFirstPersonArmPose animated(PTransform transform)
	{
		return new PFirstPersonArmPose(PFirstPersonRenderMode.ANIMATED, transform);
	}

}
