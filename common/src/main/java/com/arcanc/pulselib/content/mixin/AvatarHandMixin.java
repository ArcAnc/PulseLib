/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;

/** Native hand setup edits both arms and both sleeves before constructing its deferred node. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarHandMixin
{
	@WrapMethod(method = "renderHand")
	private void pulselib$restoreHand(PoseStack poses, SubmitNodeCollector collector, int light,
			Identifier skin, ModelPart arm, boolean sleeve, Operation<Void> original)
	{
		var model = ((AvatarRenderer<?>)(Object)this).getModel();
		int depth = ((PoseStackAccessor)poses).pulselib$getDepth();
		PoseStack.Pose entry = poses.last().copy();
		try (var snapshot = PModelPartSnapshot.capture(model.root()))
		{
			original.call(poses, collector, light, skin, arm, sleeve);
		}
		finally
		{
			while (((PoseStackAccessor)poses).pulselib$getDepth() > depth)
				poses.popPose();
			poses.last().set(entry);
		}
	}
}
