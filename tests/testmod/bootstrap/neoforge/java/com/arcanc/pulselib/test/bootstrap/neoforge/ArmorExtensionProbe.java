/**
 * @author ArcAnc
 * Created at: 11.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.bootstrap.neoforge;

import com.arcanc.pulselib.test.bootstrap.BootstrapChecks;
import com.arcanc.pulselib.test.bootstrap.ClientTestPlugin;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentDefinition;
import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorClientExtensions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Exercises custom item-extension composition, native callbacks and armor-hiding precedence. */
public final class ArmorExtensionProbe
{
	private static int epochs;

	private ArmorExtensionProbe()
	{
	}

	public static void tick(Minecraft mc)
	{
		if (mc.level == null || mc.player == null || mc.getOverlay() != null || ClientTestPlugin.models <= epochs)
			return;
		var item = Items.LEATHER_CHESTPLATE;
		BootstrapChecks.check(!PLivingAttachments.contains(item), "Extension probe requires an untouched attachment key");
		var stack = new ItemStack(item);
		Model<Unit> original = new Model.Simple(new ModelPart(List.of(), Map.of()), RenderTypes::armorCutoutNoCull);
		var humanoid = new HumanoidModel<HumanoidRenderState>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0), 64, 32).bakeRoot());
		Model<Unit> selected = new Model.Simple(new ModelPart(List.of(), Map.of()), RenderTypes::armorCutoutNoCull);
		Identifier texture = BootstrapChecks.id("armor_delegate");
		Map<String, Integer> calls = new HashMap<>();
		Map<String, List<Object>> arguments = new HashMap<>();
		IClientItemExtensions base = (IClientItemExtensions)Proxy.newProxyInstance(IClientItemExtensions.class.getClassLoader(),
				new Class<?>[]{IClientItemExtensions.class}, (proxy, method, args) ->
				{
					calls.merge(method.getName(), 1, Integer::sum);
					arguments.put(method.getName(), List.of(args));
					return switch (method.getName())
					{
						case "getFont" -> mc.font;
						case "getArmPose" -> HumanoidModel.ArmPose.EMPTY;
						case "getHumanoidArmorModel" -> humanoid;
						case "getGenericArmorModel" -> selected;
						case "applyForgeHandTransform" -> true;
						case "shouldBobAsEntity", "shouldSpreadAsEntity" -> false;
						case "getArmorLayerTintColor" -> 0x11223344;
						case "getDefaultDyeColor" -> 0x55667788;
						case "getArmorTexture", "getScopeOverlayTexture" -> texture;
						case "setupModelAnimations", "renderFirstPersonOverlay" -> null;
						default -> throw new AssertionError("Unexpected delegated method " + method.getName());
					};
				});
		try
		{
			BootstrapChecks.check(PArmorClientExtensions.buildFor(item, base) == base, "Unrelated item preserves custom extension identity");
			BootstrapChecks.check(PArmorClientExtensions.buildFor(item, null) == IClientItemExtensions.DEFAULT, "Null base resolves to native default");
			PLivingAttachments.register(item, definition(false));
			var wrapper = PArmorClientExtensions.buildFor(item, base);
			BootstrapChecks.check(wrapper instanceof PArmorClientExtensions && wrapper != base, "Applicable item composes the existing extension");
			var layer = new EquipmentClientInfo.Layer(texture);
			var pose = new PoseStack();
			var graphics = new GuiGraphicsExtractor(mc, new GuiRenderState(), 0, 0);
			var delta = mc.getDeltaTracker();
			var type = EquipmentClientInfo.LayerType.HUMANOID;
			BootstrapChecks.check(wrapper.getFont(stack, IClientItemExtensions.FontContext.ITEM_COUNT) == mc.font, "Delegated font");
			BootstrapChecks.check(wrapper.getArmPose(mc.player, InteractionHand.MAIN_HAND, stack) == HumanoidModel.ArmPose.EMPTY, "Delegated arm pose");
			BootstrapChecks.check(wrapper.applyForgeHandTransform(pose, mc.player, HumanoidArm.RIGHT, stack, 0.25f, 0.5f, 0.75f), "Delegated hand transform");
			BootstrapChecks.check(wrapper.getHumanoidArmorModel(stack, type, original) == humanoid, "Delegated humanoid model");
			BootstrapChecks.check(wrapper.getGenericArmorModel(stack, type, original) == selected, "Delegated generic model");
			wrapper.setupModelAnimations(mc.player, stack, EquipmentSlot.CHEST, original, 1, 2, 3, 4, 5, 6);
			wrapper.renderFirstPersonOverlay(stack, EquipmentSlot.CHEST, mc.player, graphics, delta);
			BootstrapChecks.check(!wrapper.shouldBobAsEntity(stack) && !wrapper.shouldSpreadAsEntity(stack), "Delegated dropped-item policy");
			BootstrapChecks.check(wrapper.getArmorLayerTintColor(stack, layer, 2, 3) == 0x11223344, "Delegated layer tint");
			BootstrapChecks.check(wrapper.getDefaultDyeColor(stack) == 0x55667788, "Delegated dye color");
			BootstrapChecks.check(wrapper.getArmorTexture(stack, type, layer, texture) == texture && wrapper.getScopeOverlayTexture(stack) == texture, "Delegated texture queries");
			BootstrapChecks.check(calls.size() == 13 && calls.values().stream().allMatch(count -> count == 1), "Every native callback delegated exactly once");
			BootstrapChecks.check(arguments.get("applyForgeHandTransform").equals(List.of(pose, mc.player, HumanoidArm.RIGHT, stack, 0.25f, 0.5f, 0.75f)), "Hand callback receives original arguments");
			BootstrapChecks.check(arguments.get("setupModelAnimations").equals(List.of(mc.player, stack, EquipmentSlot.CHEST, original, 1f, 2f, 3f, 4f, 5f, 6f)), "Setup callback receives original arguments");
			BootstrapChecks.check(arguments.get("renderFirstPersonOverlay").equals(List.of(stack, EquipmentSlot.CHEST, mc.player, graphics, delta)), "Overlay callback receives original arguments");
			BootstrapChecks.check(new PArmorClientExtensions().getArmorLayerTintColor(stack, layer, 0, 123) == 123, "Native default tint policy preserved");
			var legacy = (PArmorClientExtensions)wrapper;
			BootstrapChecks.check(legacy.getHumanoidArmorModel(mc.player, stack, EquipmentSlot.LEGS, humanoid) == humanoid, "Canonical humanoid callback retains custom model");
			BootstrapChecks.check(arguments.get("getHumanoidArmorModel").get(1) == EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, "Canonical slot maps to native layer type");
			BootstrapChecks.check(legacy.getGenericArmorModel(mc.player, stack, EquipmentSlot.CHEST, humanoid) == selected, "Canonical generic callback delegates native model");
			legacy.renderHelmetOverlay(stack, mc.player, graphics, delta);
			BootstrapChecks.check(arguments.get("renderFirstPersonOverlay").get(1) == EquipmentSlot.HEAD, "Canonical helmet overlay maps to native head slot");
			BootstrapChecks.check(legacy.getArmorLayerTintColor(stack, mc.player, layer, 2, 3) == 0x11223344, "Canonical tint callback delegates custom color");
			PLivingAttachments.register(item, definition(true));
			BootstrapChecks.check(wrapper.getGenericArmorModel(stack, type, original) != selected && wrapper.getArmorLayerTintColor(stack, layer, 2, 3) == 0, "Hidden armor takes precedence over custom model/tint");
			BootstrapChecks.check(calls.get("getGenericArmorModel") == 2 && calls.get("getArmorLayerTintColor") == 2, "Hidden armor does not query the delegated model or tint");
			BootstrapChecks.check(legacy.getHumanoidArmorModel(mc.player, stack, EquipmentSlot.CHEST, humanoid) == humanoid &&
					humanoid.root().getAllParts().stream().noneMatch(part -> part.visible), "Canonical hidden humanoid hides every native model part");
			epochs = ClientTestPlugin.models;
			PLibDatabase.LOGGER.info("ARMOR EXTENSION PASS: epoch={}, delegatedCallbacks=13, factoryCases=3, canonicalCalls=5, hidePrecedence=true", epochs);
		}
		finally
		{
			PLivingAttachments.items().remove(item);
		}
	}

	public static void stopped()
	{
		BootstrapChecks.check(epochs == 2, "Extension composition verified before and after reload");
	}

	private static PLivingAttachmentDefinition definition(boolean hide)
	{
		return new PLivingAttachmentDefinition(ClientTestPlugin.GLTF, (entity, slot, stack) -> true, List.of(),
				(entity, stack, bone, mesh, inherited, partialTick) -> inherited, hide);
	}
}
