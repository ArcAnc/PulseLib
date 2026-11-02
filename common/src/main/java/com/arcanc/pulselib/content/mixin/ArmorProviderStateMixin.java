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
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;

@Mixin(HumanoidRenderState.class)
public abstract class ArmorProviderStateMixin implements PArmorModelSnapshots.State
{
	@Unique
	private Map<Object, Map<EquipmentSlot, PArmorModelSnapshots.Entry>> pulselib$providers = Map.of();
	@Unique
	private PArmorModelSnapshots.@Nullable Entry pulselib$provider;

	@Override
	public Map<Object, Map<EquipmentSlot, PArmorModelSnapshots.Entry>> pulselib$getArmorProviders()
	{
		return this.pulselib$providers;
	}

	@Override
	public void pulselib$setArmorProviders(Map<Object, Map<EquipmentSlot, PArmorModelSnapshots.Entry>> entries)
	{
		this.pulselib$providers = entries;
	}

	@Override
	public PArmorModelSnapshots.@Nullable Entry pulselib$getSubmittingArmorProvider()
	{
		return this.pulselib$provider;
	}

	@Override
	public void pulselib$setSubmittingArmorProvider(PArmorModelSnapshots.@Nullable Entry entry)
	{
		this.pulselib$provider = entry;
	}
}
