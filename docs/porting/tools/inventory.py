#!/usr/bin/env python3
"""PulseLib stage 0 inventory. ArcAnc, created 21.10.2026, copyright 2026.
Licensed under Arc's License of Common Sense; see LICENSE.txt at repository root.

Parse pinned Git objects, never the working source tree. No Gradle/network needed.
"""

import argparse
import csv
import hashlib
import io
import json
from pathlib import Path
import re
import subprocess
import tarfile
import tempfile


ROOT = Path(__file__).resolve().parents[3]
OUTPUT = ROOT / 'docs/porting/26.1.2-multiloader'
SOURCES = {
    '26.1': '67bea7ccc6bd1d4d15f417179ef82d5a7b8aacb1',
    '1.21.1-multiloader': '7ce6f024b307bb821ce543344d3896c8e1fcf26b',
}
PREFIX = 'com.arcanc.pulselib.'


def git(*args):
    return subprocess.check_output(['git', '-C', str(ROOT), *args])


def lines(rows):
    return ''.join(json.dumps(row, ensure_ascii=False, separators=(',', ':')) + '\n' for row in rows)


def api_shape(row):
    """Declaration comparison; imports participate, even when a short type name is equal."""
    keys = ['kind', 'modifiers', 'type_parameters', 'extends', 'implements', 'permits', 'record_components', 'annotations']
    result = {key: row[key] for key in keys}
    result['members'] = sorted(member['signature'] for member in row['members'])
    signature_text = json.dumps(result)
    result['signature_imports'] = [value for value in row['imports']
                                   if value.endswith('.*') or re.search(r'\b' + re.escape(value.split('.')[-1]) + r'\b', signature_text)]
    return result


def policy(fqcn, base, modern):
    """Reviewed subsystem policies, not inferred equivalence from source filenames."""
    name = fqcn.removeprefix(PREFIX)
    if name.startswith(('api.', 'plugin.')):
        return '2', 'common', 'modern-plugin-contract', 'PulsePluginManager/Lifecycle; side filtering before class loading'
    if name.startswith('platform.'):
        return '2', 'common', 'modern-service-contract', 'One ServiceLoader provider per loader; client services isolated'
    if name.startswith(('fabric.', 'neoforge.')):
        return ('6' if name.startswith('fabric.') else '2'), name.split('.')[0], 'adapt-loader', '26.1.2 loader hooks and service descriptors'
    if name.startswith('content.renderer.gl.'):
        return '4', 'common-client', 'preserve-26.1-gpu', 'Retain algorithms; implement modern PRenderExecutor and resource handles'
    if name.startswith('content.renderer.legacy.'):
        return '4', 'common-client', 'compatibility-review', 'Public API is canonical; keep adapters where possible; no second legacy GPU backend'
    if name.startswith('content.renderer.base.'):
        return '1/4', 'common-client', 'preserve-26.1-state', 'Entity/BlockEntity/Item render states and SubmitNodeCollector'
    if name.startswith('content.renderer.plan.'):
        return '4', 'common-client', 'modern-render-plan', 'One compiler; opaque pass, global alpha sort, separate OIT groups'
    if name.startswith('content.renderer.'):
        return '4', 'common-client', 'adapt-modern-render-contract', 'Preserve modern API where possible; 26.1 state extraction/submission adapters'
    if name.startswith(('content.player.', 'util.attachments.')):
        return '5', ('neoforge' if name.startswith('util.attachments.humanoid.armor.PArmorClientExtensions') else 'common-client'), 'modern-player-attachments', 'M-P2/M-P3 spaces; 26.1 Mixins/render states; loader armor hooks'
    if name.startswith('content.mixin.'):
        return ('3' if name.startswith('content.mixin.ItemStackMixin') else '4/5/6'), 'common-or-loader-client', 'review-injection-target', 'Use 26.1 target descriptors; port missing modern behavior, not old injection sites'
    if name.startswith('content.animatable.'):
        return '3', 'common', 'modern-animation-manager', 'Lazy initialization, owner/key isolation, runtime ItemStack IDs, refreshed timestamps'
    if name.startswith(('content.model.animation.', 'data.gecko.', 'data.gltf.')):
        return '3', 'common', 'modern-animation-core', 'Seconds/time context, Catmull-Rom, Euler/quaternion decoding, reverse/cyclic events, snapshots'
    if name.startswith(('content.model.baked.', 'content.model.deformer.')):
        return '3/4', 'common-client', 'modern-pose-26.1-gpu', 'One PPose evaluation; preserve 26.1 buffers, deformation and subdivision behavior'
    if name.startswith('content.model.textures.'):
        return '2/4', 'common-client', 'modern-resource-26.1-atlas', 'Plugin resource declarations; preserve current atlas/texture API'
    if name.startswith('content.model.'):
        return '3', 'common', 'modern-model-contract', 'Preserve FQCN/fields/generics; adapt Minecraft resource identifiers'
    if name.startswith('content.registration.PLibRegistration'):
        return '2', 'common', 'modern-internal-registries', 'Keep channel/event/deformer registries; move demo game registries to tests'
    if name.startswith('content.registration.'):
        return '1/7', 'tests/testmod', 'extract-demo', 'Preserve real assets and adapted 26.1 example render states'
    if name.startswith('content.event.') or name == 'PLib':
        return '1/2', ('common' if name.startswith('content.event.PulseRuntime') else 'neoforge'), 'split-bootstrap-runtime', 'Loader hooks delegate to PulseRuntime/plugins; retire PulseLibEvents'
    if name.startswith('util.helpers.PLibHelper'):
        return '3/4', 'common', 'modern-helper-contract', 'Factories/getManager overloads; GUI helper requires 26.1 submit context adapter'
    if name.startswith(('util.PResourceCache', 'util.PRenderTypes')):
        return '2/4', 'common-client', 'modern-contract-26.1-resources', 'Loader shader/atlas registration hooks; 26.1 RenderPipeline and reload'
    if name in ('data.PModelLoader', 'data.PLoadedModel', 'data.PAnimationSidecarParser'):
        return '3/4', 'common', 'modern-loader-26.1-reload', 'Keep loadModel/modelResourceCandidates API; bridge 26.1 reload batching and Identifier'
    if name == 'util.PModelCache':
        return '2/4', 'common-client', 'modern-cache-26.1-baking', 'Modern plugin/model contracts; 26.1 SharedState, atlas and GPU baking; preserve concurrent reload safety'
    if name == 'util.helpers.PLibParserHelper':
        return '3/4', 'common', 'modern-parser-26.1-textures', 'Preserve public helpers; retain 26.1 external image URI resolution behind modern model loader contract'
    if name in ('util.PLibDatabase', 'util.helpers.PLibCodecs', 'util.helpers.PLibRenderHelper'):
        return '1/3', 'common-review-client-boundary', 'preserve-modern-utility-api', 'Adapt Identifier/namespace/codec APIs; retain necessary 26.1 additions without renaming modern API'
    return '1/3/4', 'common-review-client-boundary', 'review-modern-contract', 'Retain modern source contract; inspect imported Minecraft/client/platform types'


