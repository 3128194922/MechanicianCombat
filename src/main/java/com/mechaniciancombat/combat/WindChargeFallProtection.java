package com.mechaniciancombat.combat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;

/** Server-persisted fall-height tracking for living entities launched by a wind burst. */
public final class WindChargeFallProtection {
    private static final String ROOT = "tkgunWindChargeFall";
    private static final String SAFE_HEIGHT = "safeHeight";
    private static final String EXPIRES_AT = "expiresAt";
    private static final String HAS_LEFT_GROUND = "hasLeftGround";
    private static final long MAX_DURATION_TICKS = 600L;

    private WindChargeFallProtection() {
    }

    public static void mark(LivingEntity entity, double burstHeight) {
        if (entity.level().isClientSide || !Double.isFinite(burstHeight)) {
            return;
        }
        CompoundTag data = entity.getPersistentData().getCompound(ROOT);
        data.putDouble(SAFE_HEIGHT, burstHeight);
        data.putLong(EXPIRES_AT, entity.level().getGameTime() + MAX_DURATION_TICKS);
        data.putBoolean(HAS_LEFT_GROUND, false);
        entity.getPersistentData().put(ROOT, data);
    }

    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(ROOT, CompoundTag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag data = persistent.getCompound(ROOT);
        if (isExpired(entity, data)) {
            persistent.remove(ROOT);
            return;
        }
        if (entity.getY() > data.getDouble(SAFE_HEIGHT) + 0.15D) {
            data.putBoolean(HAS_LEFT_GROUND, true);
        } else if (data.getBoolean(HAS_LEFT_GROUND) && entity.onGround()) {
            persistent.remove(ROOT);
        }
    }

    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(ROOT, CompoundTag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag data = persistent.getCompound(ROOT);
        if (!isExpired(entity, data)) {
            double safeHeight = data.getDouble(SAFE_HEIGHT);
            double currentY = entity.getY();
            double fallStartY = currentY + event.getDistance();
            if (currentY < safeHeight && fallStartY > safeHeight) {
                event.setDistance((float) Math.max(0.0D, currentY + event.getDistance() - safeHeight));
            }
        }
        persistent.remove(ROOT);
    }

    private static boolean isExpired(LivingEntity entity, CompoundTag data) {
        return entity.level().getGameTime() > data.getLong(EXPIRES_AT);
    }
}
