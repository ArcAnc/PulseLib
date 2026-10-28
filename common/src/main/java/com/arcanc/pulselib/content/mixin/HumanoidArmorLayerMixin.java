/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.player.deformer.PPlayerArmorGeometryLayer;
import com.arcanc.pulselib.content.player.deformer.PPlayerGeometryRenderState;
import com.arcanc.pulselib.content.player.deformer.PPlayerModelGeometry;
import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import com.arcanc.pulselib.platform.PClientRenderHooks;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.Map;

/** Resolves equipped shapes during extraction and binds immutable entries during native submission. */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin implements PPlayerArmorGeometryLayer
{
	@Unique
	private final Object pulselib$geometryKey = new Object();

	@Shadow
	protected abstract HumanoidModel<?> getArmorModel(HumanoidRenderState state, EquipmentSlot slot);

	@Override
	public void pulselib$captureArmorGeometry(Player player, AvatarRenderState state, float partialTick,
			Map<Object, Map<EquipmentSlot, PPlayerModelGeometry.Entry>> armor)
	{
		for (EquipmentSlot slot : List.of(EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD))
		{
			ItemStack stack = switch (slot)
			{
				case CHEST -> state.chestEquipment;
				case LEGS -> state.legsEquipment;
				case FEET -> state.feetEquipment;
				case HEAD -> state.headEquipment;
				default -> ItemStack.EMPTY;
			};
			if (!HumanoidArmorLayer.shouldRender(stack, slot))
				continue;
			HumanoidModel<?> original = this.getArmorModel(state, slot);
			EquipmentClientInfo.LayerType type = state.isBaby && state.entityType != EntityType.ARMOR_STAND ?
					EquipmentClientInfo.LayerType.HUMANOID_BABY : slot == EquipmentSlot.LEGS ?
					EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS : EquipmentClientInfo.LayerType.HUMANOID;
			try (var caller = PModelPartSnapshot.capture(original.root()))
			{
				PPlayerModelGeometry.setup(original, state);
				var model = PClientRenderHooks.armorModel(stack, type, original);
				var entry = PPlayerModelGeometry.capture(player, state, partialTick, original, model);
				PPlayerModelGeometry.putArmor(armor, this.pulselib$geometryKey, slot, entry);
			}
		}
	}

	@WrapMethod(method = "renderArmorPiece")
	private void pulselib$submitExtractedArmor(PoseStack poses, SubmitNodeCollector collector, ItemStack stack,
			EquipmentSlot slot, int light, HumanoidRenderState state, Operation<Void> original)
	{
		var owner = state instanceof PPlayerGeometryRenderState access ? access : null;
		var geometry = owner == null ? null : owner.pulselib$getPlayerGeometry();
		var entry = geometry == null ? null : geometry.armor(this.pulselib$geometryKey, slot);
		if (entry == null)
		{
			original.call(poses, collector, stack, slot, light, state);
			return;
		}
		var previous = owner.pulselib$getSubmittingGeometry();
		owner.pulselib$setSubmittingGeometry(entry);
		int depth = ((PoseStackAccessor)poses).pulselib$getDepth();
		PoseStack.Pose pose = poses.last().copy();
		try (var caller = PModelPartSnapshot.capture(entry.model().root()))
		{
			original.call(poses, collector, stack, slot, light, state);
		}
		finally
		{
			owner.pulselib$setSubmittingGeometry(previous);
			while (((PoseStackAccessor)poses).pulselib$getDepth() > depth)
				poses.popPose();
			poses.last().set(pose);
		}
	}
}
