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
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition.ItemRenderPolicy;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public record PFirstPersonItemPose(
		PFirstPersonRenderMode mode,
		@Nullable PTransform transform,
		ItemRenderPolicy renderPolicy)
{
	/**
	 * {@code transform} places the item at the camera-space point of the bone
	 * bound to {@code RIGHT_ITEM} or {@code LEFT_ITEM}. ItemInHandRenderer
	 * applies the item's FIRST_PERSON_*_HAND JSON display transform, including
	 * item orientation and scale, after this transform.
	 */
	public PFirstPersonItemPose
	{
		Objects.requireNonNull(mode);
		renderPolicy = Objects.requireNonNull(renderPolicy);
		if (mode == PFirstPersonRenderMode.ANIMATED && transform == null)
			throw new IllegalArgumentException("An animated first-person item needs a transform");
		if (mode != PFirstPersonRenderMode.ANIMATED && transform != null)
			throw new IllegalArgumentException("Only an animated first-person item may have a transform");
	}

	public static PFirstPersonItemPose vanilla()
	{
		return new PFirstPersonItemPose(PFirstPersonRenderMode.VANILLA, null, ItemRenderPolicy.RENDER);
	}

	public static PFirstPersonItemPose animated(PTransform transform, ItemRenderPolicy renderPolicy)
	{
		return new PFirstPersonItemPose(PFirstPersonRenderMode.ANIMATED, transform, renderPolicy);
	}

	public static PFirstPersonItemPose animated(PTransform transform)
	{
		return animated(transform, ItemRenderPolicy.RENDER);
	}

	public static PFirstPersonItemPose hidden()
	{
		return new PFirstPersonItemPose(PFirstPersonRenderMode.HIDDEN, null, ItemRenderPolicy.HIDE);
	}
}
