/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1.mixin;

import com.arcanc.pulselib.test.stage1.Stage43bGuiProbe;
import net.minecraft.client.gui.render.pip.OversizedItemRenderer;
import net.minecraft.client.renderer.state.gui.pip.OversizedItemRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OversizedItemRenderer.class)
public abstract class Stage43bOversizedMixin
{
	@Inject(method = "renderToTexture", at = @At("HEAD"))
	private void pulselib$observePip(OversizedItemRenderState state, PoseStack poses, CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.stage43bProbe"))
			Stage43bGuiProbe.atlas(true);
	}
}
