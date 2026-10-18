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
 * Animation manager lookup helpers and GUI model rendering entry points.
 * <p>
 * Part of this code copied from Geckolib: <a href="https://github.com/bernie-g/geckolib/blob/1.21.1/common/src/main/java/software/bernie/geckolib/util/GeckoLibUtil.java">GeckoLibUtil</a>
 * <p>Stop crying, Tslat!</p>
 * <p>Modified by ArcAnc</p>
 *

 */
public class PLibHelper
{
	/**
	 * Creates an instance manager for Entity/BlockEntity owners and a singleton manager otherwise.
	 * Use an explicit factory when a custom owner's lifetime differs from this default.
	 * Controller registration is deferred, so this is safe in an owner's field initializer.
	 *
	 * @param <T> the animatable owner type
	 * @param animatable the owner whose animation state is being processed
	 * @return the newly created manager with the requested ownership policy
	 */
	public static <T extends PAnimatable<T>> PAnimationManager<T> createManager(T animatable)
	{
		if (animatable instanceof Entity || animatable instanceof BlockEntity)
			return createInstanceManager(animatable);
		return createSingletonManager(animatable);
	}

	/**
	 * Creates a new directly owned manager without invoking owner callbacks.
	 *
	 * @param <T> the animatable owner type
	 * @param animatable the owner whose animation state is being processed
	 * @return the newly created manager with the requested ownership policy
	 */
	public static <T extends PAnimatable<T>> InstanceAnimationManager<T> createInstanceManager(T animatable)
	{
		return new InstanceAnimationManager<>(animatable);
	}

	/**
	 * Creates access to the owner's keyed singleton contexts without invoking owner callbacks.
	 *
	 * @param <T> the animatable owner type
	 * @param animatable the owner whose animation state is being processed
	 * @return the newly created manager with the requested ownership policy
	 */
	public static <T extends PAnimatable<T>> SingletonAnimationManager<T> createSingletonManager(T animatable)
	{
		return new SingletonAnimationManager<>(animatable);
	}

	/**
	 * Creates a new manager with an explicitly selected ownership strategy.
	 * Prefer getManager for access to an existing context.
	 *
	 * @param <T> the animatable owner type
	 * @param animatable the owner whose animation state is being processed
	 * @param singleton whether the manager is owned by a shared animatable
	 * @return the newly created manager with the requested ownership policy
	 */
	public static <T extends PAnimatable<T>> PAnimationManager<T> createManager(T animatable, boolean singleton)
	{
		return singleton ? createSingletonManager(animatable) : createInstanceManager(animatable);
	}

	/**
	 * Resolves the owner's strategy and initializes controllers after owner construction.
	 *
	 * @param <T> the animatable owner type
	 * @param animatable the owner whose animation state is being processed
	 * @return the initialized animation manager selected by the owner's storage strategy
	 */
	public static <T extends PAnimatable<T>> PAnimationManager<T> getManager(T animatable)
	{
		return getManager(animatable, AnimManagerKey.ofObject(animatable));
	}

	/**
	 * Resolves a context without choosing or replacing the owner's storage strategy.
	 *
	 * @param <T> the animatable owner type
	 * @param animatable the owner whose animation state is being processed
	 * @param key the key
	 * @return the initialized animation manager selected by the owner's storage strategy
	 */
	public static <T extends PAnimatable<T>> PAnimationManager<T> getManager(T animatable, AnimManagerKey key)
	{
		return animatable.getAnimationManager(key).getManager(key);
	}

	/**
	 * Resolves the registered item's strategy with the stack's existing runtime identity.
	 * @throws IllegalArgumentException if the stack is empty or is not animated.
	 *
	 * @param stack the item stack supplying render or animation state
	 * @return the initialized animation manager selected by the owner's storage strategy
	 */
	public static PAnimationManager<?> getManager(ItemStack stack)
	{
		if (!(stack.getItem() instanceof PAnimatable<?> animatable))
			throw new IllegalArgumentException("Animation managers require an animated ItemStack");
		AnimManagerKey key = AnimManagerKey.of(stack);
		return animatable.getAnimationManager(key).getManager(key);
	}

	/**
	 * Renders an animated model in an isolated GUI render scope at the supplied screen position.
	 *
	 * @param <T> the animatable owner type
	 * @param guiGraphics the GUI drawing context
	 * @param modelData the canonical model and loader selection
	 * @param controllers the animation controllers in pose composition order
	 * @param renderType the render type
	 * @param x the GUI x coordinate in screen pixels
	 * @param y the GUI y coordinate in screen pixels
	 * @param scale the local scale vector
	 * @param packedColor the packed ARGB vertex color
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 */
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
