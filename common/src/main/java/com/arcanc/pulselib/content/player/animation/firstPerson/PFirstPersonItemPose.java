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
 * First-person held-item rendering decision, transform and composition mode.
 *
 * @param mode the rule used to combine the layer with the accumulated value
 * @param transform the transform; may be {@code null}
 * @param transformMode the transform mode
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
	 *
	 * @param mode the rule used to combine the layer with the accumulated value
	 * @param transform the transform; may be {@code null}
	 * @param transformMode the transform mode
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
	 * Creates a held-item presentation that retains vanilla first-person rendering.
	 *
	 * @return an item command retaining vanilla rendering
	 */
	public static PFirstPersonItemPose vanilla()
	{
		return new PFirstPersonItemPose(PFirstPersonRenderMode.VANILLA, null, PFirstPersonTransformMode.OVERRIDE);
	}

	/**
	 * Creates an animated held-item presentation with the requested transform composition policy.
	 *
	 * @param transform the transform
	 * @return an item command using the supplied animation transform
	 */
	public static PFirstPersonItemPose animated(PTransform transform)
	{
		return new PFirstPersonItemPose(PFirstPersonRenderMode.ANIMATED, transform, PFirstPersonTransformMode.OVERRIDE);
	}

	/**
	 * Creates an animated held-item presentation with the requested transform composition policy.
	 *
	 * @param transform the transform
	 * @param transformMode the transform mode
	 * @return an item command using the supplied animation transform
	 */
	public static PFirstPersonItemPose animated(PTransform transform, PFirstPersonTransformMode transformMode)
	{
		return new PFirstPersonItemPose(PFirstPersonRenderMode.ANIMATED, transform, transformMode);
	}

	/**
	 * Creates a held-item presentation that suppresses item geometry.
	 *
	 * @return an item command suppressing held-item geometry
	 */
	public static PFirstPersonItemPose hidden()
	{
		return new PFirstPersonItemPose(PFirstPersonRenderMode.HIDDEN, null, PFirstPersonTransformMode.OVERRIDE);
	}
}
