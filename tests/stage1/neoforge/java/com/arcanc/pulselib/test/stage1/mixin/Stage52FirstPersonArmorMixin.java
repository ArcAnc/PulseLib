/**
 * @author ArcAnc
 * Created at: 30.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1.mixin;

import com.arcanc.pulselib.test.stage1.Stage52FirstPersonArmorProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Observes the real native hand feature pass without replacing any submissions or draw calls. */
@Mixin(ItemInHandRenderer.class)
public abstract class Stage52FirstPersonArmorMixin
{
	@WrapOperation(method = "renderHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures()V"))
	private void pulselib$observeFeatures(FeatureRenderDispatcher renderer, Operation<Void> original)
	{
		if (Boolean.getBoolean("pulselib.firstPersonArmorProbe"))
			Stage52FirstPersonArmorProbe.beforeNative();
		original.call(renderer);
		if (Boolean.getBoolean("pulselib.firstPersonArmorProbe"))
			Stage52FirstPersonArmorProbe.afterNative();
	}
}
