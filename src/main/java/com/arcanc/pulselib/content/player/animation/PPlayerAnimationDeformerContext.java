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

public final class PPlayerAnimationDeformerContext
{
	private final Player player;
	private final PPlayerAnimationInstance instance;
	private final float partialTick;
	private final float weight;

	/**
	 * Creates an instance of the enclosing type.
	 * @param player the player to use.
	 * @param instance the instance to use.
	 * @param partialTick the partial tick to use.
	 * @param weight the weight to use.
	 */
	PPlayerAnimationDeformerContext(Player player, PPlayerAnimationInstance instance, float partialTick, float weight)
	{
		this.player = Objects.requireNonNull(player);
		this.instance = Objects.requireNonNull(instance);
		this.partialTick = partialTick;
		this.weight = weight;
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
	 * Performs the definition operation.
	 * @return the value produced by this operation.
	 */
	public PPlayerAnimationDefinition definition()
	{
		return this.instance.definition();
	}

	/**
	 * Performs the partial tick operation.
	 * @return the value produced by this operation.
	 */
	public float partialTick()
	{
		return this.partialTick;
	}
	
	/**
	 * Performs the weight operation.
	 * @return the value produced by this operation.
	 */
	public float weight()
	{
		return this.weight;
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
	 * Performs the controller ticks operation.
	 * @param controllerName the controller name to use.
	 * @return the value produced by this operation.
	 */
	public float controllerTicks(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		return controller == null ? 0.0f : controller.getInterpolatedTime(this.partialTick);
	}

	/**
	 * Performs the controller seconds operation.
	 * @param controllerName the controller name to use.
	 * @return the value produced by this operation.
	 */
	public float controllerSeconds(String controllerName)
	{
		return controllerTicks(controllerName) / 20.0f;
	}

	/**
	 * Performs the controller operation.
	 * @param controllerName the controller name to use.
	 * @return the value produced by this operation.
	 */
	private @Nullable PAnimationController<PPlayerAnimationInstance> controller(String controllerName)
	{
		return this.instance.controller(controllerName);
	}
}
