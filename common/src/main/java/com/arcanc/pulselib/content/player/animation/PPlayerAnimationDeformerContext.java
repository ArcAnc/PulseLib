/**
 * @author ArcAnc
 * Created at: 09.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.content.animatable.PAnimationController;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Player animation state exposed when resolving animated mesh deformation values.
 */
public final class PPlayerAnimationDeformerContext
{
	private final Player player;
	private final PPlayerAnimationInstance instance;
	private final float partialTick;
	private final float weight;

	/**
	 * Captures player and animation inputs used to resolve deformation channels.
	 *
	 * @param player the player whose animation or presentation is being processed
	 * @param instance the instance
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param weight the contribution weight
	 */
	PPlayerAnimationDeformerContext(Player player, PPlayerAnimationInstance instance, float partialTick, float weight)
	{
		this.player = Objects.requireNonNull(player);
		this.instance = Objects.requireNonNull(instance);
		this.partialTick = partialTick;
		this.weight = weight;
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
	 * Returns the definition belonging to the current player animation instance.
	 *
	 * @return the definition belonging to the current player animation instance
	 */
	public PPlayerAnimationDefinition definition()
	{
		return this.instance.definition();
	}

	/**
	 * Returns the interpolation fraction between the previous and current game ticks.
	 *
	 * @return the interpolation fraction between the previous and current game ticks
	 */
	public float partialTick()
	{
		return this.partialTick;
	}
	
	/**
	 * Returns the contribution weight.
	 *
	 * @return the contribution weight
	 */
	public float weight()
	{
		return this.weight;
	}

	/**
	 * Returns whether the named animation controller is currently playing.
	 *
	 * @param controllerName the registered controller name
	 * @return whether the named animation controller is currently playing
	 */
	public boolean isPlaying(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		return controller != null && controller.isPlaying();
	}
	
	/**
	 * Returns the named controller's interpolated playback time in game ticks.
	 *
	 * @param controllerName the registered controller name
	 * @return the named controller's interpolated playback time in game ticks
	 */
	public float controllerTicks(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		return controller == null ? 0.0f : controller.getInterpolatedTime(this.partialTick);
	}

	/**
	 * Returns the named controller's interpolated playback time in seconds.
	 *
	 * @param controllerName the registered controller name
	 * @return the named controller's interpolated playback time in seconds
	 */
	public float controllerSeconds(String controllerName)
	{
		return controllerTicks(controllerName) / 20.0f;
	}

	private @Nullable PAnimationController<PPlayerAnimationInstance> controller(String controllerName)
	{
		return this.instance.controller(controllerName);
	}
}
