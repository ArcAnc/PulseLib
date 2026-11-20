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
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
import java.util.Map;

/** Installs the fixture renderer only in a fresh integration client. */
@Mixin(EntityRenderers.class)
public abstract class EntityFixtureRegistration
{
	@ModifyReturnValue(method = "createEntityRenderers", at = @At("RETURN"))
	private static Map<EntityType<?>, EntityRenderer<?, ?>> scene$register(Map<EntityType<?>, EntityRenderer<?, ?>> renderers,
			EntityRendererProvider.Context context)
	{
		if (!Boolean.getBoolean("pulselib.scene.integration"))
			return renderers;
		var fixtureRenderers = new HashMap<>(renderers);
		fixtureRenderers.put(EntityTypes.ARMOR_STAND, new OwnerIntegrationChecks.EntityRenderer(context));
		return fixtureRenderers;
	}
}
