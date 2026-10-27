"""Resolve reviewed integration conflicts without replacing Forge-adapted classes."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]; J=ROOT/'src/main/java/com/mechaniciancombat'; E=ROOT/'docs/evidence/r396-port'

def change(name,old,new,count=-1):
    p=J/name;s=p.read_text(encoding='utf-8')
    if old not in s: raise ValueError(f'{name}: {old[:70]}')
    p.write_text(s.replace(old,new,count),encoding='utf-8')

for name in ['entity/GrapeshotVolley.java','entity/MoltenTridentEntity.java']:
    change(name,'package com.mechaniciancombat.entity;','package com.mechaniciancombat.entity;\n\nimport com.mechaniciancombat.CombatTargeting;')
phases=json.loads((E/'conflicts/net_CombatBackpackStatusPayload.java.json').read_text(encoding='utf-8'))[0]['new']
change('net/CombatBackpackStatusPayload.java','\tpublic static final int PHASE_AUTO_PAUSED = 4;','\tpublic static final int PHASE_AUTO_PAUSED = 4;\n'+phases)

# Source anti-friendly-fire applies to every equivalent attack-area predicate.
change('TkGunServer.java','e -> e != player && e.isAlive() && !e.isSpectator() && e.isPickable()',
       'e -> e != player && e.isAlive() && !e.isSpectator() && e.isPickable() && !CombatTargeting.isGolem(e)')
p=J/'TkGunServer.java';s=p.read_text(encoding='utf-8')
conflicts=json.loads((E/'conflicts/TkGunServer.java.json').read_text(encoding='utf-8'))
# resetBackpackCharge distinguishes the antenna's existing-golem cooldown.
a=s.index('private static BackpackCharge backpackState(');b=s.index('\n\t}',a)
section=s[a:b];assert 'state.cooldownUntil = 0;' in section
section=section.replace('state.cooldownUntil = 0;',conflicts[5]['new'].strip())
s=s[:a]+section+s[b:]
# Tag both launcher projectile creation paths, without changing native wind charges.
s=s.replace('Entity projectile;', 'Entity projectile;')
for marker in ['private static void launchBackpackProjectile(', 'private static void launchAutoBackpackProjectile(']:
    if marker not in s: continue
    a=s.index(marker);b=s.index('\n\t}',a)
    section=s[a:b]
    if 'projectile.addTag(CombatTargeting.BACKPACK_PROJECTILE_TAG)' not in section:
        section=section.replace('projectile.setDeltaMovement(', 'projectile.setDeltaMovement(')
        # After type selection, the owner assignment marks a fully created projectile.
        section=section.replace('projectile.setOwner(player);','projectile.addTag(CombatTargeting.BACKPACK_PROJECTILE_TAG);\n\t\tprojectile.setOwner(player);')
    s=s[:a]+section+s[b:]
p.write_text(s,encoding='utf-8')

# GeckoLib 4.8.4 uses float color components instead of the 1.21 packed color.
p=J/'client/CombatBackpackGeoRenderer.java';s=p.read_text(encoding='utf-8')
anchor='boolean autoLauncher = attachedLauncherType\n\t\t\t\t== BackpackUpgradeData.LauncherType.AUTO;'
# Only add the missing declaration in the recursive render method.
a=s.index('public void renderRecursively(');b=s.index('\n\t}',a)
section=s[a:b]
if 'boolean summonLauncher' not in section:
    assert anchor in section;section=section.replace(anchor,anchor+'\n\t\tboolean summonLauncher = attachedLauncherType == BackpackUpgradeData.LauncherType.SUMMON_ANTENNA;')
s=s[:a]+section+s[b:]
a=s.index('public void renderMaintenancePreview(') if 'public void renderMaintenancePreview(' in s else -1
if a>=0:
    b=s.index('CombatBackpackAnimation.beginModificationPreview();',a)
    s=s[:b]+'''this.attachedLauncherType = BackpackUpgradeData.canUseLauncher(stack)
                ? BackpackUpgradeData.launcherType(stack) : BackpackUpgradeData.LauncherType.NONE;
        this.attachedCoreInstalled = BackpackUpgradeData.hasCore(stack);
        '''+s[b:]
p.write_text(s,encoding='utf-8')

source=ROOT.parent/'MechanicianCombat-r396-source/MechanicianCombat-source-r396/mod/src/main'
for row in json.loads((E/'source-delta.json').read_text(encoding='utf-8')):
    rel=row['path']
    if not rel.startswith('resources/assets/'): continue
    src=source/rel;dst=ROOT/'src/main'/rel;dst.parent.mkdir(parents=True,exist_ok=True)
    if '/lang/' in rel and dst.exists():
        merged=json.loads(dst.read_text(encoding='utf-8-sig'));merged.update(json.loads(src.read_text(encoding='utf-8-sig')))
        dst.write_text(json.dumps(merged,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    else: dst.write_bytes(src.read_bytes())

change('net/ModNetwork.java','forge-r349-1','forge-r396-1')
p=ROOT/'gradle.properties';s=p.read_text(encoding='utf-8').replace('mod_version=0.1.0-porting','mod_version=0.2.0-r396-porting');p.write_text(s,encoding='utf-8')
