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
import com.arcanc.pulselib.content.renderer.base.PModelPartSubmitState;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

/** Freezes each native ModelPart submission; reusable Model.Simple instances must not retain an old pose. */
@Mixin(ModelFeatureRenderer.Submit.class)
public abstract class ModelPartSubmitMixin implements PModelPartSubmitState
{
	@Shadow
	@Final
	private Model<?> model;

	@Unique
	private @Nullable PModelPartSnapshot pulselib$pose;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void pulselib$capturePart(CallbackInfo callback)
	{
		if (this.model instanceof Model.Simple)
			this.pulselib$pose = PModelPartSnapshot.captureForSubmit(this.model.root());
	}

	@Override
	public PModelPartSnapshot pulselib$getModelPartPose()
	{
		return Objects.requireNonNull(this.pulselib$pose, "Native node does not submit a ModelPart");
	}
}
