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

public final class PAnimationEventContext
{
	private final PAnimatable<?> animatable;
	private final PAnimationController<?> controller;
	private final @Nullable PBakedModel model;
	private final Collection<? extends PAnimationController<?>> poseControllers;
	private final @Nullable Level level;
	private final PAnimationEventDispatcherBridge positions;

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

	public PAnimatable<?> animatable()
	{
		return this.animatable;
	}
	public PAnimationController<?> controller()
	{
		return this.controller;
	}
	public @Nullable PBakedModel model()
	{
		return this.model;
	}
	public Collection<? extends PAnimationController<?>> poseControllers()
	{
		return this.poseControllers;
	}
	public @Nullable Level level()
	{
		return this.level;
	}
	public boolean isClientSide()
	{
		return this.level == null ||
				this.level.isClientSide();
	}
	public @Nullable PAnimationEventDispatcherBridge.Position position(String locator)
	{
		return this.positions.position(locator);
	}
	
	public interface PAnimationEventDispatcherBridge
	{
		@Nullable Position position(String locator);
		record Position(Level level, double x, double y, double z) { }
	}

	private static final class ListCopy
	{
		private static Collection<? extends PAnimationController<?>> copy(Collection<? extends PAnimationController<?>> source)
		{
			return List.copyOf(source);
		}
	}
}
