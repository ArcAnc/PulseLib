/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.base;

/** The extraction clock carried by a vanilla entity render state. */
public interface PPartialTickRenderState
{
	float pulselib$getPartialTick();

	void pulselib$setPartialTick(float partialTick);
}
