/**
 * @author ArcAnc
 * Created at: 06.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import org.jspecify.annotations.Nullable;

/** Canonical SmokeHarness assertion/reflection helpers, shared by migrated suites. */
public final class RegressionChecks
{
	static long assertions;
	static int maintenancePasses;

	private RegressionChecks()
	{
	}

	static @Nullable Object field(Class<?> type, String name, @Nullable Object instance)
	{
		try
		{
			var field = type.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(instance);
		}
		catch (ReflectiveOperationException exception)
		{
			throw new AssertionError(exception);
		}
	}

	static void check(boolean condition, String message)
	{
		assertions++;
		if (!condition)
			throw new AssertionError("PulseLib smoke test: " + message);
	}

	public static void maintained()
	{
		maintenancePasses++;
	}
}
