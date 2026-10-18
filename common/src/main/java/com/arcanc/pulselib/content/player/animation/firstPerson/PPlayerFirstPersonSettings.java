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

/**
 * First-person enablement, activation transition durations and camera mode.
 *
 * @param enabled whether animation can contribute to first-person presentation
 * @param transitionIn the first-person activation fade-in duration in game ticks
 * @param transitionOut the first-person activation fade-out duration in game ticks
 * @param cameraMode the animated or vanilla first-person camera policy
 */
public record PPlayerFirstPersonSettings(
		boolean enabled,
		float transitionIn,
		float transitionOut,
		PFirstPersonCameraMode cameraMode)
{
	/**
	 * Settings that disable animated first-person presentation.
	 */
	public static final PPlayerFirstPersonSettings DISABLED =
			new PPlayerFirstPersonSettings(false, 0.0f, 0.0f, PFirstPersonCameraMode.VANILLA);

	/**
	 * Default settings that enable animated first-person presentation.
	 */
	public static final PPlayerFirstPersonSettings ENABLED =
			new PPlayerFirstPersonSettings(true, 3.0f, 3.0f, PFirstPersonCameraMode.ANIMATED);

	/**
	 * Creates first-person settings with finite, non-negative transition durations.
	 *
	 * @param enabled whether animation can contribute to first-person presentation
	 * @param transitionIn the first-person activation fade-in duration in game ticks
	 * @param transitionOut the first-person activation fade-out duration in game ticks
	 * @param cameraMode the animated or vanilla first-person camera policy
	 */
	public PPlayerFirstPersonSettings
	{
		if (!Float.isFinite(transitionIn) || !Float.isFinite(transitionOut) || transitionIn < 0.0f || transitionOut < 0.0f)
			throw new IllegalArgumentException("First-person transition durations must be finite and non-negative");
		cameraMode = Objects.requireNonNull(cameraMode);
	}

}
