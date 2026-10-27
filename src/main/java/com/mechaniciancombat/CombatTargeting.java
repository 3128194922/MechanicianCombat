package com.mechaniciancombat;

import com.mechaniciancombat.entity.BoltProjectileEntity;
import com.mechaniciancombat.entity.BulletEntity;
import com.mechaniciancombat.entity.MoltenGolemEntity;
import com.mechaniciancombat.entity.MoltenTridentEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

/** Shared exclusions for MechanicianCombat attacks, projectiles, and locks. */
public final class CombatTargeting {
	public static final String BACKPACK_PROJECTILE_TAG = "tkgun_backpack_projectile";

	private static final ResourceKey<DamageType> GUN_DAMAGE =
			ResourceKey.create(Registries.DAMAGE_TYPE, TkGun.id("bolt"));
	private static final ResourceKey<DamageType> SONIC_DAMAGE =
			ResourceKey.create(Registries.DAMAGE_TYPE, TkGun.id("sonic"));

	public static boolean isGolem(Entity entity) {
		return entity instanceof MoltenGolemEntity;
	}

	public static boolean isTargetable(Entity entity) {
		return entity != null && !isGolem(entity);
	}

	public static boolean isTkGunDamage(DamageSource source) {
		if (source == null) {
			return false;
		}
		if (source.is(GUN_DAMAGE) || source.is(SONIC_DAMAGE)) {
			return true;
		}
		Entity direct = source.getDirectEntity();
		Entity attacker = source.getEntity();
		if (attacker instanceof Player player && TkGun.holdingGun(player)) {
			return true;
		}
		return direct instanceof BulletEntity
				|| direct instanceof BoltProjectileEntity
				|| direct instanceof MoltenTridentEntity
				|| (direct != null
						&& direct.getTags().contains(BACKPACK_PROJECTILE_TAG));
	}

	private CombatTargeting() {
	}
}
