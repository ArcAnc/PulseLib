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
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Observes real world deferred model execution without changing its state or queue. */
@Mixin(ModelFeatureRenderer.class)
public abstract class Stage52RigFeatureMixin
{
	@WrapOperation(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V"))
	private void pulselib$observeDeferred(Model<Object> model, Object state, Operation<Void> original)
	{
		original.call(model, state);
		if (Boolean.getBoolean("pulselib.stage52b2Probe"))
			Stage52RigProbe.deferred(state);
	}
}
