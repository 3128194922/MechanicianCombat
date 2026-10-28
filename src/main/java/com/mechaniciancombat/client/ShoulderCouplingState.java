package com.mechaniciancombat.client;

import com.mechaniciancombat.TkGun;
import net.minecraft.client.Minecraft;

/**
 * ShoulderSurfing 的相机耦合策略。
 *
 * <p>腰射期间让 ShoulderSurfing 同步相机和玩家朝向；瞄准、专注及全身动作期间保留相机解耦，
 * 避免本模组的枪械姿态控制与 ShoulderSurfing 同时改写玩家朝向。
 */
public final class ShoulderCouplingState {
	private static boolean registered;

	public static boolean shoulderCouplingRegistered() {
		return registered;
	}

	static void markRegistered() {
		registered = true;
	}

	public static boolean shouldCoupleCamera(Minecraft client) {
		if (client == null || client.player == null || client.screen != null) {
			return false;
		}
		if (!TkGun.holdingGun(client.player)) {
			// ShoulderSurfing may expose vanilla THIRD_PERSON_BACK underneath
			// its own perspective. Keep non-item shoulder use decoupled.
			if (net.minecraftforge.fml.ModList.get().isLoaded("shouldersurfing") && ShoulderSurfBridge.isOverShoulderActive()
					&& !client.player.isUsingItem()) {
				return false;
			}
			// Outside gun handling, vanilla third-person should keep camera and
			// player coupled instead of inheriting the gun's decoupled mode.
			return client.options.getCameraType()
					== net.minecraft.client.CameraType.THIRD_PERSON_BACK;
		}
		if (client.options.getCameraType() == net.minecraft.client.CameraType.THIRD_PERSON_FRONT
				|| client.player.isUsingItem()) {
			return false;
		}
		int id = client.player.getId();
		if (GunClientState.focusish(id)) {
			return false;
		}
		GunClientState.Entry state = GunClientState.get(id);
		if (state == null) {
			return true;
		}
		if (state.state == TkGun.State.IDLE && !TkGunClient.isMoving(id)
				&& !TkGunClient.hasMovementInput(id)
				&& !com.mechaniciancombat.client.rig.BowgunRig.recentUpperOperation(client.player)) return false;
		return switch (state.state) {
			case ROLL, ROLL_TO_IDLE, SLIDE, SLIDE_READY, OVERHEAT, OVER, INSPECT -> false;
			default -> true;
		};
	}

	/**
	 * True when the camera/body split must survive a held right-click.
	 * ShoulderSurfing's when_using_item=ALWAYS path would otherwise turn the body.
	 */
	public static boolean requiresDecoupledView(Minecraft client) {
		return decoupledReason(client) != 0;
	}

	/**
	 * Identifies which decoupled state currently owns the camera. Returning the
	 * actual reason lets aiming start inside one state, then release only when a
	 * different decoupled state is entered while right-click is still held.
	 */
	public static int decoupledReason(Minecraft client) {
		if (client == null || client.player == null || client.screen != null
				|| !TkGun.holdingGun(client.player)) {
			return 0;
		}
		int id = client.player.getId();
		if (client.player.isSprinting() || GunClientState.dashing(id)) {
			return client.player.isSprinting() ? 1 : 2;
		}
		if (client.player.isUsingItem()
				&& client.player.getUseItem().is(com.mechaniciancombat.TkGunServer.BOWGUN)) {
			return 12;
		}
		if (GunClientState.focusish(id)) {
			return 3;
		}
		GunClientState.Entry e = GunClientState.get(id);
		if (e == null) {
			return GunClientState.aimLocked(id) ? 12 : 0;
		}
		return switch (e.state) {
			case ROLL -> 4;
			case ROLL_TO_IDLE -> 5;
			case SLIDE -> 7;
			case SLIDE_READY -> 8;
			case OVERHEAT -> 9;
			case OVER -> 10;
			case INSPECT -> 11;
			default -> GunClientState.aimLocked(id) ? 12 : 0;
		};
	}

	private ShoulderCouplingState() {
	}
}
