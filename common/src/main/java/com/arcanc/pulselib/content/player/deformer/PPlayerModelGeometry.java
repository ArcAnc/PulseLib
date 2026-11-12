/**
 * @author ArcAnc
 * Created at: 28.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.deformer;

import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import com.arcanc.pulselib.content.renderer.base.PPlayerRigRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

/** One avatar's finite baked shapes. Entries hold no live animation or owner callbacks. */
@ApiStatus.Internal
public final class PPlayerModelGeometry
{
	private final Entry main;
	private final Map<Object, Map<EquipmentSlot, Entry>> armor;

	public PPlayerModelGeometry(Entry main, Map<Object, Map<EquipmentSlot, Entry>> armor)
	{
		this.main = main;
		this.armor = new IdentityHashMap<>();
		armor.forEach((key, slots) -> this.armor.put(key, Map.copyOf(slots)));
	}

	public @Nullable Entry main(Model<?> model)
	{
		return this.main.model == model ? this.main : null;
	}

	public @Nullable Entry armor(Object layerKey, EquipmentSlot slot)
	{
		Map<EquipmentSlot, Entry> slots = this.armor.get(layerKey);
		return slots == null ? null : slots.get(slot);
	}

	/** Armor hooks may choose material or submit additional geometry later. */
	public boolean hasArmor()
	{
		return !this.armor.isEmpty();
	}

	public static @Nullable Entry submitting(Object state)
	{
		return state instanceof PPlayerGeometryRenderState access ? access.pulselib$getSubmittingGeometry() : null;
	}

	public static @Nullable Entry main(Object state, Model<?> model)
	{
		PPlayerModelGeometry geometry = state instanceof PPlayerGeometryRenderState access ? access.pulselib$getPlayerGeometry() : null;
		return geometry == null ? null : geometry.main(model);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void setup(Model<?> model, AvatarRenderState state)
	{
		((Model)model).setupAnim(state);
		if (model instanceof HumanoidModel<?> humanoid && !(model instanceof PlayerModel) && state instanceof PPlayerRigRenderState rig && rig.pulselib$getPlayerRig() != null)
			rig.pulselib$getPlayerRig().apply(humanoid);
	}

	public static Entry capture(Player player, AvatarRenderState state, float partialTick, Model<?> original, Model<?> model)
	{
		try (var caller = PModelPartSnapshot.capture(model.root()))
		{
			setup(model, state);
			if (model instanceof PlayerModel playerModel)
				PPlayerMeshDeformers.apply(player, playerModel, partialTick);
			else if (model instanceof HumanoidModel<?> humanoid)
				PPlayerMeshDeformers.apply(player, humanoid, partialTick);
			return new Entry(original, model, PPlayerDeformerSnapshot.captureGeometry(model.root()));
		}
	}

	public static void putArmor(Map<Object, Map<EquipmentSlot, Entry>> armor, Object layer, EquipmentSlot slot, Entry entry)
	{
		armor.computeIfAbsent(layer, ignored -> new EnumMap<>(EquipmentSlot.class)).put(slot, entry);
	}

	/** Model references are the same baked resources retained by native ModelSubmit. */
	public record Entry(Model<?> original, Model<?> model, PPlayerDeformerSnapshot geometry)
	{
	}
}
