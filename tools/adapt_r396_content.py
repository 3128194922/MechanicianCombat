"""Reviewed Forge integration for r396 content; run once against the staged delta."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
J=ROOT/'src/main/java/com/mechaniciancombat'
E=ROOT/'docs/evidence/r396-port'

def edit(name,pairs):
    p=J/name;s=p.read_text(encoding='utf-8')
    for old,new in pairs:
        if old not in s: raise ValueError(f'{name}: missing {old[:75]}')
        s=s.replace(old,new)
    p.write_text(s,encoding='utf-8')

edit('entity/MoltenGolemEntity.java',[
    ('EntityDataSerializers.INTEGER','EntityDataSerializers.INT'),
    ('goalSelector.add(', 'goalSelector.addGoal('),
    ('protected void defineSynchedData(SynchedEntityData.Builder builder) {\n\t\tsuper.defineSynchedData(builder);\n\t\tbuilder.add(',
     'protected void defineSynchedData() {\n\t\tsuper.defineSynchedData();\n\t\tentityData.define('),
    ('boolean tryAttack(', 'boolean doHurtTarget('),
    ('AABB.of(', 'AABB.ofSize('), ('.expand(', '.inflate('),
    ('entity.getType().getType()', 'entity.getType().getCategory()'),
    ('.damage(', '.hurt('), ('boolean damage(', 'boolean hurt('),
    ('.multiply(', '.scale('), ('.isFollowingPath()', '.isInProgress()'),
    ('.isIdle()', '.isDone()'), ('.startMovingTo(', '.moveTo('),
    ('getMovementSpeed()', 'getSpeed()'),
    ('NbtUtils.fromUuid(', 'NbtUtils.createUUID('), ('NbtUtils.toUuid(', 'NbtUtils.loadUUID('),
    ('Control.MOVE, Control.LOOK', 'Flag.MOVE, Flag.LOOK'),
    ('boolean canStart()', 'boolean canUse()'), ('boolean shouldContinue()', 'boolean canContinueToUse()'),
    ('requestTeleport(x, y, z)', 'teleportTo(x, y, z)'),
])
# Registered sound events are required for Forge's registry-backed sound packets.
p=J/'entity/MoltenGolemEntity.java';s=p.read_text(encoding='utf-8')
for field,name in [('SWING','golem_swing'),('HAMMER','golem_hammer'),('EXPLOSION','golem_explosion'),('ELECTRIC','golem_electric')]:
    s=s.replace(f'SoundEvent.createVariableRangeEvent(TkGun.id("{name}"))',f'com.mechaniciancombat.registry.ModRegistries.GOLEM_{field}.get()')
s=s.replace('\t@Override\n\tprotected void defineSynchedData()', '''\t@Override
    protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dimensions) {
        return 2.35F;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }

\t@Override
\tprotected void defineSynchedData()''')
p.write_text(s,encoding='utf-8')
edit('CombatTargeting.java', [('ResourceKey.of(', 'ResourceKey.create('),('.isOf(','.is('),('.getSource()', '.getDirectEntity()'),('.getAttacker()', '.getEntity()')])
edit('client/GolemEnergyParticle.java', [('this.scale =','this.quadSize ='),('this.velocityX','this.xd'),('this.velocityY','this.yd'),('this.velocityZ','this.zd'),('this.gravityStrength','this.gravity'),('markDead()', 'remove()'),('getBrightness(', 'getLightColor('),('getType()', 'getRenderType()'),('this.world.', 'this.level.')])
edit('item/EntityRemovalToolItem.java',[('useOnEntity(', 'interactLivingEntity('),('SoundEvents.ENTITY_ENDERMAN_TELEPORT','SoundEvents.ENDERMAN_TELEPORT')])

p=J/'client/entity/MoltenGolemEntityRenderer.java';s=p.read_text(encoding='utf-8')
s=s.replace('\n\t@Override\n\tprotected float getShadowRadius(MoltenGolemEntity entity) {\n\t\treturn 1.4F;\n\t}\n','\n')
s=s.replace('super(context, new MoltenGolemGeoModel());','super(context, new MoltenGolemGeoModel());\n\t\tthis.shadowRadius = 1.4F;')
s=s.replace('int packedOverlay, int colour)', 'int packedOverlay, float red, float green, float blue, float alpha)')
s=s.replace('packedOverlay, colour);','packedOverlay, red, green, blue, alpha);')
p.write_text(s,encoding='utf-8')

# Content registration stays in the existing DeferredRegister, never in server initialization.
p=J/'registry/ModRegistries.java';s=p.read_text(encoding='utf-8')
anchor='    public static final RegistryObject<Item> BOWGUN ='
new='''    public static final RegistryObject<Item> SUMMON_ANTENNA = ITEMS.register("summon_antenna",
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

'''
assert anchor in s;s=s.replace(anchor,new+anchor)
s=s.replace('output.accept(AUTO_PORTABLE_LAUNCHER.get());','output.accept(AUTO_PORTABLE_LAUNCHER.get());\n                        output.accept(SUMMON_ANTENNA.get());\n                        output.accept(BACKPACK_COOLDOWN_RESET.get());\n                        output.accept(MOLTEN_GOLEM_SPAWN_EGG.get());\n                        output.accept(ENTITY_REMOVAL_TOOL.get());')
s=s.replace('        PARTICLES.register(modBus);','''        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener(ModRegistries::entityAttributes);''')
s=s.replace('    private ModRegistries() { }','''    private static RegistryObject<net.minecraft.sounds.SoundEvent> sound(String id) {
        return SOUNDS.register(id, () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(TkGun.id(id)));
    }
    private static void entityAttributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(MOLTEN_GOLEM.get(), net.minecraft.world.entity.animal.IronGolem.createAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 400.0D).build());
    }
    private ModRegistries() { }''')
for name in ['GOLEM_ENERGY_CONVERGE','GOLEM_ENERGY_CONVERGE_2','EXPLOSIVE_BOLT_IMPACT']:
    s=s.replace('    public static void register(IEventBus modBus) {',f'    public static final RegistryObject<SimpleParticleType> {name} = particle("{name.lower()}");\n\n    public static void register(IEventBus modBus) {{')
p.write_text(s,encoding='utf-8')
p=J/'TkGunParticles.java';s=p.read_text(encoding='utf-8')
for name in ['GOLEM_ENERGY_CONVERGE','GOLEM_ENERGY_CONVERGE_2','EXPLOSIVE_BOLT_IMPACT']:
    s=s.replace('\n\tprivate TkGunParticles()',f'\n\tpublic static final SimpleParticleType {name} = ModRegistries.{name}.get();\n\n\tprivate TkGunParticles()')
p.write_text(s,encoding='utf-8')

edit('TkGunServer.java', [
    ('public static Item AUTO_PORTABLE_LAUNCHER;', 'public static Item AUTO_PORTABLE_LAUNCHER;\n\tpublic static net.minecraft.world.entity.EntityType<com.mechaniciancombat.entity.MoltenGolemEntity> MOLTEN_GOLEM;\n\tpublic static Item MOLTEN_GOLEM_SPAWN_EGG;\n\tpublic static Item ENTITY_REMOVAL_TOOL;'),
    ('public boolean dashSyncActive;', 'public boolean dashSyncActive;\n\t\tpublic long fallbackRollReadyAt;'),
    ('// Every registry-backed object is now available;',
     'SUMMON_ANTENNA = com.mechaniciancombat.registry.ModRegistries.SUMMON_ANTENNA.get();\n\t\tBACKPACK_COOLDOWN_RESET = com.mechaniciancombat.registry.ModRegistries.BACKPACK_COOLDOWN_RESET.get();\n\t\tMOLTEN_GOLEM = com.mechaniciancombat.registry.ModRegistries.MOLTEN_GOLEM.get();\n\t\tMOLTEN_GOLEM_SPAWN_EGG = com.mechaniciancombat.registry.ModRegistries.MOLTEN_GOLEM_SPAWN_EGG.get();\n\t\tENTITY_REMOVAL_TOOL = com.mechaniciancombat.registry.ModRegistries.ENTITY_REMOVAL_TOOL.get();\n\t\t// Every registry-backed object is now available;'),
    ('now < st.dashUntil, (int) Math.max(0, st.dashUntil - now), aimLockTicks, (int) Math.max(0, st.overheatUntil - now), st.magAmmo, 0)',
     'now < st.dashUntil, dashTicks, aimLockTicks, heatLockTicks, st.magAmmo, rollCooldownTicks)'),
])

edit('client/TkGunClient.java',[
    ('event.registerSpriteSet(TkGunParticles.HEAT_VENT,', 'event.registerSpriteSet(TkGunParticles.GOLEM_ENERGY_CONVERGE, GolemEnergyParticle.Factory::new);\n\t\tevent.registerSpriteSet(TkGunParticles.GOLEM_ENERGY_CONVERGE_2, GolemEnergyParticle.Factory::new);\n\t\tevent.registerSpriteSet(TkGunParticles.EXPLOSIVE_BOLT_IMPACT, EnergyParticle.HitFactory::new);\n\t\tevent.registerSpriteSet(TkGunParticles.HEAT_VENT,'),
    ('event.registerEntityRenderer(com.mechaniciancombat.registry.ModRegistries.BOLT_ENTITY.get(),', 'event.registerEntityRenderer(com.mechaniciancombat.registry.ModRegistries.MOLTEN_GOLEM.get(), com.mechaniciancombat.client.entity.MoltenGolemEntityRenderer::new);\n\t\tevent.registerEntityRenderer(com.mechaniciancombat.registry.ModRegistries.BOLT_ENTITY.get(),')
])
edit('client/GunCamera.java',[
    ('e -> e.isAlive() && !e.isSpectator() && e != client.player && e.isPickable(),',
     'e -> e.isAlive() && !e.isSpectator() && e != client.player && e.isPickable() && !com.mechaniciancombat.CombatTargeting.isGolem(e),')])

# Preserve Forge-only assets and translations while updating the reviewed r396 delta.
source=ROOT.parent/'MechanicianCombat-r396-source/MechanicianCombat-source-r396/mod/src/main'
for row in json.loads((E/'source-delta.json').read_text(encoding='utf-8')):
    rel=row['path']
    if not rel.startswith('resources/assets/'):
        continue
    src=source/rel; dst=ROOT/'src/main'/rel
    dst.parent.mkdir(parents=True,exist_ok=True)
    if '/lang/' in rel and dst.exists():
        merged=json.loads(dst.read_text(encoding='utf-8'));merged.update(json.loads(src.read_text(encoding='utf-8')))
        dst.write_text(json.dumps(merged,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    else: dst.write_bytes(src.read_bytes())

# Shared network ABI changes are not compatible with the previous Forge port.
edit('net/ModNetwork.java', [('forge-r349-1','forge-r396-1')])
p=ROOT/'gradle.properties';s=p.read_text(encoding='utf-8').replace('mod_version=0.1.0-porting','mod_version=0.2.0-r396-porting');p.write_text(s,encoding='utf-8')
