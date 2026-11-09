/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

/**
 * Validates actual loader hook order without logging each render frame.
 */
public final class SmokeStages
{
	private static int phase;

	private SmokeStages()
	{
	}

	public static void blocks()
	{
		if (!SmokeHarness.enabled())
			return;
		if (phase != 0)
			throw new AssertionError("Block-entity stage out of order: " + phase);
		phase = 1;
	}

	public static void particles()
	{
		if (!SmokeHarness.enabled())
			return;
		if (phase != 1)
			throw new AssertionError("Particle stage out of order: " + phase);
		phase = 2;
	}

	public static void endFrame()
	{
		if (!SmokeHarness.enabled())
			return;
		if (phase != 2)
			throw new AssertionError("Weather stage out of order: " + phase);
		phase = 0;
		ClientTestPlugin.stageFrames++;
	}
}
