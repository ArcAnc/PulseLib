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

	PPlayerAnimationDeformerContext(Player player, PPlayerAnimationInstance instance, float partialTick, float weight)
	{
		this.player = Objects.requireNonNull(player);
		this.instance = Objects.requireNonNull(instance);
		this.partialTick = partialTick;
		this.weight = weight;
	}

	public Player player()
	{
		return this.player;
	}

	public PPlayerAnimationDefinition definition()
	{
		return this.instance.definition();
	}

	public float partialTick()
	{
		return this.partialTick;
	}
	
	public float weight()
	{
		return this.weight;
	}

	public boolean isPlaying(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		return controller != null && controller.isPlaying();
	}
	
	public float controllerTicks(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		return controller == null ? 0.0f : controller.getInterpolatedTime(this.partialTick);
	}

	public float controllerSeconds(String controllerName)
	{
		return controllerTicks(controllerName) / 20.0f;
	}

	private @Nullable PAnimationController<PPlayerAnimationInstance> controller(String controllerName)
	{
		return this.instance.controller(controllerName);
	}
}
