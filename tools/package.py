#!/usr/bin/env python3
"""Builds the Factory Ascension downloads into dist/:

  FactoryAscension-Full-v<v>-MC1.20.1.zip             kubejs folder + mods/factoryascension jar (the blocks and machines)
  FactoryAscension-Half-v<v>-MC1.20.1.zip             kubejs folder only (everything except the mod blocks)
  FactoryAscension-Full-v<v>-MC1.20.1-CurseForge.zip  import into the CurseForge app: downloads the mods, adds kubejs + the jar
  FactoryAscension-Half-v<v>-MC1.20.1-CurseForge.zip  same, without the jar
The pack's own version (v19.0.0 ...) always comes with the Minecraft version it's for, so the two can't be mixed up.

Mod ids for the CurseForge manifests come from tools/curseforge-mods.json (the versions the test server runs).
Builds the jar first if mod/build/libs doesn't have it.

  python3 tools/package.py [version]
"""
import json, os, subprocess, sys, zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PROPS = dict(l.strip().split('=', 1) for l in open(os.path.join(ROOT, 'mod/gradle.properties')) if '=' in l and not l.startswith('#'))
VERSION = (sys.argv[1] if len(sys.argv) > 1 else PROPS['mod_version']).lstrip('v')
MC = PROPS['minecraft_version']
DIST = os.path.join(ROOT, 'dist')


def mod_jar():
    jar = os.path.join(ROOT, 'mod/build/libs', 'factoryascension-mc%s-%s.jar' % (MC, PROPS['mod_version']))
    if not os.path.exists(jar):
        env = dict(os.environ, JAVA_HOME=os.environ.get('JAVA_HOME', os.path.expanduser('~/fa-mod-tools/jdk')))
        subprocess.run(['./gradlew', 'build', '--no-daemon'], cwd=os.path.join(ROOT, 'mod'), env=env, check=True)
    return jar


def add_tree(z, src, prefix):
    for base, dirs, files in os.walk(src):
        dirs.sort()
        for f in sorted(files):
            path = os.path.join(base, f)
            z.write(path, os.path.join(prefix, os.path.relpath(path, src)))


def manifest(full):
    cf = json.load(open(os.path.join(ROOT, 'tools/curseforge-mods.json')))
    return {
        'minecraft': {'version': cf['minecraft'], 'modLoaders': [{'id': 'forge-' + cf['forge'], 'primary': True}]},
        'manifestType': 'minecraftModpack', 'manifestVersion': 1,
        'name': 'Factory Ascension' + ('' if full else ' (Half)') + ' for Minecraft ' + MC, 'version': 'v' + VERSION, 'author': 'thenameiscobmarley',
        'files': [{'projectID': m['projectID'], 'fileID': m['fileID'], 'required': True} for m in cf['mods']],
        'overrides': 'overrides',
    }


def build(full, curseforge, jar):
    name = 'FactoryAscension-%s-v%s-MC%s%s.zip' % ('Full' if full else 'Half', VERSION, MC, '-CurseForge' if curseforge else '')
    out = os.path.join(DIST, name)
    with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
        base = 'overrides/' if curseforge else ''
        if curseforge:
            z.writestr('manifest.json', json.dumps(manifest(full), indent=2))
        add_tree(z, os.path.join(ROOT, 'kubejs'), base + 'kubejs')
        # The FTB Quests quest book (tools/make_quests.py): the Half one skips the mod's blocks
        add_tree(z, os.path.join(ROOT, 'ftbquests', 'full' if full else 'half', 'quests'), base + 'config/ftbquests/quests')
        if full:
            z.write(jar, base + 'mods/' + os.path.basename(jar))
        if not curseforge:
            for f in ('INSTALL.md', 'MODLIST.md', 'LICENSE'):
                z.write(os.path.join(ROOT, f), f)
    print('%-48s %6d KB' % (name, os.path.getsize(out) // 1024))


os.makedirs(DIST, exist_ok=True)
jar = mod_jar()
for full in (True, False):
    for cf in (False, True):
        build(full, cf, jar)
