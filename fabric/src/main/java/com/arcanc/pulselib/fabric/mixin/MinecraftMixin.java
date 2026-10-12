package com.arcanc.pulselib.fabric.mixin;

import com.arcanc.pulselib.content.event.PulseRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin
{
	@Shadow public ClientLevel level;

	@Inject(method = "setLevel", at = @At("HEAD"))
	private void pulselib$worldChanged(ClientLevel next, net.minecraft.client.gui.screens.ReceivingLevelScreen.Reason reason, CallbackInfo callback)
	{
		if (this.level != null && this.level != next)
			PulseRuntime.disconnected();
	}
}
