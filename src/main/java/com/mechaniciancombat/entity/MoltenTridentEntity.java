package com.mechaniciancombat.entity;

import com.mechaniciancombat.CombatTargeting;

import com.mechaniciancombat.TkGunServer;
import com.mechaniciancombat.mixin.TridentEntityAccessor;
import com.mechaniciancombat.net.ModNetwork;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 熔铁弩矢专用三叉戟实体：继承原版三叉戟的伤害、拾取、忠诚等逻辑，
 * 但使用独立的实体类型，以便客户端渲染资源模型。
 */
public class MoltenTridentEntity extends ThrownTrident {
	private static final EntityDataAccessor<Boolean> GUN_FIRED =
			SynchedEntityData.defineId(MoltenTridentEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> STOPPED =
			SynchedEntityData.defineId(MoltenTridentEntity.class, EntityDataSerializers.BOOLEAN);
	private final Set<UUID> tkgun$hitEntities = new HashSet<>();

	public MoltenTridentEntity(EntityType<? extends MoltenTridentEntity> type, Level world) {
		super(type, world);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(GUN_FIRED, false);
		this.entityData.define(STOPPED, false);
	}

	public void setGunFired(boolean gunFired) {
		this.getEntityData().set(GUN_FIRED, gunFired);
	}

	public boolean isGunFired() {
		return this.getEntityData().get(GUN_FIRED);
	}

	public boolean isStopped() {
		return this.getEntityData().get(STOPPED);
	}

	private void setStopped(boolean stopped) {
		this.getEntityData().set(STOPPED, stopped);
	}

	@Override
	protected EntityHitResult findHitEntity(Vec3 currentPosition, Vec3 nextPosition) {
		// 关闭原版单体碰撞，实体命中统一走下面的 3 格直径胶囊判定，才能穿透。
		return null;
	}

	@Override
	protected void onHitBlock(BlockHitResult blockHitResult) {
		boolean firstBlockHit = !isStopped();
		super.onHitBlock(blockHitResult);
		tkgun$applyChanneling(null, blockHitResult, getWeaponItem());
		setStopped(true);
		if (firstBlockHit) {
			sendGunHitMarker();
		}
		// 只在服务端判定一次；命中方块停止后 50% 消失，否则留在原地可拾取。
		if (!level().isClientSide && level().random.nextFloat() < 0.5F) {
			discard();
		}
	}

	@Override
	public void tick() {
		Vec3 before = this.position();
		super.tick();
		if (this.level().isClientSide || this.isRemoved()) {
			return;
		}
		Vec3 after = this.position();
		if (before.distanceToSqr(after) < 1.0E-8D) {
			return;
		}
		AABB search = new AABB(before, after).inflate(2.0D);
		for (Entity entity : this.level().getEntities(this, search,
				e -> e.isAlive() && !e.isSpectator() && e != this.getOwner()
						&& e.isPickable() && !CombatTargeting.isGolem(e))) {
			if (this.tkgun$hitEntities.contains(entity.getUUID())) {
				continue;
			}
			if (tkgun$horizontalDistanceToSegment(
					entity.getBoundingBox().getCenter(), before, after) > 2.0D) {
				continue;
			}
			this.tkgun$hitEntities.add(entity.getUUID());
			Vec3 velocity = this.getDeltaMovement();
			this.onHitEntity(new EntityHitResult(entity, entity.getBoundingBox().getCenter()));
			sendGunHitMarker();
			// onEntityHit 会设置 dealtDamage=true；穿透期间清掉，
			// 避免带忠诚附魔时第一只实体命中后立刻折返。
			((TridentEntityAccessor) (Object) this).tkgun$setDealtDamage(false);
			this.setDeltaMovement(velocity);
		}
	}

	private void sendGunHitMarker() {
		if (!level().isClientSide && isGunFired()
				&& getOwner() instanceof ServerPlayer owner) {
			ModNetwork.send(owner, new com.mechaniciancombat.net.GunHitMarkerPayload());
		}
	}

	private static double tkgun$horizontalDistanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
		Vec3 delta = end.subtract(start);
		double lengthSquared = delta.x * delta.x + delta.z * delta.z;
		if (lengthSquared < 1.0E-8D) {
			double dx = point.x - start.x;
			double dz = point.z - start.z;
			return Math.sqrt(dx * dx + dz * dz);
		}
		double t = Mth.clamp(
				((point.x - start.x) * delta.x + (point.z - start.z) * delta.z)
						/ lengthSquared,
				0.0D, 1.0D);
		double dx = point.x - (start.x + delta.x * t);
		double dz = point.z - (start.z + delta.z * t);
		return Math.sqrt(dx * dx + dz * dz);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		nbt.putBoolean("TkGunFired", isGunFired());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		setGunFired(nbt.getBoolean("TkGunFired"));
		setStopped(this.inGround);
	}

