package com.mechaniciancombat.entity;

import com.mechaniciancombat.CombatTargeting;
import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.TkGunServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

/**
 * 熔铁弩矢的**弹丸实体**（tkgun:bolt_projectile）。
 *
 * <p>⚠️ 2026-10-12 用户反复强调："**不要用弓箭当弹药**"。之前偷懒直接复用 {@code minecraft:arrow}，
 * 结果实体类型、死亡信息、掉落物全是"箭"。现在换成我们自己的类型：
 * <ul>
 *   <li>实体 = {@code tkgun:bolt_projectile}（用 {@code /data get entity @e[type=minecraft:arrow]} 查不到）；</li>
 *   <li>命中伤害走**自己的 damage_type**（{@code tkgun:bolt}）→ 死亡信息是"被熔铁弩矢打穿"，不再出现"箭"；</li>
 *   <li>客户端渲染器什么都不画（见 {@code client/entity/BoltProjectileRenderer}），飞行表现全靠曳光粒子。</li>
 * </ul>
 * 伤害口径与原版箭一致：**速度 × damage 字段**（{@link PersistentProjectileEntity#getDamage()}）。
 */
public class BoltProjectileEntity extends AbstractArrow {
	public BoltProjectileEntity(EntityType<? extends BoltProjectileEntity> type, Level world) {
		super(type, world);
	}

	public BoltProjectileEntity(EntityType<? extends BoltProjectileEntity> type, LivingEntity owner, Level world,
								ItemStack stack, ItemStack weapon) {
		 super(type, owner, world);
	}

	@Override
	protected ItemStack getPickupItem() {
		return new ItemStack(TkGunServer.BOLT);
	}

	/** 弩矢自己的伤害类型（数据在 data/tkgun/damage_type/bolt.json，文案在 lang 里）。 */
	public static ResourceKey<DamageType> damageTypeKey() {
		return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(TkGun.MOD_ID, "bolt"));
	}

	/** DamageSources.create(...) 是 private，所以直接 new DamageSource(entry, 弹丸, 射手)。 */
	private DamageSource boltDamageSource() {
		Entity owner = getOwner();
		Holder<DamageType> entry = level().registryAccess()
				.registryOrThrow(Registries.DAMAGE_TYPE).getHolder(damageTypeKey()).orElse(null);
		if (entry == null) {
			return damageSources().arrow(this, owner);   // 数据没加载出来时的兜底
		}
		return new DamageSource(entry, this, owner == null ? this : owner);
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		Entity target = result.getEntity();
		if (CombatTargeting.isGolem(target)) {
			return;
		}
		DamageSource source = boltDamageSource();
		// 口径照抄原版箭：伤害 = 速度 × damage 字段（我们生成弹丸时已经折算过）
		double speed = getDeltaMovement().length();
		int amount = Mth.ceil(Mth.clamp(speed * getBaseDamage(), 0.0D, Integer.MAX_VALUE));
		if (target instanceof LivingEntity living) {
			if (getKnockback() > 0) {
				Vec3 direction = getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).normalize()
						.scale(getKnockback() * 0.6D);
				if (direction.lengthSqr() > 0.0D) {
					living.push(direction.x, 0.1D, direction.z);
				}
			}
		}
		target.hurt(source, (float) amount);
		// 子弹打进目标就没了（不做穿透），下一 tick 由服务端的曳光表补命中爆裂粒子
		this.remove(RemovalReason.KILLED);
	}
}
