/**
 * @author ArcAnc
 * Created at: 12.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting;

import com.arcanc.pulselib.platform.IPulsePlatform;
import com.arcanc.pulselib.plugin.IPulsePluginFinder;

/** Checks actual adapter sources against actual loader dependencies, without simulating a running mod list. */
public final class DiscoveryChecks
{
	public static void main(String[] args) throws Exception
	{
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Object platform = Class.forName(args[1], true, loader).getConstructor().newInstance();
		Object finder = Class.forName(args[2], true, loader).getConstructor().newInstance();
		if (!(platform instanceof IPulsePlatform) || !(finder instanceof IPulsePluginFinder))
			throw new AssertionError("Loader adapter contract changed");
		System.out.println("PASS headless classloading and ABI of " + args[1] + " and " + args[2]);
		System.out.println("Loader initialization, mod-list discovery and PulsePluginManager registration are not exercised.");
	}
}
