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

/**
 * Clears client animation and rendering state when Fabric disconnects or changes client levels.
 */
@Mixin (Minecraft.class)
public abstract class MinecraftMixin
{
	/**
	 * Current client level, or {@code null} while disconnected.
	 */
	@Shadow
	public @Nullable ClientLevel level;

	/**
	 * Clears shared runtime state when disconnecting from an active client level.
	 *
	 * @param callback the Mixin callback used to inspect or replace the intercepted result
	 */
	@Inject (method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At ("HEAD"))
	private void pulselib$disconnect(CallbackInfo callback)
	{
		if (this.level != null)
			PulseRuntime.disconnected();
	}

	/**
	 * Clears shared runtime state before replacing the current client level.
	 *
	 * @param next the next; may be {@code null}
	 * @param reason the reason
	 * @param callback the Mixin callback used to inspect or replace the intercepted result
	 */
	@Inject (method = "setLevel", at = @At ("HEAD"))
	private void pulselib$worldChanged(@Nullable ClientLevel next, ReceivingLevelScreen.Reason reason, CallbackInfo callback)
	{
		if (this.level != null && this.level != next)
			PulseRuntime.disconnected();
	}
}
