package com.mechaniciancombat.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import com.mechaniciancombat.entity.MoltenGolemEntity;
import com.mechaniciancombat.registry.ModRegistries;
import com.mechaniciancombat.net.GunStatePayload;
import com.mechaniciancombat.net.CombatBackpackStatusPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.UUID;

@GameTestHolder("tkgun")
@PrefixGameTestTemplate(false)
public final class R396ContentGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void r396ContentIsRegistered(GameTestHelper helper) {
        helper.assertTrue(ForgeRegistries.ENTITY_TYPES.containsKey(id("molten_golem")),
                "r396 molten golem entity is missing");
        for (String item : new String[]{"summon_antenna", "molten_golem_spawn_egg",
                "backpack_cooldown_reset", "entity_removal_tool"}) {
            helper.assertTrue(ForgeRegistries.ITEMS.containsKey(id(item)), "Missing r396 item " + item);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void r396StatePacketsRoundTrip(GameTestHelper helper) {
        var expected = new GunStatePayload(123, (byte) 7, 11, 75, 3, 6, true, true,
                19, true, 60, 20, 100, "tkgun:explosive_bolt", 39);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            GunStatePayload.CODEC.encoder().accept(expected, buf);
            helper.assertTrue(expected.equals(GunStatePayload.CODEC.decoder().apply(buf)),
                    "r396 state fields were lost or reordered");
            helper.assertTrue(buf.readableBytes() == 0, "state packet left unread bytes");
            for (int phase = 5; phase <= 9; phase++) {
                var status = new CombatBackpackStatusPayload(123, 456, phase, 0, 60);
                buf.clear();
                CombatBackpackStatusPayload.CODEC.encoder().accept(status, buf);
                helper.assertTrue(status.equals(CombatBackpackStatusPayload.CODEC.decoder().apply(buf)),
                        "summon phase did not round trip: " + phase);
            }
        } finally { buf.release(); }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void golemKeepsOwnerAndAttackStateOnReload(GameTestHelper helper) {
        var level = helper.getLevel();
        MoltenGolemEntity golem = ModRegistries.MOLTEN_GOLEM.get().create(level);
        helper.assertTrue(golem != null && golem.getMaxHealth() == 400.0F, "golem attributes must be 400 HP");
        UUID owner = UUID.randomUUID();
        golem.setSummoner(owner);
        CompoundTag saved = new CompoundTag();
        golem.saveWithoutId(saved);
        helper.assertTrue(saved.getInt("SummonTicks") == 70, "landing duration changed");
        saved.putInt("SummonTicks", 0);
        saved.putString("AttackKind", "ATK3");
        saved.putInt("AttackElapsed", 38);
        saved.putBoolean("AttackDamageDone", true);
        saved.putInt("Atk2Cooldown", 100);
        saved.putInt("Atk3Cooldown", 400);
        MoltenGolemEntity restored = ModRegistries.MOLTEN_GOLEM.get().create(level);
        restored.load(saved);
        CompoundTag after = new CompoundTag();
        restored.saveWithoutId(after);
        helper.assertTrue(restored.isSummonedBy(owner), "summoner UUID was lost");
        helper.assertTrue(restored.isCombatAttacking(), "attack state was lost");
        for (String key : new String[]{"Owner", "SummonTicks", "AttackKind", "AttackElapsed",
                "AttackDamageDone", "Atk2Cooldown", "Atk3Cooldown", "VisualState"}) {
            helper.assertTrue(saved.get(key).equals(after.get(key)), "NBT changed on reload: " + key);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void golemThirdAttackDamagesOnceAtFrame38(GameTestHelper helper) {
        var level = helper.getLevel();
        var position = helper.absolutePos(new BlockPos(8, 3, 8));
        MoltenGolemEntity golem = ModRegistries.MOLTEN_GOLEM.get().create(level);
        golem.moveTo(position.getX(), position.getY(), position.getZ(), 0, 0);
        CompoundTag ready = new CompoundTag();
        golem.saveWithoutId(ready);
        ready.putInt("SummonTicks", 0);
        golem.load(ready);
        var target = EntityType.ZOMBIE.create(level);
        target.moveTo(position.getX(), position.getY(), position.getZ() + 2, 0, 0);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.setHealth(1000);
        target.setNoAi(true);
        level.addFreshEntity(target);
        golem.setTarget(target);
        helper.assertTrue(golem.doHurtTarget(target), "golem did not begin its first attack");
        for (int tick = 0; tick < 37; tick++) golem.tick();
        helper.assertTrue(target.getHealth() == 1000, "damage arrived before frame 38");
        golem.tick();
        helper.assertTrue(target.getHealth() == 985, "ATK3 must deal 15 damage at frame 38");
        CompoundTag saved = new CompoundTag();
        golem.saveWithoutId(saved);
        MoltenGolemEntity restored = ModRegistries.MOLTEN_GOLEM.get().create(level);
        restored.load(saved);
        restored.setTarget(target);
        for (int tick = 0; tick < 19; tick++) {
            target.invulnerableTime = 0;
            restored.tick();
        }
        helper.assertTrue(target.getHealth() == 985, "reloaded attack applied damage twice");
        target.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void antennaSummonsAndReusesOwnedGolem(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var owner = net.minecraftforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "r396_summoner"));
        var pos = helper.absolutePos(new BlockPos(8, 3, 8));
        owner.moveTo(pos.getX(), pos.getY(), pos.getZ(), 180, 0);
        var backpack = new net.minecraft.world.item.ItemStack(ModRegistries.COMBAT_BACKPACK.get());
        com.mechaniciancombat.item.BackpackUpgradeData.installCore(backpack);
        com.mechaniciancombat.item.BackpackUpgradeData.setLauncherType(backpack,
                com.mechaniciancombat.item.BackpackUpgradeData.LauncherType.SUMMON_ANTENNA);
        var inventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(owner).resolve().orElseThrow();
        inventory.setEquippedCurio("back", 0, backpack);
        helper.assertTrue(com.mechaniciancombat.item.CombatBackpackItem.equippedStack(owner) == backpack,
                "summon antenna must use the actual equipped Curios backpack");
        var eye = owner.getEyePosition();
        var payload = new com.mechaniciancombat.net.CombatBackpackFirePayload(pos.getX(), pos.getY(), pos.getZ(),
                eye.x, eye.y, eye.z, 180, 0, 0.01, 0.01);
        var server = com.mechaniciancombat.TkGunServer.class;
        var stateMethod = server.getDeclaredMethod("backpackState", net.minecraft.server.level.ServerPlayer.class, long.class);
        stateMethod.setAccessible(true);
        var action = server.getDeclaredMethod("handleBackpackAction", net.minecraft.server.level.ServerPlayer.class,
                com.mechaniciancombat.net.CombatBackpackFirePayload.class);
        action.setAccessible(true);
        long now = level.getGameTime();
        Object state = stateMethod.invoke(null, owner, now);
        var chargeClass = state.getClass();
        var ready = chargeClass.getDeclaredField("summonReadyUntil"); ready.setAccessible(true);
        var cast = chargeClass.getDeclaredField("summonCastUntil"); cast.setAccessible(true);
        var cooldown = chargeClass.getDeclaredField("cooldownUntil"); cooldown.setAccessible(true);
        var tick = server.getDeclaredMethod("tickSummonAntenna", net.minecraft.server.level.ServerPlayer.class, chargeClass, long.class);
        tick.setAccessible(true);
        com.mechaniciancombat.TkGunServer.clearBackpackCooldown(owner);
        action.invoke(null, owner, payload);
        helper.assertTrue(ready.getLong(state) == now + 20, "antenna preparation must last 20 ticks");
        // Advance the controller clock without changing the shared GameTest world clock.
        ready.setLong(state, Math.max(1, now - 1));
        action.invoke(null, owner, payload);
        helper.assertTrue(cast.getLong(state) == now + 60, "antenna cast must last 60 ticks");
        tick.invoke(null, owner, state, now + 60);
        java.util.List<MoltenGolemEntity> owned = new java.util.ArrayList<>();
        for (var entity : level.getAllEntities()) {
            if (entity instanceof MoltenGolemEntity golem && golem.isSummonedBy(owner.getUUID())) owned.add(golem);
        }
        helper.assertTrue(owned.size() == 1, "antenna must spawn exactly one owned golem");
        var first = owned.get(0);
        helper.assertTrue(cooldown.getLong(state) == now + 60 + 300, "successful summon must enter 300-tick cooldown");
        first.setHealth(200);
        com.mechaniciancombat.TkGunServer.clearBackpackCooldown(owner);
        action.invoke(null, owner, payload);
        ready.setLong(state, Math.max(1, now - 1));
        action.invoke(null, owner, payload);
        tick.invoke(null, owner, state, now + 60);
        int count = 0;
        for (var entity : level.getAllEntities()) {
            if (entity instanceof MoltenGolemEntity golem && golem.isSummonedBy(owner.getUUID())) {
                count++;
                helper.assertTrue(golem.getUUID().equals(first.getUUID()), "resummon replaced the existing golem");
            }
        }
        helper.assertTrue(count == 1 && first.getHealth() == 280, "resummon must reuse and heal by 20% max HP");
        first.discard();
        helper.succeed();
    }
    private static ResourceLocation id(String name) { return new ResourceLocation("tkgun", name); }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void autoTurretReacquiresAndCompletesCooldown(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var owner = net.minecraftforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "turret_cycle"));
        var pos = helper.absolutePos(new BlockPos(8, 3, 8));
        owner.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0, 0);
        var backpack = new net.minecraft.world.item.ItemStack(ModRegistries.COMBAT_BACKPACK.get());
        com.mechaniciancombat.item.BackpackUpgradeData.installCore(backpack);
        com.mechaniciancombat.item.BackpackUpgradeData.setLauncherType(backpack,
                com.mechaniciancombat.item.BackpackUpgradeData.LauncherType.AUTO);
        var curios = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(owner).resolve().orElseThrow();
        curios.setEquippedCurio("back", 0, backpack);
        owner.getInventory().offhand.set(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET, 16));
        var first = EntityType.PIG.create(level);
        var second = EntityType.PIG.create(level);
        first.moveTo(pos.getX(), pos.getY(), pos.getZ() + 3, 0, 0);
        second.moveTo(pos.getX() + 2, pos.getY(), pos.getZ() + 6, 0, 0);
        first.setNoAi(true); second.setNoAi(true);
        level.addFreshEntity(first); level.addFreshEntity(second);
        var server = com.mechaniciancombat.TkGunServer.class;
        var action = server.getDeclaredMethod("handleBackpackAction", net.minecraft.server.level.ServerPlayer.class,
                com.mechaniciancombat.net.CombatBackpackFirePayload.class);
        action.setAccessible(true);
        var stateMethod = server.getDeclaredMethod("backpackState", net.minecraft.server.level.ServerPlayer.class, long.class);
        stateMethod.setAccessible(true);
        long now = level.getGameTime();
        Object state = stateMethod.invoke(null, owner, now);
        var cls = state.getClass();
        var tick = server.getDeclaredMethod("tickAutoBackpack", net.minecraft.server.level.ServerPlayer.class, cls, long.class);
        tick.setAccessible(true);
        var charges = cls.getDeclaredField("charges"); charges.setAccessible(true);
        var active = cls.getDeclaredField("autoActive"); active.setAccessible(true);
        var target = cls.getDeclaredField("targetEntityId"); target.setAccessible(true);
        var cooldown = cls.getDeclaredField("cooldownUntil"); cooldown.setAccessible(true);
        var eye = owner.getEyePosition();
        var payload = new com.mechaniciancombat.net.CombatBackpackFirePayload(eye.x, eye.y, eye.z + 0.5,
                eye.x, eye.y, eye.z, 0, 0, 1, 1);
        try {
            action.invoke(null, owner, payload);
            helper.assertTrue(charges.getInt(state) == 9 && target.getInt(state) == first.getId(),
                    "first shot must select the nearest target and consume one charge");
            first.discard();
            tick.invoke(null, owner, state, now + 1);
            helper.assertTrue(target.getInt(state) == second.getId() && charges.getInt(state) == 9,
                    "removed target must be replaced without an early shot");
            for (int shot = 1; shot < 10; shot++) tick.invoke(null, owner, state, now + shot * 20L);
            helper.assertTrue(charges.getInt(state) == 0 && !active.getBoolean(state)
                    && owner.getOffhandItem().getCount() == 6, "ten shots must consume ten rockets and enter cooldown");
            long end = cooldown.getLong(state);
            helper.assertTrue(end == now + 180 + 200, "cooldown must last 200 ticks");
            tick.invoke(null, owner, state, end - 1);
            helper.assertTrue(charges.getInt(state) == 0, "cooldown must not finish early");
            tick.invoke(null, owner, state, end);
            helper.assertTrue(charges.getInt(state) == 10 && cooldown.getLong(state) == 0,
                    "cooldown must restore all charges at its deadline");
        } finally {
            first.discard(); second.discard();
            curios.setEquippedCurio("back", 0, net.minecraft.world.item.ItemStack.EMPTY);
            owner.getInventory().offhand.set(0, net.minecraft.world.item.ItemStack.EMPTY);
            var clear = server.getDeclaredMethod("clearBackpackState", net.minecraft.server.level.ServerPlayer.class);
            clear.setAccessible(true); clear.invoke(null, owner);
        }
        helper.succeed();
    }
}
