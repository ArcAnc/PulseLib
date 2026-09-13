/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Carries animation event context.
 */
public final class PAnimationEventContext
{
	private final PAnimatable<?> animatable;
	private final PAnimationController<?> controller;
	private final @Nullable PBakedModel model;
	private final Collection<? extends PAnimationController<?>> poseControllers;
	private final @Nullable Level level;
	private final PAnimationEventDispatcherBridge positions;

	/**
	 * Creates an instance of the enclosing type.
	 * @param animatable the animatable to use.
	 * @param controller the controller to use.
	 * @param model the model to use.
	 * @param poseControllers the pose controllers to use.
	 * @param level the level to use.
	 * @param positions the positions to use.
	 */
	public PAnimationEventContext(PAnimatable<?> animatable,
	                              PAnimationController<?> controller,
	                              @Nullable PBakedModel model,
	                              Collection<? extends PAnimationController<?>> poseControllers,
	                              @Nullable Level level,
	                              PAnimationEventDispatcherBridge positions)
	{
		this.animatable = Objects.requireNonNull(animatable);
		this.controller = Objects.requireNonNull(controller);
		this.model = model;
		this.poseControllers = ListCopy.copy(poseControllers);
		this.level = level;
		this.positions = Objects.requireNonNull(positions);
	}

	/**
	 * Performs the animatable operation.
	 * @return the value produced by this operation.
	 */
	public PAnimatable<?> animatable()
	{
		return this.animatable;
	}
	/**
	 * Performs the controller operation.
	 * @return the value produced by this operation.
	 */
	public PAnimationController<?> controller()
	{
		return this.controller;
	}
	/**
	 * Performs the model operation.
	 * @return the value produced by this operation.
	 */
	public @Nullable PBakedModel model()
	{
		return this.model;
	}
	/**
	 * Performs the pose controllers operation.
	 * @return the value produced by this operation.
	 */
	public Collection<? extends PAnimationController<?>> poseControllers()
	{
		return this.poseControllers;
	}
	/**
	 * Performs the level operation.
	 * @return the value produced by this operation.
	 */
	public @Nullable Level level()
	{
		return this.level;
	}
	/**
	 * Determines whether client side.
	 * @return the value produced by this operation.
	 */
	public boolean isClientSide()
	{
		return this.level == null ||
				this.level.isClientSide();
	}
	/**
	 * Performs the position operation.
	 * @param locator the locator to use.
	 * @return the value produced by this operation.
	 */
	public @Nullable PAnimationEventDispatcherBridge.Position position(String locator)
	{
		return this.positions.position(locator);
	}
	
/**
 * Defines the contract for animation event dispatcher bridge.
 */
	public interface PAnimationEventDispatcherBridge
	{
		/**
		 * Performs the position operation.
		 * @param locator the locator to use.
		 * @return the value produced by this operation.
		 */
		@Nullable Position position(String locator);
		record Position(Level level, double x, double y, double z) { }
	}

/**
 * Provides support for list copy.
 */
	private static final class ListCopy
	{
		/**
		 * Performs the copy operation.
		 * @param source the source to use.
		 * @return the value produced by this operation.
		 */
		private static Collection<? extends PAnimationController<?>> copy(Collection<? extends PAnimationController<?>> source)
		{
			return List.copyOf(source);
		}
	}
}
