package com.mechaniciancombat.block;

import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;


/**
 * Author-supplied backpack modification table.
 *
 * <p>The workstation reuses vanilla smithing recipes, but uses an authored
 * vertical slot layout and a draggable armor preview.</p>
 */
public final class BackpackModificationTableBlock extends SmithingTableBlock
		implements EntityBlock {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final VoxelShape NORTH_SHAPE = Shapes.or(
			Block.box(1.5D, 0.0D, 2.0D, 14.0D, 2.0D, 14.0D),
			Block.box(2.0D, 2.0D, 5.0D, 14.0D, 16.0D, 14.0D));
	private static final VoxelShape EAST_SHAPE = Shapes.or(
			Block.box(2.0D, 0.0D, 1.5D, 14.0D, 2.0D, 14.0D),
			Block.box(2.0D, 2.0D, 2.0D, 11.0D, 16.0D, 14.0D));
	private static final VoxelShape SOUTH_SHAPE = Shapes.or(
			Block.box(2.0D, 0.0D, 2.0D, 14.5D, 2.0D, 14.0D),
			Block.box(2.0D, 2.0D, 2.0D, 14.0D, 16.0D, 11.0D));
	private static final VoxelShape WEST_SHAPE = Shapes.or(
			Block.box(2.0D, 0.0D, 2.0D, 14.0D, 2.0D, 14.5D),
			Block.box(5.0D, 2.0D, 2.0D, 14.0D, 16.0D, 14.0D));

	public BackpackModificationTableBlock(Properties settings) {
		super(settings);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BackpackModificationTableBlockEntity(pos, state);
	}

	@Override
	public void onRemove(
			BlockState state,
			Level world,
			BlockPos pos,
			BlockState newState,
			boolean moved) {
		if (state.getBlock() != newState.getBlock()
				&& world.getBlockEntity(pos) instanceof BackpackModificationTableBlockEntity table) {
			table.dropInventory(world, pos);
		}
		super.onRemove(state, world, pos, newState, moved);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// The model's north face is its front. Point it toward the placing player.
		return defaultBlockState().setValue(
				FACING,
				context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	/**
	 * Supply the screen position to the custom smithing handler. The handler keeps
	 * vanilla recipe calculations while allowing this non-vanilla block.
	 */
	@Override
	public net.minecraft.world.InteractionResult use(
			BlockState state, Level world, BlockPos pos, net.minecraft.world.entity.player.Player player,
			net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
		if (!world.isClientSide && player instanceof ServerPlayer serverPlayer) {
			BlockPos screenPos = pos.immutable();
			net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer,
					new net.minecraft.world.SimpleMenuProvider(
							(containerId, inventory, menuPlayer) ->
									new com.mechaniciancombat.screen.BackpackModificationTableScreenHandler(
										containerId, inventory, screenPos),
							Component.translatable("block.tkgun.backpack_modification_table")),
					buffer -> buffer.writeBlockPos(screenPos));
		}
		return net.minecraft.world.InteractionResult.sidedSuccess(world.isClientSide);
	}

	@Override
	public VoxelShape getShape(
			BlockState state,
			BlockGetter world,
			BlockPos pos,
			CollisionContext context) {
		return shapeFor(state);
	}

	@Override
	public VoxelShape getCollisionShape(
			BlockState state,
			BlockGetter world,
			BlockPos pos,
			CollisionContext context) {
		return shapeFor(state);
	}

	private static VoxelShape shapeFor(BlockState state) {
		return switch (state.getValue(FACING)) {
			case EAST -> EAST_SHAPE;
			case SOUTH -> SOUTH_SHAPE;
			case WEST -> WEST_SHAPE;
			default -> NORTH_SHAPE;
		};
	}
}
