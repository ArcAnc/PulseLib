/**
 * @author ArcAnc
 * Created at: 26.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1.mixin;

import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.test.stage1.Stage52LegacyOcclusionProbe;
import com.arcanc.pulselib.test.stage1.Stage52FirstPersonArmorProbe;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.test.stage1.Stage52AttachmentProbe;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes real submissions and brackets the existing GPU hand flush without replacing it. */
@Mixin(PRenderQueue.class)
public abstract class Stage52AttachmentQueueMixin
{
	@Inject(method = {"submitEntityMesh(Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/arcanc/pulselib/content/renderer/plan/PGeometryData;Lcom/arcanc/pulselib/content/renderer/plan/PInstanceHeader;)V",
			"submitFirstPersonMesh"}, at = @At("HEAD"))
	private static void pulselib$captureMatrix(RenderType type, PGeometryData geometry, PInstanceHeader data, CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.stage52aProbe"))
			Stage52AttachmentProbe.submitted(data.transform());
	}

	@Inject(method = "submit(Lcom/arcanc/pulselib/content/renderer/PRenderQueue$RenderStage;Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/arcanc/pulselib/content/renderer/plan/PGeometryData;Lcom/arcanc/pulselib/content/renderer/plan/PInstanceHeader;Z)V", at = @At("RETURN"))
	private static void pulselib$verifyQueuedStage(PRenderQueue.RenderStage stage, RenderType type, PGeometryData geometry, PInstanceHeader data, boolean transparent, CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.stage52aProbe"))
			Stage52AttachmentProbe.queued(stage, geometry, data.transform());
	}

	@Inject(method = "submitEntityMesh(Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/arcanc/pulselib/content/model/baked/PBakedMesh;Lcom/arcanc/pulselib/content/model/deformer/PMeshDeformation;Lcom/arcanc/pulselib/content/renderer/PRenderQueue$InstanceData;Lcom/arcanc/pulselib/content/renderer/plan/PTransparencyMode;)V", at = @At("HEAD"))
	private static void pulselib$observeLegacyPolicy(RenderType type, PBakedMesh mesh, @Nullable PMeshDeformation deformation,
			PRenderQueue.InstanceData data, PTransparencyMode transparency, CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.stage52b4cProbe"))
			Stage52LegacyOcclusionProbe.submitted(type, transparency);
		if (Boolean.getBoolean("pulselib.firstPersonArmorProbe"))
			Stage52FirstPersonArmorProbe.submitted(type, transparency);
	}

	@WrapMethod(method = "flush(Lcom/arcanc/pulselib/content/renderer/PRenderQueue$RenderStage;Lcom/mojang/blaze3d/pipeline/RenderTarget;)V")
	private static void pulselib$measureNativeHandFlush(PRenderQueue.RenderStage stage, RenderTarget depth, Operation<Void> original)
	{
		int query = Boolean.getBoolean("pulselib.stage52aProbe") && stage == PRenderQueue.RenderStage.FIRST_PERSON ? Stage52AttachmentProbe.beginHandFlush() : 0;
		try
		{
			original.call(stage, depth);
			if (Boolean.getBoolean("pulselib.firstPersonArmorProbe") && stage == PRenderQueue.RenderStage.FIRST_PERSON)
				Stage52FirstPersonArmorProbe.afterFlush();
		}
		finally
		{
			if (query != 0)
				Stage52AttachmentProbe.endHandFlush(query);
		}
	}
}