	public MoltenTridentEntity(EntityType<? extends MoltenTridentEntity> type, Level world,
							   LivingEntity owner, ItemStack stack) {
		super(type, world);
		this.setOwner(owner);
		ItemStack projectileStack = stack.copyWithCount(1);
		((TridentEntityAccessor) (Object) this).tkgun$setTridentItem(projectileStack);
		this.moveTo(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ(),
				owner.getYRot(), owner.getXRot());
		this.tkgun$syncStackData(projectileStack);
	}

	public ItemStack getWeaponItem() {
		return ((TridentEntityAccessor) (Object) this).tkgun$getTridentItem();
	}

	private void tkgun$syncStackData(ItemStack stack) {
		TridentEntityAccessor accessor = (TridentEntityAccessor) (Object) this;
		byte loyalty = (byte) Mth.clamp(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LOYALTY, stack), 0, 127);
		this.getEntityData().set(TridentEntityAccessor.tkgun$getLoyaltyData(), loyalty);
		this.getEntityData().set(TridentEntityAccessor.tkgun$getEnchantedData(), stack.isEnchanted());
	}

	@Override
	protected ItemStack getPickupItem() {
		ItemStack weapon = getWeaponItem();
		if (!weapon.isEmpty()) return weapon.copyWithCount(1);
		return TkGunServer.BOLT == null ? ItemStack.EMPTY : new ItemStack(TkGunServer.BOLT);
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		Entity hitEntity = result.getEntity();
		if (CombatTargeting.isGolem(hitEntity)) {
			return;
		}
		float totalHealthBefore = hitEntity instanceof LivingEntity living
				? living.getHealth() + living.getAbsorptionAmount() : Float.NaN;
		int hurtTimeBefore = hitEntity instanceof LivingEntity living
				? living.hurtTime : -1;
		super.onHitEntity(result);
		if (hitEntity instanceof LivingEntity living
				&& level() instanceof ServerLevel world
				&& (living.getHealth() + living.getAbsorptionAmount() < totalHealthBefore
				|| living.hurtTime > hurtTimeBefore || !living.isAlive())) {
			Vec3 impact = result.getLocation();
			world.sendParticles(com.mechaniciancombat.TkGunParticles.GRAPESHOT_IMPACT,
					impact.x, impact.y, impact.z, 1,
					0.0D, 0.0D, 0.0D, 0.0D);
		}
		// 自定义实体类型不满足原版引雷的 direct_attacker=minecraft:trident 条件，这里补上。
		tkgun$applyChanneling(result.getEntity(), null, getWeaponItem());
	}

	private void tkgun$applyChanneling(Entity target, BlockHitResult blockHit, ItemStack weaponStack) {
		if (!(level() instanceof ServerLevel serverWorld) || !serverWorld.isThundering()) {
			return;
		}
		if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.CHANNELING, weaponStack) <= 0) {
			return;
		}
		BlockPos pos;
		if (target != null) {
			pos = target.blockPosition();
		} else if (blockHit != null
				&& serverWorld.getBlockState(blockHit.getBlockPos()).is(Blocks.LIGHTNING_ROD)) {
			pos = blockHit.getBlockPos();
		} else {
			return;
		}
		if (!serverWorld.canSeeSky(pos)) {
			return;
		}
		LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverWorld);
		if (lightning != null) {
			lightning.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
					0.0F, 0.0F);
			serverWorld.addFreshEntity(lightning);
		}
		serverWorld.playSound(null, pos, SoundEvents.TRIDENT_THUNDER,
				SoundSource.PLAYERS, 5.0F, 1.0F);
	}
}
