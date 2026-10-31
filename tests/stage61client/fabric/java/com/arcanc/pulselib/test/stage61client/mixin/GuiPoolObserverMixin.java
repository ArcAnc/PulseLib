/**
 * @author ArcAnc
 * Created at: 31.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage61client.mixin;

import com.arcanc.pulselib.content.renderer.PModelGuiRenderer;
import com.arcanc.pulselib.content.renderer.PModelGuiRenderState;
import com.arcanc.pulselib.test.stage61client.GuiProbe;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts actual renderer identities selected by the native Fabric PiP pool. */
@Mixin(PModelGuiRenderer.class)
public abstract class GuiPoolObserverMixin
{
	@Inject(method = "prepare(Lcom/arcanc/pulselib/content/renderer/PModelGuiRenderState;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;I)V", at = @At("HEAD"))
	private void prepare(PModelGuiRenderState state, GuiRenderState gui, int scale, CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.fabricGuiProbe"))
			GuiProbe.prepared(this);
	}
}
