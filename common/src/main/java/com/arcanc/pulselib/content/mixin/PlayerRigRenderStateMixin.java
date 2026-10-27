/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.player.animation.PPlayerRigPose;
import com.arcanc.pulselib.content.renderer.base.PPlayerRigRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Stores rig contributions on the avatar rather than on a shared renderer or model. */
@Mixin(AvatarRenderState.class)
public abstract class PlayerRigRenderStateMixin implements PPlayerRigRenderState
{
	@Unique
	private @Nullable PPlayerRigPose pulselib$rig;

	@Override
	public @Nullable PPlayerRigPose pulselib$getPlayerRig()
	{
		return this.pulselib$rig;
	}

	@Override
	public void pulselib$setPlayerRig(@Nullable PPlayerRigPose rig)
	{
		this.pulselib$rig = rig;
	}
}