def collect(label, sha, scratch):
    folder = scratch / label.replace('.', '_')
    folder.mkdir()
    with tarfile.open(fileobj=io.BytesIO(git('archive', sha)), mode='r:') as archive:
        archive.extractall(folder, filter='data')
    parsed = subprocess.check_output(['java', str(ROOT / 'docs/porting/tools/ExtractApi.java'), str(folder)], text=True)
    types = [json.loads(line) for line in parsed.splitlines()]
    assert len({row['fqcn'] for row in types}) == len(types), 'Duplicate source FQCN'
    tree = {}
    for line in git('ls-tree', '-r', sha).decode().splitlines():
        meta, path = line.split('\t', 1)
        tree[path] = meta.split()[2]
    units = []
    known = {row['fqcn'] for row in types}
    for path in sorted(tree):
        if '/src/main/java/' not in '/' + path or path.startswith('tests/') or not path.endswith('.java'):
            continue
        source = (folder / path).read_text()
        imports = re.findall(r'^import\s+(?:static\s+)?([\w.*]+)\s*;', source, re.M)
        package = re.search(r'^package\s+([\w.]+)\s*;', source, re.M).group(1)
        direct = sorted(value for value in imports if value.startswith(('net.neoforged.', 'net.fabricmc.')))
        internal = sorted(value for value in imports if value.startswith(PREFIX))
        tokens = set(re.findall(r'\b[A-Za-z_$][\w$]*\b', re.sub(r'/\*.*?\*/|//[^\n]*', '', source, flags=re.S)))
        same_package = sorted(value for value in known if value.rsplit('.', 1)[0] == package and value.rsplit('.', 1)[1] in tokens)
        units.append({'branch': label, 'source': path, 'git_blob': tree[path], 'package': package, 'imports': imports,
                      'internal_imports': internal, 'same_package_candidates': same_package,
                      'loader_imports': direct,
                      'loader_references': sorted(set(re.findall(r'net\.(?:neoforged|fabricmc)\.[\w.]+', source))),
                      'minecraft_imports': sorted(value for value in imports if value.startswith(('net.minecraft.', 'com.mojang.'))),
                      'package_annotations': re.findall(r'^@[^\n]+', source.split('package ', 1)[0], re.M)})
    by_path = {unit['source']: unit for unit in units}
    for row in types:
        unit = by_path[row['source']]
        row.update(branch=label, source_commit=sha, git_blob=unit['git_blob'], imports=unit['imports'])
    manifest = {'branch': label, 'commit': sha, 'tracked_files': len(tree), 'production_java_units': len(units),
                'named_types': len(types), 'accessible_types': sum(row['api'] for row in types),
                'accessible_members': sum(len(row['members']) for row in types if row['api']),
                'module_types': {module: sum(row['api'] and row['source'].split('/')[0] == module for row in types)
                                 for module in sorted({row['source'].split('/')[0] for row in types})},
                'junit_sources': [path for path in tree if '/src/test/java/' in path],
                'testmod_sources': [path for path in tree if path.startswith('tests/') and path.endswith('.java')],
                'testmod_assets': {path: blob for path, blob in tree.items() if path.startswith('tests/testmod/src/main/resources/')},
                'services': {path: (folder / path).read_text().strip() for path in tree if '/META-INF/services/' in path},
                'mixins': {path: json.loads((folder / path).read_text()) for path in tree if path.endswith('.mixins.json')},
                'shader_resources': {path: blob for path, blob in tree.items() if '/shaders/' in path},
                'loader_coupled_units': [unit['source'] for unit in units if unit['loader_references']]}
    return types, units, manifest


