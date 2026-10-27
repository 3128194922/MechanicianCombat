package com.mechaniciancombat.registry;

import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.block.BackpackModificationTableBlock;
import com.mechaniciancombat.block.BackpackModificationTableBlockEntity;
import com.mechaniciancombat.entity.BoltProjectileEntity;
import com.mechaniciancombat.entity.BulletEntity;
import com.mechaniciancombat.entity.MoltenTridentEntity;
import com.mechaniciancombat.entity.WindChargeEntity;
import com.mechaniciancombat.item.BackpackPartItem;
import com.mechaniciancombat.item.BoltItem;
import com.mechaniciancombat.item.BowgunItem;
import com.mechaniciancombat.item.CombatBackpackItem;
import com.mechaniciancombat.item.MoltenTridentItem;
import com.mechaniciancombat.item.WindChargeItem;
import com.mechaniciancombat.screen.AmmoStorageScreenHandler;
import com.mechaniciancombat.screen.BackpackModificationTableScreenHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/** All game content is registered through Forge's mod event bus. */
public final class ModRegistries {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, TkGun.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, TkGun.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TkGun.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, TkGun.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, TkGun.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TkGun.MOD_ID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, TkGun.MOD_ID);

    public static final RegistryObject<Block> BACKPACK_TABLE_BLOCK = BLOCKS.register(
            "backpack_modification_table", () -> new BackpackModificationTableBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5F)
                            .requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistryObject<BlockItem> BACKPACK_TABLE_ITEM = ITEMS.register(
            "backpack_modification_table", () -> new BlockItem(BACKPACK_TABLE_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<BackpackModificationTableBlockEntity>> BACKPACK_TABLE_ENTITY =
            BLOCK_ENTITIES.register("backpack_modification_table", () -> BlockEntityType.Builder.of(
                    BackpackModificationTableBlockEntity::new, BACKPACK_TABLE_BLOCK.get()).build(null));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<BackpackModificationTableScreenHandler>>
            BACKPACK_TABLE_MENU = MENUS.register("backpack_modification_table",
                    () -> net.minecraftforge.common.extensions.IForgeMenuType.create(
                            (id, inventory, data) -> new BackpackModificationTableScreenHandler(
                                    id, inventory, data.readBlockPos())));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<AmmoStorageScreenHandler>> AMMO_MENU =
            MENUS.register("ammo_storage", () -> net.minecraftforge.common.extensions.IForgeMenuType.create(
                    (id, inventory, data) -> new AmmoStorageScreenHandler(id, inventory, data)));

    public static final RegistryObject<Item> BACKPACK_CORE = ITEMS.register("backpack_core",
            () -> new BackpackPartItem(BackpackPartItem.Kind.CORE, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PORTABLE_LAUNCHER = ITEMS.register("portable_launcher",
            () -> new BackpackPartItem(BackpackPartItem.Kind.LAUNCHER, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> AUTO_PORTABLE_LAUNCHER = ITEMS.register("auto_portable_launcher",
            () -> new BackpackPartItem(BackpackPartItem.Kind.AUTO_LAUNCHER, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SUMMON_ANTENNA = ITEMS.register("summon_antenna",
            () -> new BackpackPartItem(BackpackPartItem.Kind.SUMMON_ANTENNA, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BACKPACK_COOLDOWN_RESET = ITEMS.register("backpack_cooldown_reset",
            () -> new com.mechaniciancombat.item.BackpackCooldownResetItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ENTITY_REMOVAL_TOOL = ITEMS.register("entity_removal_tool",
            () -> new com.mechaniciancombat.item.EntityRemovalToolItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<EntityType<com.mechaniciancombat.entity.MoltenGolemEntity>> MOLTEN_GOLEM =
            ENTITIES.register("molten_golem", () -> EntityType.Builder.<com.mechaniciancombat.entity.MoltenGolemEntity>of(
                    com.mechaniciancombat.entity.MoltenGolemEntity::new, MobCategory.MISC).sized(1.4F, 2.7F)
                    .build(TkGun.id("molten_golem").toString()));
    public static final RegistryObject<Item> MOLTEN_GOLEM_SPAWN_EGG = ITEMS.register("molten_golem_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(MOLTEN_GOLEM, 0xD7D7D7, 0x6A6A6A, new Item.Properties()));
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, TkGun.MOD_ID);
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> GOLEM_SWING = sound("golem_swing");
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> GOLEM_HAMMER = sound("golem_hammer");
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> GOLEM_EXPLOSION = sound("golem_explosion");
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> GOLEM_ELECTRIC = sound("golem_electric");

    public static final RegistryObject<Item> BOWGUN = ITEMS.register("bowgun",
            () -> new BowgunItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BOLT = ITEMS.register("bolt",
            () -> new MoltenTridentItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> WIND_CHARGE = ITEMS.register("wind_charge",
            () -> new WindChargeItem(new Item.Properties()));
    public static final RegistryObject<Item> TAB_ICON = ITEMS.register("tab_icon",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COMBAT_BACKPACK = backpack("combat_backpack", "combat_backpack");
    public static final List<RegistryObject<Item>> COMBAT_BACKPACKS = List.of(
            COMBAT_BACKPACK,
            backpack("combat_backpack_black", "combat_backpack_black"),
            backpack("combat_backpack_blue", "combat_backpack_blue"),
            backpack("combat_backpack_brown", "combat_backpack_brown"),
            backpack("combat_backpack_cyan", "combat_backpack_cyan"),
            backpack("combat_backpack_gray", "combat_backpack_gray"),
            backpack("combat_backpack_green", "combat_backpack_green"),
            backpack("combat_backpack_light_blue", "combat_backpack_light_blue"),
            backpack("combat_backpack_light_gray", "combat_backpack_light_gray"),
            backpack("combat_backpack_lime", "combat_backpack_lime"),
            backpack("combat_backpack_magenta", "combat_backpack_magenta"),
            backpack("combat_backpack_orange", "combat_backpack_orange"),
            backpack("combat_backpack_pink", "combat_backpack_pink"),
            backpack("combat_backpack_purple", "combat_backpack_purple"),
            backpack("combat_backpack_red", "combat_backpack_red"),
            backpack("combat_backpack_shulker_box", "combat_backpack_shulker_box"),
            backpack("combat_backpack_white", "combat_backpack_white"),
            backpack("combat_backpack_yellow", "combat_backpack_yellow"));

    public static final List<RegistryObject<Item>> BULLETS = List.of(
            bullet("cutting_bolt"), bullet("sculk_bolt"), bullet("explosive_bolt"),
            bullet("rose_quartz_bolt"), bullet("armor_piercing_bolt"));

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.tkgun"))
                    .icon(() -> new ItemStack(TAB_ICON.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(BACKPACK_TABLE_ITEM.get());
                        output.accept(BOWGUN.get());
                        output.accept(BOLT.get());
                        output.accept(WIND_CHARGE.get());
                        output.accept(COMBAT_BACKPACK.get());
                        output.accept(BACKPACK_CORE.get());
                        output.accept(PORTABLE_LAUNCHER.get());
                        output.accept(AUTO_PORTABLE_LAUNCHER.get());
                        output.accept(SUMMON_ANTENNA.get());
                        output.accept(BACKPACK_COOLDOWN_RESET.get());
                        output.accept(MOLTEN_GOLEM_SPAWN_EGG.get());
                        output.accept(ENTITY_REMOVAL_TOOL.get());
                        BULLETS.forEach(item -> output.accept(item.get()));
                    }).build());

    public static final RegistryObject<net.minecraft.world.entity.EntityType<BoltProjectileEntity>> BOLT_ENTITY =
            ENTITIES.register("bolt_projectile", () -> EntityType.Builder.<BoltProjectileEntity>of(
                    BoltProjectileEntity::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .build(TkGun.id("bolt_projectile").toString()));
    public static final RegistryObject<net.minecraft.world.entity.EntityType<BulletEntity>> BULLET_ENTITY =
            ENTITIES.register("bullet", () -> EntityType.Builder.<BulletEntity>of(
                    BulletEntity::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .updateInterval(1)
                    .build(TkGun.id("bullet").toString()));
    public static final RegistryObject<net.minecraft.world.entity.EntityType<MoltenTridentEntity>> MOLTEN_TRIDENT_ENTITY =
            ENTITIES.register("molten_trident", () -> EntityType.Builder.<MoltenTridentEntity>of(
                    MoltenTridentEntity::new, MobCategory.MISC).sized(0.5F, 0.5F)
                    .build(TkGun.id("molten_trident").toString()));
    public static final RegistryObject<net.minecraft.world.entity.EntityType<WindChargeEntity>> WIND_CHARGE_ENTITY =
            ENTITIES.register("wind_charge", () -> EntityType.Builder.<WindChargeEntity>of(
                    WindChargeEntity::new, MobCategory.MISC).sized(0.3125F, 0.3125F)
                    .clientTrackingRange(4).updateInterval(10)
                    .build(TkGun.id("wind_charge").toString()));

    public static final RegistryObject<SimpleParticleType> HEAT_VENT = particle("heat_vent");
    public static final RegistryObject<SimpleParticleType> ENERGY_CONVERGE = particle("energy_converge");
    public static final RegistryObject<SimpleParticleType> ENERGY_XPLO = particle("energy_xplo");
    public static final RegistryObject<SimpleParticleType> SHELL_BOOST = particle("shell_boost");
    public static final RegistryObject<SimpleParticleType> MUZZLE_GLOW = particle("muzzle_glow");
    public static final RegistryObject<SimpleParticleType> CUTTING_EXPLO = particle("cutting_explo");
    public static final RegistryObject<SimpleParticleType> SONIC_BOOM = particle("sonic_boom");
    public static final RegistryObject<SimpleParticleType> GRAPESHOT_IMPACT = particle("grapeshot_impact");
    public static final RegistryObject<SimpleParticleType> ROSE_QUARTZ_IMPACT = particle("rose_quartz_explo");
    public static final RegistryObject<SimpleParticleType> ROSE_QUARTZ_TRAIL = particle("rose_quartz_trail");

    public static final RegistryObject<SimpleParticleType> GOLEM_ENERGY_CONVERGE = particle("golem_energy_converge");

    public static final RegistryObject<SimpleParticleType> GOLEM_ENERGY_CONVERGE_2 = particle("golem_energy_converge_2");

    public static final RegistryObject<SimpleParticleType> EXPLOSIVE_BOLT_IMPACT = particle("explosive_bolt_impact");

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        ENTITIES.register(modBus);
        CREATIVE_TABS.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener(ModRegistries::entityAttributes);
    }

    private static RegistryObject<Item> backpack(String id, String texture) {
        return ITEMS.register(id, () -> new CombatBackpackItem(new Item.Properties().stacksTo(1), texture));
    }

    private static RegistryObject<Item> bullet(String id) {
        return ITEMS.register(id, () -> new BoltItem(new Item.Properties()));
    }

    private static RegistryObject<SimpleParticleType> particle(String id) {
        return PARTICLES.register(id, () -> new SimpleParticleType(true));
    }

    private static RegistryObject<net.minecraft.sounds.SoundEvent> sound(String id) {
        return SOUNDS.register(id, () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(TkGun.id(id)));
    }
    private static void entityAttributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(MOLTEN_GOLEM.get(), net.minecraft.world.entity.animal.IronGolem.createAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 400.0D).build());
    }
    private ModRegistries() { }
}
