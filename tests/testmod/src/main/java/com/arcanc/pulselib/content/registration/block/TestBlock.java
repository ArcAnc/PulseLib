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

public class TestBlock extends Block implements EntityBlock
{
	//public static final EnumProperty<Direction> HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
	
	public TestBlock(Properties props)
	{
		super(props);
//		registerDefaultState(defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH));
	}
	
	@Override
	protected InteractionResult useWithoutItem(BlockState state,
	                                                    Level level,
	                                                    BlockPos pos,
	                                                    Player player,
	                                                    BlockHitResult hitResult)
	{
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof TestBlockEntity test))
			return super.useWithoutItem(state, level, pos, player, hitResult);
		test.changePlayAnimation();
		return InteractionResult.SUCCESS;
	}
	
	@Override
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState)
	{
		return new TestBlockEntity(blockPos, blockState);
	}
	
	@Override
	protected RenderShape getRenderShape(BlockState state)
	{
		return RenderShape.INVISIBLE;
	}
/*
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		Direction dir = context.getHorizontalDirection();
		return super.getStateForPlacement(context).setValue(HORIZONTAL_FACING, dir.getOpposite());
	}
	
	@Override
	protected BlockState mirror(BlockState state, Mirror mirror)
	{
		return state.rotate(mirror.getRotation(state.getValue(HORIZONTAL_FACING)));
	}
	
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation)
	{
		return state.setValue(HORIZONTAL_FACING, Direction.from2DDataValue((state.getValue(HORIZONTAL_FACING).get2DDataValue() + 1) % 4));
	}
	
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(HORIZONTAL_FACING);
	}
	*/
}
