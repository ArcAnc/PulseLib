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
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

/** Captures transforms, visibility and deformed geometry before a shared native ModelPart can be reused. */
@Mixin(SubmitNodeStorage.ModelPartSubmit.class)
public abstract class ModelPartSubmitMixin implements PModelPartSubmitState
{
	@Shadow
	@Final
	private ModelPart modelPart;

	@Unique
	private @Nullable PModelPartSnapshot pulselib$pose;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void pulselib$capturePart(CallbackInfo callback)
	{
		this.pulselib$pose = PModelPartSnapshot.captureForSubmit(this.modelPart);
	}

	@Override
	public PModelPartSnapshot pulselib$getModelPartPose()
	{
		return Objects.requireNonNull(this.pulselib$pose);
	}
}
