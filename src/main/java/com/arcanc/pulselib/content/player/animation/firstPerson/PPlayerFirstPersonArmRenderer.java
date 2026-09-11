/**
 * @author ArcAnc
 * Created at: 06.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.model.animation.PTransform;
import com.arcanc.pulselib.content.player.deformer.PModelPartCubes;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidAttachmentLayer;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class PPlayerFirstPersonArmRenderer
{
	private static final Map<PlayerModel, EnumMap<HumanoidArm, ArmGeometry>> GEOMETRY_CACHE =
			new WeakHashMap<>();
	
	private PPlayerFirstPersonArmRenderer()
	{
	}
	
	public static void render(
			AbstractClientPlayer player,
			HumanoidArm arm,
			PTransform transform,
			PoseStack poseStack,
			OrderedSubmitNodeCollector armCollector,
			int packedLight)
	{
		Minecraft mc = PLibRenderHelper.mc();
		
		AvatarRenderer<AbstractClientPlayer> renderer = mc.
				getEntityRenderDispatcher().
				getPlayerRenderer(player);
		
		PlayerModel model = renderer.getModel();
		
		ArmGeometry geometry = getGeometry(model, arm);
		
		Identifier skinTexture = player.
				getSkin().
				body().
				texturePath();
		
		RenderType renderType = RenderTypes.entityTranslucent(skinTexture);
		
		poseStack.pushPose();
		
		try
		{
			poseStack.mulPose(transform.matrix());
			
			geometry.sleeve().visible = isSleeveVisible(player, arm);
			armCollector.submitModelPart(
					geometry.arm(),
					poseStack,
					renderType,
					packedLight,
					OverlayTexture.NO_OVERLAY,
					null);
		}
		finally
		{
			poseStack.popPose();
		}
	}

	public static void renderAttachments(
			AbstractClientPlayer player,
			HumanoidArm arm,
			PTransform transform,
			PoseStack poseStack,
			int packedLight,
			float partialTick)
	{
		PlayerModel model = PLibRenderHelper.mc().getEntityRenderDispatcher().getPlayerRenderer(player).getModel();
		ModelPart armPart = getGeometry(model, arm).arm();
		poseStack.pushPose();
		try
		{
			poseStack.mulPose(transform.matrix());
			PHumanoidAttachmentLayer.renderFirstPersonArm(poseStack, packedLight, player, arm, armPart, partialTick);
		}
		finally
		{
			poseStack.popPose();
		}
	}
	
	private static ArmGeometry getGeometry(
			PlayerModel model,
			HumanoidArm arm)
	{
		EnumMap<HumanoidArm, ArmGeometry> byArm = GEOMETRY_CACHE.
				computeIfAbsent(
				model,
				ignored -> new EnumMap<>(HumanoidArm.class));
		
		return byArm.
				computeIfAbsent(
				arm,
				ignored -> createGeometry(model, arm));
	}
	
	private static ArmGeometry createGeometry(
			PlayerModel model,
			HumanoidArm arm)
	{
		ModelPart sourceArm = switch (arm)
		{
			case RIGHT -> model.rightArm;
			case LEFT -> model.leftArm;
		};
		
		ModelPart sourceSleeve = switch (arm)
		{
			case RIGHT -> model.rightSleeve;
			case LEFT -> model.leftSleeve;
		};
		
		ModelPart sleeve = createGeometryOnlyPart(sourceSleeve);
		return new ArmGeometry(
				createGeometryOnlyPart(sourceArm, Map.of("sleeve", sleeve)),
				sleeve);
	}
	
	private static ModelPart createGeometryOnlyPart(ModelPart source)
	{
		return createGeometryOnlyPart(source, Map.of());
	}

	private static ModelPart createGeometryOnlyPart(ModelPart source, Map<String, ModelPart> children)
	{
		List<ModelPart.Cube> cubes = List.copyOf(
				((PModelPartCubes)(Object)source).pulselib$cubes());

		ModelPart result = new ModelPart(
				cubes,
				children);
		
		result.setPos(
				0.0F,
				0.0F,
				0.0F);
		
		result.setRotation(
				0.0F,
				0.0F,
				0.0F);
		
		result.xScale = 1.0F;
		result.yScale = 1.0F;
		result.zScale = 1.0F;
		
		result.visible = true;
		result.skipDraw = false;
		
		return result;
	}
	
	private static boolean isSleeveVisible(
			AbstractClientPlayer player,
			HumanoidArm arm)
	{
		return player.isModelPartShown(
				switch (arm)
				{
					case RIGHT -> PlayerModelPart.RIGHT_SLEEVE;
					case LEFT -> PlayerModelPart.LEFT_SLEEVE;
				});
	}
	
	private record ArmGeometry(
			ModelPart arm,
			ModelPart sleeve)
	{
	}
}
