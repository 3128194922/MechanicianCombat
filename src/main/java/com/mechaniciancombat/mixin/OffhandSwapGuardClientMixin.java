package com.mechaniciancombat.mixin;

import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.item.BackpackLauncherRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops the offhand-swap packet locally so the gun never visually enters the offhand. */
@Mixin(ClientPacketListener.class)
public abstract class OffhandSwapGuardClientMixin {
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
	private void tkgun$rejectGunOffhandSwap(Packet<?> packet, CallbackInfo ci) {
		if (packet instanceof ServerboundPlayerActionPacket action
				&& action.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
			Minecraft client = Minecraft.getInstance();
			if (client.player != null
					&& (TkGun.isGun(client.player.getMainHandItem())
					|| (BackpackLauncherRules.hasActiveLauncher(client.player)
					&& !BackpackLauncherRules.isLaunchable(client.player.getMainHandItem())))) {
				ci.cancel();
			}
		}
	}
}
