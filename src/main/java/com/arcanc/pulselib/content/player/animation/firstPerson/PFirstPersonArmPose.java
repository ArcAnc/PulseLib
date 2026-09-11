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

public record PFirstPersonArmPose(
		PFirstPersonRenderMode mode,
		@Nullable Matrix4f transform)
{
	public PFirstPersonArmPose
	{
		Objects.requireNonNull(mode);
		if (mode == PFirstPersonRenderMode.ANIMATED && transform == null)
			throw new IllegalArgumentException("An animated first-person arm needs a transform");
		if (mode != PFirstPersonRenderMode.ANIMATED && transform != null)
			throw new IllegalArgumentException("Only an animated first-person arm may have a transform");
		if (transform != null)
			transform = new Matrix4f(transform);
	}

	public static PFirstPersonArmPose vanilla()
	{
		return new PFirstPersonArmPose(PFirstPersonRenderMode.VANILLA, null);
	}

	public static PFirstPersonArmPose animated(Matrix4f transform)
	{
		return new PFirstPersonArmPose(PFirstPersonRenderMode.ANIMATED, transform);
	}

	public static PFirstPersonArmPose hidden()
	{
		return new PFirstPersonArmPose(PFirstPersonRenderMode.HIDDEN, null);
	}

	@Override
	public @Nullable Matrix4f transform()
	{
		return this.transform == null ? null : new Matrix4f(this.transform);
	}
}
