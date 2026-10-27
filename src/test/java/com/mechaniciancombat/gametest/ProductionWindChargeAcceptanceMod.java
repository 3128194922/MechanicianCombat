package com.mechaniciancombat.gametest;

import com.mojang.authlib.GameProfile;
import com.mechaniciancombat.combat.WindChargeFallProtection;
import com.mechaniciancombat.entity.WindChargeEntity;
import com.mechaniciancombat.entity.MoltenTridentEntity;
import com.mechaniciancombat.item.BackpackUpgradeData;
import com.mechaniciancombat.item.GunAmmoData;
import com.mechaniciancombat.item.BackpackPartItem;
import com.mechaniciancombat.block.BackpackModificationTableBlockEntity;
import com.mechaniciancombat.registry.ModRegistries;
import com.mechaniciancombat.screen.AmmoStorageScreenHandler;
import com.mechaniciancombat.screen.BackpackModificationTableScreenHandler;
import com.mechaniciancombat.TkGunServer;
import com.mechaniciancombat.TkGun;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Runs a focused acceptance check in an official-mapped Forge server install. */
@Mod("tkguntests")
public final class ProductionWindChargeAcceptanceMod {
    private static MinecraftServer testServer;
    private static Pig target;
    private static float targetInitialHealth;
    private static double maxTargetVelocitySquared;
    private static Snowball deflectedProjectile;
    private static int elapsedTicks;
    private static boolean awaitingCollision;
    private static boolean buttonWasPowered;
    private static BlockPos testButtonPos;
    private static int testY;
    private static FakePlayer pendingHandLoyaltyOwner;
    private static ItemStack pendingHandLoyaltyBolt;
    private static MoltenTridentEntity pendingHandLoyaltyProjectile;
    private static net.minecraft.world.entity.monster.Zombie actualFireTarget;
    private static float actualFireTargetInitialHealth;

    public ProductionWindChargeAcceptanceMod() {
        if (Boolean.getBoolean("tkgun.gametest.only")) return;
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
    }

    private void onServerStarted(ServerStartedEvent event) {
        try {
            testServer = event.getServer();
            testY = testServer.overworld().getSharedSpawnPos().getY() + 32;
            verifyCuriosBackpackIntegration(testServer.overworld());
            verifyModificationTableTag();
            verifyLoyaltyEnchantCompatibility();
            verifyGunReloadTransaction(testServer.overworld());
            verifyGunTriggerGates(testServer.overworld());
            verifyHandThrownLoyaltyBolt(testServer.overworld());
            verifyGunAmmoServerImpacts(testServer.overworld());
            verifyMenuGuards(testServer.overworld());
            verifyPortableBackpackLauncher(testServer.overworld());
            verifyAutoBackpackLauncher(testServer.overworld());
            verifyLoyaltyBlockDisappearance(testServer.overworld());
            verifyBackpackLoyaltyPickup(testServer.overworld());
            verifyOrPreparePersistentItems(testServer.overworld());
            verifyFailedSpawnPreservesCharge(testServer.overworld());
            verifyFallProtection(testServer.overworld());
            runWindChargeUseAndCollisionCheck(testServer.overworld());
            runButtonInteractionCheck(testServer.overworld());
            runProjectileDeflectionCheck(testServer.overworld());
            awaitingCollision = true;
            System.out.println("[tkguntests] Loyalty API and table tag passed; hand use consumed one item and spawned one projectile; waiting for target hit/knockback and button interaction");
        } catch (Throwable failure) {
            finish("FAIL: " + failure.getClass().getName() + ": " + failure.getMessage(), failure);
        }
    }

    private static void verifyCuriosBackpackIntegration(ServerLevel level) {
        try {
            if (!CuriosApi.getPlayerSlots(level).containsKey("back")) {
                throw new IllegalStateException("Curios back slot is not assigned to players");
            }
            ItemStack backpack = new ItemStack(ModRegistries.COMBAT_BACKPACK.get());
            if (!CuriosApi.getItemStackSlots(backpack, level).containsKey("back")) {
                throw new IllegalStateException("Combat backpack is not assigned to Curios back slot");
            }
            FakePlayer player = FakePlayerFactory.get(level,
                    new GameProfile(UUID.fromString("4f03183a-1034-4ba9-b24b-14651b15f893"), "tkgun_curios_acceptance"));
            var inventory = CuriosApi.getCuriosInventory(player).resolve().orElseThrow(
                    () -> new IllegalStateException("Fake player has no Curios inventory"));
            inventory.getStacksHandler("back").orElseThrow(
                    () -> new IllegalStateException("Fake player has no Curios back handler"));
            SlotContext slot = new SlotContext("back", player, 0, false, true);
            if (!CuriosApi.isStackValid(slot, backpack)) {
                throw new IllegalStateException("Combat backpack is rejected by the Curios back slot");
            }
            inventory.setEquippedCurio("back", 0, backpack);
            if (com.mechaniciancombat.item.CombatBackpackItem.equippedStack(player).getItem()
                    != ModRegistries.COMBAT_BACKPACK.get()) {
                throw new IllegalStateException("Combat backpack lookup did not find the equipped Curios item");
            }
            writeCuriosResult("PASS: Curios loaded the player back slot; the combat backpack tag resolves to back; the slot accepts the item; equippedStack finds it through the Curios inventory");
        } catch (RuntimeException | Error failure) {
            writeCuriosResult("FAIL: " + failure.getClass().getName() + ": " + failure.getMessage());
            throw failure;
        }
    }

