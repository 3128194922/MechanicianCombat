package com.mechaniciancombat.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Applies the non-destructive block reactions of a wind burst on the logical server. */
public final class WindChargeBlockInteractions {
    private static final int RADIUS = 4;

    private WindChargeBlockInteractions() {
    }

    public static void apply(Level level, Vec3 center, Entity source) {
        if (level.isClientSide) {
            return;
        }

        BlockPos min = BlockPos.containing(center.x - RADIUS, center.y - RADIUS, center.z - RADIUS);
        BlockPos max = BlockPos.containing(center.x + RADIUS, center.y + RADIUS, center.z + RADIUS);
        BlockPos.betweenClosed(min, max).forEach(pos -> {
            Vec3 blockCenter = Vec3.atCenterOf(pos);
            if (blockCenter.distanceToSqr(center) > RADIUS * RADIUS) {
                return;
            }
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();

            if (state.hasProperty(BlockStateProperties.POWERED)
                    && state.getValue(BlockStateProperties.POWERED)) {
                return;
            }
            if (block instanceof ButtonBlock button) {
                button.press(state, level, pos);
            } else if (block instanceof LeverBlock lever) {
                lever.pull(state, level, pos);
            } else if (block instanceof DoorBlock door && block != Blocks.IRON_DOOR) {
                door.setOpen(source, level, state, pos, !state.getValue(DoorBlock.OPEN));
            } else if (block instanceof TrapDoorBlock && block != Blocks.IRON_TRAPDOOR) {
                level.setBlock(pos, state.setValue(TrapDoorBlock.OPEN, !state.getValue(TrapDoorBlock.OPEN)), 3);
            } else if (block instanceof FenceGateBlock) {
                level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, !state.getValue(FenceGateBlock.OPEN)), 3);
            } else if (block instanceof BellBlock bell) {
                Direction direction = source == null ? Direction.UP : Direction.getNearest(
                        center.x - blockCenter.x, center.y - blockCenter.y, center.z - blockCenter.z);
                bell.attemptToRing(source, level, pos, direction);
            } else if (block instanceof CandleBlock && state.getValue(CandleBlock.LIT)) {
                level.setBlock(pos, state.setValue(CandleBlock.LIT, false), 3);
            } else if (block instanceof CandleCakeBlock && state.getValue(CandleCakeBlock.LIT)) {
                level.setBlock(pos, state.setValue(CandleCakeBlock.LIT, false), 3);
            } else if (block == Blocks.DECORATED_POT || block == Blocks.CHORUS_FLOWER) {
                level.destroyBlock(pos, true, source);
            }
        });
    }
}
