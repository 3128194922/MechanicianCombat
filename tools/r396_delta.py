"""Stage r349->r396 deltas without overwriting reviewed Forge implementations.

Reference trees are read-only. Apply only explicitly selected files. Conflicting
hunks are written to the evidence directory for manual adaptation.
"""
from pathlib import Path
import ast
import difflib
import hashlib
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
OLD = ROOT.parent / 'MechanicianCombat-source-r349-20261025_030512/mod/src/main'
NEW = ROOT.parent / 'MechanicianCombat-r396-source/MechanicianCombat-source-r396/mod/src/main'
JAVA = ROOT / 'src/main/java/com/mechaniciancombat'
OUT = ROOT / 'docs/evidence/r396-port'
mapping = json.loads((ROOT / 'tools/cache/name-map.json').read_text(encoding='utf-8'))
classes, members = mapping['classes'], mapping['members']
members.update({'getWorld': 'level', 'getTime': 'getGameTime', 'isClient': 'isClientSide',
                'movementForward': 'forwardImpulse', 'movementSideways': 'leftImpulse',
                'forwardKey': 'keyUp', 'backKey': 'keyDown', 'leftKey': 'keyLeft',
                'rightKey': 'keyRight', 'sprintKey': 'keySprint', 'prevYaw': 'yRotO',
                'prevPitch': 'xRotO', 'getLerpedPos': 'getPosition'})
tree = ast.parse((ROOT / 'tools/port-common.py').read_text(encoding='utf-8-sig'))
replacements = ast.literal_eval(next(n.value for n in tree.body if isinstance(n, ast.Assign)
                                   and any(isinstance(t, ast.Name) and t.id == 'replacements' for t in n.targets)))
lexer = re.compile(r'//[^\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*')

def convert(content):
    imports = re.findall(r'import\s+([\w.]+);', content)
    simple = {old.rsplit('.', 1)[-1]: classes[old].rsplit('.', 1)[-1] for old in imports if old in classes}
    def token_replace(match):
        token = match[0]
        if token.startswith(('//', '/*', '"', "'")):
            return token
        if token.startswith('com.tk.gun'):
            return token.replace('com.tk.gun', 'com.mechaniciancombat', 1)
        if token.startswith(('net.fabricmc.', 'software.', 'dev.', 'io.', 'org.', 'java.', 'com.github.', 'net.combat_roll.')):
            return token
        if token in classes:
            return classes[token]
        if token.startswith('net.minecraft.'):
            parts = token.split('.')
            for split in range(len(parts)-1, 2, -1):
                prefix = '.'.join(parts[:split])
                if prefix in classes:
                    return classes[prefix] + '.' + '.'.join(members.get(p, p) for p in parts[split:])
        return '.'.join(simple.get(p, members.get(p, p)) for p in token.split('.'))
    s = lexer.sub(token_replace, content)
    for old, new in replacements.items():
        if old == '.world':
            s = re.sub(r'(?<![\w.])(client|mc|minecraft)\.world\b', lambda m: m[1]+'.level', s)
        else:
            s = s.replace(old, new)
    s = re.sub(r'\bSettings\b', 'Properties', s)
    s = re.sub(r'(BuiltInRegistries\.[A-Z_]+)\s*\.getId\(', r'\1.getKey(', s)
    s = s.replace('client.currentScreen', 'client.screen').replace('client.options.getPerspective()', 'client.options.getCameraType()')
    s = s.replace('client.options.setPerspective(', 'client.options.setCameraType(')
    s = s.replace('net.minecraft.client.option.Perspective', 'net.minecraft.client.CameraType')
    s = re.sub(r'\bPerspective\b', 'CameraType', s) if 'import net.minecraft.client.CameraType;' in s else s
    s = s.replace('FabricLoader.getInstance().isModLoaded(', 'net.minecraftforge.fml.ModList.get().isLoaded(')
    s = s.replace('net.fabricmc.loader.api.net.minecraftforge.fml.ModList', 'net.minecraftforge.fml.ModList')
    s = s.replace('import net.fabricmc.loader.api.FabricLoader;', '')
    s = s.replace('client.gameRenderer.getCamera()', 'client.gameRenderer.getMainCamera()')
    s = s.replace('client.options.hudHidden', 'client.options.hideGui')
    s = s.replace('client.world', 'client.level')
    s = s.replace('getScaledWindowWidth()', 'guiWidth()').replace('getScaledWindowHeight()', 'guiHeight()')
    s = s.replace('client.textRenderer', 'client.font').replace('.drawText(', '.drawString(')
    s = s.replace('.getWidth(', '.width(').replace('.getHeight(', '.getHeight(')
    s = s.replace('ClientPlayNetworking.', 'ClientNetwork.').replace('ServerPlayNetworking.', 'ModNetwork.')
    s = s.replace('getRegistryManager()', 'registryAccess()')
    return s

