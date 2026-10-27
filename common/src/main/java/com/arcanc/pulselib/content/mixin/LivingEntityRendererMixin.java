/**
 * @author ArcAnc
 * Created at: 30.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.player.animation.PPlayerRigPose;
import com.arcanc.pulselib.content.renderer.base.PPlayerRigRenderState;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies PulseLib integration to {@code PlayerModel}.
 */
@Mixin(PlayerModel.class)
public abstract class LivingEntityRendererMixin
{
	/**
	 * Performs the pulselib$apply player animations operation.
	 * @param state the state to use.
	 * @param ci the ci to use.
	 */
	@Inject(method = "setupAnim", at = @At("TAIL"))
	private void pulselib$applyPlayerAnimations(AvatarRenderState state, CallbackInfo ci)
	{
		if (state instanceof PPlayerRigRenderState snapshot)
		{
			@Nullable PPlayerRigPose rig = snapshot.pulselib$getPlayerRig();
			if (rig != null)
				rig.apply((PlayerModel)(Object)this);
		}
	}
}
