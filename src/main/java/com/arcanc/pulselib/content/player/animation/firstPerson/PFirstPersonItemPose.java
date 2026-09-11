/**
 * @author ArcAnc
 * Created at: 11.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;


import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public record PFirstPersonItemPose(
		PFirstPersonRenderMode mode,
		@Nullable Matrix4f transform)
{
	public PFirstPersonItemPose
	{
		Objects.requireNonNull(mode);
		if (mode == PFirstPersonRenderMode.ANIMATED && transform == null)
			throw new IllegalArgumentException("An animated first-person item needs a transform");
		if (mode != PFirstPersonRenderMode.ANIMATED && transform != null)
			throw new IllegalArgumentException("Only an animated first-person item may have a transform");
		if (transform != null)
			transform = new Matrix4f(transform);
	}

	public static PFirstPersonItemPose vanilla()
	{
		return new PFirstPersonItemPose(PFirstPersonRenderMode.VANILLA, null);
	}

	public static PFirstPersonItemPose animated(Matrix4f transform)
	{
		return new PFirstPersonItemPose(PFirstPersonRenderMode.ANIMATED, transform);
	}

	public static PFirstPersonItemPose hidden()
	{
		return new PFirstPersonItemPose(PFirstPersonRenderMode.HIDDEN, null);
	}

	@Override
	public @Nullable Matrix4f transform()
	{
		return this.transform == null ? null : new Matrix4f(this.transform);
	}
}
