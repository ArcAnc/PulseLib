/**
 * @author ArcAnc
 * Created at: 02.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorModelSnapshots;
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
public abstract class ArmorProviderSubmitMixin implements PArmorModelSnapshots.Node
{
	@Shadow
	@Final
	private Model<?> model;
	@Shadow
	@Final
	private Object state;
	@Unique
	private PArmorModelSnapshots.@Nullable Entry pulselib$armorProvider;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void pulselib$captureProvider(CallbackInfo callback)
	{
		var entry = PArmorModelSnapshots.submitting(this.state);
		this.pulselib$armorProvider = entry != null && entry.model() == this.model ? entry : null;
	}

	@Override
	public PArmorModelSnapshots.@Nullable Entry pulselib$getArmorProvider()
	{
		return this.pulselib$armorProvider;
	}
}
