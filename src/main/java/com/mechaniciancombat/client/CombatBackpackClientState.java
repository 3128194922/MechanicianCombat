package com.mechaniciancombat.client;

import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.item.CombatBackpackAnimation;
import com.mechaniciancombat.item.CombatBackpackItem;
import com.mechaniciancombat.item.BackpackUpgradeData;
import com.mechaniciancombat.net.CombatBackpackStatusPayload;
import com.mechaniciancombat.client.ClientNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Camera;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class CombatBackpackClientState {
	private static final int FIRE_RESULT_TIMEOUT_TICKS = 40;
	private static final int MAX_CHARGES = 10;
	private static final double PROJECTILE_OFFSET_X = -3.0D / 16.0D;
	private static final double PROJECTILE_OFFSET_Y = 13.0D / 16.0D;
	private static final double PROJECTILE_OFFSET_Z = 8.0D / 16.0D;

	private static final class Entry {
		boolean firingRequested;
		int firingRequestedAt;
		int targetEntityId = -1;
		int phase = CombatBackpackStatusPayload.PHASE_READY;
		int charges;
		int remainingTicks;
		boolean aimLatched;
		BackpackUpgradeData.LauncherType launcherType = BackpackUpgradeData.LauncherType.NONE;
		CombatBackpackAnimation.Phase animationPhase = CombatBackpackAnimation.Phase.READY;
		boolean animationInitialized;
		int openSoundAt = -1;
		int closeSoundAt = -1;
	}

	private static final Map<Integer, Entry> ENTRIES = new HashMap<>();
	private static int ticks;

	public static void trigger(LocalPlayer player) {
		if (player == null) {
			return;
		}
		if (!BackpackUpgradeData.canUseLauncher(
				CombatBackpackItem.equippedStack(player))) {
			return;
		}
		Entry entry = ENTRIES.computeIfAbsent(player.getId(), ignored -> new Entry());
		if (entry.phase == CombatBackpackStatusPayload.PHASE_COOLDOWN
				|| entry.firingRequested) {
			return;
		}
		if (hasActiveTarget(player, entry)) {
			entry.firingRequested = true;
			entry.firingRequestedAt = ticks;
		}
		sendAction(player);
	}

	public static void tick(Minecraft client) {
		ticks++;
		for (Map.Entry<Integer, Entry> pair : ENTRIES.entrySet()) {
			Entry state = pair.getValue();
			if (state.remainingTicks > 0) {
				state.remainingTicks--;
			}
			if (state.phase == CombatBackpackStatusPayload.PHASE_MARKED
					&& state.remainingTicks == 0) {
				state.phase = CombatBackpackStatusPayload.PHASE_READY;
				state.targetEntityId = -1;
			}
			updateAnimationPhase(pair.getKey(), state);
			if (pair.getKey() == localPlayerId()
					&& state.openSoundAt >= 0 && ticks >= state.openSoundAt) {
				state.openSoundAt = -1;
				playBackpackSound("open");
			}
			if (pair.getKey() == localPlayerId()
					&& state.closeSoundAt >= 0 && ticks >= state.closeSoundAt) {
				state.closeSoundAt = -1;
				playBackpackSound("close");
			}
		}
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		Entry entry = ENTRIES.computeIfAbsent(player.getId(), ignored -> new Entry());
		if (!BackpackUpgradeData.canUseLauncher(
				CombatBackpackItem.equippedStack(player))) {
			reset(player.getId());
			return;
		}
		if (entry.firingRequested
				&& ticks - entry.firingRequestedAt >= FIRE_RESULT_TIMEOUT_TICKS) {
			// A disconnect or rejected packet must not strand C3 in the firing state.
			entry.firingRequested = false;
		}
	}

	private static void sendAction(LocalPlayer player) {
		Vec3 front = projectileSpawnPosition(player);
		Minecraft client = Minecraft.getInstance();
		Camera camera = client.gameRenderer.getMainCamera();
		Vec3 cameraPos = camera.getPosition();
		double verticalFov = Math.toRadians(
				Math.max(1.0D, Math.min(170.0D, GunCamera.renderedFovDegrees())));
		double halfVerticalTan = Math.tan(verticalFov * 0.5D);
		int framebufferWidth = Math.max(1, client.getWindow().getWidth());
		int framebufferHeight = Math.max(1, client.getWindow().getHeight());
		double halfHorizontalTan = halfVerticalTan
				* framebufferWidth / (double) framebufferHeight;
		ClientNetwork.send(new com.mechaniciancombat.net.CombatBackpackFirePayload(
				front.x, front.y, front.z,
				cameraPos.x, cameraPos.y, cameraPos.z,
				camera.getYRot(), camera.getXRot(),
				halfHorizontalTan, halfVerticalTan));
	}

	public static void applyStatus(int ownerEntityId, int targetEntityId, int phase,
								   int charges, int remainingTicks) {
		Entry entry = ENTRIES.computeIfAbsent(ownerEntityId, ignored -> new Entry());
		entry.targetEntityId = Math.max(-1, targetEntityId);
		entry.phase = Math.max(CombatBackpackStatusPayload.PHASE_READY,
				Math.min(CombatBackpackStatusPayload.PHASE_SUMMON_CAST, phase));
		Minecraft client = Minecraft.getInstance();
		if (client.level != null
				&& client.level.getEntity(ownerEntityId)
				instanceof net.minecraft.world.entity.LivingEntity owner) {
			entry.launcherType = BackpackUpgradeData.launcherType(
					CombatBackpackItem.equippedStack(owner));
		}
		entry.charges = Math.max(0, Math.min(MAX_CHARGES, charges));
		entry.remainingTicks = Math.max(0, remainingTicks);
		if (entry.phase == CombatBackpackStatusPayload.PHASE_MARKED) {
			entry.aimLatched = true;
		} else if (entry.phase == CombatBackpackStatusPayload.PHASE_COOLDOWN
				|| entry.phase == CombatBackpackStatusPayload.PHASE_SUMMON_DORMANT) {
			entry.aimLatched = false;
		}
		updateAnimationPhase(ownerEntityId, entry);
	}

	private static boolean hasActiveTarget(LocalPlayer player, Entry entry) {
		if (entry.phase != CombatBackpackStatusPayload.PHASE_MARKED
				|| entry.targetEntityId < 0 || entry.remainingTicks <= 0) {
			return false;
		}
		net.minecraft.world.entity.Entity target = player.level().getEntity(entry.targetEntityId);
		if (target == null || !target.isAlive()) {
			clearTarget(entry);
			return false;
		}
		return entry.charges > 0;
	}

	private static void clearTarget(Entry entry) {
		entry.targetEntityId = -1;
	}

	public static void completeFire(int playerId) {
		Entry entry = ENTRIES.get(playerId);
		if (entry == null) {
			return;
		}
		entry.firingRequested = false;
	}

	public static void reset(int playerId) {
		ENTRIES.remove(playerId);
		CombatBackpackAnimation.clear(playerId);
		com.mechaniciancombat.client.rig.CombatBackpackRigLayer.clearCachedStack(playerId);
	}

	private static void updateAnimationPhase(int playerId, Entry entry) {
		CombatBackpackAnimation.Phase next = animationPhase(entry);
		if (entry.animationInitialized && next == entry.animationPhase) {
			return;
		}
		entry.animationInitialized = true;
		entry.animationPhase = next;
		CombatBackpackAnimation.setPhase(playerId, next);
		entry.openSoundAt = -1;
		entry.closeSoundAt = -1;
		if (playerId != localPlayerId()) {
			return;
		}
		if (next == CombatBackpackAnimation.Phase.READY) {
			playBackpackSound("open");
			entry.closeSoundAt = ticks + 6;
		} else if (next == CombatBackpackAnimation.Phase.CD) {
			entry.openSoundAt = ticks + 1;
			entry.closeSoundAt = ticks + 7;
		}
	}

	private static int localPlayerId() {
		Minecraft client = Minecraft.getInstance();
		return client.player == null ? -1 : client.player.getId();
	}

	private static void playBackpackSound(String name) {
		Minecraft client = Minecraft.getInstance();
		if (client.getSoundManager() == null) {
			return;
		}
		SoundEvent sound = SoundEvent.createVariableRangeEvent(TkGun.id("backpack_" + name));
		client.getSoundManager().play(SimpleSoundInstance.forUI(
				sound, 1.0F, TkGun.BACKPACK_SOUND_VOLUME));
	}

	public static CombatBackpackAnimation.Phase phase(int playerId) {
		Entry entry = ENTRIES.get(playerId);
		return entry == null ? CombatBackpackAnimation.Phase.READY : animationPhase(entry);
	}

	public static int statusPhase(int playerId) {
		Entry entry = ENTRIES.get(playerId);
		return entry == null ? CombatBackpackStatusPayload.PHASE_READY : entry.phase;
	}

	public static int targetEntityId(int ownerEntityId) {
		Entry entry = ENTRIES.get(ownerEntityId);
		if (entry == null
				|| (entry.phase != CombatBackpackStatusPayload.PHASE_MARKED
						&& entry.phase != CombatBackpackStatusPayload.PHASE_AUTO_ACTIVE)
				|| entry.targetEntityId < 0 || entry.remainingTicks <= 0) {
			return -1;
		}
		Minecraft client = Minecraft.getInstance();
		net.minecraft.world.entity.Entity target = client.level == null ? null
				: client.level.getEntity(entry.targetEntityId);
		if (target == null || !target.isAlive()) {
			clearTarget(entry);
			return -1;
		}
		return entry.targetEntityId;
	}

	public static int charges(int playerId) {
		Entry entry = ENTRIES.get(playerId);
		return entry == null ? 0 : entry.charges;
	}

	public static int remainingTicks(int playerId) {
		Entry entry = ENTRIES.get(playerId);
		return entry == null ? 0 : entry.remainingTicks;
	}

	private static CombatBackpackAnimation.Phase animationPhase(Entry entry) {
		if (entry.launcherType == BackpackUpgradeData.LauncherType.SUMMON_ANTENNA) {
			return entry.phase == CombatBackpackStatusPayload.PHASE_COOLDOWN
					|| entry.phase == CombatBackpackStatusPayload.PHASE_SUMMON_DORMANT
					? CombatBackpackAnimation.Phase.CD
					: CombatBackpackAnimation.Phase.READY;
		}
		if (entry.phase == CombatBackpackStatusPayload.PHASE_READY && entry.aimLatched) {
			return CombatBackpackAnimation.Phase.AIM;
		}
		return switch (entry.phase) {
			case CombatBackpackStatusPayload.PHASE_COOLDOWN ->
					CombatBackpackAnimation.Phase.CD;
			case CombatBackpackStatusPayload.PHASE_MARKED ->
					CombatBackpackAnimation.Phase.AIM;
			case CombatBackpackStatusPayload.PHASE_AUTO_ACTIVE ->
					CombatBackpackAnimation.Phase.AIM;
			case CombatBackpackStatusPayload.PHASE_AUTO_PAUSED ->
					CombatBackpackAnimation.Phase.AIM;
			default -> CombatBackpackAnimation.Phase.READY;
		};
	}

	public static Vec3 projectileSpawnPosition(LocalPlayer player) {
		Vec3 front = CombatBackpackGeoRenderer.canonicalLaunchPosition(player.getId());
		if (front == null) {
			front = player.position().add(0.0D, 1.0D, 0.0D);
		}
		double yaw = Math.toRadians(player.yBodyRot);
		Vec3 right = new Vec3(-Math.cos(yaw), 0.0D, -Math.sin(yaw));
		Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
		Vec3 offset = right.scale(PROJECTILE_OFFSET_X)
				.add(0.0D, PROJECTILE_OFFSET_Y, 0.0D)
				.add(forward.scale(PROJECTILE_OFFSET_Z));
		return front.add(offset);
	}

	private CombatBackpackClientState() {
	}
}
