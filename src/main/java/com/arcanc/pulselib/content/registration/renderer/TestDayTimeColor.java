/**
 * @author ArcAnc
 * Created at: 07.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.renderer;


import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public final class TestDayTimeColor
{
	/**
	 * Creates an instance of the enclosing type.
	 */
	private TestDayTimeColor()
	{
	}
	
	/**
	 * Performs the color operation.
	 * @param level the level to use.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
	public static int color(Level level, float partialTick)
	{
		if (level == null)
			return -1;
		
		float time = ((level.getOverworldClockTime() % 24000L) + partialTick) / 24000f;
		
		if (time < 0.25f)
			return lerpColor(0xFFFFD36A, 0xFFFFFFFF, time / 0.25f);
		if (time < 0.50f)
			return lerpColor(0xFFFFFFFF, 0xFFFF9A3D, (time - 0.25f) / 0.25f);
		if (time < 0.75f)
			return lerpColor(0xFFFF9A3D, 0xFF5E7CFF, (time - 0.50f) / 0.25f);
		return lerpColor(0xFF5E7CFF, 0xFFFFD36A, (time - 0.75f) / 0.25f);
	}
	
	/**
	 * Performs the lerp color operation.
	 * @param from the from to use.
	 * @param to the to to use.
	 * @param delta the delta to use.
	 * @return the value produced by this operation.
	 */
	private static int lerpColor(int from, int to, float delta)
	{
		int alpha = Mth.lerpInt(delta,(from >>> 24) & 0xFF, (to >>> 24) & 0xFF);
		int red = Mth.lerpInt(delta, (from >>> 16) & 0xFF, (to >>> 16) & 0xFF);
		int green = Mth.lerpInt(delta, (from >>> 8) & 0xFF, (to >>> 8) & 0xFF);
		int blue = Mth.lerpInt(delta, from & 0xFF, to & 0xFF);
		
		return alpha << 24 | red << 16 | green << 8 | blue;
	}
}
