package com.mechaniciancombat.item;

import com.mechaniciancombat.TkGunServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;

/** Shared server/client rules for the portable launcher's occupied offhand. */
public final class BackpackLauncherRules {
	public static boolean hasActiveLauncher(LivingEntity entity) {
		return entity != null
				&& BackpackUpgradeData.canUseLauncher(
						CombatBackpackItem.equippedStack(entity));
	}

	public static boolean isLaunchable(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		return stack.is(Items.FIREWORK_ROCKET)
				|| stack.is(Items.TIPPED_ARROW)
				|| stack.is(TkGunServer.WIND_CHARGE)
				|| isTridentLike(stack);
	}

	public static boolean isTridentLike(ItemStack stack) {
		return stack != null && !stack.isEmpty()
				&& (stack.getItem() instanceof TridentItem
				|| (TkGunServer.BOLT != null && stack.is(TkGunServer.BOLT)));
	}

	private BackpackLauncherRules() {
	}
}
