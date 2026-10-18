/**
 * @author ArcAnc
 * Created at: 30.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Control facade for named controllers belonging to one player and registered animation.
 */
public final class PPlayerAnimationHandle
{
	private final Player player;
	private final ResourceLocation id;

	/**
	 * Targets the registered animation of the supplied player for controller commands.
	 *
	 * @param player the player whose animation or presentation is being processed
	 * @param id the namespaced resource or registration identifier
	 */
	PPlayerAnimationHandle(Player player, ResourceLocation id)
	{
		this.player = Objects.requireNonNull(player);
		this.id = Objects.requireNonNull(id);
	}

	/**
	 * Returns the player whose animation or presentation is being processed.
	 *
	 * @return the player whose animation or presentation is being processed
	 */
	public Player player()
	{
		return this.player;
	}

	/**
	 * Returns the namespaced resource or registration identifier.
	 *
	 * @return the namespaced resource or registration identifier
	 */
	public ResourceLocation id()
	{
		return this.id;
	}

	/**
	 * Returns whether the target player animation declares the named controller.
	 *
	 * @param controllerName the registered controller name
	 * @return whether the target player animation declares the named controller
	 */
	public boolean hasController(String controllerName)
	{
		return controller(controllerName) != null;
	}
	
	/**
	 * Selects a raw animation on the named controller and reports whether the controller exists.
	 *
	 * @param controllerName the registered controller name
	 * @param animation the raw animation sequence to play
	 * @return whether the controller exists
	 */
	public boolean play(String controllerName, PRawAnimation animation)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		if (controller == null)
			return false;

		controller.play(Objects.requireNonNull(animation));
		return true;
	}

	/**
	 * Stops the named controller and reports whether the controller exists.
	 *
	 * @param controllerName the registered controller name
	 * @return whether the controller exists
	 */
	public boolean stop(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		if (controller == null)
			return false;

		controller.stop();
		return true;
	}

	/**
	 * Pauses the named controller and reports whether the controller exists.
	 *
	 * @param controllerName the registered controller name
	 * @return whether the controller exists
	 */
	public boolean pause(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		if (controller == null)
			return false;

		controller.pause();
		return true;
	}

	/**
	 * Resumes the named controller and reports whether the controller exists.
	 *
	 * @param controllerName the registered controller name
	 * @return whether the controller exists
	 */
	public boolean resume(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		if (controller == null)
			return false;

		controller.resume();
		return true;
	}
	
	/**
	 * Stops every controller belonging to the target player animation.
	 */
	public void stopAll()
	{
		PPlayerAnimationInstance instance = instance();
		if (instance == null)
			return;

		instance.stopAllControllers();
	}

	/**
	 * Returns whether the named controller is currently playing.
	 *
	 * @param controllerName the registered controller name
	 * @return whether the named controller is currently playing
	 */
	public boolean isPlaying(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		return controller != null && controller.isPlaying();
	}

	/**
	 * Returns whether the named controller is currently paused.
	 *
	 * @param controllerName the registered controller name
	 * @return whether the named controller is currently paused
	 */
	public boolean isPaused(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		return controller != null && controller.isPaused();
	}
	
	/**
	 * Returns the named controller's current playback state.
	 *
	 * @param controllerName the registered controller name
	 * @return the named controller's current playback state, or {@code null} when the requested controller does not exist
	 */
	public @Nullable ControllerState state(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		return controller == null ? null : controller.getState();
	}
	
	/**
	 * Resolves the named controller of the target player animation.
	 *
	 * @param controllerName the registered controller name
	 * @return the controller supplying playback and persistent expression state, or {@code null} when the requested controller does not exist
	 */
	public @Nullable PAnimationController<PPlayerAnimationInstance> controller(String controllerName)
	{
		Objects.requireNonNull(controllerName);
		PPlayerAnimationInstance instance = instance();
		return instance == null ? null : instance.controller(controllerName);
	}

	private @Nullable PPlayerAnimationInstance instance()
	{
		return PPlayerAnimations.getInstance(this.player, this.id);
	}
}
