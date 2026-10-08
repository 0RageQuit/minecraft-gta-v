"""Assemble a Melty release from rebuilt artifacts and verified official dependencies.

Never copy a game install, launcher accounts, saves or screenshots into this stage.
"""
import hashlib
import json
import re
import shutil
import sys
import zipfile
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[1]
WORK = PROJECT.parent
DOWNLOADS = WORK / 'downloads'
VERSION = '0.1.3-melty'
MOD_VERSION = '0.1.1-melty'  # Gameplay components are unchanged in this packaging fix.
STAGE = WORK / ('melty-stage-' + VERSION)
OUTPUTS = WORK.parent / 'outputs'

def copy(source, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, destination)

def extract(source, destination, prefix=''):
    with zipfile.ZipFile(source) as archive:
        for entry in archive.infolist():
            path = Path(entry.filename)
            if path.is_absolute() or '..' in path.parts:
                raise ValueError('Unsafe dependency archive path')
            if prefix:
                if not entry.filename.startswith(prefix):
                    continue
                path = Path(entry.filename[len(prefix):])
            if entry.is_dir() or not str(path) or str(path) == '.':
                continue
            target = destination / path
            target.parent.mkdir(parents=True, exist_ok=True)
            with archive.open(entry) as src, target.open('wb') as dst:
                shutil.copyfileobj(src, dst)

