package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.event.PulseRuntime;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.PackType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ReloadableResourceManager.class)
public abstract class ResourceReloadMixin
{
	@Shadow @Final private PackType type;

	@Inject(method = "createReload", at = @At("HEAD"))
	private void pulselib$collectModelDeclarations(CallbackInfoReturnable<?> callback)
	{
		if (this.type == PackType.CLIENT_RESOURCES)
			PulseRuntime.beforeResourceReload();
	}
}
