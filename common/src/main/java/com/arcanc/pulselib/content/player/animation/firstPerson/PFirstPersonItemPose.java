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
 * Immutable value object representing first person item pose.
 */
public record PFirstPersonItemPose(
		PFirstPersonRenderMode mode,
		@Nullable PTransform transform,
		PFirstPersonTransformMode transformMode)
{
	/**
	 * {@code transform} places the resolved camera-space item socket. Minecraft
	 * still applies the item's FIRST_PERSON_*_HAND display transform and submits
	 * the model through its normal item renderer.
	 */
	public PFirstPersonItemPose
	{
		Objects.requireNonNull(mode);
		transformMode = Objects.requireNonNull(transformMode);
		if (mode == PFirstPersonRenderMode.ANIMATED && transform == null)
			throw new IllegalArgumentException("An animated first-person item needs a transform");
		if (mode != PFirstPersonRenderMode.ANIMATED && transform != null)
			throw new IllegalArgumentException("Only an animated first-person item may have a transform");
	}

	/**
	 * Performs the vanilla operation.
	 * @return the value produced by this operation.
	 */
	public static PFirstPersonItemPose vanilla()
	{
		return new PFirstPersonItemPose(PFirstPersonRenderMode.VANILLA, null, PFirstPersonTransformMode.OVERRIDE);
	}

	/**
	 * Performs the animated operation.
	 * @param transform the transform to use.
	 * @return the value produced by this operation.
	 */
	public static PFirstPersonItemPose animated(PTransform transform)
	{
		return new PFirstPersonItemPose(PFirstPersonRenderMode.ANIMATED, transform, PFirstPersonTransformMode.OVERRIDE);
	}

	/**
	 * Performs the animated operation.
	 * @param transform the transform to use.
	 * @param transformMode the transform mode to use.
	 * @return the value produced by this operation.
	 */
	public static PFirstPersonItemPose animated(PTransform transform, PFirstPersonTransformMode transformMode)
	{
		return new PFirstPersonItemPose(PFirstPersonRenderMode.ANIMATED, transform, transformMode);
	}

	/**
	 * Performs the hidden operation.
	 * @return the value produced by this operation.
	 */
	public static PFirstPersonItemPose hidden()
	{
		return new PFirstPersonItemPose(PFirstPersonRenderMode.HIDDEN, null, PFirstPersonTransformMode.OVERRIDE);
	}
}
