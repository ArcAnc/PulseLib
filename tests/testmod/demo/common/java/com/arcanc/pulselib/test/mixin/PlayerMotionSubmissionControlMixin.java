/**
 * @author ArcAnc
 * Created at: 10.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAutomaticMeshAttachments;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.test.PlayerMotionHarness;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Negative control bypasses the actual queue call only during the direct matrix probe. */
@Mixin(PPlayerAutomaticMeshAttachments.class)
public abstract class PlayerMotionSubmissionControlMixin
{
	@WrapOperation(method = "submitBone", at = @At(value = "INVOKE", target = "Lcom/arcanc/pulselib/content/renderer/PRenderQueue;submitEntityMesh(Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/arcanc/pulselib/content/renderer/plan/PGeometryData;Lcom/arcanc/pulselib/content/renderer/plan/PInstanceHeader;)V"))
	private static void pulselibtest$submission(RenderType type, PGeometryData geometry, PInstanceHeader data, Operation<Void> original)
	{
		if (!PlayerMotionHarness.probing() || !Boolean.getBoolean("pulselib.test.motionSkipSubmission"))
			original.call(type, geometry, data);
	}
}
