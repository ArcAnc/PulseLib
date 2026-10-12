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

public final class PPlayerAnimationHandle
{
	private final Player player;
	private final ResourceLocation id;

	PPlayerAnimationHandle(Player player, ResourceLocation id)
	{
		this.player = Objects.requireNonNull(player);
		this.id = Objects.requireNonNull(id);
	}

	public Player player()
	{
		return this.player;
	}

	public ResourceLocation id()
	{
		return this.id;
	}

	public boolean hasController(String controllerName)
	{
		return controller(controllerName) != null;
	}
	
	public boolean play(String controllerName, PRawAnimation animation)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		if (controller == null)
			return false;

		controller.play(Objects.requireNonNull(animation));
		return true;
	}

	public boolean stop(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		if (controller == null)
			return false;

		controller.stop();
		return true;
	}

	public boolean pause(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		if (controller == null)
			return false;

		controller.pause();
		return true;
	}

	public boolean resume(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		if (controller == null)
			return false;

		controller.resume();
		return true;
	}
	
	public void stopAll()
	{
		PPlayerAnimationInstance instance = instance();
		if (instance == null)
			return;

		instance.stopAllControllers();
	}

	public boolean isPlaying(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		return controller != null && controller.isPlaying();
	}

	public boolean isPaused(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		return controller != null && controller.isPaused();
	}
	
	public @Nullable ControllerState state(String controllerName)
	{
		PAnimationController<PPlayerAnimationInstance> controller = controller(controllerName);
		return controller == null ? null : controller.getState();
	}
	
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
