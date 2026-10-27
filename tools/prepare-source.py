"""One-time, reviewable source migration; never writes to the reference project.

Names are derived from the reference Yarn/Intermediary and official Mojang
mapping files. This is a baseline only: compilation and semantic porting follow.
"""
from pathlib import Path
from collections import defaultdict
import json
import re
import shutil

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT.parent / 'MechanicianCombat-source-r349-20261025_030512'
CACHE = ROOT / 'tools/cache'

def tiny(path):
    classes, members = {}, []
    current = None
    for line in path.read_text(encoding='utf-8').splitlines():
        parts = line.split('\t')
        if parts[0] == 'c':
            current = parts[1]
            classes[current] = parts[2]
        elif len(parts) >= 5 and parts[0] == '' and parts[1] in ('m', 'f'):
            members.append((current, parts[1], parts[2], parts[3], parts[4]))
    return classes, members

def descriptor(typename, classes):
    primitive = {'void':'V','boolean':'Z','byte':'B','char':'C','short':'S','int':'I','long':'J','float':'F','double':'D'}
    if typename.endswith('[]'):
        return '[' + descriptor(typename[:-2], classes)
    return primitive.get(typename) or 'L' + classes.get(typename, typename).replace('.', '/') + ';'

def mappings():
    official = (CACHE / 'mojang-1.21.1-client.txt').read_text(encoding='utf-8').splitlines()
    mojang_to_obf = {}
    for line in official:
        if line and not line.startswith((' ', '#')) and ' -> ' in line:
            name, obf = line[:-1].split(' -> ')
            mojang_to_obf[name] = obf
    obf_to_mojang = {v:k.replace('.', '/') for k,v in mojang_to_obf.items()}
    mojang_members = {}
    owner = None
    for line in official:
        if line and not line.startswith((' ', '#')) and ' -> ' in line:
            owner = line[:-1].split(' -> ')[1]
        elif line.startswith('    ') and ' -> ' in line:
            signature, obf_name = line.strip().split(' -> ')
            signature = re.sub(r'^\d+:\d+:', '', signature)
            signature = re.sub(r':\d+(?::\d+)?$', '', signature)
            result, rest = signature.split(' ', 1)
            if '(' in rest:
                name, args = rest[:-1].split('(')
                desc = '(' + ''.join(descriptor(t, mojang_to_obf) for t in args.split(',') if t) + ')' + descriptor(result, mojang_to_obf)
            else:
                name, desc = rest, descriptor(result, mojang_to_obf)
            mojang_members[(owner, obf_name, desc)] = name
    obf_to_inter, inter_members = tiny(SOURCE / 'tools/remap/intermediary-1.21.1.tiny')
    inter_to_obf = {v:k for k,v in obf_to_inter.items()}
    inter_to_yarn, yarn_members = tiny(SOURCE / 'tools/remap/yarn-1.21.1.tiny')
    remapped_members = {}
    for owner, kind, desc, obf_name, inter_name in inter_members:
        inter_desc = re.sub(r'L([^;]+);', lambda m: 'L'+obf_to_inter.get(m[1],m[1])+';', desc)
        mapped = mojang_members.get((owner,obf_name,desc))
        if mapped:
            remapped_members[(obf_to_inter[owner],kind,inter_desc,inter_name)] = mapped
    candidates = defaultdict(set)
    for owner,kind,desc,inter_name,yarn_name in yarn_members:
        mapped = remapped_members.get((owner,kind,desc,inter_name))
        if mapped and mapped not in ('<init>','<clinit>'):
            candidates[yarn_name].add(mapped)
    classes = {}
    for inter,yarn in inter_to_yarn.items():
        mapped = obf_to_mojang.get(inter_to_obf.get(inter,''))
        if mapped:
            classes[yarn.replace('/','.').replace('$','.')] = mapped.replace('/','.').replace('$','.')
    # Never rename arbitrary locals, JDK/Netty/JOML members or third-party API.
    safe = set('rotateY fromPolar rotateX crossProduct horizontalLength dotProduct horizontalLengthSquared textRenderer setDefaultState ofFloored down createScreenHandlerFactory requiresTool dataTracker STONE_GRAY maxV maxU minU minV forwardSpeed headYaw prevBodyYaw sidewaysSpeed upwardSpeed bodyYaw tickMovement syncId splitStack MAX_LIGHT_COORDINATE movementSideways movementForward prevZ prevY noClip prevYaw velocityModified prevX prevPitch timeUntilRegen changeLookDirection initDataTracker readCustomDataFromNbt refreshPositionAndAngles writeCustomDataToNbt prevPosZ maxAge collidesWithWorld prevPosY prevPosX buildGeometry renderParticles DEFAULT_UV GENERIC_JUMP_STRENGTH GENERIC_ATTACK_DAMAGE GENERIC_ATTACK_SPEED GENERIC_MOVEMENT_SPEED GENERIC_STEP_HEIGHT multiplyPositionMatrix BASE_ATTACK_SPEED_MODIFIER_ID BASE_ATTACK_DAMAGE_MODIFIER_ID onClicked onStackClicked postDamageEntity onStoppedUsing postProcessComponents SCREEN_HANDLER ITEM_GROUP createCuboidShape areItemsAndComponentsEqual decrement areEqual setSpriteForAge backgroundHeight focusedSlot backgroundWidth drawMouseoverTooltip drawForeground OFF_HAND_SLOT offerOrDrop pickupType onBlockHitEnchantmentEffects currentScreenHandler playerScreenHandler openHandledScreen onEntityHit onBlockHit registerData ENTITY_FIREWORK_ROCKET_LAUNCH ITEM_TRIDENT_THUNDER ENTITY_ARROW_SHOOT ITEM_TRIDENT_HIT ITEM_TRIDENT_THROW BLOCK_FIRE_EXTINGUISH ENTITY_WIND_CHARGE_THROW BLOCK_ENCHANTMENT_TABLE_USE BLOCK_WOODEN_BUTTON_CLICK_ON ARMOR_TRIMS_ATLAS_TEXTURE jumpKey attackKey useKey hotbarKeys sprintKey sneakKey inventoryKey hudHidden rightKey allKeys backKey forwardKey leftKey runDirectory currentScreen handleInputEvents POSITIVE_Y POSITIVE_Z POSITIVE_X insertItem addProperties onContentChanged onSlotClick sendContentUpdates quickMove vertexConsumers drawTexture drawGuiTexture'.split())
    safe.update(k for k in candidates if k.startswith(('get','set','addStatus','removeStatus','isInCreative','isClient','takeKnockback','stopUsing','createExplosion','wasPressed')))
    members = {k:next(iter(v)) for k,v in candidates.items() if len(v)==1 and k in safe}
    members.update(dict(getWorld='level',getServerWorld='serverLevel',getPos='position',getYaw='getYRot',getPitch='getXRot',setPitch='setXRot',getVelocity='getDeltaMovement',setVelocity='setDeltaMovement',squaredDistanceTo='distanceToSqr',getUuid='getUUID',getMainHandStack='getMainHandItem',getStack='getItem',getBoundingBox='getBoundingBox',getBlockPos='blockPosition',getRotationVector='getLookAngle',getRandom='getRandom',isRemoved='isRemoved',isSpectator='isSpectator',isSneaking='isShiftKeyDown',isOnGround='onGround',spawnEntity='addFreshEntity',spawnParticles='sendParticles',getTime='getGameTime',getPlayers='getPlayerList',isEmpty='isEmpty',getDefaultState='defaultBlockState',getDefaultStack='getDefaultInstance',getRegistryManager='registryAccess',getEntityWorld='level',getRotationClient='getRotation',appendTooltip='appendHoverText',getAttributeValue='getAttributeValue',getAttributeInstance='getAttribute',getEyeHeight='getEyeHeight',getHeight='getBbHeight',getWidth='getBbWidth',getHealth='getHealth',getArmor='getArmorValue',isAttackable='isAttackable',isAlive='isAlive',canHit='isPickable',getOwner='getOwner',getItemUseTime='getTicksUsingItem',getItemUseTimeLeft='getUseItemRemainingTicks',isTouchingWater='isInWater',isSubmergedInWater='isUnderWater',getScreenHandler='getMenu',getData='getData',markDirty='setChanged',canPlayerUse='stillValid',canUse='stillValid',onClosed='removed',getMaxCount='getMaxStackSize',copyAndEmpty='copyAndClear',getCount='getCount',getSoundCategory='getSoundSource',getMovementDirection='getMotionDirection',getHorizontalFacing='getDirection',getDisplayName='getDisplayName'))
    return classes,members

