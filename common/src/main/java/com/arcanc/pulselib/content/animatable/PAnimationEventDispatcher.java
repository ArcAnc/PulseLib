/**
 * @author ArcAnc
 * Created at: 27.05.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */
package com.arcanc.pulselib.content.animatable;

import com.arcanc.pulselib.content.model.animation.*;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Routes animation events to side-appropriate handlers and resolves locator positions from the animated
 * pose.
 */
public final class PAnimationEventDispatcher
{
	private PAnimationEventDispatcher() { }

	/**
	 * Dispatches an animation event using the owner, controller and available pose information.
	 *
	 * @param <T> the animatable owner type
	 * @param animatable the owner whose animation state is being processed
	 * @param controller the controller supplying playback and persistent expression state
	 * @param event the typed animation event to dispatch
	 * @param model the baked model supplying indexed bones and animations; may be {@code null}
	 * @param controllers the animation controllers in pose composition order
	 */
	public static <T extends PAnimatable<T>> void dispatch(T animatable, PAnimationController<T> controller,
	                                                       PAnimationEvent<?> event, @Nullable PBakedModel model,
	                                                       Collection<PAnimationController<T>> controllers)
	{
		@Nullable PositionContext fallback = position(animatable);
		@Nullable Level level = fallback == null ? null : fallback.level();
		if (!runsOn(event.type().side(), level))
			return;

		PAnimationEventContext context = new PAnimationEventContext(animatable, controller, model, controllers, level,
				locator -> positionFor(animatable, model, controllers, fallback, locator));
		execute(context, event);
	}
	
	/**
	 * Dispatches an animation event using the owner, controller and available pose information.
	 *
	 * @param <T> the animatable owner type
	 * @param animatable the owner whose animation state is being processed
	 * @param controller the controller supplying playback and persistent expression state
	 * @param event the typed animation event to dispatch
	 */
	public static <T extends PAnimatable<T>> void dispatch(T animatable, PAnimationController<T> controller,
	                                                       PAnimationEvent<?> event)
	{
		dispatch(animatable, controller, event, null, List.of(controller));
	}

	private static boolean runsOn(PEventSide side, @Nullable Level level)
	{
		boolean client = level == null || level.isClientSide();
		return switch (side)
		{
			case CLIENT, PRESENTATION_ONLY -> client;
			case SERVER -> !client;
			case BOTH -> true;
		};
	}

	@SuppressWarnings("unchecked")
	private static <T> void execute(PAnimationEventContext context, PAnimationEvent<?> event)
	{
		PAnimationEvent<T> typed = (PAnimationEvent<T>)event;
		PAnimationEventType<T> type = typed.type();
		type.execute(context, typed.data());
	}

	private static <T extends PAnimatable<T>> @Nullable PAnimationEventContext.PAnimationEventDispatcherBridge.Position positionFor(
			T animatable, @Nullable PBakedModel model, Collection<PAnimationController<T>> controllers,
			@Nullable PositionContext fallback, String locator)
	{
		if (fallback == null)
			return null;
		PositionContext position = fallback;
		if (model != null && !locator.isBlank())
			position = resolveLocatorPosition(animatable, locator, model, controllers, position);
		return new PAnimationEventContext.PAnimationEventDispatcherBridge.Position(position.level(), position.x(), position.y(), position.z());
	}

	private static <T extends PAnimatable<T>> PositionContext resolveLocatorPosition(T animatable, String locator,
			PBakedModel model, Collection<PAnimationController<T>> controllers, PositionContext fallback)
	{
		Vector3f localPosition = new Vector3f();
		if (!findBonePosition(model, controllers, locator, localPosition))
			return fallback;
		if (animatable instanceof Entity entity)
		{
			float scale = entity instanceof LivingEntity living ? living.getScale() : 1f;
			float yRot = entity instanceof LivingEntity living ? living.yBodyRot : entity.getYRot();
			localPosition.mul(scale).rotateY((float)Math.toRadians(180f - yRot));
			return new PositionContext(fallback.level(), entity.getX() + localPosition.x(), entity.getY() + localPosition.y(),
					entity.getZ() + localPosition.z());
		}
		if (animatable instanceof BlockEntity blockEntity)
		{
			BlockPos pos = blockEntity.getBlockPos();
			rotateForBlockState(localPosition, blockEntity.getBlockState());
			return new PositionContext(fallback.level(), pos.getX() + .5 + localPosition.x(), pos.getY() + .5 + localPosition.y(),
					pos.getZ() + .5 + localPosition.z());
		}
		return fallback;
	}

	private static void rotateForBlockState(Vector3f localPosition, BlockState blockState)
	{
		Direction direction = Direction.NORTH;
		if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) direction = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
		if (blockState.hasProperty(BlockStateProperties.FACING)) direction = blockState.getValue(BlockStateProperties.FACING);
		if (direction.getAxis() == Direction.Axis.Z) direction = direction.getOpposite();
		if (direction.getAxis().isHorizontal()) localPosition.rotateY((float)Math.toRadians(direction.toYRot()));
		else localPosition.rotateX((float)Math.toRadians(90f * direction.getNormal().getY()));
	}

	private static <T extends PAnimatable<T>> boolean findBonePosition(PBakedModel model,
			Collection<PAnimationController<T>> controllers, String boneName, Vector3f result)
	{
		int boneIndex = model.boneIndex(boneName);
		if (boneIndex < 0) return false;
		PPose localPose = model.evaluate(controllers, Map.of(), 1f);
		PModelPose modelPose = new PModelPose(model.boneCount());
		modelPose.update(model, localPose);
		result.set(modelPose.transform(boneIndex).translation());
		return true;
	}

	private static @Nullable PositionContext position(PAnimatable<?> animatable)
	{
		if (animatable instanceof Entity entity)
			return new PositionContext(entity.level(), entity.getX(), entity.getY(), entity.getZ());
		if (animatable instanceof BlockEntity blockEntity && blockEntity.getLevel() != null)
		{
			BlockPos pos = blockEntity.getBlockPos();
			return new PositionContext(blockEntity.getLevel(), pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
		}
		return null;
	}

	/**
	 * Model and controller inputs used to resolve an event locator into world space.
	 *
	 * @param level the level
	 * @param x the x component or coordinate
	 * @param y the y component or coordinate
	 * @param z the z component or coordinate
	 */
	private record PositionContext(Level level, double x, double y, double z) { }
}
