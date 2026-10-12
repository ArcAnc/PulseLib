/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.player.deformer.PDeformedCuboid;
import com.arcanc.pulselib.content.player.deformer.PDeformableCubeBakeScope;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(CubeDefinition.class)
public class CubeDefinitionMixin
{
	@Shadow @Final
	private Vector3f origin;
	@Shadow @Final
	private Vector3f dimensions;
	@Shadow @Final
	private CubeDeformation grow;
	@Shadow @Final
	private boolean mirror;
	@Shadow @Final
	private UVPair texCoord;
	@Shadow @Final
	private UVPair texScale;
	@Shadow @Final
	private Set<Direction> visibleFaces;

	@Inject(method = "bake", at = @At("HEAD"), cancellable = true)
	private void pulselib$bakePlayerDeformedCube(int textureWidth, int textureHeight,
												CallbackInfoReturnable<ModelPart.Cube> callback)
	{
		if (!PDeformableCubeBakeScope.isActive())
			return;
		callback.setReturnValue(new PDeformedCuboid(
				(int)this.texCoord.u(), (int)this.texCoord.v(),
				this.origin.x(), this.origin.y(), this.origin.z(),
				this.dimensions.x(), this.dimensions.y(), this.dimensions.z(),
				this.grow.growX, this.grow.growY, this.grow.growZ,
				this.mirror,
				textureWidth * this.texScale.u(), textureHeight * this.texScale.v(), this.visibleFaces));
	}
}
