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
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
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
	/**
	 * Creates an instance manager for Entity/BlockEntity owners and a singleton manager otherwise.
	 * Use an explicit factory when a custom owner's lifetime differs from this default.
	 * Controller registration is deferred, so this is safe in an owner's field initializer.
	 */
	public static <T extends PAnimatable<T>> PAnimationManager<T> createManager(T animatable)
	{
		if (animatable instanceof Entity || animatable instanceof BlockEntity)
			return createInstanceManager(animatable);
		return createSingletonManager(animatable);
	}

	/** Creates a new directly owned manager without invoking owner callbacks. */
	public static <T extends PAnimatable<T>> InstanceAnimationManager<T> createInstanceManager(T animatable)
	{
		return new InstanceAnimationManager<>(animatable);
	}

	/** Creates access to the owner's keyed singleton contexts without invoking owner callbacks. */
	public static <T extends PAnimatable<T>> SingletonAnimationManager<T> createSingletonManager(T animatable)
	{
		return new SingletonAnimationManager<>(animatable);
	}

	/**
	 * Creates a new manager with an explicitly selected ownership strategy.
	 * Prefer getManager for access to an existing context.
	 */
	public static <T extends PAnimatable<T>> PAnimationManager<T> createManager(T animatable, boolean singleton)
	{
		return singleton ? createSingletonManager(animatable) : createInstanceManager(animatable);
	}

	/** Resolves the owner's strategy and initializes controllers after owner construction. */
	public static <T extends PAnimatable<T>> PAnimationManager<T> getManager(T animatable)
	{
		return getManager(animatable, AnimManagerKey.ofObject(animatable));
	}

	/** Resolves a context without choosing or replacing the owner's storage strategy. */
	public static <T extends PAnimatable<T>> PAnimationManager<T> getManager(T animatable, AnimManagerKey key)
	{
		return animatable.getAnimationManager(key).getManager(key);
	}

	/**
	 * Resolves the registered item's strategy with the stack's existing runtime identity.
	 * @throws IllegalArgumentException if the stack is empty or is not animated.
	 */
	public static PAnimationManager<?> getManager(ItemStack stack)
	{
		if (!(stack.getItem() instanceof PAnimatable<?> animatable))
			throw new IllegalArgumentException("Animation managers require an animated ItemStack");
		AnimManagerKey key = AnimManagerKey.of(stack);
		return animatable.getAnimationManager(key).getManager(key);
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
