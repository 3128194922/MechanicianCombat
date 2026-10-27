package com.mechaniciancombat.client;

import com.mechaniciancombat.TkGun;
import net.minecraft.client.Minecraft;

/** Shared client-side rule for jumping while carrying the gun. */
public final class GunJumpRules {
	public static boolean canJump(Minecraft client) {
		if (client.player == null || client.level == null || client.screen != null
				|| !TkGun.holdingGun(client.player)) {
			return true;
		}
		GunClientState.Entry entry = GunClientState.get(client.player.getId());
		if (entry != null && (entry.state == TkGun.State.IDLE
				|| entry.state == TkGun.State.RUN)) {
			return !client.player.isSprinting() && !client.player.isUsingItem()
					&& !GunClientState.dashing(client.player.getId())
					&& !GunClientState.overheated(client.player.getId());
		}
		if (client.player.isSprinting() || client.player.isUsingItem()
				|| GunClientState.dashing(client.player.getId())
				|| GunClientState.focusish(client.player.getId())
				|| GunClientState.overheated(client.player.getId())) {
			return false;
		}
		return entry == null;
	}

	private GunJumpRules() {
	}
}
