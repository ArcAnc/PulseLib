/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.base;

import com.arcanc.pulselib.content.player.animation.PPlayerRigPose;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/** Sampled rig commands belonging to one extracted avatar state. */
@ApiStatus.Internal
public interface PPlayerRigRenderState
{
	@Nullable
	PPlayerRigPose pulselib$getPlayerRig();

	void pulselib$setPlayerRig(@Nullable PPlayerRigPose rig);
}
