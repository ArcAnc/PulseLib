package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.test.CommonTestPlugin;
import com.arcanc.pulselib.test.DemoContent;
import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import net.minecraft.core.BlockPos;

import com.arcanc.pulselib.plugin.PulsePluginManager;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class SmokeServerMixin
{
	@Shadow public abstract boolean isDedicatedServer();
	@Shadow public abstract void halt(boolean wait);

	@Inject(method = "tickServer", at = @At("TAIL"))
	private void pulselibtest$serverCheck(CallbackInfo callback)
	{
		if (!Boolean.getBoolean("pulselib.test.auto") || !this.isDedicatedServer())
			return;
		if (CommonTestPlugin.constructions != 1 || CommonTestPlugin.registrations != 1 || CommonTestPlugin.events != 1 || CommonTestPlugin.deformers != 1 || PulsePluginManager.get().pluginIds().size() != 1)
			throw new AssertionError("Dedicated server plugin discovery failed");
		var server = (MinecraftServer)(Object)this;
		var entity = DemoContent.entity.get().create(server.overworld());
		if (entity == null) throw new AssertionError("Original demo entity must construct on server");
		var block = new TestBlockEntity(BlockPos.ZERO, DemoContent.block.get().defaultBlockState());
		InstanceAnimationManager.removeManager(entity);
		InstanceAnimationManager.removeManager(block);
		PLibDatabase.LOGGER.info("PULSE TEST SERVER PASS: common plugin registered once, client plugin skipped before class loading");
		this.halt(false);
	}
}
