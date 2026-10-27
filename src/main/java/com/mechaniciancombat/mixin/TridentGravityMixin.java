package com.mechaniciancombat.mixin;

import com.mechaniciancombat.TkGunServer;
import com.mechaniciancombat.entity.MoltenTridentEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** 1.20.1 在箭矢 tick 内直接扣除重力；仅对本模组指定弹体调整该 Y 分量。 */
@Mixin(AbstractArrow.class)
public abstract class TridentGravityMixin {
	@ModifyArg(
			method = "tick",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;setDeltaMovement(DDD)V"),
			index = 1)
	private double tkgun$adjustArrowGravity(double nextY) {
		Entity entity = (Entity) (Object) this;
		if (TkGunServer.isBackpackProjectile(entity)) {
			if (entity instanceof ThrownTrident trident
					&& TkGunServer.BOLT != null
					&& ((TridentEntityAccessor) trident).tkgun$getTridentItem().is(TkGunServer.BOLT)) {
				return nextY + 0.05D - 0.017D;
			}
			return nextY + 0.05D - (0.05D / 3.0D);
		}
		if (entity instanceof MoltenTridentEntity molten && molten.isGunFired()) {
			return nextY + 0.05D;
		}
		if (entity instanceof ThrownTrident trident
				&& TkGunServer.BOLT != null
				&& ((TridentEntityAccessor) trident).tkgun$getTridentItem().is(TkGunServer.BOLT)) {
			return nextY + 0.05D - 0.017D;
		}
		return nextY;
	}
}
