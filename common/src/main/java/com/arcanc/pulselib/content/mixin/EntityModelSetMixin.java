/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.player.deformer.PDeformableCubeBakeScope;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityModelSet.class)
public class EntityModelSetMixin
{
	@Redirect(method = "bakeLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/builders/LayerDefinition;bakeRoot()Lnet/minecraft/client/model/geom/ModelPart;"))
	private ModelPart pulselib$bakePlayerLayerWithDeformableCubes(LayerDefinition definition, ModelLayerLocation location)
	{
		String modelPath = location.getModel().getPath();
		if (!"minecraft".equals(location.getModel().getNamespace()) ||
				(!"player".equals(modelPath) && !"player_slim".equals(modelPath)))
			return definition.bakeRoot();
		try (PDeformableCubeBakeScope.Scope ignored = PDeformableCubeBakeScope.begin())
		{
			return definition.bakeRoot();
		}
	}
}