def digest(path, algorithm='sha256'):
    h = hashlib.new(algorithm)
    with path.open('rb') as source:
        for chunk in iter(lambda: source.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()

def main():
    if STAGE.exists():
        raise SystemExit('Stage already exists: inspect it or choose a new version; no automatic deletion.')
    assert digest(DOWNLOADS/'java25-jre.zip') == '4c95451cea98556def2c54f7782933f52a26d4a36bd85e1d59f0364464828b07'
    java_source_hash = (DOWNLOADS/'java25-source.sha256.txt').read_text().split()[0]
    assert digest(DOWNLOADS/'java25-source.tar.gz') == java_source_hash
    assert digest(DOWNLOADS/'fabric-api-0.161.0+26.3.jar', 'sha512') == 'ed6b2586d6fde11fde8472f5a527c51e99b67026e46f94d4bfd85e7e28ce5ee299173ee16ad576ceb51f39f98d30a811086a6deb1a86a524859cc16e12da109d'
    STAGE.mkdir()
    gta = STAGE/'gta'
    companion = STAGE/'companion'
    docs = STAGE/'docs'
    copy(PROJECT/'gta/build/MCPassthrough.asi', gta/'MCPassthrough.asi')
    copy(DOWNLOADS/'reshade-runtime/ReShade64.dll', gta/'ReShade64.asi')
    for name in ('ReShade.ini', 'ReShadePreset.ini'):
        copy(PROJECT/'packaging/gta-config'/name, gta/name)
    for name in ('MCPassthrough.fx',):
        copy(PROJECT/'gta/shaders'/name, gta/'reshade-shaders/Shaders'/name)
    for name in ('ReShade.fxh',):
        copy(DOWNLOADS/name, gta/'reshade-shaders/Shaders'/name)
    # Preserve the GPL-licensed mapping's complete preferred source and license.
    mapping_text=(PROJECT/'docs/native-mapping/crossmap-original.hpp').read_text()
    pairs=dict(re.findall(r'\{(0x[0-9A-F]+),\s*(0x[0-9A-F]+)\}', mapping_text))
    required=set()
    for name in ('natives.h', 'native_runtime.hpp', 'native_runtime.cpp'):
        required.update(re.findall(r'invoke<[^>]+>\((0x[0-9A-F]+)', (PROJECT/'gta/src'/name).read_text()))
    required.add('0x4EDE34FBADD967A6')
    missing=required-set(pairs)
    if missing:
        raise ValueError('Missing native mappings: '+repr(sorted(missing)))
    mapping=gta/'MCPassthrough/native-map.csv'
    mapping.parent.mkdir(parents=True)
    mapping.write_text(''.join(a+','+pairs[a]+'\n' for a in sorted(required)))
    copy(PROJECT/'docs/native-mapping/crossmap-original.hpp', docs/'native-mapping/crossmap-original.hpp')
    copy(PROJECT/'docs/native-mapping/GPL-2.0.txt', docs/'native-mapping/GPL-2.0.txt')
    copy(PROJECT/'LICENSE', docs/'Minecraft-GTA-V-MIT.txt')
    copy(PROJECT/'mc/src/main/resources/LICENSE-minecraft-ring', docs/'Minecraft-Ring-MIT.txt')
    copy(PROJECT/'gta/third_party/minhook/LICENSE.txt', docs/'MinHook-BSD.txt')
    copy(PROJECT/'docs/licenses/GTAVE-reference-MIT.txt', docs/'GTAVE-reference-MIT.txt')
    for name in ('ReShade', 'Prism', 'Fabric-API'):
        copy(DOWNLOADS/(name+'-LICENSE.txt'), docs/(name+'-LICENSE.txt'))
    (docs/'Shader-includes.txt').write_text('ReShade.fxh: crosire/reshade-shaders, CC0-1.0. SPDX notice retained in the file.\n')
    copy(DOWNLOADS/'PrismLauncher-11.1.1.tar.gz', docs/'sources/PrismLauncher-11.1.1.tar.gz')
    copy(DOWNLOADS/'java25-source.tar.gz', docs/'sources/Temurin-25.0.4.1-source.tar.gz')
    copy(DOWNLOADS/'java25-source.sha256.txt', docs/'sources/Temurin-25.0.4.1-source.sha256.txt')
    copy(DOWNLOADS/'Qt-LGPL-3.0.txt', docs/'Qt-LGPL-3.0.txt')
    (docs/'Qt-notices.txt').write_text('Prism portable includes dynamically linked Qt 6.11.2 libraries and plugins, Qt contributors. LGPL-3.0 text included. Corresponding official source: https://download.qt.io/official_releases/qt/6.11/6.11.2/single/ . These shared libraries may be replaced with compatible modified Qt libraries; they are not statically linked into our companion helper.\n')
    extract(DOWNLOADS/'prism-portable.zip', companion/'launcher')
    with zipfile.ZipFile(DOWNLOADS/'java25-jre.zip') as archive:
        java_prefix=archive.namelist()[0].split('/')[0]+'/'
    extract(DOWNLOADS/'java25-jre.zip', companion/'java', java_prefix)
    copy(PROJECT/'packaging/MinecraftCompanion.exe', companion/'MinecraftCompanion.exe')
    jars=list((PROJECT/'mc/build/libs').glob('passthrough-'+MOD_VERSION+'.jar'))
    if len(jars) != 1:
        raise ValueError('Minecraft rebuilt artifact not found')
    copy(jars[0], companion/'minecraft/mods'/jars[0].name)
    copy(DOWNLOADS/'fabric-api-0.161.0+26.3.jar', companion/'minecraft/mods/fabric-api-0.161.0+26.3.jar')
    (companion/'minecraft/mmc-pack.json').write_text(json.dumps({
        'formatVersion':1, 'components':[
            {'uid':'net.minecraft','version':'26.3','important':True},
            {'uid':'net.fabricmc.fabric-loader','version':'0.19.5'}
        ]},indent=2))
    credits='''Minecraft GTA V: Rehan/universal-modder contributors; Minecraft Ring adaptations: siddoff and justbustin (MIT).
Native adapter: new independent implementation; ABI approach studied in jason920612/GTAVE-ModLoader (MIT notice retained).
Native mapping data: TupoyeMenu/BigBaseV2-fix, GPL-2.0; complete unmodified preferred source and GPL text included in native-mapping/.
MinHook 1.3.4: Tsuda Kageyu and contributors (BSD).
ReShade 6.8.0: Patrick Mours/crosire (BSD); official shader includes retain their CC0 notices.
Prism Launcher 11.1.1: Prism Launcher contributors (GPL-3.0). Official corresponding source: https://github.com/PrismLauncher/PrismLauncher/tree/11.1.1
Prism source archive: https://github.com/PrismLauncher/PrismLauncher/archive/refs/tags/11.1.1.zip
Eclipse Temurin Java 25.0.4.1+1: OpenJDK and Eclipse Adoptium contributors. Full component notices and GPL/classpath-exception texts are included under companion/java/legal/; matching official source archive and checksum are included under docs/sources/.
Temurin corresponding source: https://github.com/adoptium/jdk25u/tree/jdk-25.0.4.1+1
Fabric Loader/Fabric API: FabricMC contributors (Apache-2.0). Loader is specified in the portable instance for Prism's managed setup; API is bundled with its notice.
Java-WebSocket: TooTallNate and contributors (MIT); nested dependency jar retains its notices.
No proprietary ScriptHookV runtime/SDK or game-owned models, textures, sounds, levels, accounts, tokens, skins or saves are included.
'''
    (docs/'CREDITS.txt').write_text(credits)
    (docs/'README.md').write_text('''# Minecraft GTA V — Melty development candidate

Real Minecraft Java runs beside GTA V Legacy Story Mode: build in Los Santos, use Minecraft movement and inventory, and fly with an elytra. The gameplay feature list remains in the project README.

Requires the player's own GTA V Legacy build 3889 and Minecraft Java 26.3 entitlement. Single player; Enhanced and GTA Online are unsupported.

Melty places all release files, installs Ultimate ASI Loader, starts the bundled configured Minecraft companion and launches GTA in Story Mode with BattlEye disabled. Rockstar can connect normally to activate the player's Steam copy; offline mode is not forced. The first Minecraft setup uses Prism's normal Microsoft sign-in. The release contains no player credentials or game content. Dedicated worlds and launcher data remain under the player's LocalAppData/MinecraftGTAV folder.

This build removes the ScriptHookV import and bundles portable Prism, Java, Fabric API, ReShade and shader includes. The revised GTA script-loop adapter was tested in GTA Legacy without ScriptHookV loaded: real Minecraft gameplay was visible, movement and menus worked, and the revised test passed the monitored stability interval. Melty's own install-and-Play verification is still required before publication. Unsupported native signatures disable the adapter and write native-adapter.log.

Controls: WASD/mouse, Space jump, Ctrl sprint, Shift sneak, E inventory, F5 perspective, F8 switch Minecraft/GTA controls, F7 view toggle, F9 relevel.

See CREDITS.txt and the included component licenses.
''' + '\n## Project feature list\n\n' + (PROJECT/'README.md').read_text().split('## Current features\n',1)[1].split('## Controls',1)[0])
    filename='Minecraft-GTA-V-'+VERSION+'.zip'
    root='{localappdata}/MinecraftGTAV'
    runtime=root+'/runtime'
    launcher={'kind':'exe','path':runtime+'/MinecraftCompanion.exe'}
    recipe={
        'schemaVersion':1,'mode':'installed',
        'games':[{'slug':'gta-v','role':'primary','version':'=1.0.3889.0'}, {'slug':'minecraft-java','role':'companion','version':'=26.3'}],
        'components':[{'id':'main','fileName':filename,'label':'Minecraft GTA V and bundled dependencies','kind':'main','required':True}],
        'requirements':[{'kind':'loader','id':'ultimate-asi-loader'}],
        'mappings':[{'component':'main','from':'gta/','to':'{game}'},{'component':'main','from':'companion/','to':runtime},{'component':'main','from':'docs/','to':root+'/docs'},{'component':'main','from':'melty.json','to':root+'/docs'}],
        'setup':{'label':'Minecraft','launch':launcher,'done':{'file':root+'/prism/instances/minecraft-gta-v/.minecraft/logs/latest.log','contains':'Minecraft companion ready'},'stopWhenDone':False},
        'together':[{'launch':launcher,'startFirst':True,'waitSeconds':5}],
        'launch':{'kind':'game','args':['-nobattleye']},
        'runtimeData':[root],
        'notes':{'firstLaunch':'Sign in to your own Minecraft account in the bundled Prism launcher during the first setup. Use Story Mode only.'}
    }
    (PROJECT/'melty.json').write_text(json.dumps(recipe,indent=2))
    (STAGE/'melty.json').write_text(json.dumps(recipe,indent=2))
    OUTPUTS.mkdir(exist_ok=True)
    archive_path=OUTPUTS/filename
    entries=[]
    files={}
    with zipfile.ZipFile(archive_path,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as archive:
        for file in sorted(STAGE.rglob('*')):
            if not file.is_file(): continue
            path=file.relative_to(STAGE).as_posix()
            archive.write(file,path)
            entries.append({'path':path,'size':file.stat().st_size})
            if path in ('melty.json','docs/README.md','docs/CREDITS.txt','companion/minecraft/mmc-pack.json','gta/ReShade.ini','gta/ReShadePreset.ini'):
                files[path]=file.read_text()
    payload={'fileName':filename,'entries':entries,'files':files,'gameSlugs':['gta-v','minecraft-java'],'says':(PROJECT/'README.md').read_text()}
    (WORK/'melty-package-input.json').write_text(json.dumps(payload,indent=2))
    print(json.dumps({'archive':str(archive_path),'bytes':archive_path.stat().st_size,'sha256':digest(archive_path),'entries':len(entries),'requiredNatives':len(required)}))

if __name__=='__main__': main()
