/**
 * @author ArcAnc
 * Created at: 20.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene.mixin;

import com.arcanc.pulselib.porting.scene.OwnerIntegrationChecks;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
import java.util.Map;

/** Installs the fixture renderer only in a fresh integration client. */
@Mixin(BlockEntityRenderers.class)
public abstract class BlockEntityFixtureRegistration
{
	@ModifyReturnValue(method = "createEntityRenderers", at = @At("RETURN"))
	private static Map<BlockEntityType<?>, BlockEntityRenderer<?, ?>> scene$register(Map<BlockEntityType<?>, BlockEntityRenderer<?, ?>> renderers,
			BlockEntityRendererProvider.Context context)
	{
		if (!Boolean.getBoolean("pulselib.scene.integration"))
			return renderers;
		var fixtureRenderers = new HashMap<>(renderers);
		fixtureRenderers.put(BlockEntityTypes.CHEST, new OwnerIntegrationChecks.BlockRenderer());
		return fixtureRenderers;
	}
}
