package com.mechaniciancombat.event;

import com.mechaniciancombat.item.BackpackFireworkData;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Prevents only marked combat-backpack rockets from damaging their owner. */
public final class BackpackFireworkSelfDamageHandler {
	public static void onLivingHurt(LivingHurtEvent event) {
		if (!(event.getSource().getDirectEntity() instanceof FireworkRocketEntity rocket)) {
			return;
		}
		if (event.getEntity() == rocket.getOwner() && BackpackFireworkData.preventsSelfDamage(rocket.getItem())) {
			event.setCanceled(true);
		}
	}

	private BackpackFireworkSelfDamageHandler() {
	}
}
