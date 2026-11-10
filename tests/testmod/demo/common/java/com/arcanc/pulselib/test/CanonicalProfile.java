/**
 * @author ArcAnc
 * Created at: 11.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

/** Canonical dispatch properties shared with the physical server. */
public final class CanonicalProfile
{
	private CanonicalProfile()
	{
	}

	public static boolean route(String name)
	{
		return name.equals(System.getProperty("pulselib.test.canonicalRoute"));
	}

	public static boolean world()
	{
		return Boolean.getBoolean("pulselib.test.world");
	}

	public static boolean smokeWorld()
	{
		return route("smoke") && world();
	}

	public static boolean oitWorld()
	{
		return (route("oit") || route("baseline")) && world();
	}
}
