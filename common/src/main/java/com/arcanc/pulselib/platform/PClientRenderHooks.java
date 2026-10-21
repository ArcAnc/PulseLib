/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.platform;

import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.function.Predicate;

/** Narrow bridges for optional loader extensions to vanilla graphics APIs. */
public final class PClientRenderHooks
{
	// Vanilla 26.1.2 exposes DEPTH32 but no stencil-bearing TextureFormat.
	private static Predicate<TextureFormat> stencilAspect = format -> false;
	private static ArmRenderer armRenderer = (renderer, pose, collector, light, skin, sleeve, player, right) ->
	{
		if (right)
		{
			renderer.renderRightHand(pose, collector, light, skin, sleeve);
		}
		else
		{
			renderer.renderLeftHand(pose, collector, light, skin, sleeve);
		}
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
		armRenderer.render(renderer, pose, collector, light, skin, sleeve, player, right);
	}

	@FunctionalInterface
	public interface ArmRenderer
	{
		void render(AvatarRenderer<AbstractClientPlayer> renderer, PoseStack pose, SubmitNodeCollector collector,
		            int light, Identifier skin, boolean sleeve, AbstractClientPlayer player, boolean right);
	}
}
