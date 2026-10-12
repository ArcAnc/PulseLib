package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.test.ClientTestPlugin;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PLivingAttachmentLayer.class)
public abstract class SmokeLayerMixin
{
	@Inject(method = "render", at = @At("TAIL"))
	private void pulselibtest$layer(CallbackInfo callback) { ClientTestPlugin.layerRenders++; }
}
