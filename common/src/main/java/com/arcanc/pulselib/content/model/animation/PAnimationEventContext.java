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
 * Animatable, controller, model, level and locator lookup available to an animation event handler.
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
	 * Captures the owner, controller and locator-resolution inputs for event execution.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param controller the controller supplying playback and persistent expression state
	 * @param model the baked model supplying indexed bones and animations; may be {@code null}
	 * @param poseControllers the controllers contributing to locator or pose resolution
	 * @param level the level; may be {@code null}
	 * @param positions the locator position resolver
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
	 * Returns the owner whose animation state is being processed.
	 *
	 * @return the owner whose animation state is being processed
	 */
	public PAnimatable<?> animatable()
	{
		return this.animatable;
	}
	/**
	 * Returns the controller supplying playback and persistent expression state.
	 *
	 * @return the controller supplying playback and persistent expression state
	 */
	public PAnimationController<?> controller()
	{
		return this.controller;
	}
	/**
	 * Returns the model.
	 *
	 * @return the model, or {@code null} when the locator cannot be resolved in the current event context
	 */
	public @Nullable PBakedModel model()
	{
		return this.model;
	}
	/**
	 * Returns the controllers contributing to locator or pose resolution.
	 *
	 * @return the controllers contributing to locator or pose resolution
	 */
	public Collection<? extends PAnimationController<?>> poseControllers()
	{
		return this.poseControllers;
	}
	/**
	 * Returns the level.
	 *
	 * @return the level, or {@code null} when the locator cannot be resolved in the current event context
	 */
	public @Nullable Level level()
	{
		return this.level;
	}
	/**
	 * Returns whether the event is being handled in a client level.
	 *
	 * @return whether the event is being handled in a client level
	 */
	public boolean isClientSide()
	{
		return this.level == null ||
				this.level.isClientSide();
	}
	/**
	 * Resolves the named event locator using the current animated pose.
	 *
	 * @param locator the named model locator or bone used to resolve the event position
	 * @return the position and orientation resolved for the requested event locator, or {@code null} when the locator cannot be resolved in the current event context
	 */
	public @Nullable PAnimationEventDispatcherBridge.Position position(String locator)
	{
		return this.positions.position(locator);
	}
	
	/**
	 * Resolves named animation event locators into position and orientation data.
	 */
	public interface PAnimationEventDispatcherBridge
	{
		/**
		 * Resolves the requested locator in the current animation event context.
		 *
		 * @param locator the named model locator or bone used to resolve the event position
		 * @return the position and orientation resolved for the requested event locator, or {@code null} when the locator cannot be resolved in the current event context
		 */
		@Nullable Position position(String locator);
		/**
		 * Position and orientation resolved for an animation event locator.
		 *
		 * @param level the level
		 * @param x the locator's world-space x coordinate
		 * @param y the locator's world-space y coordinate
		 * @param z the locator's world-space z coordinate
		 */
		record Position(Level level, double x, double y, double z) { }
	}

	/**
	 * Creates stable controller collections for event dispatch.
	 */
	private static final class ListCopy
	{
		private static Collection<? extends PAnimationController<?>> copy(Collection<? extends PAnimationController<?>> source)
		{
			return List.copyOf(source);
		}
	}
}
