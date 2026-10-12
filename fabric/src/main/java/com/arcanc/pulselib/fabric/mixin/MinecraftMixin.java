/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.fabric.mixin;

import com.arcanc.pulselib.content.event.PulseRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin (Minecraft.class)
public abstract class MinecraftMixin
{
	@Shadow
	public @Nullable ClientLevel level;

	@Inject (method = "setLevel", at = @At ("HEAD"))
	private void pulselib$worldChanged(@Nullable ClientLevel next, ReceivingLevelScreen.Reason reason, CallbackInfo callback)
	{
		if (this.level != null && this.level != next)
			PulseRuntime.disconnected();
	}
}
