package com.mechaniciancombat.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/** Shared 1.20.1 ItemStack NBT access for data formerly stored in 1.21 item components. */
public final class StackNbt {
	public static CompoundTag copy(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? new CompoundTag() : tag.copy();
	}

	public static void update(ItemStack stack, Consumer<CompoundTag> update) {
		if (stack == null || stack.isEmpty()) {
			return;
		}
		update.accept(stack.getOrCreateTag());
	}

	private StackNbt() {
	}
}
