/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.block;


import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TestBlock extends Block implements EntityBlock
{
	/**
	 * Creates an instance of the enclosing type.
	 * @param props the props to use.
	 */
	public TestBlock(Properties props)
	{
		super(props);
	}
	
	/**
	 * Performs the use without item operation.
	 * @param state the state to use.
	 * @param level the level to use.
	 * @param pos the pos to use.
	 * @param player the player to use.
	 * @param hitResult the hit result to use.
	 * @return the value produced by this operation.
	 */
	@Override
	protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState state,
	                                                    @NonNull Level level,
	                                                    @NonNull BlockPos pos,
	                                                    @NonNull Player player,
	                                                    @NonNull BlockHitResult hitResult)
	{
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof TestBlockEntity test))
			return super.useWithoutItem(state, level, pos, player, hitResult);
		test.changePlayAnimation();
		return InteractionResult.SUCCESS;
	}
	
	/**
	 * Performs the new block entity operation.
	 * @param blockPos the block pos to use.
	 * @param blockState the block state to use.
	 * @return the value produced by this operation.
	 */
	@Override
	public @Nullable BlockEntity newBlockEntity(@NonNull BlockPos blockPos, @NonNull BlockState blockState)
	{
		return new TestBlockEntity(blockPos, blockState);
	}
	
	/**
	 * Returns the render shape.
	 * @param state the state to use.
	 * @return the value produced by this operation.
	 */
	@Override
	protected @NonNull RenderShape getRenderShape(@NonNull BlockState state)
	{
		return RenderShape.INVISIBLE;
	}
}
