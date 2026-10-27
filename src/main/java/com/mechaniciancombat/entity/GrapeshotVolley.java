package com.mechaniciancombat.entity;

import com.mechaniciancombat.CombatTargeting;

import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.net.ModNetwork;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Shared per-tick collision cache and hit-sound coalescing for one grapeshot volley. */
public final class GrapeshotVolley {
	private final int ownerId;
	private final AABB searchBox;
	private long candidateTick = Long.MIN_VALUE;
	private long soundTick = Long.MIN_VALUE;
	private List<Entity> candidates = List.of();

	public GrapeshotVolley(int ownerId, Vec3 muzzle, Vec3 forward) {
		this.ownerId = ownerId;
		Vec3 end = muzzle.add(forward.scale(TkGun.GRAPESHOT_RANGE));
		double radius = TkGun.GRAPESHOT_CONE_RADIUS + 1.0D;
		this.searchBox = new AABB(
				Math.min(muzzle.x, end.x) - radius, Math.min(muzzle.y, end.y) - radius,
				Math.min(muzzle.z, end.z) - radius, Math.max(muzzle.x, end.x) + radius,
				Math.max(muzzle.y, end.y) + radius, Math.max(muzzle.z, end.z) + radius);
	}

	public List<Entity> candidates(ServerLevel world) {
		long now = world.getGameTime();
		if (candidateTick != now) {
			candidateTick = now;
			candidates = world.getEntitiesOfClass(Entity.class, searchBox,
					e -> e.getId() != ownerId && e.isAlive() && !e.isSpectator()
							&& e.isPickable() && !CombatTargeting.isGolem(e));
		}
		return candidates;
	}

	public void playMergedHitSound(ServerLevel world, Vec3 pos) {
		long now = world.getGameTime();
		if (soundTick == now) {
			return;
		}
		soundTick = now;
		world.playSound(null, net.minecraft.core.BlockPos.containing(pos),
				net.minecraft.sounds.SoundEvent.createVariableRangeEvent(TkGun.SND_GRAPESHOT_HIT),
				net.minecraft.sounds.SoundSource.PLAYERS, 0.45F,
				0.92F + world.getRandom().nextFloat() * 0.16F);
		if (world.getEntity(ownerId) instanceof ServerPlayer owner) {
			ModNetwork.send(owner, new com.mechaniciancombat.net.GunHitMarkerPayload());
		}
	}
}
