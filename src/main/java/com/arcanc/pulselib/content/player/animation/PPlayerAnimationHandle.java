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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class PPlayerAnimationHandle
{
	private final Player player;
	private final Identifier id;

	/**
	 * Creates an instance of the enclosing type.
	 * @param player the player to use.
	 * @param id the id to use.
	 */
	PPlayerAnimationHandle(Player player, Identifier id)
	{
		this.player = Objects.requireNonNull(player);
		this.id = Objects.requireNonNull(id);
	}

	/**
	 * Performs the player operation.
	 * @return the value produced by this operation.
	 */
	public Player player()
	{
		return this.player;
	}

	/**
	 * Performs the id operation.
	 * @return the value produced by this operation.
	 */
	public Identifier id()
	{
		return this.id;
	}

	/**
	 * Determines whether the object has controller.
	 * @param controllerName the controller name to use.
	 * @return the value produced by this operation.
	 */
	public boolean hasController(String controllerName)
	{
		return controller(controllerName) != null;
	}
	
	/**
	 * Performs the play operation.
	 * @param controllerName the controller name to use.
	 * @param animation the animation to use.
	 * @return the value produced by this operation.
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
	 * Performs the stop operation.
	 * @param controllerName the controller name to use.
	 * @return the value produced by this operation.
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
	 * Performs the pause operation.
	 * @param controllerName the controller name to use.
	 * @return the value produced by this operation.
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
	 * Performs the resume operation.
	 * @param controllerName the controller name to use.
	 * @return the value produced by this operation.
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
	 * Stops the all.
	 */
	public void stopAll()
	{
		PPlayerAnimationInstance instance = instance();
		if (instance == null)
			return;

		instance.stopAllControllers();
	}

	/**
	 * Determines whether playing.
	 * @param controllerName the controller name to use.
	 * @return the value produced by this operation.
	 */
	public boolean isPlaying(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		return controller != null && controller.isPlaying();
	}

	/**
	 * Determines whether paused.
	 * @param controllerName the controller name to use.
	 * @return the value produced by this operation.
	 */
	public boolean isPaused(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		return controller != null && controller.isPaused();
	}
	
	/**
	 * Performs the state operation.
	 * @param controllerName the controller name to use.
	 * @return the value produced by this operation.
	 */
	public @Nullable ControllerState state(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		return controller == null ? null : controller.getState();
	}
	
	/**
	 * Performs the controller operation.
	 * @param controllerName the controller name to use.
	 * @return the value produced by this operation.
	 */
	public @Nullable PAnimationController<PPlayerAnimationInstance> controller(String controllerName)
	{
		Objects.requireNonNull(controllerName);
		PPlayerAnimationInstance instance = instance();
		return instance == null ? null : instance.controller(controllerName);
	}

	/**
	 * Performs the instance operation.
	 * @return the value produced by this operation.
	 */
	private @Nullable PPlayerAnimationInstance instance()
	{
		return PPlayerAnimations.getInstance(this.player, this.id);
	}
}
