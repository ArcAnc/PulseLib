/**
 * @author ArcAnc
 * Created at: 26.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.base;

import org.jetbrains.annotations.ApiStatus;

/** Extraction result for Pulse geometry submitted by an otherwise vanilla entity renderer. */
@ApiStatus.Internal
public interface PWorldRenderState
{
	boolean pulselib$requiresSeparateTargets();

	void pulselib$setRequiresSeparateTargets(boolean required);
}