    private static void verifyAutoBackpackLauncher(ServerLevel level) {
        FakePlayer owner = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("8b4cc51e-76b3-4ce5-b8db-75b7d967224d"),
                        "tkgun_auto_backpack_acceptance"));
        BlockPos origin = level.getSharedSpawnPos().offset(80, 32, 80);
        level.getChunkAt(origin);
        double x = origin.getX() + 0.5D;
        double y = origin.getY();
        double z = origin.getZ() + 0.5D;
        owner.moveTo(x, y, z, 0.0F, 0.0F);
        Pig target = EntityType.PIG.create(level);
        if (target == null) {
            throw new AssertionError("could not create auto-backpack target");
        }
        target.moveTo(x, y, z + 3.5D, 0.0F, 0.0F);
        if (!level.addFreshEntity(target)) {
            throw new AssertionError("could not spawn auto-backpack target");
        }
        Pig nextTarget = EntityType.PIG.create(level);
        if (nextTarget == null) {
            target.discard();
            throw new AssertionError("could not create auto-backpack target-switch candidate");
        }
        nextTarget.moveTo(x + 2.0D, y, z + 6.0D, 0.0F, 0.0F);
        if (!level.addFreshEntity(nextTarget)) {
            target.discard();
            nextTarget.discard();
            throw new AssertionError("could not spawn auto-backpack target-switch candidate");
        }

        ItemStack backpack = new ItemStack(ModRegistries.COMBAT_BACKPACK.get());
        BackpackUpgradeData.installCore(backpack);
        BackpackUpgradeData.setLauncherType(backpack, BackpackUpgradeData.LauncherType.AUTO);
        var curios = CuriosApi.getCuriosInventory(owner).resolve().orElseThrow(
                () -> new IllegalStateException("auto-launcher FakePlayer has no Curios inventory"));
        curios.setEquippedCurio("back", 0, backpack);
        owner.getInventory().offhand.set(0, new ItemStack(Items.FIREWORK_ROCKET, 3));

        var payload = new com.mechaniciancombat.net.CombatBackpackFirePayload(
                x, y + owner.getEyeHeight() - 0.15D, z + 0.7D,
                x, y + owner.getEyeHeight(), z,
                0.0D, 0.0D, 1.0D, 1.0D);
        AABB search = new AABB(x, y, z, x, y, z).inflate(12.0D);
        BlockPos obstruction = null;
        try {
            if (!BackpackUpgradeData.canUseLauncher(backpack)
                    || BackpackUpgradeData.launcherType(backpack)
                    != BackpackUpgradeData.LauncherType.AUTO) {
                throw new AssertionError("Curios backpack did not expose its auto launcher");
            }
            invokeBackpackAction(owner, payload);
            List<FireworkRocketEntity> rockets = level.getEntitiesOfClass(
                    FireworkRocketEntity.class, search);
            Object state = backpackStateFor(owner.getUUID());
            if (rockets.size() != 1 || !TkGunServer.isBackpackProjectile(rockets.get(0))
                    || owner.getOffhandItem().getCount() != 2
                    || readBackpackChargeInt(state, "charges") != 9
                    || !readBackpackChargeBoolean(state, "autoActive")
                    || readBackpackChargeInt(state, "targetEntityId") != target.getId()) {
                throw new AssertionError("auto launcher did not acquire/fire once with one rocket and one charge: rockets="
                        + rockets.size() + ", offhand=" + owner.getOffhandItem()
                        + ", charges=" + readBackpackChargeInt(state, "charges")
                        + ", active=" + readBackpackChargeBoolean(state, "autoActive")
                        + ", target=" + readBackpackChargeInt(state, "targetEntityId"));
            }

            long firstAttackAt = readBackpackChargeLong(state, "nextAutoAttackAt");
            long currentTick = level.getGameTime();
            for (long tick = currentTick + 1; tick < firstAttackAt; tick++) {
                invokeAutoBackpackTick(owner, state, tick);
            }
            if (level.getEntitiesOfClass(FireworkRocketEntity.class, search).size() != 1
                    || owner.getOffhandItem().getCount() != 2
                    || readBackpackChargeInt(state, "charges") != 9) {
                throw new AssertionError("auto launcher fired again before its attack interval elapsed");
            }
            invokeAutoBackpackTick(owner, state, firstAttackAt);
            rockets = level.getEntitiesOfClass(FireworkRocketEntity.class, search);
            if (rockets.size() != 2 || !TkGunServer.isBackpackProjectile(rockets.get(1))
                    || owner.getOffhandItem().getCount() != 1
                    || readBackpackChargeInt(state, "charges") != 8
                    || !readBackpackChargeBoolean(state, "autoActive")
                    || readBackpackChargeInt(state, "targetEntityId") != nextTarget.getId()) {
                throw new AssertionError("auto launcher failed interval fire or target rotation: rockets="
                        + rockets.size() + ", offhand=" + owner.getOffhandItem()
                        + ", charges=" + readBackpackChargeInt(state, "charges")
                        + ", active=" + readBackpackChargeBoolean(state, "autoActive")
                        + ", target=" + readBackpackChargeInt(state, "targetEntityId")
                        + "/" + nextTarget.getId());
            }

            obstruction = BlockPos.containing(x + 1.0D, y + 1.0D, z + 3.0D);
            if (!level.getBlockState(obstruction).isAir()) {
                throw new AssertionError("auto-backpack occlusion fixture position is not empty: " + obstruction);
            }
            level.setBlockAndUpdate(obstruction, Blocks.STONE.defaultBlockState());
            long nextAttackAt = readBackpackChargeLong(state, "nextAutoAttackAt");
            invokeAutoBackpackTick(owner, state, nextAttackAt - 1L);
            if (level.getEntitiesOfClass(FireworkRocketEntity.class, search).size() != 2
                    || readBackpackChargeInt(state, "targetEntityId") != target.getId()
                    || owner.getOffhandItem().getCount() != 1
                    || readBackpackChargeInt(state, "charges") != 8) {
                throw new AssertionError("auto launcher did not drop the occluded target without firing early");
            }
            invokeAutoBackpackTick(owner, state, nextAttackAt);
            rockets = level.getEntitiesOfClass(FireworkRocketEntity.class, search);
            if (rockets.size() != 3 || !TkGunServer.isBackpackProjectile(rockets.get(2))
                    || readBackpackChargeInt(state, "targetEntityId") != target.getId()
                    || owner.getOffhandItem().getCount() != 0
                    || readBackpackChargeInt(state, "charges") != 7) {
                throw new AssertionError("auto launcher did not reacquire/fire at the unoccluded target on schedule");
            }

            invokeBackpackAction(owner, payload);
            if (readBackpackChargeBoolean(state, "autoActive")
                    || owner.getOffhandItem().getCount() != 0
                    || level.getEntitiesOfClass(FireworkRocketEntity.class, search).size() != 3) {
                throw new AssertionError("second auto-launcher action did not stop without duplicating a rocket");
            }
            System.out.println("[tkguntests] PASS: Curios auto backpack selected the nearest visible target, waited exactly one 20-tick interval, rotated after two hits, abandoned an occluded target, reacquired the clear target on schedule, charged exactly three rockets/charges, and stopped without a duplicate launch");
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not invoke auto-backpack server action", failure);
        } finally {
            for (FireworkRocketEntity rocket : level.getEntitiesOfClass(
                    FireworkRocketEntity.class, search)) {
                clearBackpackProjectile(rocket.getUUID());
                rocket.discard();
            }
            if (obstruction != null) {
                level.setBlockAndUpdate(obstruction, Blocks.AIR.defaultBlockState());
            }
            target.discard();
            nextTarget.discard();
            curios.setEquippedCurio("back", 0, ItemStack.EMPTY);
            removeBackpackState(owner.getUUID());
            owner.getInventory().offhand.set(0, ItemStack.EMPTY);
        }
    }

    private static void verifyPortableBackpackLauncher(ServerLevel level) {
        FakePlayer owner = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("e702988a-c466-4206-8e23-1ce4fb1d6867"),
                        "tkgun_portable_backpack_acceptance"));
        BlockPos origin = level.getSharedSpawnPos().offset(112, 32, 80);
        level.getChunkAt(origin);
        double x = origin.getX() + 0.5D;
        double y = origin.getY();
        double z = origin.getZ() + 0.5D;
        owner.moveTo(x, y, z, 0.0F, 0.0F);
        Pig target = EntityType.PIG.create(level);
        if (target == null) {
            throw new AssertionError("could not create portable-backpack target");
        }
        target.moveTo(x, y, z + 4.0D, 0.0F, 0.0F);
        if (!level.addFreshEntity(target)) {
            throw new AssertionError("could not spawn portable-backpack target");
        }

        ItemStack backpack = new ItemStack(ModRegistries.COMBAT_BACKPACK.get());
        BackpackUpgradeData.installCore(backpack);
        BackpackUpgradeData.setLauncherType(backpack, BackpackUpgradeData.LauncherType.PORTABLE);
        var curios = CuriosApi.getCuriosInventory(owner).resolve().orElseThrow(
                () -> new IllegalStateException("portable-launcher FakePlayer has no Curios inventory"));
        curios.setEquippedCurio("back", 0, backpack);
        owner.getInventory().offhand.set(0,
                new ItemStack(ModRegistries.WIND_CHARGE.get(), 3));

        var payload = new com.mechaniciancombat.net.CombatBackpackFirePayload(
                x, y + owner.getEyeHeight() - 0.15D, z + 0.7D,
                x, y + owner.getEyeHeight(), z,
                0.0D, 0.0D, 1.0D, 1.0D);
        AABB search = new AABB(x, y, z, x, y, z).inflate(12.0D);
        try {
            if (!BackpackUpgradeData.canUseLauncher(backpack)
                    || BackpackUpgradeData.launcherType(backpack)
                    != BackpackUpgradeData.LauncherType.PORTABLE) {
                throw new AssertionError("Curios backpack did not expose its portable launcher");
            }
            long chargeTick = level.getGameTime();
            Object state = invokeBackpackState(owner, chargeTick);
            if (readBackpackChargeInt(state, "charges") != 0) {
                throw new AssertionError("new portable backpack did not start with zero launcher charges");
            }
            while (readBackpackChargeInt(state, "charges") < 3) {
                long readyAt = readBackpackChargeLong(state, "nextChargeAt");
                if (readyAt <= chargeTick) {
                    throw new AssertionError("portable launcher has no future charge deadline: " + readyAt);
                }
                int chargesBefore = readBackpackChargeInt(state, "charges");
                for (long tick = chargeTick + 1L; tick < readyAt; tick++) {
                    invokePortableBackpackTick(owner, state, tick);
                }
                if (readBackpackChargeInt(state, "charges") != chargesBefore) {
                    throw new AssertionError("portable launcher charged before its 40-tick deadline");
                }
                invokePortableBackpackTick(owner, state, readyAt);
                if (readBackpackChargeInt(state, "charges") != chargesBefore + 1) {
                    throw new AssertionError("portable launcher missed its exact 40-tick charge deadline");
                }
                chargeTick = readyAt;
            }
            if (readBackpackChargeInt(state, "charges") != 3
                    || readBackpackChargeLong(state, "nextChargeAt") != 0L) {
                throw new AssertionError("portable launcher failed to reach exactly three naturally charged slots");
            }
            invokeBackpackAction(owner, payload);
            state = backpackStateFor(owner.getUUID());
            if (readBackpackChargeInt(state, "targetEntityId") != target.getId()
                    || readBackpackChargeInt(state, "charges") != 3
                    || owner.getOffhandItem().getCount() != 3
                    || !level.getEntitiesOfClass(WindChargeEntity.class, search).isEmpty()) {
                throw new AssertionError("portable launcher marking state mismatch: target="
                        + readBackpackChargeInt(state, "targetEntityId") + "/" + target.getId()
                        + ", charges=" + readBackpackChargeInt(state, "charges")
                        + ", offhand=" + owner.getOffhandItem()
                        + ", projectiles="
                        + level.getEntitiesOfClass(WindChargeEntity.class, search).size()
                        + ", launcher=" + BackpackUpgradeData.launcherType(
                                com.mechaniciancombat.item.CombatBackpackItem.equippedStack(owner))
                        + ", equipped=" + com.mechaniciancombat.item.CombatBackpackItem.equippedStack(owner)
                        + ", playerPos=" + owner.position() + ", targetPos=" + target.position()
                        + ", targetAlive=" + target.isAlive()
                        + ", nearbyLiving=" + level.getEntitiesOfClass(
                                net.minecraft.world.entity.LivingEntity.class,
                                owner.getBoundingBox().inflate(32.0D)).size());
            }

            for (int shots = 1; shots <= 3; shots++) {
                invokeBackpackAction(owner, payload);
                List<WindChargeEntity> charges = level.getEntitiesOfClass(WindChargeEntity.class, search);
                int remaining = 3 - shots;
                if (charges.size() != shots || !TkGunServer.isBackpackProjectile(charges.get(shots - 1))
                        || owner.getOffhandItem().getCount() != remaining
                        || readBackpackChargeInt(state, "charges") != remaining) {
                    throw new AssertionError("portable launcher shot did not settle one item and one charge: shots="
                            + charges.size() + ", offhand=" + owner.getOffhandItem()
                            + ", charges=" + readBackpackChargeInt(state, "charges"));
                }
            }
            long cooldownUntil = readBackpackChargeLong(state, "cooldownUntil");
            if (cooldownUntil <= level.getGameTime()
                    || readBackpackChargeLong(state, "nextChargeAt") != 0L
                    || readBackpackChargeInt(state, "targetEntityId") != -1) {
                throw new AssertionError("portable launcher did not enter a clean cooldown after its third shot");
            }
            invokePortableBackpackTick(owner, state, cooldownUntil - 1L);
            if (readBackpackChargeInt(state, "charges") != 0
                    || readBackpackChargeLong(state, "cooldownUntil") != cooldownUntil) {
                throw new AssertionError("portable launcher recovered before cooldown elapsed");
            }
            invokePortableBackpackTick(owner, state, cooldownUntil);
            if (readBackpackChargeInt(state, "charges") != 3
                    || readBackpackChargeLong(state, "cooldownUntil") != 0L
                    || readBackpackChargeLong(state, "nextChargeAt") != 0L) {
                throw new AssertionError("portable launcher did not restore full charge at cooldown deadline");
            }
            System.out.println("[tkguntests] PASS: Curios portable backpack naturally charged from zero to three at exact 40-tick intervals, marked a target without cost, fired three marked wind charges at one item/charge each, held zero charge until the 100-tick cooldown deadline, then restored full charge");
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not invoke portable-backpack server action", failure);
        } finally {
            for (WindChargeEntity projectile : level.getEntitiesOfClass(
                    WindChargeEntity.class, search)) {
                net.minecraft.world.entity.Entity entity = projectile;
                clearBackpackProjectile(entity.getUUID());
                entity.discard();
            }
            target.discard();
            curios.setEquippedCurio("back", 0, ItemStack.EMPTY);
            removeBackpackState(owner.getUUID());
            owner.getInventory().offhand.set(0, ItemStack.EMPTY);
        }
    }

    /** Exercises the production ammo impact branches on the official-mapped target server. */
    private static void verifyGunAmmoServerImpacts(ServerLevel level) throws ReflectiveOperationException {
        FakePlayer shooter = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("a74a7367-525d-4f35-b613-86cf8cfc089e"),
                        "tkgun_ammo_acceptance"));
        BlockPos origin = level.getSharedSpawnPos().offset(160, 32, 80);
        level.getChunkAt(origin);
        double x = origin.getX() + 0.5D;
        double y = origin.getY();
        double z = origin.getZ() + 0.5D;
        shooter.moveTo(x, y, z, 0.0F, 0.0F);
        Vec3 forward = new Vec3(0.0D, 0.0D, 1.0D);
        List<net.minecraft.world.entity.Entity> spawned = new java.util.ArrayList<>();
        try {
            net.minecraft.world.entity.monster.Zombie normal = acceptanceZombie(level, x, y, z + 8.0D);
            spawned.add(normal);
            float normalBefore = normal.getHealth();
            invokeBulletImpact(level, shooter, normal, normal.position(), true,
                    false, false, false, false, 1.0F, forward);
            if (!(normal.getHealth() < normalBefore)) {
                throw new AssertionError("standard bolt hitscan did not damage its direct target");
            }

            net.minecraft.world.entity.monster.Zombie explosiveDirect = acceptanceZombie(level, x, y, z + 16.0D);
            spawned.add(explosiveDirect);
            net.minecraft.world.entity.monster.Zombie explosiveSplash = acceptanceZombie(level, x + 1.0D, y, z + 16.0D);
            spawned.add(explosiveSplash);
            float explosiveDirectBefore = explosiveDirect.getHealth();
            float explosiveSplashBefore = explosiveSplash.getHealth();
            invokeBulletImpact(level, shooter, explosiveDirect, explosiveDirect.position(), true,
                    true, false, false, false, 1.0F, forward);
            if (!(explosiveDirect.getHealth() < explosiveDirectBefore)
                    || !(explosiveSplash.getHealth() < explosiveSplashBefore)) {
                throw new AssertionError("explosive bolt did not damage both direct and splash targets");
            }

            net.minecraft.world.entity.monster.Zombie cuttingDirect = acceptanceZombie(level, x, y, z + 24.0D);
            spawned.add(cuttingDirect);
            net.minecraft.world.entity.monster.Zombie cuttingSplash = acceptanceZombie(level, x + 1.0D, y, z + 24.0D);
            spawned.add(cuttingSplash);
            float cuttingDirectBefore = cuttingDirect.getHealth();
            float cuttingSplashBefore = cuttingSplash.getHealth();
            invokeBulletImpact(level, shooter, cuttingDirect, cuttingDirect.position(), true,
                    false, true, false, false, 1.0F, forward);
            if (!(cuttingDirect.getHealth() < cuttingDirectBefore)
                    || !(cuttingSplash.getHealth() < cuttingSplashBefore)) {
                throw new AssertionError("cutting bolt did not damage the direct and area targets");
            }

            net.minecraft.world.entity.monster.Zombie sculkDirect = acceptanceZombie(level, x, y, z + 32.0D);
            spawned.add(sculkDirect);
            net.minecraft.world.entity.monster.Zombie sculkNearby = acceptanceZombie(level, x + 1.0D, y, z + 32.0D);
            spawned.add(sculkNearby);
            float sculkDirectBefore = sculkDirect.getHealth();
            float sculkNearbyBefore = sculkNearby.getHealth();
            invokeBulletImpact(level, shooter, sculkDirect, sculkDirect.position(), true,
                    false, false, true, false, 1.0F, forward);
            if (sculkDirectBefore - sculkDirect.getHealth() < 7.1F
                    || sculkNearbyBefore - sculkNearby.getHealth() < 7.1F) {
                throw new AssertionError("sculk bolt did not deliver the armor-bypassing direct and sonic-burst damage");
            }

            net.minecraft.world.entity.monster.Zombie roseQuartzDirect = acceptanceZombie(level, x, y, z + 40.0D);
            spawned.add(roseQuartzDirect);
            net.minecraft.world.entity.monster.Zombie roseQuartzNearby = acceptanceZombie(level, x + 1.0D, y, z + 40.0D);
            spawned.add(roseQuartzNearby);
            float roseDirectBefore = roseQuartzDirect.getHealth();
            float roseNearbyBefore = roseQuartzNearby.getHealth();
            invokeBulletImpact(level, shooter, roseQuartzDirect, roseQuartzDirect.position(), true,
                    false, false, false, true, 1.0F, forward);
            float roseDirectDamage = roseDirectBefore - roseQuartzDirect.getHealth();
            if (roseDirectDamage < 4.5F || roseDirectDamage > 5.01F
                    || roseQuartzNearby.getHealth() != roseNearbyBefore) {
                throw new AssertionError("rose quartz bolt did not apply only its five-point direct hit: direct="
                        + roseDirectBefore + "->" + roseQuartzDirect.getHealth()
                        + ", nearby=" + roseNearbyBefore + "->" + roseQuartzNearby.getHealth());
            }

            net.minecraft.world.entity.monster.Zombie grapeshotTarget =
                    acceptanceZombie(level, x + 2.0D, y, z + 8.0D);
            spawned.add(grapeshotTarget);
            float grapeshotBefore = grapeshotTarget.getHealth();
            Method grapeshot = TkGunServer.class.getDeclaredMethod("spawnGrapeshot",
                    ServerLevel.class, net.minecraft.server.level.ServerPlayer.class,
                    Vec3.class, Vec3.class, float.class);
            grapeshot.setAccessible(true);
            grapeshot.invoke(null, level, shooter,
                    new Vec3(x, y + shooter.getEyeHeight() - 0.1D, z), forward, 1.0F);
            AABB pelletSearch = new AABB(x - 1.0D, y - 2.0D, z - 1.0D,
                    x + 1.0D, y + 4.0D, z + 1.0D).inflate(2.0D);
            List<com.mechaniciancombat.entity.BulletEntity> pellets = level.getEntitiesOfClass(
                    com.mechaniciancombat.entity.BulletEntity.class, pelletSearch,
                    bullet -> bullet.bulletStyle() == com.mechaniciancombat.entity.BulletEntity.STYLE_GRAPESHOT);
            spawned.addAll(pellets);
            if (pellets.size() != com.mechaniciancombat.TkGun.GRAPESHOT_PELLETS
                    || !(grapeshotTarget.getHealth() < grapeshotBefore)) {
                throw new AssertionError("armor-piercing grapeshot did not deal cone damage and spawn all pellets: pellets="
                        + pellets.size() + ", targetHealth=" + grapeshotTarget.getHealth());
            }

            Method molten = TkGunServer.class.getDeclaredMethod("spawnMoltenTrident",
                    ServerLevel.class, net.minecraft.server.level.ServerPlayer.class,
                    Vec3.class, Vec3.class, ItemStack.class);
            molten.setAccessible(true);
            ItemStack loyaltyBolt = new ItemStack(ModRegistries.BOLT.get());
            EnchantmentHelper.setEnchantments(Map.of(Enchantments.LOYALTY, 3), loyaltyBolt);
            BlockPos gunWall = BlockPos.containing(x + 12.0D, y, z + 8.0D);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 3; dy++) {
                    level.setBlock(gunWall.offset(dx, dy, 0), Blocks.STONE.defaultBlockState(), 3);
                }
            }
            molten.invoke(null, level, shooter,
                    new Vec3(x + 12.0D, y + shooter.getEyeHeight(), z), forward, loyaltyBolt);
            AABB tridentSearch = new AABB(x + 11.0D, y, z, x + 13.0D, y + 4.0D, z + 2.0D);
            List<MoltenTridentEntity> tridents = level.getEntitiesOfClass(
                    MoltenTridentEntity.class, tridentSearch, MoltenTridentEntity::isGunFired);
            spawned.addAll(tridents);
            if (tridents.size() != 1
                    || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LOYALTY,
                            tridents.get(0).getWeaponItem()) != 3) {
                throw new AssertionError("gun-fired molten bolt did not preserve Loyalty III in its custom trident projectile");
            }
            MoltenTridentEntity gunTrident = tridents.get(0);
            boolean gunBoltReturned = false;
            int gunNaturalTicks = 0;
            for (int attempt = 0; attempt < 16 && !gunBoltReturned; attempt++) {
                if (attempt > 0) {
                    ItemStack retryBolt = loyaltyBolt.copyWithCount(1);
                    molten.invoke(null, level, shooter,
                            new Vec3(x + 12.0D, y + shooter.getEyeHeight(), z), forward, retryBolt);
                    List<MoltenTridentEntity> retries = level.getEntitiesOfClass(
                            MoltenTridentEntity.class, tridentSearch, MoltenTridentEntity::isGunFired);
                    if (retries.size() != 1) {
                        throw new AssertionError("gun Loyalty retry did not create exactly one projectile: count="
                                + retries.size());
                    }
                    gunTrident = retries.get(0);
                    spawned.add(gunTrident);
                }
                int ticks = runNaturalLoyaltyReturn(level, gunTrident, shooter, 256);
                if (ticks >= 0) {
                    gunNaturalTicks += ticks;
                    gunBoltReturned = true;
                } else if (!((net.minecraft.world.entity.Entity) gunTrident).isRemoved()) {
                    throw new AssertionError("gun Loyalty projectile did not naturally return or disappear after block impact");
                }
            }
            int returnedGunBolts = countItem(shooter, ModRegistries.BOLT.get());
            if (!gunBoltReturned || returnedGunBolts != 1
                    || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LOYALTY,
                            shooter.getInventory().items.stream()
                                    .filter(stack -> stack.is(ModRegistries.BOLT.get()))
                                    .findFirst().orElse(ItemStack.EMPTY)) != 3) {
                throw new AssertionError("gun-fired Loyalty bolt did not naturally return and give exactly one enchanted bolt");
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 3; dy++) {
                    level.setBlock(gunWall.offset(dx, dy, 0), Blocks.AIR.defaultBlockState(), 3);
                }
            }
            shooter.getInventory().clearContent();
            System.out.println("[tkguntests] PASS: production gun ammo branches damaged direct and splash targets for standard/explosive/cutting/sculk/rose-quartz rounds; sculk avoided double-hitting its direct target; rose quartz applied five base direct damage (4.92 effective after zombie armor) without splash; armor-piercing spawned all eight grapeshot pellets and damaged its cone target; gun-fired Loyalty III bolt hit a real wall, survived the 50% disappearance rule, naturally returned and was picked up once in "
                    + gunNaturalTicks + " projectile ticks");
            armActualFireOnceAcceptance(level);
        } finally {
            for (net.minecraft.world.entity.Entity entity : spawned) {
                entity.discard();
            }
        }
    }

    private static net.minecraft.world.entity.monster.Zombie acceptanceZombie(
            ServerLevel level, double x, double y, double z) {
        net.minecraft.world.entity.monster.Zombie target =
                EntityType.ZOMBIE.create(level);
        if (target == null) {
            throw new AssertionError("could not create gun-ammo acceptance target");
        }
        target.setNoAi(true);
        target.setSilent(true);
        target.moveTo(x, y, z, 0.0F, 0.0F);
        level.getChunkAt(BlockPos.containing(x, y, z));
        if (!level.addFreshEntity(target)) {
            throw new AssertionError("could not spawn gun-ammo acceptance target");
        }
        return target;
    }

    private static void invokeBulletImpact(ServerLevel level,
            net.minecraft.server.level.ServerPlayer shooter,
            net.minecraft.world.entity.Entity target, Vec3 hitPos, boolean hitSomething,
            boolean explosive, boolean cutting, boolean sculk, boolean roseQuartz,
            float damageMultiplier, Vec3 direction) throws ReflectiveOperationException {
        Method impact = TkGunServer.class.getDeclaredMethod("applyBulletImpact",
                ServerLevel.class, net.minecraft.server.level.ServerPlayer.class,
                net.minecraft.world.entity.Entity.class, Vec3.class, boolean.class,
                boolean.class, boolean.class, boolean.class, boolean.class, float.class, Vec3.class);
        impact.setAccessible(true);
        impact.invoke(null, level, shooter, target, hitPos, hitSomething,
                explosive, cutting, sculk, roseQuartz, damageMultiplier, direction);
    }

    private static void armActualFireOnceAcceptance(ServerLevel level) throws ReflectiveOperationException {
        FakePlayer shooter = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("64c94c7c-6da6-48e2-9db5-65a33055f15a"),
                        "tkgun_authoritative_fire_acceptance"));
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos origin = new BlockPos(spawn.getX() + 15, testY, spawn.getZ() + 16);
        level.getChunkAt(origin);
        double x = origin.getX() + 0.5D;
        double y = origin.getY();
        double z = origin.getZ() + 0.5D;
        shooter.moveTo(x, y, z, 180.0F, 0.0F);
        actualFireTarget = acceptanceZombie(level, x, y, z - 8.0D);
        actualFireTargetInitialHealth = actualFireTarget.getHealth();
        TkGunServer.PState state = new TkGunServer.PState();
        Method fireOnce = TkGunServer.class.getDeclaredMethod("fireOnce",
                net.minecraft.server.level.ServerPlayer.class, TkGunServer.PState.class,
                float.class, String.class, ItemStack.class);
        fireOnce.setAccessible(true);
        ItemStack ammunition = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                new net.minecraft.resources.ResourceLocation("tkgun", "explosive_bolt")));
        fireOnce.invoke(null, shooter, state, 1.0F, "tkgun:explosive_bolt", ammunition);
    }

    private static void invokeBackpackAction(FakePlayer owner,
            com.mechaniciancombat.net.CombatBackpackFirePayload payload)
            throws ReflectiveOperationException {
        Method action = TkGunServer.class.getDeclaredMethod("handleBackpackAction",
                net.minecraft.server.level.ServerPlayer.class,
                com.mechaniciancombat.net.CombatBackpackFirePayload.class);
        action.setAccessible(true);
        action.invoke(null, owner, payload);
    }

    private static void invokeAutoBackpackTick(FakePlayer owner, Object state, long now)
            throws ReflectiveOperationException {
        Method tick = TkGunServer.class.getDeclaredMethod("tickAutoBackpack",
                net.minecraft.server.level.ServerPlayer.class, state.getClass(), long.class);
        tick.setAccessible(true);
        tick.invoke(null, owner, state, now);
    }

    private static Object invokeBackpackState(FakePlayer owner, long now)
            throws ReflectiveOperationException {
        Method getState = TkGunServer.class.getDeclaredMethod("backpackState",
                net.minecraft.server.level.ServerPlayer.class, long.class);
        getState.setAccessible(true);
        return getState.invoke(null, owner, now);
    }

    private static void invokePortableBackpackTick(FakePlayer owner, Object state, long now)
            throws ReflectiveOperationException {
        Method tick = TkGunServer.class.getDeclaredMethod("tickPortableBackpack",
                net.minecraft.server.level.ServerPlayer.class, state.getClass(), long.class);
        tick.setAccessible(true);
        tick.invoke(null, owner, state, now);
    }

    private static Object backpackStateFor(UUID owner) throws ReflectiveOperationException {
        Field field = TkGunServer.class.getDeclaredField("BACKPACK_STATES");
        field.setAccessible(true);
        Object value = ((Map<?, ?>) field.get(null)).get(owner);
        if (value == null) {
            throw new AssertionError("auto-backpack server state was not created");
        }
        return value;
    }

    private static void removeBackpackState(UUID owner) {
        try {
            Field field = TkGunServer.class.getDeclaredField("BACKPACK_STATES");
            field.setAccessible(true);
            ((Map<?, ?>) field.get(null)).remove(owner);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not clear auto-backpack state", failure);
        }
    }

    private static void clearBackpackProjectile(UUID projectile) {
        try {
            Field field = TkGunServer.class.getDeclaredField("BACKPACK_PROJECTILES");
            field.setAccessible(true);
            ((Map<?, ?>) field.get(null)).remove(projectile);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not clear auto-backpack projectile marker", failure);
        }
    }

    private static void writeCuriosResult(String result) {
        String configuredPath = System.getProperty("tkguntests.curiosResultFile");
        if (configuredPath == null || configuredPath.isBlank()) {
            return;
        }
        try {
            Path path = Path.of(configuredPath);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, result, StandardCharsets.UTF_8);
        } catch (Exception failure) {
            System.err.println("[tkguntests] Could not write Curios result marker: " + failure);
        }
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (pendingHandLoyaltyOwner != null) {
            try {
                completeHandThrownLoyaltyBolt(testServer.overworld());
            } catch (Throwable failure) {
                finish("FAIL: " + failure.getClass().getName() + ": " + failure.getMessage(), failure);
                return;
            }
        }
        if (!awaitingCollision) return;

        elapsedTicks++;
        Vec3 velocity = target.getDeltaMovement();
        maxTargetVelocitySquared = Math.max(maxTargetVelocitySquared, velocity.lengthSqr());
        double projectileVelocitySquared = deflectedProjectile.getDeltaMovement().lengthSqr();
        buttonWasPowered |= testServer.overworld().getBlockState(testButtonPos).getValue(ButtonBlock.POWERED);
        if (target.getHealth() < targetInitialHealth && maxTargetVelocitySquared > 0.01D
                && buttonWasPowered && projectileVelocitySquared > 0.01D
                && actualFireTarget != null && actualFireTarget.getHealth() < actualFireTargetInitialHealth) {
            finish("PASS: failed spawn preserved both charges without cooldown; fall protection removed the burst-height segment once; survival hand use consumed one of two charges, spawned one projectile, hit and knocked back a pig; wind burst activated a stone button and deflected a snowball; the production fireOnce path hit its server-selected target and applied delayed explosive-bolt damage; max pig velocity squared="
                    + maxTargetVelocitySquared + ", snowball velocity squared=" + projectileVelocitySquared
                    + ", fireOnce target health=" + actualFireTargetInitialHealth + "->" + actualFireTarget.getHealth(), null);
        } else if (elapsedTicks >= 15) {
            finish("FAIL: wind target hit/knockback, button/projectile interaction, or authoritative fireOnce hit missing within " + elapsedTicks
                    + " server ticks; health=" + target.getHealth() + ", max velocity squared=" + maxTargetVelocitySquared
                    + ", snowball velocity squared=" + projectileVelocitySquared + ", button was powered=" + buttonWasPowered
                    + ", testY=" + testY + ", target=" + target.position() + ", snowball=" + deflectedProjectile.position()
                    + ", fireOnceTargetHealth=" + (actualFireTarget == null ? "missing" :
                    actualFireTargetInitialHealth + "->" + actualFireTarget.getHealth()), null);
        }
    }

    private static void verifyFailedSpawnPreservesCharge(ServerLevel level) {
        FakePlayer player = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("c3937147-8913-4a26-b9b2-76b54c28d1a8"), "tkgun_spawn_reject"));
        ItemStack stack = new ItemStack(ModRegistries.WIND_CHARGE.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        RejectWindChargeSpawn rejector = new RejectWindChargeSpawn();
        MinecraftForge.EVENT_BUS.register(rejector);
        net.minecraft.world.InteractionResultHolder<ItemStack> result;
        try {
            result = ModRegistries.WIND_CHARGE.get().use(level, player, InteractionHand.MAIN_HAND);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(rejector);
        }
        if (result.getResult().consumesAction() || stack.getCount() != 2
                || player.getCooldowns().isOnCooldown(ModRegistries.WIND_CHARGE.get())) {
            throw new AssertionError("cancelled projectile insertion consumed a charge, applied cooldown, or reported success");
        }
    }

    private static void verifyFallProtection(ServerLevel level) {
        Pig entity = EntityType.PIG.create(level);
        if (entity == null) {
            throw new AssertionError("could not create a living entity for fall protection check");
        }
        entity.setPos(1.5D, 0.0D, 1.5D);
        WindChargeFallProtection.mark(entity, 5.0D);
        LivingFallEvent fall = new LivingFallEvent(entity, 10.0F, 1.0F);
        MinecraftForge.EVENT_BUS.post(fall);
        if (fall.getDistance() != 5.0F) {
            throw new AssertionError("fall protection did not remove the segment above the burst height; distance="
                    + fall.getDistance());
        }
        LivingFallEvent secondFall = new LivingFallEvent(entity, 10.0F, 1.0F);
        MinecraftForge.EVENT_BUS.post(secondFall);
        if (secondFall.getDistance() != 10.0F) {
            throw new AssertionError("fall protection was not consumed after the first fall event");
        }
    }

    private static void runWindChargeUseAndCollisionCheck(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos origin = new BlockPos(spawn.getX() + 1, testY, spawn.getZ() + 1);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 9; z++) {
                level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y < 5; y++) {
                    level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }

        FakePlayer player = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("0f188b59-6745-4d7c-80d8-24d38b057b18"), "tkgun_acceptance"));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.getAbilities().instabuild = false;

        ItemStack stack = new ItemStack(ModRegistries.WIND_CHARGE.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        AABB searchArea = new AABB(origin).inflate(8.0D);
        int before = level.getEntitiesOfClass(WindChargeEntity.class, searchArea).size();

        target = EntityType.PIG.create(level);
        if (target == null) {
            throw new AssertionError("could not create a pig target");
        }
        target.setNoGravity(true);
        target.setPos(origin.getX() + 0.5D, origin.getY() + 1.0D, origin.getZ() + 4.5D);
        if (!level.addFreshEntity(target)) {
            throw new AssertionError("could not add the pig target to the test world");
        }
        targetInitialHealth = target.getHealth();

        var useResult = ModRegistries.WIND_CHARGE.get().use(level, player, InteractionHand.MAIN_HAND);
        if (!useResult.getResult().consumesAction()) {
            throw new AssertionError("survival hand use was not accepted");
        }
        if (stack.getCount() != 1) {
            throw new AssertionError("survival hand use did not consume exactly one item; count=" + stack.getCount());
        }
        int after = level.getEntitiesOfClass(WindChargeEntity.class, searchArea).size();
        if (after != before + 1) {
            throw new AssertionError("hand use did not add exactly one server projectile; before=" + before
                    + ", after=" + after);
        }
    }

    private static void verifyLoyaltyEnchantCompatibility() {
        ItemStack bolt = new ItemStack(ModRegistries.BOLT.get());
        var item = ModRegistries.BOLT.get();
        if (!item.canApplyAtEnchantingTable(bolt, Enchantments.LOYALTY)) {
            throw new AssertionError("molten bolt does not accept Loyalty at an enchanting table");
        }
        if (item.canApplyAtEnchantingTable(bolt, Enchantments.SHARPNESS)) {
            throw new AssertionError("molten bolt unexpectedly accepts Sharpness at an enchanting table");
        }

        ItemStack loyaltyBook = EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(Enchantments.LOYALTY, 3));
        ItemStack sharpnessBook = EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(Enchantments.SHARPNESS, 1));
        if (!item.isBookEnchantable(bolt, loyaltyBook)) {
            throw new AssertionError("molten bolt does not accept a Loyalty book in an anvil");
        }
        if (item.isBookEnchantable(bolt, sharpnessBook)) {
            throw new AssertionError("molten bolt unexpectedly accepts a Sharpness book in an anvil");
        }
    }

    private static void verifyLoyaltyBlockDisappearance(ServerLevel level) {
        final int samples = 256;
        int disappeared = 0;
        java.util.ArrayList<MoltenTridentEntity> projectiles = new java.util.ArrayList<>(samples);
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos blockPos = new BlockPos(spawn.getX() + 64, testY + 4, spawn.getZ());
        level.getChunkAt(blockPos);
        level.setBlockAndUpdate(blockPos, Blocks.STONE.defaultBlockState());
        FakePlayer owner = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("68c1f90e-352f-4497-aace-f4d6dad7439b"), "tkgun_loyalty_acceptance"));
        for (int i = 0; i < samples; i++) {
            ItemStack bolt = new ItemStack(ModRegistries.BOLT.get());
            EnchantmentHelper.setEnchantments(java.util.Map.of(Enchantments.LOYALTY, 3), bolt);
            MoltenTridentEntity projectile = new MoltenTridentEntity(
                    ModRegistries.MOLTEN_TRIDENT_ENTITY.get(), level, owner, bolt);
            if (!level.addFreshEntity(projectile)) {
                throw new AssertionError("could not add Loyalty block-hit sample " + i);
            }
            projectiles.add(projectile);
            if (projectile.getWeaponItem().getItem() != ModRegistries.BOLT.get()
                    || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LOYALTY,
                            projectile.getWeaponItem()) != 3) {
                throw new AssertionError("molten trident did not preserve its Loyalty III weapon stack");
            }
            invokeTridentBlockHit(projectile, new net.minecraft.world.phys.BlockHitResult(
                    Vec3.atCenterOf(blockPos), Direction.NORTH, blockPos, false));
            if (((net.minecraft.world.entity.Entity) projectile).isRemoved()) {
                disappeared++;
            } else if (!projectile.isStopped()) {
                throw new AssertionError("surviving block-hit trident was not stopped in place");
            }
        }
        for (MoltenTridentEntity projectile : projectiles) {
            ((net.minecraft.world.entity.Entity) projectile).discard();
        }
        if (disappeared < 86 || disappeared > 170) {
            throw new AssertionError("50% block-hit disappearance fell outside the broad statistical acceptance interval: "
                    + disappeared + "/" + samples);
        }
        System.out.println("[tkguntests] PASS: Loyalty III stack survives projectile creation; block-hit disappearance sampled "
                + disappeared + "/" + samples + " (expected near 50%); survivors stop in place");
    }

    private static void invokeTridentBlockHit(MoltenTridentEntity projectile,
            net.minecraft.world.phys.BlockHitResult hit) {
        for (Class<?> type = projectile.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getReturnType() == void.class
                        && Arrays.equals(method.getParameterTypes(),
                                new Class<?>[] {net.minecraft.world.phys.BlockHitResult.class})) {
                    try {
                        method.setAccessible(true);
                        method.invoke(projectile, hit);
                        return;
                    } catch (ReflectiveOperationException failure) {
                        throw new AssertionError("could not invoke mapped trident block-hit handler", failure);
                    }
                }
            }
        }
        throw new AssertionError("could not find mapped trident block-hit handler");
    }

    @SuppressWarnings("unchecked")
    private static void verifyBackpackLoyaltyPickup(ServerLevel level) {
        FakePlayer owner = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("d1450f3b-13c6-42e8-b557-3e56831ad02a"), "tkgun_backpack_loyalty"));
        ItemStack backpack = new ItemStack(ModRegistries.COMBAT_BACKPACK.get());
        BackpackUpgradeData.installCore(backpack);
        BackpackUpgradeData.installLauncher(backpack);
        var curios = CuriosApi.getCuriosInventory(owner).resolve().orElseThrow(
                () -> new IllegalStateException("loyalty owner has no Curios inventory"));
        curios.setEquippedCurio("back", 0, backpack);
        if (!com.mechaniciancombat.item.BackpackLauncherRules.hasActiveLauncher(owner)) {
            throw new AssertionError("equipped backpack core and launcher were not recognized");
        }
        java.util.Map<UUID, Object> chargeStates = backpackChargeStates();
        Object chargeState = newBackpackChargeState(level, 1, 0L);
        chargeStates.put(((net.minecraft.world.entity.Entity) owner).getUUID(), chargeState);

        ItemStack bolt = new ItemStack(ModRegistries.BOLT.get());
        EnchantmentHelper.setEnchantments(java.util.Map.of(Enchantments.LOYALTY, 3), bolt);
        java.lang.reflect.Field trackedField;
        try {
            trackedField = com.mechaniciancombat.TkGunServer.class.getDeclaredField("BACKPACK_PROJECTILES");
            trackedField.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not access the backpack-projectile provenance marker", failure);
        }

        owner.moveTo(level.getSharedSpawnPos().getX() + 260.5D,
                level.getSharedSpawnPos().getY() + 40.0D,
                level.getSharedSpawnPos().getZ() + 260.5D, 0.0F, 0.0F);
        level.getChunkAt(owner.blockPosition());
        BlockPos wall = owner.blockPosition().offset(0, 0, 8);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                level.setBlock(wall.offset(dx, dy, 0), Blocks.STONE.defaultBlockState(), 3);
            }
        }

        MoltenTridentEntity projectile = null;
        java.util.List<UUID> markedProjectiles = new java.util.ArrayList<>();
        int naturalReturnTicks = 0;
        boolean pickedUp = false;
        try {
            for (int attempt = 0; attempt < 16 && !pickedUp; attempt++) {
                projectile = new MoltenTridentEntity(
                        ModRegistries.MOLTEN_TRIDENT_ENTITY.get(), level, owner, bolt);
                ((net.minecraft.world.entity.Entity) projectile).setDeltaMovement(0.0D, 0.0D, 5.0D);
                if (!level.addFreshEntity(projectile)) {
                    throw new AssertionError("could not add backpack Loyalty projectile to the test world");
                }
                UUID projectileId = ((net.minecraft.world.entity.Entity) projectile).getUUID();
                markedProjectiles.add(projectileId);
                try {
                    ((java.util.Map<UUID, Long>) trackedField.get(null)).put(
                            projectileId, level.getGameTime() + 1200L);
                } catch (ReflectiveOperationException failure) {
                    throw new AssertionError("could not set the real backpack-projectile provenance marker", failure);
                }
                int ticks = runNaturalLoyaltyReturn(level, projectile, owner, 256);
                if (ticks >= 0) {
                    naturalReturnTicks += ticks;
                    pickedUp = true;
                } else if (!((net.minecraft.world.entity.Entity) projectile).isRemoved()) {
                    throw new AssertionError("backpack Loyalty projectile did not naturally return or disappear after block impact");
                }
            }
            ItemStack returned = owner.getInventory().getItem(
                    net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND);
            int loyalty = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LOYALTY, returned);
            int boltCount = 0;
            for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) {
                ItemStack inventoryStack = owner.getInventory().getItem(slot);
                if (inventoryStack.is(ModRegistries.BOLT.get())) {
                    boltCount += inventoryStack.getCount();
                }
            }
            if (!pickedUp || returned.getItem() != ModRegistries.BOLT.get()
                    || returned.getCount() != 1 || loyalty != 3 || boltCount != 1) {
                throw new AssertionError("backpack Loyalty return was not a single enchanted bolt in offhand: pickedUp="
                        + pickedUp + ", offhand=" + returned + ", Loyalty=" + loyalty + ", totalBolts=" + boltCount);
            }
            if (readBackpackChargeInt(chargeState, "charges") != 2) {
                throw new AssertionError("backpack Loyalty recovery did not restore exactly one missing charge");
            }
            writeBackpackChargeLong(chargeState, "cooldownUntil", level.getGameTime() + 100L);
            writeBackpackChargeInt(chargeState, "charges", 1);
            com.mechaniciancombat.TkGunServer.restoreBackpackChargeAfterLoyalty(owner);
            if (readBackpackChargeInt(chargeState, "charges") != 3
                    || readBackpackChargeLong(chargeState, "cooldownUntil") != 0L) {
                throw new AssertionError("backpack Loyalty recovery did not clear cooldown and restore full charges");
            }
            System.out.println("[tkguntests] PASS: Curios-equipped active backpack Loyalty projectile hit a real wall, survived the 50% disappearance rule, naturally returned and restored one charge exactly once in "
                    + naturalReturnTicks + " projectile ticks; non-cooldown recovery added one charge and cooldown recovery cleared cooldown/restored full charges");
        } finally {
            try {
                for (UUID projectileId : markedProjectiles) {
                    ((java.util.Map<UUID, Long>) trackedField.get(null)).remove(projectileId);
                }
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError("could not clear backpack projectile acceptance marker", failure);
            }
            if (projectile != null) {
                ((net.minecraft.world.entity.Entity) projectile).discard();
            }
            chargeStates.remove(((net.minecraft.world.entity.Entity) owner).getUUID());
            owner.getInventory().clearContent();
            curios.setEquippedCurio("back", 0, ItemStack.EMPTY);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 3; dy++) {
                    level.setBlock(wall.offset(dx, dy, 0), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static boolean invokeTridentPickup(MoltenTridentEntity projectile, FakePlayer owner) {
        for (Class<?> type = projectile.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getReturnType() == boolean.class
                        && Arrays.equals(method.getParameterTypes(),
                                new Class<?>[] {net.minecraft.world.entity.player.Player.class})) {
                    try {
                        method.setAccessible(true);
                        return (boolean) method.invoke(projectile, owner);
                    } catch (ReflectiveOperationException failure) {
                        throw new AssertionError("could not invoke mapped trident pickup path", failure);
                    }
                }
            }
        }
        throw new AssertionError("could not find mapped trident pickup path");
    }

    private static void invokeTridentPlayerTouch(MoltenTridentEntity projectile, FakePlayer owner) {
        for (Class<?> type = projectile.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getReturnType() != void.class
                        || !Arrays.equals(method.getParameterTypes(),
                                new Class<?>[] {net.minecraft.world.entity.player.Player.class})) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    method.invoke(projectile, owner);
                    return;
                } catch (ReflectiveOperationException failure) {
                    throw new AssertionError("could not invoke mapped trident player-touch pickup path", failure);
                }
            }
        }
        throw new AssertionError("could not find mapped trident player-touch pickup path");
    }

    /** Ticks the real projectile through block collision and Loyalty return, then uses normal owner touch. */
    private static int runNaturalLoyaltyReturn(ServerLevel level, MoltenTridentEntity projectile,
            FakePlayer owner, int maxTicks) {
        net.minecraft.world.entity.Entity entity = projectile;
        double previousDistance = entity.position().distanceTo(owner.getEyePosition());
        boolean observedReturn = false;
        for (int tick = 0; tick < maxTicks && !entity.isRemoved(); tick++) {
            // Invoke through the mapped Minecraft Entity API. Calling the mod
            // override by its concrete class name is not stable in the
            // reobfuscated GameTest jar (runtime linkage can look for the
            // development symbol MoltenTridentEntity.tick()).
            entity.tick();
            if (entity.isRemoved()) {
                return -1;
            }
            double distance = entity.position().distanceTo(owner.getEyePosition());
            if (projectile.isStopped() && distance + 0.01D < previousDistance) {
                observedReturn = true;
            }
            if (observedReturn && distance <= 2.25D) {
                invokeTridentPlayerTouch(projectile, owner);
                return entity.isRemoved() ? tick + 1 : -1;
            }
            previousDistance = distance;
        }
        return entity.isRemoved() ? -1 : -2;
    }

    private static void invokeSetNoPhysics(net.minecraft.world.entity.Entity entity, boolean value) {
        for (Class<?> type = entity.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getReturnType() != void.class
                        || !Arrays.equals(method.getParameterTypes(), new Class<?>[] {boolean.class})) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    method.invoke(entity, value);
                    return;
                } catch (ReflectiveOperationException failure) {
                    throw new AssertionError("could not invoke mapped no-physics setter", failure);
                }
            }
        }
        throw new AssertionError("could not find mapped no-physics setter");
    }

    private static void invokeSetPosition(net.minecraft.world.entity.Entity entity,
            double x, double y, double z) {
        Class<?>[] signature = new Class<?>[] {double.class, double.class, double.class};
        for (Class<?> type = entity.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getReturnType() != void.class
                        || !Arrays.equals(method.getParameterTypes(), signature)) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    method.invoke(entity, x, y, z);
                    return;
                } catch (ReflectiveOperationException failure) {
                    throw new AssertionError("could not invoke mapped entity position setter", failure);
                }
            }
        }
        throw new AssertionError("could not find mapped entity position setter");
    }

    private static void invokeReleaseUsing(ItemStack stack, ServerLevel level,
                                           net.minecraft.world.entity.LivingEntity user,
                                           int remainingUseTicks) {
        Class<?>[] signature = new Class<?>[] {
                ItemStack.class, net.minecraft.world.level.Level.class,
                net.minecraft.world.entity.LivingEntity.class, int.class};
        for (Class<?> type = ModRegistries.BOLT.get().getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getReturnType() != void.class
                        || !Arrays.equals(method.getParameterTypes(), signature)) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    method.invoke(ModRegistries.BOLT.get(), stack, level, user, remainingUseTicks);
                    return;
                } catch (ReflectiveOperationException failure) {
                    throw new AssertionError("could not invoke mapped molten-trident release behavior", failure);
                }
            }
        }
        throw new AssertionError("could not find mapped molten-trident release behavior by signature");
    }

    @SuppressWarnings("unchecked")
    private static java.util.Map<UUID, Object> backpackChargeStates() {
        try {
            java.lang.reflect.Field field = com.mechaniciancombat.TkGunServer.class
                    .getDeclaredField("BACKPACK_STATES");
            field.setAccessible(true);
            return (java.util.Map<UUID, Object>) field.get(null);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not access backpack state map for charge-recovery verification", failure);
        }
    }

    private static Object newBackpackChargeState(ServerLevel level, int charges, long cooldownUntil) {
        try {
            Class<?> type = Class.forName("com.mechaniciancombat.TkGunServer$BackpackCharge");
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object state = constructor.newInstance();
            writeBackpackChargeField(state, "launcherType",
                    com.mechaniciancombat.item.BackpackUpgradeData.LauncherType.PORTABLE);
            writeBackpackChargeInt(state, "charges", charges);
            writeBackpackChargeLong(state, "nextChargeAt", level.getGameTime() + 40L);
            writeBackpackChargeLong(state, "cooldownUntil", cooldownUntil);
            return state;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not create a controlled backpack charge state", failure);
        }
    }

    private static int readBackpackChargeInt(Object state, String name) {
        try {
            java.lang.reflect.Field field = state.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(state);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not read backpack charge field " + name, failure);
        }
    }

    private static boolean readBackpackChargeBoolean(Object state, String name) {
        try {
            java.lang.reflect.Field field = state.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.getBoolean(state);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not read backpack charge field " + name, failure);
        }
    }

    private static long readBackpackChargeLong(Object state, String name) {
        try {
            java.lang.reflect.Field field = state.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.getLong(state);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not read backpack charge field " + name, failure);
        }
    }

    private static void writeBackpackChargeInt(Object state, String name, int value) {
        writeBackpackChargeField(state, name, value);
    }

    private static void writeBackpackChargeLong(Object state, String name, long value) {
        writeBackpackChargeField(state, name, value);
    }

    private static void writeBackpackChargeField(Object state, String name, Object value) {
        try {
            java.lang.reflect.Field field = state.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(state, value);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not write backpack charge field " + name, failure);
        }
    }

    private static void verifyModificationTableTag() {
        var state = ModRegistries.BACKPACK_TABLE_BLOCK.get().defaultBlockState();
        if (!state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            throw new AssertionError("backpack modification table is absent from minecraft:mineable/pickaxe after datapack loading");
        }
    }

    private static void verifyGunReloadTransaction(ServerLevel level) {
        FakePlayer player = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("926f203a-b0e0-45a7-93e0-6e4e0f945d69"),
                        "tkgun_reload_acceptance"));
        ItemStack gun = new ItemStack(ModRegistries.BOWGUN.get());
        NonNullList<ItemStack> ammoRing = NonNullList.withSize(GunAmmoData.SIZE, ItemStack.EMPTY);
        ammoRing.set(0, new ItemStack(TkGunServer.BOLT, TkGun.MAG_CAPACITY_HIP));
        GunAmmoData.save(gun, ammoRing, 0, level.registryAccess());
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);

        TkGunServer.reload(player);
        TkGunServer.PState state = TkGunServer.state(player);
        int remainingBolts = GunAmmoData.read(gun, level.registryAccess()).stream()
                .filter(stack -> stack.getItem() == TkGunServer.BOLT)
                .mapToInt(ItemStack::getCount).sum();
        String boltId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(TkGunServer.BOLT).toString();
        if (remainingBolts != 0 || state.mag != 0 || !state.magAmmo.equals(boltId)
                || state.reloadUntil <= level.getGameTime()
                || GunAmmoData.savedMagazineCount(gun) != 0) {
            throw new AssertionError("production reload did not atomically transfer exactly six bolts into the hip magazine");
        }

        FakePlayer insufficient = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("a1e9b75e-9e8b-4af3-a3d7-06ad113c2c61"),
                        "tkgun_reload_insufficient_acceptance"));
        ItemStack shortGun = new ItemStack(ModRegistries.BOWGUN.get());
        NonNullList<ItemStack> shortRing = NonNullList.withSize(GunAmmoData.SIZE, ItemStack.EMPTY);
        shortRing.set(0, new ItemStack(TkGunServer.BOLT, TkGun.MAG_CAPACITY_HIP - 1));
        GunAmmoData.save(shortGun, shortRing, 0, level.registryAccess());
        insufficient.setItemInHand(InteractionHand.MAIN_HAND, shortGun);
        TkGunServer.reload(insufficient);
        TkGunServer.PState shortState = TkGunServer.state(insufficient);
        int shortRemaining = GunAmmoData.read(shortGun, level.registryAccess()).stream()
                .filter(stack -> stack.getItem() == TkGunServer.BOLT)
                .mapToInt(ItemStack::getCount).sum();
        if (shortRemaining != TkGun.MAG_CAPACITY_HIP - 1
                || shortState.reloadUntil > level.getGameTime()
                || !shortState.magAmmo.isEmpty()) {
            throw new AssertionError("insufficient reload consumed ammunition or started a partial magazine");
        }
        System.out.println("[tkguntests] PASS: production hip reload consumed exactly six bolts into magazine NBT; insufficient ammo preserved all five and did not begin reload");
    }

    private static void verifyGunTriggerGates(ServerLevel level) {
        FakePlayer player = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("ba4b44b4-5029-490b-9e9e-b3cb80e7f16a"),
                        "tkgun_trigger_gate_acceptance"));
        ItemStack gun = new ItemStack(ModRegistries.BOWGUN.get());
        NonNullList<ItemStack> ammoRing = NonNullList.withSize(GunAmmoData.SIZE, ItemStack.EMPTY);
        ammoRing.set(0, new ItemStack(TkGunServer.BOLT, 4));
        String boltId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(TkGunServer.BOLT).toString();
        GunAmmoData.save(gun, ammoRing, 0, level.registryAccess());
        GunAmmoData.saveMagazine(gun, 4, boltId);
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);

        TkGunServer.PState state = TkGunServer.state(player);
        long now = level.getGameTime();
        state.lastShotAt = now - 1L;
        TkGunServer.tryFire(player, false);
        if (!state.shotQueue.isEmpty() || state.mag != 4) {
            throw new AssertionError("server accepted a hip-fire trigger without the gun-use/aim state");
        }

        state.lastFireTick = now - 1L;
        state.lastShotAt = now - 1L;
        var use = ModRegistries.BOWGUN.get().use(level, player, InteractionHand.MAIN_HAND);
        if (!use.getObject().equals(gun) || !player.isUsingItem()) {
            throw new AssertionError("production bowgun use did not enter the server item-use/aim state");
        }
        TkGunServer.tryFire(player, false);
        if (state.shotQueue.size() != 1 || state.mag != 5
                || GunAmmoData.savedMagazineCount(gun) != 5) {
            throw new AssertionError("aimed trigger did not enqueue exactly one shot and persist its magazine count");
        }
        TkGunServer.tryFire(player, false);
        if (state.shotQueue.size() != 1 || state.mag != 5) {
            throw new AssertionError("same-tick duplicate trigger queued or charged a second shot");
        }

        state.shotQueue.clear();
        state.shotAnimating = false;
        state.overheatUntil = now + TkGun.OVERHEAT_TICKS;
        state.lastFireTick = now - 1L;
        TkGunServer.tryFire(player, false);
        if (!state.shotQueue.isEmpty() || state.mag != 5) {
            throw new AssertionError("overheated weapon accepted a trigger or changed magazine state");
        }
        player.stopUsingItem();
        System.out.println("[tkguntests] PASS: production trigger rejected un-aimed/overheated/duplicate input; aimed input queued one shot and persisted exactly one magazine increment");
    }

    private static void verifyHandThrownLoyaltyBolt(ServerLevel level) {
        FakePlayer owner = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("ea947aa5-b273-49c0-9e44-91f456b96d42"),
                        "tkgun_hand_loyalty_acceptance"));
        owner.moveTo(level.getSharedSpawnPos().getX() + 220.5D,
                level.getSharedSpawnPos().getY() + 40.0D,
                level.getSharedSpawnPos().getZ() + 220.5D, 0.0F, 0.0F);
        level.getChunkAt(owner.blockPosition());
        BlockPos wall = owner.blockPosition().offset(0, 0, 8);
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 3; y++) {
                level.setBlock(wall.offset(x, y, 0), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        ItemStack bolt = new ItemStack(ModRegistries.BOLT.get());
        EnchantmentHelper.setEnchantments(Map.of(Enchantments.LOYALTY, 3), bolt);
        owner.setItemInHand(InteractionHand.MAIN_HAND, bolt);
        var use = ModRegistries.BOLT.get().use(level, owner, InteractionHand.MAIN_HAND);
        if (!use.getObject().equals(bolt) || !owner.isUsingItem()) {
            throw new AssertionError("hand-thrown molten bolt did not enter item-use state");
        }
        CaptureHandLoyaltyProjectile capture = new CaptureHandLoyaltyProjectile();
        MinecraftForge.EVENT_BUS.register(capture);
        try {
            invokeReleaseUsing(bolt, level, owner,
                    ModRegistries.BOLT.get().getUseDuration(bolt) - 10);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(capture);
        }
        if (bolt.getCount() != 0) {
            throw new AssertionError("hand throw did not consume one bolt after successful entity insertion: remaining="
                    + bolt.getCount());
        }
        if (capture.projectile == null) {
            throw new AssertionError("hand throw consumed one bolt without a Loyalty III entity-join event");
        }
        pendingHandLoyaltyOwner = owner;
        pendingHandLoyaltyBolt = bolt;
        pendingHandLoyaltyProjectile = capture.projectile;
    }

    private static void completeHandThrownLoyaltyBolt(ServerLevel level) {
        FakePlayer owner = pendingHandLoyaltyOwner;
        ItemStack bolt = pendingHandLoyaltyBolt;
        MoltenTridentEntity projectile = pendingHandLoyaltyProjectile;
        pendingHandLoyaltyOwner = null;
        pendingHandLoyaltyBolt = null;
        pendingHandLoyaltyProjectile = null;
        if (projectile == null || EnchantmentHelper.getItemEnchantmentLevel(
                Enchantments.LOYALTY, projectile.getWeaponItem()) != 3) {
            throw new AssertionError("hand throw did not consume one bolt and create an enchanted custom trident: remaining="
                    + bolt.getCount() + ", capturedProjectile=" + projectile);
        }
        int throwsAttempted = 0;
        int naturalReturnTicks = 0;
        boolean returned = false;
        ItemStack loyaltyBoltTemplate = projectile.getWeaponItem().copyWithCount(1);
        for (int attempt = 0; attempt < 16 && !returned; attempt++) {
            if (attempt > 0) {
                ItemStack retryBolt = loyaltyBoltTemplate.copyWithCount(1);
                owner.setItemInHand(InteractionHand.MAIN_HAND, retryBolt);
                var retryUse = ModRegistries.BOLT.get().use(level, owner, InteractionHand.MAIN_HAND);
                if (!retryUse.getObject().equals(retryBolt) || !owner.isUsingItem()) {
                    throw new AssertionError("retry hand throw did not enter item-use state");
                }
                CaptureHandLoyaltyProjectile retryCapture = new CaptureHandLoyaltyProjectile();
                MinecraftForge.EVENT_BUS.register(retryCapture);
                try {
                    invokeReleaseUsing(retryBolt, level, owner,
                            ModRegistries.BOLT.get().getUseDuration(retryBolt) - 10);
                } finally {
                    MinecraftForge.EVENT_BUS.unregister(retryCapture);
                }
                if (retryBolt.getCount() != 0 || retryCapture.projectile == null) {
                    throw new AssertionError("retry hand throw did not consume one bolt and spawn one projectile");
                }
                projectile = retryCapture.projectile;
            }
            throwsAttempted++;
            double previousOwnerDistance = ((net.minecraft.world.entity.Entity) projectile)
                    .position().distanceTo(owner.getEyePosition());
            boolean observedReturnFlight = false;
            for (int tick = 0; tick < 256 && !((net.minecraft.world.entity.Entity) projectile).isRemoved(); tick++) {
                ((net.minecraft.world.entity.Entity) projectile).tick();
                naturalReturnTicks++;
                double ownerDistance = ((net.minecraft.world.entity.Entity) projectile)
                        .position().distanceTo(owner.getEyePosition());
                if (projectile.isStopped() && ownerDistance + 0.01D < previousOwnerDistance) {
                    observedReturnFlight = true;
                }
                if (observedReturnFlight && ownerDistance <= 2.25D) {
                    invokeTridentPlayerTouch(projectile, owner);
                    returned = ((net.minecraft.world.entity.Entity) projectile).isRemoved();
                    break;
                }
                previousOwnerDistance = ownerDistance;
            }
            if (!returned && !((net.minecraft.world.entity.Entity) projectile).isRemoved()) {
                throw new AssertionError("surviving hand-thrown Loyalty bolt did not naturally return within 256 projectile ticks");
            }
        }
        int returnedBolts = countItem(owner, ModRegistries.BOLT.get());
        ItemStack recovered = owner.getInventory().items.stream()
                .filter(stack -> stack.is(ModRegistries.BOLT.get()))
                .findFirst().orElse(ItemStack.EMPTY);
        if (!returned || returnedBolts != 1
                || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LOYALTY, recovered) != 3) {
            throw new AssertionError("hand-thrown Loyalty bolt did not return exactly once with its enchantment");
        }
        if (!((net.minecraft.world.entity.Entity) projectile).isRemoved()) {
            throw new AssertionError("successful vanilla pickup did not remove the returned projectile");
        }
        ((net.minecraft.world.entity.Entity) projectile).discard();
        owner.getInventory().clearContent();
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 3; y++) {
                level.setBlock(owner.blockPosition().offset(x, y, 8), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        System.out.println("[tkguntests] PASS: production hand-use consumed one Loyalty III bolt per throw; natural flight hit a wall, survived the 50% disappearance rule, returned by Loyalty, and owner pickup recovered one enchanted bolt after "
                + throwsAttempted + " throw(s)/" + naturalReturnTicks + " natural projectile ticks; pickup removed the projectile and left exactly one item");
    }

    private static int countItem(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void verifyMenuGuards(ServerLevel level) {
        FakePlayer player = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("8f1b78e4-6073-49b8-a4e9-4be6aec85fb3"), "tkgun_menu_acceptance"));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistries.BOWGUN.get()));
        AmmoStorageScreenHandler ammoMenu = new AmmoStorageScreenHandler(71, player.getInventory());
        if (!invokeQuickMove(ammoMenu, player, -1).isEmpty()
                || !invokeQuickMove(ammoMenu, player, menuSlotCount(ammoMenu)).isEmpty()) {
            throw new AssertionError("ammo menu accepted an out-of-range quick-move slot");
        }
        if (invokeMenuSlot(ammoMenu, 0).mayPlace(new ItemStack(Items.STONE))
                || !invokeMenuSlot(ammoMenu, 0).mayPlace(new ItemStack(ModRegistries.BOLT.get()))) {
            throw new AssertionError("ammo ring slot filter does not match ammunition items");
        }
        GunAmmoData.AmmoInventory directRing = GunAmmoData.inventory(player);
        invokeMappedSetItem(directRing, 0, new ItemStack(ModRegistries.BOLT.get(), 7));
        ItemStack directSaved = GunAmmoData.read(player.getMainHandItem(), level.registryAccess()).get(0);
        if (directSaved.getItem() != ModRegistries.BOLT.get() || directSaved.getCount() != 7) {
            throw new AssertionError("direct ammo-ring inventory write failed: " + directSaved);
        }
        invokeMappedSetItem(directRing, 0, ItemStack.EMPTY);
        boolean ringTransferAccepted = invokeMappedMoveItemStackTo(
                ammoMenu, new ItemStack(ModRegistries.BOLT.get()), 0, AmmoStorageScreenHandler.SIZE);
        ItemStack probeRing = invokeMenuSlot(ammoMenu, AmmoStorageScreenHandler.SIZE - 1).getItem();
        if (!ringTransferAccepted || probeRing.getItem() != ModRegistries.BOLT.get()
                || probeRing.getCount() != 1) {
            throw new AssertionError("menu ring transfer rejected a valid bolt: accepted="
                    + ringTransferAccepted + ", ring=" + probeRing);
        }
        player.getInventory().setItem(1, new ItemStack(ModRegistries.BOLT.get(), 7));
        ItemStack movedAmmo = invokeQuickMove(ammoMenu, player, AmmoStorageScreenHandler.SIZE + 1);
        ItemStack ringAmmo = invokeMenuSlot(ammoMenu, AmmoStorageScreenHandler.SIZE - 1).getItem();
        ItemStack inventoryAmmo = player.getInventory().getItem(1);
        NonNullList<ItemStack> savedAmmo = GunAmmoData.read(player.getMainHandItem(), level.registryAccess());
        if (!inventoryAmmo.isEmpty()
                || ringAmmo.getItem() != ModRegistries.BOLT.get()
                || ringAmmo.getCount() != 8
                || savedAmmo.get(AmmoStorageScreenHandler.SIZE - 1).getCount() != 8) {
            throw new AssertionError("shift-click ammo transfer invalid: moved=" + movedAmmo
                    + ", inventory=" + inventoryAmmo + ", ring=" + ringAmmo
                    + ", savedRing=" + savedAmmo.get(AmmoStorageScreenHandler.SIZE - 1)
                    + ", heldGun=" + player.getMainHandItem());
        }

        BlockPos tablePos = new BlockPos(level.getSharedSpawnPos().getX() + 42,
                testY, level.getSharedSpawnPos().getZ());
        level.getChunkAt(tablePos);
        if (!level.getBlockState(tablePos).isAir()) {
            throw new AssertionError("menu validation fixture position is not empty");
        }
        level.setBlockAndUpdate(tablePos, ModRegistries.BACKPACK_TABLE_BLOCK.get().defaultBlockState());
        if (!(level.getBlockEntity(tablePos)
                instanceof com.mechaniciancombat.block.BackpackModificationTableBlockEntity)) {
            throw new AssertionError("menu validation fixture did not create its table block entity");
        }
        player.setPos(tablePos.getX() + 0.5D, tablePos.getY() + 1.0D, tablePos.getZ() + 0.5D);
        BackpackModificationTableScreenHandler tableMenu =
                new BackpackModificationTableScreenHandler(72, player.getInventory(), tablePos);
        if (!invokeStillValid(tableMenu, player)
                || !invokeMenuSlot(tableMenu, 0).mayPlace(new ItemStack(ModRegistries.COMBAT_BACKPACK.get()))
                || invokeMenuSlot(tableMenu, 0).mayPlace(new ItemStack(Items.STONE))
                || !invokeMenuSlot(tableMenu, 1).mayPlace(new ItemStack(ModRegistries.BACKPACK_CORE.get()))
                || invokeMenuSlot(tableMenu, 1).mayPlace(new ItemStack(ModRegistries.PORTABLE_LAUNCHER.get()))
                || !invokeQuickMove(tableMenu, player, -1).isEmpty()
                || !invokeQuickMove(tableMenu, player, menuSlotCount(tableMenu)).isEmpty()) {
            throw new AssertionError("backpack table menu validity, slot filters, or index bounds failed");
        }
        level.setBlockAndUpdate(tablePos, Blocks.AIR.defaultBlockState());
        if (invokeStillValid(tableMenu, player)) {
            throw new AssertionError("backpack table menu remained valid after its table block was removed");
        }
        verifyModificationTableBreakDropsOnce(level, player);
        player.getInventory().clearContent();
        System.out.println("[tkguntests] PASS: ammo menu bounds/filter/shift-deposit and backpack table bounds/slot filters/block validity passed");
    }

    private static void verifyModificationTableBreakDropsOnce(ServerLevel level, FakePlayer player) {
        BlockPos pos = new BlockPos(level.getSharedSpawnPos().getX() + 64,
                testY, level.getSharedSpawnPos().getZ());
        level.getChunkAt(pos);
        if (!level.getBlockState(pos).isAir()) {
            throw new AssertionError("modification-table break fixture position is not empty");
        }
        level.setBlockAndUpdate(pos, ModRegistries.BACKPACK_TABLE_BLOCK.get().defaultBlockState());
        if (!(level.getBlockEntity(pos) instanceof BackpackModificationTableBlockEntity table)) {
            throw new AssertionError("modification-table break fixture has no block entity");
        }
        table.inventory().setItem(BackpackModificationTableBlockEntity.BACKPACK_SLOT,
                new ItemStack(ModRegistries.COMBAT_BACKPACK.get()));
        table.inventory().setItem(BackpackModificationTableBlockEntity.PART_SLOT_START,
                new ItemStack(ModRegistries.BACKPACK_CORE.get()));
        table.inventory().setItem(BackpackModificationTableBlockEntity.PART_SLOT_START + 1,
                new ItemStack(ModRegistries.PORTABLE_LAUNCHER.get()));
        player.setPos(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
        BackpackModificationTableScreenHandler menu =
                new BackpackModificationTableScreenHandler(73, player.getInventory(), pos);
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        if (invokeStillValid(menu, player) || !table.inventory().isEmpty()) {
            throw new AssertionError("destroying the open modification table did not invalidate its menu and clear stored items");
        }
        AABB dropArea = new AABB(pos).inflate(3.0D);
        long firstDropCount = level.getEntitiesOfClass(ItemEntity.class, dropArea).stream()
                .mapToLong(entity -> entity.getItem().getCount()).sum();
        if (firstDropCount != 3L) {
            throw new AssertionError("destroying the open modification table did not drop exactly three stored items: "
                    + firstDropCount);
        }
        table.dropInventory(level, pos);
        long secondDropCount = level.getEntitiesOfClass(ItemEntity.class, dropArea).stream()
                .mapToLong(entity -> entity.getItem().getCount()).sum();
        if (secondDropCount != firstDropCount) {
            throw new AssertionError("repeated table inventory cleanup duplicated drops: before="
                    + firstDropCount + ", after=" + secondDropCount);
        }
        System.out.println("[tkguntests] PASS: destroying an open backpack table invalidated its menu, dropped all three stored items once, and a repeated cleanup did not duplicate them");
    }

    private static ItemStack invokeQuickMove(net.minecraft.world.inventory.AbstractContainerMenu menu,
            FakePlayer player, int slotIndex) {
        try {
            Method method = findMappedMenuMethod(menu, ItemStack.class,
                    net.minecraft.world.entity.player.Player.class, int.class);
            return (ItemStack) method.invoke(menu, player, slotIndex);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not invoke the mapped menu quick-move implementation", failure);
        }
    }

    private static boolean invokeStillValid(net.minecraft.world.inventory.AbstractContainerMenu menu,
            FakePlayer player) {
        try {
            Method method = findMappedMenuMethod(menu, boolean.class,
                    net.minecraft.world.entity.player.Player.class);
            return (boolean) method.invoke(menu, player);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not invoke the mapped menu validity implementation", failure);
        }
    }

    private static net.minecraft.world.inventory.Slot invokeMenuSlot(
            net.minecraft.world.inventory.AbstractContainerMenu menu, int slotIndex) {
        try {
            Method method = findMappedMenuMethod(menu,
                    net.minecraft.world.inventory.Slot.class, int.class);
            return (net.minecraft.world.inventory.Slot) method.invoke(menu, slotIndex);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not inspect the mapped menu slot", failure);
        }
    }

    private static Method findMappedMenuMethod(net.minecraft.world.inventory.AbstractContainerMenu menu,
            Class<?> returnType, Class<?>... parameterTypes) throws NoSuchMethodException {
        for (Class<?> type = menu.getClass(); type != null; type = type.getSuperclass()) {
            for (Method candidate : type.getDeclaredMethods()) {
                if (candidate.getReturnType() == returnType
                        && Arrays.equals(candidate.getParameterTypes(), parameterTypes)) {
                    candidate.setAccessible(true);
                    return candidate;
                }
            }
        }
        throw new NoSuchMethodException("mapped menu method " + Arrays.toString(parameterTypes));
    }

    private static void invokeMappedSetItem(GunAmmoData.AmmoInventory inventory,
            int slot, ItemStack stack) {
        try {
            for (Method candidate : inventory.getClass().getDeclaredMethods()) {
                if (candidate.getReturnType() == void.class
                        && Arrays.equals(candidate.getParameterTypes(),
                                new Class<?>[] {int.class, ItemStack.class})) {
                    candidate.setAccessible(true);
                    candidate.invoke(inventory, slot, stack);
                    return;
                }
            }
            throw new NoSuchMethodException("setItem(int, ItemStack)");
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not invoke mapped ammo inventory setItem", failure);
        }
    }

    private static boolean invokeMappedMoveItemStackTo(
            net.minecraft.world.inventory.AbstractContainerMenu menu,
            ItemStack stack, int start, int end) {
        try {
            Method method = findMappedMenuMethod(menu, boolean.class,
                    ItemStack.class, int.class, int.class, boolean.class);
            return (boolean) method.invoke(menu, stack, start, end, true);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not invoke mapped menu inventory transfer", failure);
        }
    }

    private static int menuSlotCount(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        try {
            for (Class<?> type = menu.getClass(); type != null; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (!List.class.isAssignableFrom(field.getType())) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object value = field.get(menu);
                    if (value instanceof List<?> entries
                            && !entries.isEmpty()
                            && entries.get(0) instanceof net.minecraft.world.inventory.Slot) {
                        return entries.size();
                    }
                }
            }
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not inspect the mapped menu slots", failure);
        }
        throw new AssertionError("menu slot list was not found");
    }

    private static void verifyOrPreparePersistentItems(ServerLevel level) {
        String mode = System.getProperty("tkguntests.persistenceMode", "prepare");
        BlockPos origin = level.getSharedSpawnPos();
        BlockPos chestPos = new BlockPos(origin.getX() + 40, testY, origin.getZ());
        level.getChunkAt(chestPos);

        if ("prepare".equals(mode)) {
            level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
            if (!(level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest)) {
                throw new AssertionError("persistence fixture chest was not created");
            }

            ItemStack gun = new ItemStack(ModRegistries.BOWGUN.get());
            NonNullList<ItemStack> ammo = NonNullList.withSize(GunAmmoData.SIZE, ItemStack.EMPTY);
            ammo.set(0, new ItemStack(ModRegistries.BOLT.get(), 7));
            ammo.set(7, new ItemStack(ModRegistries.BULLETS.get(0).get(), 3));
            GunAmmoData.save(gun, ammo, 7, level.registryAccess());
            GunAmmoData.saveMagazine(gun, 4, "tkgun:bolt");

            ItemStack backpack = new ItemStack(ModRegistries.COMBAT_BACKPACK.get());
            BackpackUpgradeData.installCore(backpack);
            BackpackUpgradeData.setLauncherType(backpack, BackpackUpgradeData.LauncherType.AUTO);
            BackpackUpgradeData.setCoreSlot(backpack, 2);
            BackpackUpgradeData.setLauncherSlot(backpack, 1);
            chest.setItem(0, gun);
            chest.setItem(1, backpack);
            chest.setChanged();
            writePersistenceResult("PREPARED: chest fixture stores an 8-slot gun ammo ring, selected slot, magazine and upgraded backpack for disk-restart verification");
            return;
        }

        if (!"verify".equals(mode)) {
            throw new AssertionError("unknown tkguntests.persistenceMode: " + mode);
        }
        if (!(level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest)) {
            throw new AssertionError("persisted chest fixture is missing after restart");
        }
        ItemStack gun = chest.getItem(0);
        NonNullList<ItemStack> ammo = GunAmmoData.read(gun, level.registryAccess());
        if (gun.getItem() != ModRegistries.BOWGUN.get()
                || GunAmmoData.selected(gun) != 7
                || ammo.get(0).getItem() != ModRegistries.BOLT.get()
                || ammo.get(0).getCount() != 7
                || ammo.get(7).getItem() != ModRegistries.BULLETS.get(0).get()
                || ammo.get(7).getCount() != 3
                || !GunAmmoData.hasSavedMagazine(gun)
                || GunAmmoData.savedMagazineCount(gun) != 4
                || !"tkgun:bolt".equals(GunAmmoData.savedMagazineAmmo(gun))) {
            throw new AssertionError("gun ammo ring or magazine did not survive world restart");
        }
        ItemStack backpack = chest.getItem(1);
        if (backpack.getItem() != ModRegistries.COMBAT_BACKPACK.get()
                || !BackpackUpgradeData.canUseLauncher(backpack)
                || BackpackUpgradeData.launcherType(backpack) != BackpackUpgradeData.LauncherType.AUTO
                || BackpackUpgradeData.coreSlot(backpack) != 2
                || BackpackUpgradeData.launcherSlot(backpack) != 1) {
            throw new AssertionError("backpack component data did not survive world restart");
        }
        writePersistenceResult("PASS: world was stopped and reopened; gun ammo-ring contents, selected slot, magazine and backpack core/auto-launcher slots were read back from the saved chest");
    }

    private static void writePersistenceResult(String result) {
        String configuredPath = System.getProperty("tkguntests.persistenceResultFile");
        if (configuredPath == null || configuredPath.isBlank()) {
            return;
        }
        try {
            Path path = Path.of(configuredPath);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, result, StandardCharsets.UTF_8);
        } catch (Exception failure) {
            throw new IllegalStateException("could not write persistence result", failure);
        }
    }

    private static void runButtonInteractionCheck(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos origin = new BlockPos(spawn.getX() + 12, testY, spawn.getZ() + 1);
        prepareFlatArena(level, origin, -2, 2, -1, 4);
        BlockPos support = origin.offset(0, 1, 2);
        testButtonPos = support.north();
        buttonWasPowered = false;
        level.setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(testButtonPos, Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,
                        Direction.NORTH));

        WindChargeEntity projectile = new WindChargeEntity(level, null);
        ((net.minecraft.world.entity.Entity) projectile).setPos(
                origin.getX() + 0.5D, origin.getY() + 1.5D, origin.getZ() + 0.5D);
        ((net.minecraft.world.entity.Entity) projectile).setDeltaMovement(0.0D, 0.0D, 1.0D);
        if (!level.addFreshEntity(projectile)) {
            throw new AssertionError("could not add wind charge block interaction projectile");
        }
    }

    private static void runProjectileDeflectionCheck(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos origin = new BlockPos(spawn.getX() + 24, testY, spawn.getZ() + 1);
        prepareFlatArena(level, origin, -3, 3, -1, 6);
        BlockPos wall = origin.offset(0, 1, 4);
        level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());

        deflectedProjectile = new Snowball(level, origin.getX() + 1.5D, origin.getY() + 1.5D,
                origin.getZ() + 3.5D);
        deflectedProjectile.setNoGravity(true);
        if (!level.addFreshEntity(deflectedProjectile)) {
            throw new AssertionError("could not add a projectile for wind burst deflection check");
        }

        WindChargeEntity windCharge = new WindChargeEntity(level, null);
        net.minecraft.world.entity.Entity entity = windCharge;
        entity.setPos(origin.getX() + 0.5D, origin.getY() + 1.5D, origin.getZ() + 0.5D);
        entity.setDeltaMovement(0.0D, 0.0D, 1.5D);
        if (!level.addFreshEntity(windCharge)) {
            throw new AssertionError("could not add wind charge for projectile deflection check");
        }
    }

    private static void prepareFlatArena(ServerLevel level, BlockPos origin,
                                         int minX, int maxX, int minZ, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y < 5; y++) {
                    level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static final class CaptureHandLoyaltyProjectile {
        private MoltenTridentEntity projectile;

        @SubscribeEvent
        public void onEntityJoin(EntityJoinLevelEvent event) {
            if (!(event.getEntity() instanceof MoltenTridentEntity molten)
                    || EnchantmentHelper.getItemEnchantmentLevel(
                            Enchantments.LOYALTY, molten.getWeaponItem()) != 3) {
                return;
            }
            projectile = molten;
            invokeSetNoPhysics(molten, true);
        }
    }

    private static final class RejectWindChargeSpawn {
        @SubscribeEvent
        public void onEntityJoin(EntityJoinLevelEvent event) {
            if (event.getEntity() instanceof WindChargeEntity) {
                event.setCanceled(true);
            }
        }
    }

    private static void finish(String result, Throwable failure) {
        awaitingCollision = false;
        if (failure != null) {
            failure.printStackTrace();
        }
        if (result.startsWith("PASS:")) {
            System.out.println("[tkguntests] " + result);
        } else {
            System.err.println("[tkguntests] " + result);
        }
        writeResult(result);
        if (testServer != null) {
            testServer.halt(false);
        }
    }

    private static void writeResult(String result) {
        String configuredPath = System.getProperty("tkguntests.resultFile");
        if (configuredPath == null || configuredPath.isBlank()) {
            return;
        }
        try {
            Path path = Path.of(configuredPath);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, result, StandardCharsets.UTF_8);
        } catch (Exception failure) {
            System.err.println("[tkguntests] Could not write result marker: " + failure);
        }
    }
}
