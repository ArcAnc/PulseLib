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

public record PFirstPersonArmPose(
		PFirstPersonRenderMode mode,
		@Nullable PTransform transform,
		PFirstPersonTransformMode transformMode)
{
	public PFirstPersonArmPose
	{
		Objects.requireNonNull(mode);
		transformMode = Objects.requireNonNull(transformMode);
		if (mode == PFirstPersonRenderMode.ANIMATED && transform == null)
			throw new IllegalArgumentException("An animated first-person arm needs a transform");
		if (mode != PFirstPersonRenderMode.ANIMATED && transform != null)
			throw new IllegalArgumentException("Only an animated first-person arm may have a transform");
	}

	/** Retained for source compatibility; new callers should state the transform mode. */
	public PFirstPersonArmPose(PFirstPersonRenderMode mode, @Nullable PTransform transform)
	{
		this(mode, transform, PFirstPersonTransformMode.OVERRIDE);
	}

	public static PFirstPersonArmPose vanilla()
	{
		return new PFirstPersonArmPose(PFirstPersonRenderMode.VANILLA, null, PFirstPersonTransformMode.OVERRIDE);
	}

	public static PFirstPersonArmPose animated(PTransform transform)
	{
		return new PFirstPersonArmPose(PFirstPersonRenderMode.ANIMATED, transform, PFirstPersonTransformMode.OVERRIDE);
	}

	public static PFirstPersonArmPose animated(PTransform transform, PFirstPersonTransformMode transformMode)
	{
		return new PFirstPersonArmPose(PFirstPersonRenderMode.ANIMATED, transform, transformMode);
	}

	public static PFirstPersonArmPose hidden()
	{
		return new PFirstPersonArmPose(PFirstPersonRenderMode.HIDDEN, null, PFirstPersonTransformMode.OVERRIDE);
	}
}