def generate():
    artifacts = {}
    with tempfile.TemporaryDirectory(prefix='pulselib-inventory-') as tmp:
        base, base_units, base_manifest = collect('26.1', SOURCES['26.1'], Path(tmp))
        modern, modern_units, modern_manifest = collect('1.21.1-multiloader', SOURCES['1.21.1-multiloader'], Path(tmp))
    artifacts['api-1.21.1-multiloader.ndjson'] = lines(sorted((row for row in modern if row['api']), key=lambda row: row['fqcn']))
    artifacts['api-26.1.ndjson'] = lines(sorted((row for row in base if row['api']), key=lambda row: row['fqcn']))
    artifacts['source-dependencies.ndjson'] = lines(base_units + modern_units)
    artifacts['source-manifest.json'] = json.dumps({'schema': 1, 'sources': [base_manifest, modern_manifest]}, ensure_ascii=False, indent=2) + '\n'
    a, b = ({row['fqcn']: row for row in rows} for rows in (base, modern))
    table = io.StringIO()
    writer = csv.writer(table, delimiter='\t', lineterminator='\n')
    writer.writerow(['fqcn', '26.1_source', 'multiloader_source', '26.1_api', 'multiloader_api', 'declaration_api_delta', 'stage', 'target', 'decision', 'adaptation'])
    changes = []
    for fqcn in sorted(a.keys() | b.keys()):
        left, right = a.get(fqcn), b.get(fqcn)
        delta = 'modern-only' if left is None else '26.1-only' if right is None else 'changed' if api_shape(left) != api_shape(right) else 'same-declarations'
        writer.writerow([fqcn, left['source'] if left else '', right['source'] if right else '',
                         left['api'] if left else False, right['api'] if right else False, delta, *policy(fqcn, left, right)])
        if (left and left['api']) or (right and right['api']):
            before = {item['signature'] for item in left['members']} if left else set()
            after = {item['signature'] for item in right['members']} if right else set()
            changes.append({'fqcn': fqcn, 'delta': delta, 'base_only_signatures': sorted(before - after),
                            'modern_only_signatures': sorted(after - before),
                            'header_or_import_changes': {key: {'base': api_shape(left)[key], 'modern': api_shape(right)[key]}
                                                         for key in api_shape(left) if key != 'members' and left and right and api_shape(left)[key] != api_shape(right)[key]}
                            if left and right else {}})
    artifacts['class-migration-map.tsv'] = table.getvalue()
    artifacts['api-delta.ndjson'] = lines(changes)
    artifacts['checksums.sha256'] = ''.join(hashlib.sha256(value.encode()).hexdigest() + '  ' + name + '\n'
                                           for name, value in sorted(artifacts.items()))
    return artifacts


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='Regenerate in memory and compare with checked-in artifacts')
    args = parser.parse_args()
    artifacts = generate()
    if args.check:
        mismatch = [name for name, data in artifacts.items() if not (OUTPUT / name).is_file() or (OUTPUT / name).read_text() != data]
        if mismatch:
            raise SystemExit('Inventory differs: ' + ', '.join(mismatch))
        print('PASS: all seven inventory artifacts reproduce from pinned commits')
    else:
        OUTPUT.mkdir(parents=True, exist_ok=True)
        for name, data in artifacts.items():
            (OUTPUT / name).write_text(data)
        print('Wrote seven inventory artifacts to', OUTPUT.relative_to(ROOT))


if __name__ == '__main__':
    main()
