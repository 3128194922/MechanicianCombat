package com.mechaniciancombat.item;

import net.minecraft.world.item.ItemStack;

/** Marks only the firework rockets launched by the combat backpack. */
public final class BackpackFireworkData {
	private static final String NO_SELF_DAMAGE = "tkgun_backpack_no_self_damage";

	public static void markNoSelfDamage(ItemStack rocket) {
		StackNbt.update(rocket, nbt -> nbt.putBoolean(NO_SELF_DAMAGE, true));
	}

	public static boolean preventsSelfDamage(ItemStack rocket) {
		return StackNbt.copy(rocket).getBoolean(NO_SELF_DAMAGE);
	}

	private BackpackFireworkData() {
	}
}
