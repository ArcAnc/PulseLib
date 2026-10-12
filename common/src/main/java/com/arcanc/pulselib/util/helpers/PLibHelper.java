/**
 * @author ArcAnc
 * Created at: 24.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.helpers;


import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.function.Function;

/**
 * Part of this code copied from Geckolib: <a href="https://github.com/bernie-g/geckolib/blob/1.21.1/common/src/main/java/software/bernie/geckolib/util/GeckoLibUtil.java">GeckoLibUtil</a>
 * <p>Stop crying, Tslat!</p>
 * <p>Modified by ArcAnc</p>
 */
public class PLibHelper
{
	public static <T extends PAnimatable<T>> PAnimationManager<T> createManager(T animatable)
	{
		if (animatable instanceof BlockEntity || animatable instanceof Entity)
			return createManager(animatable, false);
		return createManager(animatable, true);
	}
	
	public static <T extends PAnimatable<T>> PAnimationManager<T> createManager(T animatable, boolean singleton)
	{
		AnimManagerKey key = AnimManagerKey.ofObject(animatable);
		PAnimationManager<T> manager = animatable.getAnimationManager(key);
		
		if (manager != null)
			return manager;
		
		return singleton ? new SingletonAnimationManager<>(animatable, key) : new InstanceAnimationManager<>(animatable, key);
	}
	
	public static <T extends PAnimatable<T>> void renderModelInGui(GuiGraphics guiGraphics,
	                                   PModelData modelData,
									   Collection<PAnimationController<T>> controllers,
									   Function<ResourceLocation, RenderType> renderType,
	                                   int x, int y,
									   Vector3f scale,
	                                   int packedColor,
	                                   int packedLight,
	                                   int packedOverlay,
	                                   float partialTick)
	{
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x, y, 200);
		guiGraphics.pose().scale(scale.x(), scale.y(), scale.z());
		PBakedModel model = modelData.getModel();
		if (model != null)
			model.instantDraw(guiGraphics.pose(), modelData, controllers, renderType, packedColor, packedLight, packedOverlay, partialTick);
		guiGraphics.pose().popPose();
	}
}
