/**
 * @author ArcAnc
 * Created at: 04.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;


import java.util.Objects;

public record PPlayerFirstPersonSettings(
		boolean enabled,
		float transitionIn,
		float transitionOut,
		PFirstPersonCameraMode cameraMode)
{
	public static final PPlayerFirstPersonSettings DISABLED =
			new PPlayerFirstPersonSettings(false, 0.0f, 0.0f, PFirstPersonCameraMode.VANILLA);
	
	public static final PPlayerFirstPersonSettings ENABLED =
			new PPlayerFirstPersonSettings(true, 3.0f, 3.0f, PFirstPersonCameraMode.ANIMATED);

	public PPlayerFirstPersonSettings
	{
		if (!Float.isFinite(transitionIn) || !Float.isFinite(transitionOut) || transitionIn < 0.0f || transitionOut < 0.0f)
			throw new IllegalArgumentException("First-person transition durations must be finite and non-negative");
		cameraMode = Objects.requireNonNull(cameraMode);
	}

	/**
	 * Retained for source compatibility with the original settings record.
	 */
	public boolean enable()
	{
		return this.enabled;
	}
	
	public PPlayerFirstPersonSettings copy()
	{
		return new PPlayerFirstPersonSettings(this.enabled, this.transitionIn, this.transitionOut, this.cameraMode);
	}
}
