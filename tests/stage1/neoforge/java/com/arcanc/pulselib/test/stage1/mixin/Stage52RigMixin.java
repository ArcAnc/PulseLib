/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1.mixin;

import com.arcanc.pulselib.test.stage1.Stage52RigProbe;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;

/** Observes the complete native setup, including production rig application. */
@Mixin(PlayerModel.class)
public abstract class Stage52RigMixin
{
	@WrapMethod(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V")
	private void pulselib$observeRig(AvatarRenderState state, Operation<Void> original)
	{
		original.call(state);
		if (Boolean.getBoolean("pulselib.stage52b2Probe"))
			Stage52RigProbe.afterSetup((PlayerModel)(Object)this, state);
	}
}
