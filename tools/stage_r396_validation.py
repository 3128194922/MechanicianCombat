"""Stage isolated r396 validation; never touches the user's launcher/modpack."""
from pathlib import Path
import json
import shutil
import hashlib
import uuid
ROOT=Path(__file__).resolve().parents[1]
V=ROOT/'validation/r396';V.mkdir(parents=True,exist_ok=True)
E=ROOT/'docs/evidence/r396-port'
main=ROOT/'build/libs/MechanicianCombat-1.20.1-forge-0.2.0-r396-porting.jar'
tests=ROOT/'build/libs/MechanicianCombat-1.20.1-forge-0.2.0-r396-porting-gametests.jar'
deps=['geckolib-forge-1.20.1-4.8.4.jar','curios-forge-5.11.1+1.20.1.jar','cloth-config-11.1.136-forge.jar']
for side in ['server','client-a','client-b']:
    dest=V/side/'mods';dest.mkdir(parents=True,exist_ok=True)
    for src in [main]+[ROOT/'libs'/d for d in deps]: shutil.copyfile(src,dest/src.name)
    if side!='server': shutil.copyfile(tests,dest/tests.name)
server=V/'server'
old=ROOT/'validation/forge-47.4.28/server-install'
args=(old/'libraries/net/minecraftforge/forge/1.20.1-47.4.28/win_args.txt').read_text(encoding='utf-8')
args=args.replace('libraries/',(old/'libraries').as_posix()+'/').replace('-DlibraryDirectory=libraries','-DlibraryDirectory='+(old/'libraries').as_posix())
(server/'forge-args.txt').write_text(args,encoding='utf-8')
shutil.copyfile(old/'eula.txt',server/'eula.txt')
(server/'server.properties').write_text('''online-mode=false
server-ip=127.0.0.1
server-port=25576
enable-rcon=false
motd=MechanicianCombat r396 isolated validation
gamemode=creative
level-name=r396-test-world
level-type=minecraft:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
spawn-protection=0
view-distance=4
simulation-distance=4
max-players=4
spawn-monsters=false
''',encoding='utf-8')
oldclient=ROOT/'validation/forge-47.4.28/client-install'
original=oldclient/'target-client-launch.json'
backup=E/'client-launch-original.json'
if not backup.exists(): shutil.copyfile(original,backup)
base=json.loads(backup.read_text(encoding='utf-8-sig'))
for suffix,username in [('a','R396TestA'),('b','R396TestB')]:
    client=V/f'client-{suffix}'
    options=(oldclient/'options.txt').read_text(encoding='utf-8') if (oldclient/'options.txt').exists() else ''
    changes={'fullscreen':'false','pauseOnLostFocus':'false','renderDistance':'4','simulationDistance':'4','guiScale':'2','onboardAccessibility':'false','lang':'zh_cn'}
    lines=options.splitlines();lines=[line for line in lines if line.partition(':')[0] not in changes]
    lines += [k+':'+v for k,v in changes.items()]
    (client/'options.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    config=dict(base);argv=list(base['arguments'])
    for flag,value in [('--gameDir',str(client)),('--username',username),('--uuid',str(uuid.UUID(bytes=hashlib.md5(('OfflinePlayer:'+username).encode()).digest(),version=3))),('--accessToken','0')]:
        if flag in argv: argv[argv.index(flag)+1]=value
    argv=[a for a in argv if not a.startswith('-Dtkgun.')]
    argv[:0]=['-Dtkgun.gametest.only=true','-Dtkgun.r396.inputlog='+str(E/f'client-{suffix}-input.csv'),'-Duser.language=en','-Duser.country=US']
    argv += ['--quickPlayMultiplayer','127.0.0.1:25576']
    config.update(arguments=argv,workingDirectory=str(client),stdout=str(E/f'client-{suffix}.out.log'),stderr=str(E/f'client-{suffix}.err.log'))
    (E/f'client-{suffix}-launch.json').write_text(json.dumps(config,ensure_ascii=False,indent=2),encoding='utf-8')
shutil.copyfile(E/'client-a-launch.json',original)
dist=ROOT/'dist';dist.mkdir(exist_ok=True);shutil.copyfile(main,dist/main.name)
(E/'candidate-sha256.txt').write_text(hashlib.sha256(main.read_bytes()).hexdigest()+'  '+main.name+'\n',encoding='utf-8')
print('Staged isolated server and two clients with GeckoLib/Curios/Cloth Config; no Shoulder Surfing, PAL or Combat Roll.')
