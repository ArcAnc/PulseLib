/**
 * @author ArcAnc
 * Created at: 17.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import org.jetbrains.annotations.Nullable;

/** Retains the synchronous caller stack; fails outside the native GL callback. */
public final class GlErrors
{
	private static @Nullable AssertionError failure;

	private GlErrors()
	{
	}

	public static void record(int id)
	{
		if (failure == null)
		{
			failure = new AssertionError("OpenGL error in automated regression: " + id);
		}
	}

	public static void check()
	{
		if (failure != null)
		{
			throw failure;
		}
	}
}
