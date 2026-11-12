/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.platform;

import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonPassTime;
import com.arcanc.pulselib.content.player.deformer.PPlayerMeshDeformers;
import com.arcanc.pulselib.content.renderer.base.PRenderCallbackScope;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Predicate;

/** Narrow bridges for optional loader extensions to vanilla graphics APIs. */
public final class PClientRenderHooks
{
	private static Predicate<TextureFormat> stencilAspect = format -> false;
	private static ArmorModelResolver armorModels = (stack, type, model) -> model;
	private static ArmRenderer armRenderer = (renderer, pose, collector, light, skin, sleeve, player, right) ->
	{
		if (right)
			renderer.renderRightHand(pose, collector, light, skin, sleeve);
		else
			renderer.renderLeftHand(pose, collector, light, skin, sleeve);
	};

	private PClientRenderHooks()
	{
	}

	public static void install(Predicate<TextureFormat> stencilQuery, ArmRenderer arms)
	{
		stencilAspect = Objects.requireNonNull(stencilQuery);
		armRenderer = Objects.requireNonNull(arms);
	}

	public static boolean hasStencilAspect(TextureFormat format)
	{
		return stencilAspect.test(format);
	}

	public static void renderArm(AvatarRenderer<AbstractClientPlayer> renderer, PoseStack pose,
	                            SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve,
	                            AbstractClientPlayer player, boolean right)
	{
		try (var caller = PRenderCallbackScope.capture(pose, renderer.getModel().root()))
		{
			float partialTick = PFirstPersonPassTime.partialTick(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
			PPlayerMeshDeformers.apply(player, renderer.getModel(), partialTick);
			armRenderer.render(renderer, pose, collector, light, skin, sleeve, player, right);
		}
	}

	public static void installArmorModels(ArmorModelResolver resolver)
	{
		armorModels = Objects.requireNonNull(resolver);
	}

	public static Model<?> armorModel(ItemStack stack, EquipmentClientInfo.LayerType type, Model<?> original)
	{
		return armorModels.resolve(stack, type, original);
	}

	@FunctionalInterface
	public interface ArmorModelResolver
	{
		Model<?> resolve(ItemStack stack, EquipmentClientInfo.LayerType type, Model<?> original);
	}

	@FunctionalInterface
	public interface ArmRenderer
	{
		void render(AvatarRenderer<AbstractClientPlayer> renderer, PoseStack pose, SubmitNodeCollector collector,
		            int light, Identifier skin, boolean sleeve, AbstractClientPlayer player, boolean right);
	}
}
