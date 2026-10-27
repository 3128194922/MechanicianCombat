package com.mechaniciancombat.mixin;

import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.client.GunClientState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents Combat Roll from physically rolling while the gun state machine owns the dodge sequence. */
@Pseudo
@Mixin(targets = "net.combatroll.internals.RollManager")
public abstract class CombatRollInputGuardMixin {
	@Inject(method = "isRollAvailable", remap = false, at = @At("HEAD"), cancellable = true)
	private void tkgun$guardDodgeSequence(Player player,
										  CallbackInfoReturnable<Boolean> cir) {
		if (player instanceof LocalPlayer && TkGun.holdingGun(player)
				&& GunClientState.combatRollSuppressed(player.getId())) {
			cir.setReturnValue(false);
		}
	}
}
