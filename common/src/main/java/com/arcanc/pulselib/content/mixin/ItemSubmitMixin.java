/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

/** ItemStackRenderState clears and reuses its mutable quad list after submission. */
@Mixin(SubmitNodeStorage.ItemSubmit.class)
public abstract class ItemSubmitMixin
{
	@ModifyVariable (method = "<init>", at = @At ("HEAD"), argsOnly = true, name = "quads")
	private static List<BakedQuad> pulselib$copyQuads(List<BakedQuad> quads)
	{
		return List.copyOf(quads);
	}

	@ModifyVariable (method = "<init>", at = @At ("HEAD"), argsOnly = true, name = "tintLayers")
	private static int[] pulselib$copyTints(int[] tintLayers)
	{
		return tintLayers.clone();
	}
}
