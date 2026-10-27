package com.mechaniciancombat.mixin;

import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.item.BackpackLauncherRules;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rejects vanilla offhand swaps when the main hand holds this mod's gun. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class OffhandSwapGuardMixin {
	@Shadow
	public ServerPlayer player;

	@Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
	private void tkgun$rejectGunOffhandSwap(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
		if (packet.getAction() != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
			return;
		}
		var mainHand = player.getMainHandItem();
		if (TkGun.isGun(mainHand)
				|| (BackpackLauncherRules.hasActiveLauncher(player)
				&& !BackpackLauncherRules.isLaunchable(mainHand))) {
			ci.cancel();
		}
	}
}
