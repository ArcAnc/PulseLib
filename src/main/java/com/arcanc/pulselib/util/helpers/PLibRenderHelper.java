/**
 * @author ArcAnc
 * Created at: 25.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.helpers;


import net.minecraft.client.Minecraft;

public class PLibRenderHelper
{
	/**
	 * Performs the mc operation.
	 * @return the value produced by this operation.
	 */
	public static Minecraft mc()
	{
		return Minecraft.getInstance();
	}
}
