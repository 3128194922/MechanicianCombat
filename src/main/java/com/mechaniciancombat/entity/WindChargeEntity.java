package com.mechaniciancombat.entity;

import com.mechaniciancombat.TkGunServer;
import com.mechaniciancombat.combat.WindChargeBlockInteractions;
import com.mechaniciancombat.combat.WindChargeFallProtection;
import com.mechaniciancombat.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Server-authoritative, non-block-damaging wind projectile. */
public final class WindChargeEntity extends ThrowableItemProjectile {
    private static final double BLAST_RADIUS = 4.0D;
    private static final double KNOCKBACK = 1.2D;
    private boolean burst;

    public WindChargeEntity(EntityType<? extends WindChargeEntity> type, Level level) {
        super(type, level);
    }

    public WindChargeEntity(Level level, LivingEntity owner) {
        this(ModRegistries.WIND_CHARGE_ENTITY.get(), level);
        setOwner(owner);
    }

    @Override
    protected Item getDefaultItem() {
        return TkGunServer.WIND_CHARGE;
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide && !burst) {
            burst = true;
            burst(result.getLocation());
            discard();
        }
    }

    @Override
    protected void onHitEntity(net.minecraft.world.phys.EntityHitResult result) {
        Entity target = result.getEntity();
        if (target != getOwner()) {
            target.hurt(damageSources().thrown(this, getOwner()), 1.0F);
        }
        super.onHitEntity(result);
    }

    private void burst(Vec3 center) {
        WindChargeBlockInteractions.apply(level(), center, this);
        AABB bounds = new AABB(center, center).inflate(BLAST_RADIUS);
        for (Entity target : level().getEntities(this, bounds,
                candidate -> candidate.isAlive() && (candidate.isPushable() || candidate instanceof Projectile))) {
            Vec3 delta = target.position().subtract(center);
            double distance = delta.length();
            if (distance > BLAST_RADIUS) {
                continue;
            }
            double seen = Explosion.getSeenPercent(center, target);
            double strength = (1.0D - distance / BLAST_RADIUS) * seen * KNOCKBACK;
            if (strength <= 0.0D) {
                continue;
            }
            Vec3 direction = distance < 1.0E-5D ? new Vec3(0.0D, 1.0D, 0.0D) : delta.scale(1.0D / distance);
            if (target instanceof Projectile projectile) {
                projectile.setDeltaMovement(projectile.getDeltaMovement().add(
                        direction.x * strength, direction.y * strength, direction.z * strength));
            } else {
                target.push(direction.x * strength, direction.y * strength, direction.z * strength);
            }
            target.hurtMarked = true;
            if (target instanceof LivingEntity living) {
                living.fallDistance = 0.0F;
                WindChargeFallProtection.mark(living, center.y);
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
