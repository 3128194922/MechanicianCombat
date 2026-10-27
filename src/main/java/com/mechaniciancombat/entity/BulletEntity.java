package com.mechaniciancombat.entity;

import com.mechaniciancombat.CombatTargeting;
import com.mechaniciancombat.TkGun;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * **看得见的那颗子弹**（`tkgun:bullet`）—— 2026-10-15 用户："里面给了那个 bullet 的那个子弹模型，
 * 你用这个去代表那个弹丸，就射出去的那个"。
 *
 * <p>⚠️ 它**不负责伤害**：伤害/命中爆裂还是服务端那条 hitscan（瞬时、权威，见
 * {@code TkGunServer#fireOnce}）。这个实体只是"画面上从枪口飞出去的那颗子弹"：
 * 出生时给一个方向 + 要飞多远（= 落点距离），飞到就自己消失。
 *
 * <p>所以它 ①不用碰撞（`noClip`）、②不受重力（每 tick 自己推进位置）、
 * ③不改任何游戏状态（`isSpectator`/无敌对 AI 无影响，只是画个贴图，
 * 见 {@code client/entity/BulletRenderer}）。
 */
public class BulletEntity extends Entity {
	public static final int STYLE_TRAIL = 0;
	public static final int STYLE_EXPLOSIVE = 1;
	public static final int STYLE_CUTTING = 2;
	/** 幽匿弹：播放素材 `粒子/幽匿弹/sonic_bullet.ase` 的 6 帧飞行弹头。 */
	public static final int STYLE_SCULK = 3;
	public static final int STYLE_GRAPESHOT = 4;
	public static final int STYLE_ROSE_QUARTZ = 5;
	private static final int ROSE_TARGET_SCAN_INTERVAL = 3;
	private static final int ROSE_LINE_OF_SIGHT_INTERVAL = 2;
	private static final int ROSE_MAX_LINE_OF_SIGHT_CHECKS = 3;
	private static final EntityDataAccessor<Integer> STYLE =
			SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.INT);
	/** 已经飞了几 tick（到 {@link TkGun#BULLET_MAX_TICKS} 兜底自毁） */
	private int age;
	/** 还要飞多远（格）：出生时算好，飞够就消失 */
	private double remaining;
	private Runnable impactAction;
	private GuidedImpactHandler guidedImpactAction;
	private int ownerId = -1;
	private double guidanceRadius;
	private int roseTargetId = -1;
	private int roseTargetScanAt;
	private int roseLineOfSightAt;
	private boolean roseTargetHighlighted;
	private boolean roseTracking;
	private final Set<Integer> grapeshotTouchedEntities = new HashSet<>();
	private double grapeshotTravelled;
	private GrapeshotVolley grapeshotVolley;

	@FunctionalInterface
	public interface GuidedImpactHandler {
		void impact(Entity target, Vec3 hitPos, Vec3 direction);
	}

	public BulletEntity(EntityType<? extends BulletEntity> type, Level world) {
		super(type, world);
		this.noPhysics = true;
	}

	/** 出膛：方向（单位向量）+ 落点距离。 */
	public void launch(Vec3 dir, double distance) {
		launch(dir, distance, TkGun.BULLET_SPEED);
	}

	public void launch(Vec3 dir, double distance, double speed) {
		Vec3 d = dir.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : dir.normalize();
		setDeltaMovement(d.scale(speed));
		remaining = Math.max(0.0D, distance);
		grapeshotTravelled = 0.0D;
		xo = getX();
		yo = getY();
		zo = getZ();
	}

	public void setBulletStyle(int style) {
		entityData.set(STYLE, style);
	}

	public int bulletStyle() {
		return entityData.get(STYLE);
	}

	public void setImpactAction(Runnable impactAction) {
		this.impactAction = impactAction;
	}

	public void configureGuidance(Entity owner, double radius) {
		ownerId = owner == null ? -1 : owner.getId();
		guidanceRadius = Math.max(0.0D, radius);
	}

	public void setGuidedImpactAction(GuidedImpactHandler action) {
		guidedImpactAction = action;
	}

	public void setGrapeshotVolley(GrapeshotVolley volley) {
		this.grapeshotVolley = volley;
	}


	@Override
	public void tick() {
		baseTick();
		age++;                            // 客户端也要走（渲染逐帧翻页用；位置由服务端同步 + 插值）
		if (level().isClientSide) {
			return;
		}
		int style = bulletStyle();
		if (age > TkGun.BULLET_MAX_TICKS) {
			discard();
			return;
		}
		Vec3 from = position();
		Vec3 v = getDeltaMovement();
		List<LivingEntity> roseCandidates = null;
		if (style == STYLE_ROSE_QUARTZ && level() instanceof ServerLevel guidedWorld) {
			Vec3 currentDirection = v.normalize();
			AABB search = new AABB(from, from)
					.expandTowards(currentDirection.scale(guidanceRadius))
					.inflate(guidanceRadius);
			roseCandidates = guidedWorld.getEntitiesOfClass(
					LivingEntity.class, search,
					e -> e.getId() != getId() && e.getId() != ownerId
							&& e.isAlive() && !e.isSpectator() && e.isPickable()
							&& !CombatTargeting.isGolem(e));
			Vec3 guided = guidedDirection(
					guidedWorld, from, currentDirection, roseCandidates);
			if (guided.distanceToSqr(currentDirection) > 1.0E-12D) {
				setDeltaMovement(guided.scale(v.length()));
				hurtMarked = true;
			}
			v = getDeltaMovement();
		}
		double stepDistance = Math.min(v.length(), Math.max(0.0D, remaining));
		Vec3 direction = v.normalize();
		Vec3 step = direction.scale(stepDistance);
		Vec3 nextPos = from.add(step);
		if (style == STYLE_ROSE_QUARTZ && level() instanceof ServerLevel trailWorld
				&& roseTracking) {
			int trailSteps = Math.max(1, (int) Math.ceil(stepDistance / 0.45D));
			for (int i = 1; i <= trailSteps; i++) {
				double f = i / (double) trailSteps;
				trailWorld.sendParticles(
						com.mechaniciancombat.TkGunParticles.ROSE_QUARTZ_TRAIL,
						from.x + step.x * f, from.y + step.y * f, from.z + step.z * f,
						1, 0.0D, 0.0D, 0.0D, 0.0D);
			}
		}
		if (style == STYLE_ROSE_QUARTZ && level() instanceof ServerLevel collisionWorld
				&& roseQuartzCollides(collisionWorld, from, nextPos, direction, roseCandidates)) {
			discard();
			return;
		}
			setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
		if (style == STYLE_TRAIL) {
			Vec3 to = position();
			int steps = Math.max(1, (int) Math.ceil(v.length() / 0.55D));
			for (int i = 1; i <= steps; i++) {
				double f = i / (double) steps;
				if (level() instanceof ServerLevel serverWorld) {
					serverWorld.sendParticles(ParticleTypes.FLAME,
							from.x + (to.x - from.x) * f,
							from.y + (to.y - from.y) * f,
							from.z + (to.z - from.z) * f,
							1, 0.0D, 0.0D, 0.0D, 0.0D);
				}
			}
		}
		if (style == STYLE_GRAPESHOT && level() instanceof ServerLevel serverWorld
				&& !grapeshotContacts(serverWorld, from, position())) {
			discard();
			return;
		}
		if (style == STYLE_GRAPESHOT && level() instanceof ServerLevel) {
			grapeshotTravelled += stepDistance;
			double nextSpeed = TkGun.GRAPESHOT_SPEED * Math.pow(
					TkGun.GRAPESHOT_SPEED_DECAY_PER_BLOCK, grapeshotTravelled);
			setDeltaMovement(v.normalize().scale(nextSpeed));
			hurtMarked = true;
		}
		remaining -= stepDistance;
		if (remaining <= 0.0D && age >= TkGun.BULLET_MIN_TICKS) {
			if (impactAction != null) {
				impactAction.run();
				impactAction = null;
			}
			discard();                    // 到落点（爆裂粒子那一下在 fireOnce 里已经放了）
		}
	}

	private Vec3 guidedDirection(ServerLevel world, Vec3 from, Vec3 direction,
								  List<LivingEntity> candidates) {
		roseTracking = false;
		if (guidanceRadius <= 1.0E-6D) {
			return direction;
		}
		LivingEntity highlighted = backpackHighlightedTarget(world);
		if (highlighted != null) {
			boolean needsCheck = !roseTargetHighlighted
					|| roseTargetId != highlighted.getId()
					|| age >= roseLineOfSightAt;
			if (!needsCheck || hasRoseLineOfSight(world, from, highlighted)) {
				roseTargetId = highlighted.getId();
				roseTargetHighlighted = true;
				roseTargetScanAt = age + ROSE_TARGET_SCAN_INTERVAL;
				roseLineOfSightAt = age + ROSE_LINE_OF_SIGHT_INTERVAL;
				roseTracking = true;
				return directionTo(from, highlighted, direction);
			}
		}
		if (roseTargetHighlighted) {
			roseTargetHighlighted = false;
			roseTargetId = -1;
			roseTargetScanAt = age;
		}
		LivingEntity target = roseTarget(world);
		boolean rescan = age >= roseTargetScanAt
				|| !isRoseTargetValid(target, from, direction);
		if (!rescan && age >= roseLineOfSightAt) {
			if (!hasRoseLineOfSight(world, from, target)) {
				rescan = true;
			} else {
				roseLineOfSightAt = age + ROSE_LINE_OF_SIGHT_INTERVAL;
			}
		}
		if (rescan) {
			target = selectRoseTarget(world, from, direction, candidates);
		}
		if (target == null) {
			return direction;
		}
		roseTracking = true;
		return directionTo(from, target, direction);
	}

	private LivingEntity backpackHighlightedTarget(ServerLevel world) {
		Entity owner = world.getEntity(ownerId);
		return owner instanceof net.minecraft.server.level.ServerPlayer player
				? com.mechaniciancombat.TkGunServer.backpackHighlightedTarget(player) : null;
	}

	private Vec3 directionTo(Vec3 from, LivingEntity target, Vec3 fallback) {
		Vec3 aimed = target.getBoundingBox().getCenter().subtract(from);
		return aimed.lengthSqr() < 1.0E-6D ? fallback : aimed.normalize();
	}

	private LivingEntity roseTarget(ServerLevel world) {
		if (roseTargetId < 0) {
			return null;
		}
		return world.getEntity(roseTargetId) instanceof LivingEntity living
				? living : null;
	}

	private boolean isRoseTargetValid(LivingEntity target, Vec3 from, Vec3 direction) {
		if (target == null || target.isRemoved() || !target.isAlive()
				|| target.isSpectator() || !target.isPickable()
				|| CombatTargeting.isGolem(target)) {
			return false;
		}
		Vec3 offset = target.getBoundingBox().getCenter().subtract(from);
		return offset.lengthSqr() <= guidanceRadius * guidanceRadius
				&& offset.dot(direction) > 0.0D;
	}

	private LivingEntity selectRoseTarget(ServerLevel world, Vec3 from, Vec3 direction,
										  List<LivingEntity> candidates) {
		roseTargetScanAt = age + ROSE_TARGET_SCAN_INTERVAL;
		roseLineOfSightAt = age + ROSE_LINE_OF_SIGHT_INTERVAL;
		if (candidates == null || candidates.isEmpty()) {
			roseTargetId = -1;
			roseTargetHighlighted = false;
			return null;
		}

		List<LivingEntity> ranked = new ArrayList<>(candidates.size());
		for (LivingEntity candidate : candidates) {
			if (isRoseTargetValid(candidate, from, direction)) {
				ranked.add(candidate);
			}
		}
		ranked.sort(Comparator.comparingDouble(candidate ->
				candidate.getBoundingBox().getCenter().distanceToSqr(from)));

		int checks = Math.min(ROSE_MAX_LINE_OF_SIGHT_CHECKS, ranked.size());
		for (int i = 0; i < checks; i++) {
			LivingEntity candidate = ranked.get(i);
			if (hasRoseLineOfSight(world, from, candidate)) {
				roseTargetId = candidate.getId();
				roseTargetHighlighted = false;
				return candidate;
			}
		}
		roseTargetId = -1;
		roseTargetHighlighted = false;
		return null;
	}

	private boolean hasRoseLineOfSight(ServerLevel world, Vec3 from, LivingEntity target) {
		if (target == null) {
			return false;
		}
		Vec3 center = target.getBoundingBox().getCenter();
		BlockHitResult obstruction = world.clip(new ClipContext(from, center,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		return obstruction.getType() == HitResult.Type.MISS
				|| from.distanceTo(obstruction.getLocation()) >= from.distanceTo(center) - 0.05D;
	}

	private boolean roseQuartzCollides(ServerLevel world, Vec3 from, Vec3 to,
									   Vec3 direction, List<LivingEntity> candidates) {
		BlockHitResult blockHit = world.clip(new ClipContext(from, to,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		double blockDistance = blockHit.getType() == HitResult.Type.MISS
				? Double.MAX_VALUE : from.distanceTo(blockHit.getLocation());

		AABB search = new AABB(from, to).inflate(1.0D);
		EntityHitResult bestEntityHit = null;
		double bestEntityDistance = Double.MAX_VALUE;
		if (candidates != null) {
			for (LivingEntity candidate : candidates) {
				AABB bounds = candidate.getBoundingBox().inflate(1.0D);
				if (!bounds.intersects(search)) {
					continue;
				}
				java.util.Optional<Vec3> hit = bounds.clip(from, to);
				if (hit.isEmpty()) {
					continue;
				}
				double distance = from.distanceTo(hit.get());
				if (distance < bestEntityDistance) {
					bestEntityDistance = distance;
					bestEntityHit = new EntityHitResult(candidate, hit.get());
				}
			}
		}

		if (bestEntityHit != null && bestEntityDistance <= blockDistance) {
			Entity target = bestEntityHit.getEntity();
			triggerGuidedImpact(target, bestEntityHit.getLocation(), direction);
			return true;
		}
		if (blockHit.getType() != HitResult.Type.MISS) {
			triggerGuidedImpact(null, blockHit.getLocation(), direction);
			return true;
		}
		return false;
	}

	private void triggerGuidedImpact(Entity target, Vec3 hitPos, Vec3 direction) {
		if (guidedImpactAction != null) {
			guidedImpactAction.impact(target, hitPos, direction);
			guidedImpactAction = null;
		}
	}

	/** Entities are pierced and only receive one contact effect each. */
	private boolean grapeshotContacts(ServerLevel world, Vec3 from, Vec3 to) {
		if (grapeshotVolley == null) {
			return true;
		}
		for (Entity entity : grapeshotVolley.candidates(world)) {
			java.util.Optional<Vec3> hit = entity.getBoundingBox().inflate(0.08D).clip(from, to);
			if (hit.isEmpty()) {
				continue;
			}
			if (grapeshotTouchedEntities.add(entity.getId())) {
				spawnImpact(world, hit.get());
			}
		}
		return true;
	}

	private void spawnImpact(ServerLevel world, Vec3 pos) {
		world.sendParticles(com.mechaniciancombat.TkGunParticles.GRAPESHOT_IMPACT,
				pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		if (grapeshotVolley != null) {
			grapeshotVolley.playMergedHitSound(world, pos);
		}
	}

	/** 别的模组/调试要看它飞了多久。 */
	public int age() {
		return age;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return true;
	}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
		protected void defineSynchedData() {
		this.entityData.define(STYLE, STYLE_TRAIL);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag nbt) {
		setBulletStyle(nbt.getInt("BulletStyle"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag nbt) {
		nbt.putInt("BulletStyle", bulletStyle());
	}
}
