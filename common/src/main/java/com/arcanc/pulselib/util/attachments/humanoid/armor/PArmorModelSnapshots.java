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
import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Owner-free provider selections and frozen poses shared by both native loader pipelines. */
@ApiStatus.Internal
public final class PArmorModelSnapshots
{
	private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
	private static final ThreadLocal<@Nullable ItemStack> OWNER_ARMOR = new ThreadLocal<>();

	private PArmorModelSnapshots()
	{
	}

	public static void capture(LivingEntity owner, HumanoidRenderState state, LivingEntityRenderer<?, ?, ?> renderer, float partialTick)
	{
		State access = (State)state;
		access.pulselib$setArmorProviders(Map.of());
		access.pulselib$setSubmittingArmorProvider(null);
		@Nullable Set<EquipmentSlot> hidden = null;
		boolean hasProvider = false;
		for (EquipmentSlot slot : ARMOR_SLOTS)
		{
			ItemStack stack = switch (slot)
			{
				case HEAD -> state.headEquipment;
				case CHEST -> state.chestEquipment;
				case LEGS -> state.legsEquipment;
				case FEET -> state.feetEquipment;
				default -> throw new IllegalArgumentException("Not a humanoid armor slot: " + slot);
			};
			if (!stack.isEmpty() && PLivingAttachments.hidesVanillaArmor(owner.getItemBySlot(slot), slot, owner))
			{
				if (hidden == null)
					hidden = EnumSet.noneOf(EquipmentSlot.class);
				hidden.add(slot);
			}
			else if (stack.getItem() instanceof PArmorModelProvider)
				hasProvider = true;
		}
		access.pulselib$setHiddenArmor(hidden == null ? Set.of() : Set.copyOf(hidden));
		if (!hasProvider)
			return;
		Map<Object, Map<EquipmentSlot, Entry>> entries = new IdentityHashMap<>();
		for (var layer : ((LivingEntityRendererAccessor)renderer).pulselib$getLayers())
			if (layer instanceof Layer provider)
				provider.pulselib$captureArmorProviders(owner, state, partialTick, entries);
		access.pulselib$setArmorProviders(Map.copyOf(entries));
		if (!entries.isEmpty())
			PWorldRenderEligibility.markGeometry();
	}

	/** Native extensions can use the extracted owner-aware policy while preserving their stack-only fallback. */
	public static <T extends @Nullable Object> T withCapturedArmor(ItemStack stack, Supplier<T> action)
	{
		ItemStack previous = OWNER_ARMOR.get();
		OWNER_ARMOR.set(stack);
		try
		{
			return action.get();
		}
		finally
		{
			if (previous == null)
				OWNER_ARMOR.remove();
			else
				OWNER_ARMOR.set(previous);
		}
	}

	public static boolean usesCapturedArmorPolicy(ItemStack stack)
	{
		return OWNER_ARMOR.get() == stack;
	}

	public static @Nullable Entry submitting(Object state)
	{
		return state instanceof State access ? access.pulselib$getSubmittingArmorProvider() : null;
	}

	public interface State
	{
		@Nullable Set<EquipmentSlot> pulselib$getHiddenArmor();

		void pulselib$setHiddenArmor(Set<EquipmentSlot> slots);

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
