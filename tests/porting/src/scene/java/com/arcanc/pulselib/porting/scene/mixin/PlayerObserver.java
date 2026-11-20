/**
 * @author ArcAnc
 * Created at: 20.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene.mixin;

import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.porting.scene.PlayerChecks;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Function;

public final class PlayerObserver
{
	private PlayerObserver()
	{
	}

	/** Uses the native registry and equipment data for the fixture's one provider item. */
	@Mixin(Items.class)
	public abstract static class ItemsFixture
	{
		@WrapOperation(method = "registerItem(Lnet/minecraft/resources/ResourceKey;Ljava/util/function/Function;Lnet/minecraft/world/item/Item$Properties;)Lnet/minecraft/world/item/Item;",
				at = @At(value = "INVOKE", target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;"))
		private static Object scene$provider(Function<?, ?> factory, Object properties, Operation<Object> original,
				@Local(argsOnly = true) ResourceKey<Item> id)
		{
			if (Boolean.getBoolean("pulselib.scene.player") && id.identifier().equals(Identifier.withDefaultNamespace("iron_chestplate")))
				return new PlayerChecks.ProviderArmor((Item.Properties)properties);
			Object item = original.call(factory, properties);
			if (Boolean.getBoolean("pulselib.scene.player") && id.identifier().equals(Identifier.withDefaultNamespace("iron_helmet")))
				PlayerChecks.registerHelmet((Item)item);
			return item;
		}
	}

	@Mixin(AvatarRenderer.class)
	public abstract static class Hand
	{
		@Inject(method = "renderHand", at = @At("HEAD"))
		private void scene$arm(PoseStack poses, SubmitNodeCollector collector, int light, Identifier skin, ModelPart arm, boolean sleeve, CallbackInfo callback)
		{
			PlayerChecks.arm(poses);
		}
	}

	@Mixin(ItemInHandRenderer.class)
	public abstract static class HeldItem
	{
		@Inject(method = "renderItem", at = @At("HEAD"))
		private void scene$item(LivingEntity owner, ItemStack stack, ItemDisplayContext display, PoseStack poses,
				SubmitNodeCollector collector, int light, CallbackInfo callback)
		{
			PlayerChecks.item(display, stack, poses);
		}
		@Inject(method = "renderPlayerArm", at = @At("HEAD"))
		private void scene$emptyArm(PoseStack poses, SubmitNodeCollector collector, int light, float equip, float swing, HumanoidArm arm, CallbackInfo callback)
		{
			PlayerChecks.emptyArm();
		}

	}

	@Mixin(ModelFeatureRenderer.class)
	public abstract static class ModelFeature
	{
		@WrapMethod(method = "buildGroup")
		private void scene$models(FeatureFrameContext context, List<ModelFeatureRenderer.Submit<?>> submits, Operation<Void> original)
		{
			PlayerChecks.modelBatch(submits, () -> original.call(context, submits));
		}

		@WrapOperation(method = "prepareModel", at = @At(value = "INVOKE", target =
				"Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
		private void scene$pose(Model<?> model, PoseStack poses, VertexConsumer vertices, int light, int overlay, int color, Operation<Void> original,
				@Local(argsOnly = true) ModelFeatureRenderer.Submit<?> submit)
		{
			PlayerChecks.replay(submit);
			original.call(model, poses, vertices, light, overlay, color);
		}
	}

	@Mixin(EquipmentLayerRenderer.class)
	public abstract static class Equipment
	{
		@Inject(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
				at = @At("HEAD"))
		private void scene$equipment(EquipmentClientInfo.LayerType type, ResourceKey<EquipmentAsset> id, Model<?> model, Object state,
				ItemStack item, PoseStack poses, SubmitNodeCollector collector, int light, @Nullable Identifier override, int outline,
				int order, CallbackInfo callback)
		{
			PlayerChecks.equipment(state, item);
		}
	}

	@Mixin(value = PRenderQueue.class, remap = false)
	public abstract static class Queue
	{
		@Inject(method = "submitFirstPersonMesh(Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/arcanc/pulselib/content/renderer/plan/PGeometryData;Lcom/arcanc/pulselib/content/renderer/plan/PInstanceHeader;)V",
				at = @At("RETURN"))
		private static void scene$handMesh(RenderType type, PGeometryData geometry, PInstanceHeader instance, CallbackInfo callback)
		{
			PlayerChecks.automaticSubmission(instance, true);
		}

		@Inject(method = "submitEntityMesh(Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/arcanc/pulselib/content/renderer/plan/PGeometryData;Lcom/arcanc/pulselib/content/renderer/plan/PInstanceHeader;)V",
				at = @At("RETURN"))
		private static void scene$worldMesh(RenderType type, PGeometryData geometry, PInstanceHeader instance, CallbackInfo callback)
		{
			PlayerChecks.automaticSubmission(instance, false);
		}
	}
}
