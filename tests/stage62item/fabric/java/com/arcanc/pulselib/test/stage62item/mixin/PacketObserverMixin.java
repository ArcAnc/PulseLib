/**
 * @author ArcAnc
 * Created at: 31.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62item.mixin;

import com.arcanc.pulselib.test.stage62item.ItemClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs after thread handoff, before the decoded packet can reach a GUI renderer. */
@Mixin(ClientPacketListener.class)
public abstract class PacketObserverMixin
{
	@Inject(method = "handleContainerContent", at = @At("RETURN"))
	private void content(ClientboundContainerSetContentPacket packet, CallbackInfo callback)
	{
		ItemClient.packet(packet);
	}
}
