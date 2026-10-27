"""Reviewed 1.20.1 adaptations of the staged r396 camera and optional hooks."""
from pathlib import Path
import json
ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/com/mechaniciancombat'

def edit(name, replacements):
    p = JAVA / name
    s = p.read_text(encoding='utf-8')
    for old, new in replacements:
        if old not in s:
            raise ValueError(f'Missing reviewed anchor in {name}: {old[:90]}')
        s = s.replace(old, new)
    p.write_text(s, encoding='utf-8')

edit('client/FallbackShoulderCamera.java', [
    ('private static float cameraYaw;\n\tprivate static float cameraPitch;',
     'private static final ShoulderCameraAngles ANGLES = new ShoulderCameraAngles(0, 0);\n\tprivate static LocalPlayer cameraPlayer;'),
    ('&& !client.player.isSpectator()', '&& client.player.isAlive() && !client.player.isSpectator()'),
    ('if (!initialized) {\n\t\t\tcameraYaw = client.player.getYRot();\n\t\t\tcameraPitch = client.player.getXRot();',
     'if (cameraPlayer != client.player) { active = false; initialized = false; }\n\t\tif (!initialized) {\n\t\t\tcameraPlayer = client.player;\n\t\t\tANGLES.reset(client.player.getYRot(), client.player.getXRot());'),
    ('client.getTimer().getGameTimeDeltaTicks()', 'client.getDeltaFrameTime()'),
    ('!isEligible(client)) {\n\t\t\treturn false;\n\t\t}\n\t\tcameraYaw = Mth.wrapDegrees(\n\t\t\t\tcameraYaw + (float) deltaX * 0.15F);\n\t\tcameraPitch = Mth.clamp(\n\t\t\t\tcameraPitch + (float) deltaY * 0.15F, -90.0F, 90.0F);',
     '!isEligible(client) || client.screen != null) {\n\t\t\treturn false;\n\t\t}\n\t\tANGLES.turn(deltaX, deltaY);'),
    ('float targetYaw = Mth.wrapDegrees(\n\t\t\t\tcameraYaw - steer * MAX_RUN_STEER_DEGREES);\n\t\tfloat delta = Mth.wrapDegrees(targetYaw - player.getYRot());\n\t\tfloat step = Mth.clamp(delta,\n\t\t\t\t-RUN_STEER_DEGREES_PER_TICK, RUN_STEER_DEGREES_PER_TICK);\n\t\tfloat yBodyRot = Mth.wrapDegrees(player.getYRot() + step);',
     'float yBodyRot = ShoulderCameraAngles.steerYaw(player.getYRot(), cameraYaw, steer);'),
    ('.multiply(distance)', '.scale(distance)'), ('.multiply(side)', '.scale(side)'),
    ('.raycast(', '.clip('), ('ClipContext.ShapeType.COLLIDER', 'ClipContext.Block.COLLIDER'),
    ('ClipContext.FluidHandling.NONE', 'ClipContext.Fluid.NONE'),
    ('.multiply(safeDistance)', '.scale(safeDistance)'),
    ('active && initialized && client.player != null', 'active && initialized && client.player != null && client.player == cameraPlayer'),
    ('initialized = false;\n\t}', 'initialized = false;\n\t\tcameraPlayer = null;\n\t}'),
])
p = JAVA / 'client/FallbackShoulderCamera.java'
s = p.read_text(encoding='utf-8')
# Keep the public names while reading the tested independent angle state.
import re
s = re.sub(r'\bcameraYaw\b(?!\()', 'ANGLES.yaw()', s)
s = re.sub(r'\bcameraPitch\b(?!\()', 'ANGLES.pitch()', s)
p.write_text(s, encoding='utf-8')

edit('mixin/FallbackShoulderCameraMixin.java', [
    ('method_19327', 'setPosition'), ('method_19325', 'setRotation'),
    ('method_19321(Lnet/minecraft/class_1922;Lnet/minecraft/class_1297;ZZF)V',
     'setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V')])
edit('mixin/FallbackShoulderMouseMixin.java', [('method_5872(DD)V', 'turn(DD)V')])
edit('mixin/FallbackShoulderRunMixin.java', [('method_3129(ZF)V', 'tick(ZF)V')])
edit('client/GunCamera.java', [
    ('lookYaw = camera.getYRot();\n\t\tlookPitch = camera.getXRot();',
     'lookYaw = FallbackShoulderCamera.isFallbackActive(client) ? FallbackShoulderCamera.cameraYaw() : camera.getYRot();\n\t\tlookPitch = FallbackShoulderCamera.isFallbackActive(client) ? FallbackShoulderCamera.cameraPitch() : camera.getXRot();\n\t\tif (FallbackShoulderCamera.isFallbackActive(client)) { p.setXRot(lookPitch); p.xRotO = lookPitch; }'),
    ('&& !ShoulderCouplingState.shoulderCouplingRegistered()', '')])
edit('client/ShoulderCouplingState.java', [
    ('return switch (state.state) {',
     'if (state.state == TkGun.State.IDLE && !TkGunClient.isMoving(id)) return false;\n\t\treturn switch (state.state) {')])
edit('client/TkGunClient.java', [
    ('ShoulderSurfBridge.registerGunCameraBehavior();',
     'if (net.minecraftforge.fml.ModList.get().isLoaded("shouldersurfing")) ShoulderSurfBridge.registerGunCameraBehavior();'),
    ('MOVES.clear();', 'FallbackShoulderCamera.leave(client);\n\t\t\tGunCamera.resetVanillaState();\n\t\t\tMOVES.clear();')])

# Fully reviewed shared roll timings: the source now isolates the optional API.
p = JAVA / 'CombatRollHook.java'
s = (ROOT / 'docs/evidence/r396-port/mapped/new/CombatRollHook.java').read_text(encoding='utf-8')
p.write_text(s, encoding='utf-8')
source = ROOT / 'docs/evidence/r396-port/mapped/new/CombatRollBridge.java'
(JAVA / 'CombatRollBridge.java').write_text(source.read_text(encoding='utf-8').replace('net.combat_roll.', 'net.combatroll.'), encoding='utf-8')
edit('mixin/CombatRollInputGuardMixin.java', [
    ('import net.combatroll.internals.RollManager;', 'import org.spongepowered.asm.mixin.Pseudo;'),
    ('@Mixin(RollManager.class)', '@Pseudo\n@Mixin(targets = "net.combatroll.internals.RollManager")')])
config = ROOT / 'src/main/resources/tkgun.mixins.json'
data = json.loads(config.read_text(encoding='utf-8'))
data['plugin'] = 'com.mechaniciancombat.mixin.OptionalMixinPlugin'
for name in ['FallbackShoulderCameraMixin', 'FallbackShoulderMouseMixin', 'FallbackShoulderRunMixin']:
    if name not in data['client']: data['client'].append(name)
config.write_text(json.dumps(data, indent=2)+'\n', encoding='utf-8')
