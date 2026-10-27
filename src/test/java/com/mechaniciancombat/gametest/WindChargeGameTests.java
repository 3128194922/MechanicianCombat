package com.mechaniciancombat.gametest;

import com.mojang.authlib.GameProfile;
import com.mechaniciancombat.combat.WindChargeFallProtection;
import com.mechaniciancombat.entity.WindChargeEntity;
import com.mechaniciancombat.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.UUID;

/** Server-world contract checks for migrated projectile item behavior. */
@GameTestHolder("tkgun")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class WindChargeGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void handUseConsumesOneAndSpawnsProjectile(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 1));
        FakePlayer player = FakePlayerFactory.get(level,
                new GameProfile(UUID.randomUUID(), "tkgun_gametest"));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.getAbilities().instabuild = false;

        ItemStack stack = new ItemStack(ModRegistries.WIND_CHARGE.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        AABB searchArea = new AABB(origin).inflate(8.0D);
        int projectilesBefore = level.getEntitiesOfClass(WindChargeEntity.class, searchArea).size();

        var result = ModRegistries.WIND_CHARGE.get().use(level, player, InteractionHand.MAIN_HAND);
        if (!result.getResult().consumesAction()) {
            helper.fail("Hand use must be accepted on the server");
            return;
        }
        if (stack.getCount() != 1) {
            helper.fail("Survival hand use must consume exactly one wind charge");
            return;
        }

        helper.runAfterDelay(1L, () -> {
            int projectilesAfter = level.getEntitiesOfClass(WindChargeEntity.class, searchArea).size();
            if (projectilesAfter != projectilesBefore + 1) {
                helper.fail("Hand use must spawn exactly one server-side wind charge projectile");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void failedProjectileSpawnDoesNotConsumeCharge(GameTestHelper helper) {
        var level = helper.getLevel();
        FakePlayer player = FakePlayerFactory.get(level,
                new GameProfile(UUID.randomUUID(), "tkgun_failed_spawn"));
        ItemStack stack = new ItemStack(ModRegistries.WIND_CHARGE.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        RejectWindChargeJoin rejector = new RejectWindChargeJoin();
        MinecraftForge.EVENT_BUS.register(rejector);
        try {
            ModRegistries.WIND_CHARGE.get().use(level, player, InteractionHand.MAIN_HAND);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(rejector);
        }
        if (stack.getCount() != 2) {
            helper.fail("Failed projectile insertion must preserve all wind charges");
            return;
        }
        if (player.getCooldowns().isOnCooldown(ModRegistries.WIND_CHARGE.get())) {
            helper.fail("Failed projectile insertion must not apply use cooldown");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void windBurstActivatesStoneButton(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos support = origin.offset(0, 1, 2);
        BlockPos button = support.north();
        level.setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(button, Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));

        WindChargeEntity projectile = new WindChargeEntity(level, null);
        ((Entity) projectile).setPos(origin.getX() + 0.5D, origin.getY() + 1.5D, origin.getZ() + 0.5D);
        ((Entity) projectile).setDeltaMovement(0.0D, 0.0D, 1.0D);
        if (!level.addFreshEntity(projectile)) {
            helper.fail("Wind charge projectile must enter the test world");
            return;
        }

        helper.runAfterDelay(5L, () -> {
            if (!level.getBlockState(button).getValue(ButtonBlock.POWERED)) {
                helper.fail("Wind burst must activate a stone button");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void windBurstProtectsFallBelowBurstHeight(GameTestHelper helper) {
        var entity = net.minecraft.world.entity.EntityType.PIG.create(helper.getLevel());
        if (entity == null) {
            helper.fail("Could not create a living entity for fall protection check");
            return;
        }
        entity.setPos(1.5D, 0.0D, 1.5D);
        WindChargeFallProtection.mark(entity, 5.0D);
        LivingFallEvent fall = new LivingFallEvent(entity, 10.0F, 1.0F);
        MinecraftForge.EVENT_BUS.post(fall);
        if (fall.getDistance() >= 10.0F) {
            helper.fail("Fall damage distance must exclude the portion above the wind burst height");
            return;
        }
        LivingFallEvent laterFall = new LivingFallEvent(entity, 10.0F, 1.0F);
        MinecraftForge.EVENT_BUS.post(laterFall);
        if (laterFall.getDistance() != 10.0F) {
            helper.fail("Wind burst fall protection must be consumed after one landing event");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void windBurstDeflectsNearbySnowball(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos wall = origin.offset(0, 1, 4);
        level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());

        Snowball snowball = new Snowball(level, origin.getX() + 2.5D, origin.getY() + 1.5D,
                origin.getZ() + 3.5D);
        snowball.setNoGravity(true);
        if (!level.addFreshEntity(snowball)) {
            helper.fail("Nearby snowball must enter the test world");
            return;
        }

        WindChargeEntity windCharge = new WindChargeEntity(level, null);
        ((Entity) windCharge).setPos(origin.getX() + 0.5D, origin.getY() + 1.5D, origin.getZ() + 0.5D);
        ((Entity) windCharge).setDeltaMovement(0.0D, 0.0D, 1.5D);
        if (!level.addFreshEntity(windCharge)) {
            helper.fail("Wind charge must enter the test world");
            return;
        }

        helper.runAfterDelay(5L, () -> {
            if (snowball.getDeltaMovement().lengthSqr() <= 0.01D) {
                helper.fail("Wind burst must deflect nearby projectiles");
                return;
            }
            helper.succeed();
        });
    }

    private WindChargeGameTests() {
    }

    private static final class RejectWindChargeJoin {
        @SubscribeEvent
        public void onEntityJoin(EntityJoinLevelEvent event) {
            if (event.getEntity() instanceof WindChargeEntity) {
                event.setCanceled(true);
            }
        }
    }
}
