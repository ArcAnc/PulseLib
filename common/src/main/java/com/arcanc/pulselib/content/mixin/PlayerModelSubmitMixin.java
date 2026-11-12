/**
 * @author ArcAnc
 * Created at: 28.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.player.deformer.PPlayerModelGeometry;
import com.arcanc.pulselib.content.player.deformer.PPlayerModelGeometry.Entry;
import com.arcanc.pulselib.content.player.deformer.PPlayerModelSubmitState;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeStorage.ModelSubmit.class)
public abstract class PlayerModelSubmitMixin implements PPlayerModelSubmitState
{
	@Shadow
	@Final
	private Model<?> model;
	@Shadow
	@Final
	private Object state;
	@Unique
	private @Nullable Entry pulselib$geometry;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void pulselib$captureGeometry(CallbackInfo callback)
	{
		var entry = PPlayerModelGeometry.submitting(this.state);
		this.pulselib$geometry = entry != null && entry.model() == this.model ? entry : PPlayerModelGeometry.main(this.state, this.model);
	}

	@Override
	public @Nullable Entry pulselib$getModelGeometry()
	{
		return this.pulselib$geometry;
	}
}
