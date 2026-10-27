/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;

/** An item layer can fail after its own pose push or after a special renderer's extra pushes. */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public abstract class ItemLayerSubmitMixin
{
	@WrapMethod(method = "submit")
	private void pulselib$restoreLayerPose(PoseStack poses, SubmitNodeCollector collector,
			int light, int overlay, int outline, Operation<Void> original)
	{
		int depth = ((PoseStackAccessor)poses).pulselib$getDepth();
		PoseStack.Pose entry = poses.last().copy();
		try
		{
			original.call(poses, collector, light, overlay, outline);
		}
		finally
		{
			while (((PoseStackAccessor)poses).pulselib$getDepth() > depth)
				poses.popPose();
			poses.last().set(entry);
		}
	}
}
