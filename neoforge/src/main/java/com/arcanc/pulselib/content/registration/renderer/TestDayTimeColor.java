/**
 * @author ArcAnc
 * Created at: 07.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.renderer;


import net.minecraft.world.level.Level;

public final class TestDayTimeColor
{
	private TestDayTimeColor()
	{
	}
	
	public static int color(Level level, float partialTick)
	{
		if (level == null)
			return -1;
		
		float time = ((level.getDayTime() % 24000L) + partialTick) / 24000f;
		
		if (time < 0.25f)
			return lerpColor(0xFFFFD36A, 0xFFFFFFFF, time / 0.25f);
		if (time < 0.50f)
			return lerpColor(0xFFFFFFFF, 0xFFFF9A3D, (time - 0.25f) / 0.25f);
		if (time < 0.75f)
			return lerpColor(0xFFFF9A3D, 0xFF5E7CFF, (time - 0.50f) / 0.25f);
		return lerpColor(0xFF5E7CFF, 0xFFFFD36A, (time - 0.75f) / 0.25f);
	}
	
	private static int lerpColor(int from, int to, float delta)
	{
		int alpha = lerp((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, delta);
		int red = lerp((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, delta);
		int green = lerp((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, delta);
		int blue = lerp(from & 0xFF, to & 0xFF, delta);
		
		return alpha << 24 | red << 16 | green << 8 | blue;
	}
	
	private static int lerp(int from, int to, float delta)
	{
		return (int)(from + (to - from) * delta);
	}
}
