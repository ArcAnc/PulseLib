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
import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorModelSnapshots;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Replays the model chosen during extraction rather than invoking the extension a second time. */
@Mixin(EquipmentLayerRenderer.class)
public abstract class NeoForgeArmorModelMixin
{
	@WrapOperation(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
			at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/extensions/common/IClientItemExtensions;getGenericArmorModel(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/client/model/Model;)Lnet/minecraft/client/model/Model;"))
	private Model<?> pulselib$useExtractedModel(IClientItemExtensions extensions, ItemStack stack,
			EquipmentClientInfo.LayerType type, Model<?> model, Operation<Model<?>> original,
			@Local(argsOnly = true) Object state)
	{
		var provider = PArmorModelSnapshots.submitting(state);
		if (provider != null && provider.model() == model)
			return provider.model();
		var entry = PPlayerModelGeometry.submitting(state);
		return entry != null && entry.original() == model ? entry.model() : original.call(extensions, stack, type, model);
	}
}