def main():
    target = ROOT / 'src/main/java/com/mechaniciancombat'
    if target.exists() and '--replace-generated' not in __import__('sys').argv:
        raise SystemExit('Refusing to overwrite a source tree that may have been manually ported.')
    classes,members = mappings()
    # Only alter identifier tokens outside comments and string/character literals.
    lexer = re.compile(r'//[^\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*')
    changes = {}
    for path in (SOURCE / 'mod/src/main/java/com/tk/gun').rglob('*.java'):
        content = path.read_text(encoding='utf-8-sig')
        imports = re.findall(r'import\s+([\w.]+);', content)
        simple = {old.rsplit('.',1)[-1]:classes[old].rsplit('.',1)[-1] for old in imports if old in classes}
        # Nested classes can be referred to through their enclosing type.
        def replace(match):
            token=match[0]
            if token.startswith(('//','/*','"',"'")):
                return token
            if token.startswith('com.tk.gun'):
                return token.replace('com.tk.gun','com.mechaniciancombat',1)
            if token.startswith(('net.fabricmc.','software.','dev.','io.','org.','java.','com.github.','net.combat_roll.')):
                return token
            if token in classes:
                return classes[token]
            # Fully qualified Minecraft references may have a trailing field/method.
            if token.startswith('net.minecraft.'):
                parts=token.split('.')
                for split in range(len(parts)-1,2,-1):
                    prefix='.'.join(parts[:split])
                    if prefix in classes:
                        return classes[prefix]+'.'+'.'.join(members.get(x,x) for x in parts[split:])
            # Leave scalar locals alone, except inherited fields and override names.
            return '.'.join(simple.get(part,members.get(part,part)) for part in token.split('.'))
        migrated = lexer.sub(replace,content)
        destination = target / path.relative_to(SOURCE / 'mod/src/main/java/com/tk/gun')
        destination.parent.mkdir(parents=True,exist_ok=True)
        destination.write_text(migrated,encoding='utf-8')
        changes[str(destination.relative_to(ROOT))] = 'API conversion pending'
    resources = SOURCE / 'mod/src/main/resources'
    for path in resources.rglob('*'):
        if not path.is_file() or path.name in ('fabric.mod.json','tkgun.mixins.json','tkgun.camera.mixins.json'):
            continue
        rel = list(path.relative_to(resources).parts)
        if rel[0]=='data':
            if rel[2]=='recipe': rel[2]='recipes'
            elif rel[2]=='loot_table': rel[2]='loot_tables'
            elif rel[2]=='tags' and rel[3]=='item': rel[3]='items'
        dest = ROOT / 'src/main/resources' / Path(*rel)
        dest.parent.mkdir(parents=True,exist_ok=True)
        if rel[0]=='data' and rel[2]=='recipes':
            data=json.loads(path.read_text(encoding='utf-8'))
            if isinstance(data.get('result'),dict) and 'id' in data['result']:
                data['result']['item']=data['result'].pop('id')
            dest.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        else:
            shutil.copyfile(path,dest)
    (ROOT/'docs/SOURCE_MIGRATION.json').write_text(json.dumps(changes,indent=2)+'\n',encoding='utf-8')
    (CACHE/'name-map.json').write_text(json.dumps({'classes':classes,'members':members},indent=2),encoding='utf-8')
    print(f'Prepared {len(changes)} Java files; class/member names are a baseline, not a completed port.')

if __name__=='__main__': main()