def stage():
    rows = []
    for path in NEW.rglob('*'):
        if not path.is_file():
            continue
        rel = path.relative_to(NEW)
        old = OLD / rel
        if old.exists() and old.read_bytes() == path.read_bytes():
            continue
        rows.append({'path': rel.as_posix(), 'kind': 'changed' if old.exists() else 'added',
                     'sourceSha256': hashlib.sha256(path.read_bytes()).hexdigest(), 'status': 'pending'})
        if path.suffix == '.java':
            short = path.relative_to(NEW / 'java/com/tk/gun')
            for label, source in [('old', old), ('new', path)]:
                if source.exists():
                    dest = OUT / 'mapped' / label / short
                    dest.parent.mkdir(parents=True, exist_ok=True)
                    dest.write_text(convert(source.read_text(encoding='utf-8-sig')), encoding='utf-8')
    (OUT / 'source-delta.json').write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'Staged {len(rows)} changed/new source entries; no production files modified.')

def apply(rel):
    old = OUT / 'mapped/old' / rel
    new = OUT / 'mapped/new' / rel
    target = JAVA / rel
    if not target.exists():
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(new.read_text(encoding='utf-8'), encoding='utf-8')
        print(f'{rel}: new file, API review required')
        return
    a = old.read_text(encoding='utf-8').splitlines(True)
    b = new.read_text(encoding='utf-8').splitlines(True)
    current = target.read_text(encoding='utf-8').splitlines(True)
    rejected, applied = [], 0
    norm = lambda lines: [line.strip() for line in lines]
    for kind, i, j, x, y in reversed(difflib.SequenceMatcher(None, a, b, autojunk=False).get_opcodes()):
        if kind == 'equal':
            continue
        expected, replacement = a[i:j], b[x:y]
        nc, ne = norm(current), norm(expected)
        matches = [n for n in range(len(current)-len(expected)+1) if nc[n:n+len(expected)] == ne] if expected else []
        if not expected:
            before, after = norm(a[max(0, i-3):i]), norm(a[i:i+3])
            matches = [n for n in range(len(current)+1) if nc[max(0,n-len(before)):n] == before and nc[n:n+len(after)] == after]
        if len(matches) == 1:
            n = matches[0]
            current[n:n+len(expected)] = replacement
            applied += 1
        else:
            rejected.append({'oldLine': i+1, 'kind': kind, 'old': ''.join(expected), 'new': ''.join(replacement),
                             'before': ''.join(a[max(0,i-3):i]), 'after': ''.join(a[j:j+3])})
    target.write_text(''.join(current), encoding='utf-8')
    reject = OUT / 'conflicts' / (rel.replace('/', '_') + '.json')
    reject.parent.mkdir(parents=True, exist_ok=True)
    reject.write_text(json.dumps(rejected, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'{rel}: {applied} exact deltas applied, {len(rejected)} require review')

if __name__ == '__main__':
    OUT.mkdir(parents=True, exist_ok=True)
    if sys.argv[1:] == ['stage']:
        stage()
    elif len(sys.argv) > 2 and sys.argv[1] == 'apply':
        for rel in sys.argv[2:]:
            apply(rel)
    else:
        raise SystemExit('Usage: r396_delta.py stage | apply relative.java ...')
