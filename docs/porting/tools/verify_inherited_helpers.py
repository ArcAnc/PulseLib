#!/usr/bin/env python3
"""Verify the behavior retained by the portable inherited API restorations.
ArcAnc, created 11.11.2026, copyright 2026.
Arc's License of Common Sense. See LICENSE.txt at repository root.
"""
import argparse
import base64
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import zipfile

sys.dont_write_bytecode = True

from inventory import ROOT, git
from verify_gpu_facade_api import BODY_TOOL
from verify_resolved_api import digest, java

PARENT = '75645e7de18f291d15a5b6e71d9ee837f7abdf64'
FILES = {
    'AtlasBufferBuilder': 'common/src/main/java/com/arcanc/pulselib/content/model/baked/AtlasBufferBuilder.java',
    'PItemRenderer': 'common/src/main/java/com/arcanc/pulselib/content/renderer/PItemRenderer.java',
    'PEntityRenderer': 'common/src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderer.java',
    'PArmorClientExtensions': 'neoforge/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PArmorClientExtensions.java',
}


def bodies(tool, path):
    text = subprocess.check_output(['java', '-XX:-UsePerfData', str(tool), str(path)], text=True)
    return {key: base64.b64decode(body).decode() for key, body in (line.split('\t') for line in text.splitlines())}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-root', type=Path, default=ROOT)
    parser.add_argument('--canonical-project', type=Path, required=True)
    parser.add_argument('--jars', type=Path, nargs=3, required=True)
    parser.add_argument('--report', type=Path, required=True)
    args = parser.parse_args()
    source_jars = {'minecraft': args.canonical_project / 'common/build/moddev/artifacts/vanilla-1.21.1-20240808.144430-sources.jar',
                   'neoforge': args.canonical_project / 'neoforge/build/moddev/artifacts/neoforge-21.1.216-sources.jar'}
    preserved, added, native = {}, {}, {}
    with tempfile.TemporaryDirectory(prefix='pulselib-inherited-bodies-') as temporary:
        root = Path(temporary)
        tool = root / 'NativeBodies.java'
        tool.write_text(BODY_TOOL)
        for name, path in FILES.items():
            old = root / (name + '.java')
            old.write_bytes(git('show', PARENT + ':' + path))
            before, after = bodies(tool, old), bodies(tool, args.source_root / path)
            if not all(after.get(key) == value for key, value in before.items()):
                raise AssertionError('Existing method body changed: ' + path)
            preserved[path] = {key: hashlib.sha256(body.encode()).hexdigest() for key, body in before.items()}
            added.update({key: value for key, value in after.items() if key not in before})
        for label, entry in [('minecraft', 'com/mojang/blaze3d/vertex/VertexConsumer.java'),
                             ('minecraft', 'net/minecraft/util/FastColor.java'),
                             ('minecraft', 'net/minecraft/server/packs/resources/PreparableReloadListener.java'),
                             ('neoforge', 'net/neoforged/neoforge/client/extensions/common/IClientItemExtensions.java')]:
            with zipfile.ZipFile(source_jars[label]) as jar:
                path = root / Path(entry).name
                path.write_bytes(jar.read(entry))
            native.update(bodies(tool, path))
        name = 'PItemRenderer#getName()'
        overlay = 'PArmorClientExtensions#renderHelmetOverlay(ItemStack,Player,int,int,float)'
        white = 'AtlasBufferBuilder#setWhiteAlpha(int)'
        if set(added) != {name, overlay, white}:
            raise AssertionError('Unexpected added method bodies: ' + repr(added))
        assert added[name] == native['PreparableReloadListener#getName()']
        assert added[overlay] == native['IClientItemExtensions#renderHelmetOverlay(ItemStack,Player,int,int,float)']
        assert added[overlay] == '{ }'
        assert native['VertexConsumer#setWhiteAlpha(int)'] == '{ return this.setColor(FastColor.ARGB32.color(alpha, -1)); }'
        assert native['FastColor.ARGB32#color(int,int)'] == '{ return alpha << 24 | color & 16777215; }'
        assert added[white] == '{ return this.setColor(alpha << 24 | 16777215); }'
        dependency = args.canonical_project / 'common/build/moddev/artifacts/vanilla-1.21.1-20240808.144430.jar'
        with zipfile.ZipFile(dependency) as jar, zipfile.ZipFile(root / 'entity-renderer.jar', 'w') as target:
            entry = 'net/minecraft/client/renderer/entity/EntityRenderer.class'
            target.writestr(entry, jar.read(entry))
        original = json.loads(java('ExtractBinaryApi.java', root / 'entity-renderer.jar'))
        fields = {field['name']: field for field in original['fields'] if field['name'] in {'LEASH_RENDER_STEPS', 'NAMETAG_SCALE'}}
        assert len(fields) == 2
        current = [json.loads(line) for line in java('ExtractBinaryApi.java', *args.jars).splitlines()]
        owners = [row for row in current if row['binaryName'] == 'com/arcanc/pulselib/content/renderer/PEntityRenderer']
        assert len(owners) == 3
        for owner in owners:
            assert all(field in owner['fields'] for field in fields.values())
    result = {'checkpoint': '7.2f', 'status': 'PASS', 'parent': PARENT, 'preservedMethodBodies': preserved,
              'preservedMethodBodyCount': sum(map(len, preserved.values())), 'addedMethodBodies': added,
              'canonicalNativeBodies': {key: native[key] for key in ['VertexConsumer#setWhiteAlpha(int)', 'FastColor.ARGB32#color(int,int)',
                                                                   'PreparableReloadListener#getName()', 'IClientItemExtensions#renderHelmetOverlay(ItemStack,Player,int,int,float)']},
              'restoredConstants': fields, 'constantClassCopies': len(owners),
              'alphaPackingProof': '-1 & 0xFFFFFF equals 0xFFFFFF; the original shift/or expression and virtual setColor(int) dispatch are retained for every int alpha.',
              'canonicalNativeSourceJars': {label: {'path': str(path), 'sha256': digest(path)} for label, path in source_jars.items()},
              'limits': ['The deprecated dimension-only overlay was an empty inherited default. It is not wired as a new automatic native callback.',
                         'getName remains a callable name helper; native item rendering continues to use SpecialModelRenderer and its existing reload owner.']}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(result, indent=2) + '\n')
    print('INHERITED HELPERS PASS:', result['preservedMethodBodyCount'], 'old bodies preserved; three helpers and two constants verified')


if __name__ == '__main__':
    main()
