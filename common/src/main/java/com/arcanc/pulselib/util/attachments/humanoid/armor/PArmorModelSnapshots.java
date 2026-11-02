/**
 * @author ArcAnc
 * Created at: 02.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments.humanoid.armor;

import com.arcanc.pulselib.content.mixin.LivingEntityRendererAccessor;
import com.arcanc.pulselib.content.renderer.PWorldRenderEligibility;
import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

/** Owner-free provider selections and frozen poses shared by both native loader pipelines. */
@ApiStatus.Internal
public final class PArmorModelSnapshots
{
	private PArmorModelSnapshots()
	{
	}

	public static void capture(LivingEntity owner, HumanoidRenderState state, LivingEntityRenderer<?, ?, ?> renderer, float partialTick)
	{
		State access = (State)state;
		access.pulselib$setArmorProviders(Map.of());
		access.pulselib$setSubmittingArmorProvider(null);
		if (!(state.chestEquipment.getItem() instanceof PArmorModelProvider) && !(state.legsEquipment.getItem() instanceof PArmorModelProvider) &&
				!(state.feetEquipment.getItem() instanceof PArmorModelProvider) && !(state.headEquipment.getItem() instanceof PArmorModelProvider))
			return;
		Map<Object, Map<EquipmentSlot, Entry>> entries = new IdentityHashMap<>();
		for (var layer : ((LivingEntityRendererAccessor)renderer).pulselib$getLayers())
			if (layer instanceof Layer provider)
				provider.pulselib$captureArmorProviders(owner, state, partialTick, entries);
		access.pulselib$setArmorProviders(Map.copyOf(entries));
		if (!entries.isEmpty())
			PWorldRenderEligibility.markGeometry();
	}

	public static @Nullable Entry submitting(Object state)
	{
		return state instanceof State access ? access.pulselib$getSubmittingArmorProvider() : null;
	}

	public interface State
	{
		Map<Object, Map<EquipmentSlot, Entry>> pulselib$getArmorProviders();

		void pulselib$setArmorProviders(Map<Object, Map<EquipmentSlot, Entry>> entries);

		@Nullable Entry pulselib$getSubmittingArmorProvider();

		void pulselib$setSubmittingArmorProvider(@Nullable Entry entry);
	}

	public interface Layer
	{
		void pulselib$captureArmorProviders(LivingEntity owner, HumanoidRenderState state, float partialTick,
				Map<Object, Map<EquipmentSlot, Entry>> entries);
	}

	public interface Node
	{
		@Nullable Entry pulselib$getArmorProvider();
	}

	/** Baked resources and frozen geometry only; no owner, stack or provider callback is retained. */
	public record Entry(HumanoidModel<?> model, PModelPartSnapshot snapshot)
	{
	}
}
