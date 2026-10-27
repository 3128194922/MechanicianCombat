package com.mechaniciancombat.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Persistent backpack and component inventory for the modification table. */
public final class BackpackModificationTableBlockEntity extends BlockEntity {
	public static final int BACKPACK_SLOT = 0;
	public static final int PART_SLOT_START = 1;
	public static final int PART_SLOT_COUNT = 4;
	public static final int SIZE = PART_SLOT_START + PART_SLOT_COUNT;
	public static BlockEntityType<BackpackModificationTableBlockEntity> TYPE;

	private final SimpleContainer inventory = new SimpleContainer(SIZE) {
		@Override
		public void setChanged() {
			BackpackModificationTableBlockEntity.this.setChanged();
		}
	};

	public BackpackModificationTableBlockEntity(BlockPos pos, BlockState state) {
		super(TYPE, pos, state);
	}

	public SimpleContainer inventory() {
		return inventory;
	}

	public void dropInventory(Level world, BlockPos pos) {
		if (world instanceof ServerLevel) {
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (!stack.isEmpty()) {
					Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
				}
			}
			inventory.clearContent();
			setChanged();
		}
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		NonNullList<ItemStack> loaded = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(nbt, loaded);
		for (int slot = 0; slot < SIZE; slot++) {
			inventory.setItem(slot, loaded.get(slot));
		}
	}

	@Override
	protected void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
				NonNullList<ItemStack> saved = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		for (int slot = 0; slot < SIZE; slot++) {
			saved.set(slot, inventory.getItem(slot));
		}
		ContainerHelper.saveAllItems(nbt, saved);
	}
}
