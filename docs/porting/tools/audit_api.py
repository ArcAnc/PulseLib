#!/usr/bin/env python3
"""Inventory all frozen/current named types and current release class contracts.
ArcAnc, created 11.11.2026, copyright 2026.
Arc's License of Common Sense. See LICENSE.txt at repository root.
"""
import argparse
from collections import Counter
import gzip
import hashlib
import io
import json
from pathlib import Path
import re
import subprocess
import sys
import tarfile
import tempfile

sys.dont_write_bytecode = True

from inventory import ROOT, SOURCES, api_shape, collect, git

HEADERS = ['api', 'kind', 'modifiers', 'annotations', 'type_parameters', 'extends',
           'implements', 'permits', 'record_components']
PREFIX = 'com.arcanc.pulselib.'


def reason(name):
    if name == PREFIX + 'content.animatable.PItemAnimatable':
        return 'User-approved removal on 2026-10-28, recorded in item-api-removal.json.'
    if name.startswith(PREFIX + 'content.renderer.legacy.'):
        return 'Review canonical facade over the single PGl backend. A native replacement alone does not preserve this public type/signature.'
    if name.startswith(PREFIX + 'content.model.deformer.gpu.'):
        return 'Review public deformer constants, stream access and visibility against native GPU ownership. Canonical caller compatibility remains open.'
    if name.startswith(PREFIX + 'util.PRenderTypes'):
        return 'Review canonical material aliases and removed shader/transparency types against native RenderPipeline policy and explicit ordinary/OIT ownership.'
    if name.startswith(PREFIX + 'content.model.textures.atlas.'):
        return 'Review metadata record compatibility and SpriteSource codec contract against native atlas registration.'
    if '.mixin.' in name or name.startswith(PREFIX + 'fabric.') or name.startswith(PREFIX + 'neoforge.'):
        return 'Minecraft/loader extraction, submission and lifecycle hook changes. Review the exact old public declaration and its native replacement.'
    if name.startswith(PREFIX + 'content.renderer.'):
        return 'Native extracted RS and SubmitNodeCollector contract. Existing renderer API proof records the selected adapters in sections 21-31.'
    if '.attachment' in name or '.firstPerson.' in name:
        return 'Native living/avatar snapshots, non-generic PlayerModel and deferred SubmitNodeCollector. Review against attachment/player evidence in sections 33-62.'
    if name == PREFIX + 'util.PModelCache':
        return 'Native PreparableReloadListener.SharedState replaces the old profiler/resource-manager reload entrypoint.'
    if name == PREFIX + 'util.helpers.PLibHelper':
        return 'Native GuiGraphicsExtractor replaces GuiGraphics for extracted GUI submission, recorded by the GUI helper API proof.'
    if name in [PREFIX + 'content.player.animation.PPlayerAnimationDefinition.' + policy for policy in ['ItemRenderPolicy', 'ItemVisibilityPolicy']]:
        return 'The four constant-return policy lambdas became anonymous implementations for dedicated-server client descriptor isolation, recorded in section 50.'
    return 'Exact declaration/import/annotation difference requires review against the frozen source contract.'


def normalized(signature):
    return signature.replace('ResourceLocation', 'Identifier').replace('PlayerModel<?>', 'PlayerModel').replace('PEntityRenderer.@Nullable HeadRotation', 'PEntityRenderer.HeadRotation')


def type_delta(name, before, after, graphics):
    row = {'fqcn': name, 'canonicalApi': bool(before and before['api']),
           'canonicalSource': before['source'] if before else None,
           'currentSource': after['source'] if after else None,
           'presentInGraphicsBaseline': graphics is not None}
    if before is None:
        row.update(status='addition', currentContract=api_shape(after))
        return row
    if after is None:
        row.update(status='approved-removal' if name.endswith('.PItemAnimatable') else 'removed-review',
                   reason=reason(name), missingMembers=before['members'], canonicalContract=api_shape(before))
        return row
    current = {normalized(member['signature']): member for member in after['members']}
    preserved, adapted, missing = [], [], []
    for member in before['members']:
        match = current.get(normalized(member['signature']))
        if match is None:
            missing.append({'canonical': member, 'reason': reason(name),
                            'sameNameCandidates': [m for m in after['members'] if m['name'] == member['name']]})
        elif member['signature'] == match['signature']:
            preserved.append(member['signature'])
        else:
            adapted.append({'canonical': member['signature'], 'current': match['signature'],
                            'reason': 'ResourceLocation becomes Identifier, native PlayerModel loses its generic parameter, or nullable nested-type placement changes.'})
    old_signatures = {normalized(member['signature']) for member in before['members']}
    header = {key: {'canonical': before[key], 'current': after[key]}
              for key in HEADERS if before[key] != after[key]}
    imports_before = api_shape(before)['signature_imports']
    imports_after = api_shape(after)['signature_imports']
    annotation_changes = []
    initializer_changes = []
    for member in before['members']:
        match = current.get(normalized(member['signature']))
        if match is None:
            continue
        if member.get('annotations') != match.get('annotations'):
            annotation_changes.append({'member': member['signature'], 'canonical': member.get('annotations'),
                                       'current': match.get('annotations')})
        if member.get('initializer') != match.get('initializer'):
            initializer_changes.append({'member': member['signature'], 'canonical': member.get('initializer'),
                                        'current': match.get('initializer')})
    changed = bool(header or adapted or annotation_changes or initializer_changes or imports_before != imports_after)
    row.update(status='changed-review' if missing or header.get('api') else 'adaptation-review' if changed else 'preserved',
               preservedMembers=preserved, adaptedMembers=adapted, missingMembers=missing,
               addedMembers=[m for m in after['members'] if normalized(m['signature']) not in old_signatures],
               headerChanges=header, annotationChanges=annotation_changes, initializerChanges=initializer_changes)
    if imports_before != imports_after:
        row['signatureImports'] = {'canonical': imports_before, 'current': imports_after}
    if row['status'] != 'preserved':
        row['reason'] = reason(name)
    return row


def current_types(folder):
    output = subprocess.check_output(['java', str(ROOT / 'docs/porting/tools/ExtractApi.java'), str(folder)], text=True)
    rows = [json.loads(line) for line in output.splitlines()]
    for row in rows:
        text = (folder / row['source']).read_text()
        row['imports'] = re.findall(r'^import\s+(?:static\s+)?([\w.*]+)\s*;', text, re.M)
    assert len({row['fqcn'] for row in rows}) == len(rows)
    return rows


def binary_name(row, folder):
    text = (folder / row['source']).read_text()
    package = re.search(r'^package\s+([\w.]+)\s*;', text, re.M).group(1)
    return package.replace('.', '/') + '/' + row['fqcn'].removeprefix(package + '.').replace('.', '$')


def build(folder, jars):
    directory = ROOT / 'docs/porting/26.1.2-multiloader'
    for line in (directory / 'checksums.sha256').read_text().splitlines():
        digest, name = line.split()
        assert hashlib.sha256((directory / name).read_bytes()).hexdigest() == digest
    with tempfile.TemporaryDirectory(prefix='pulselib-final-api-') as temporary:
        baseline = {}
        for label, commit in SOURCES.items():
            rows = collect(label, commit, Path(temporary))[0]
            baseline[label] = {row['fqcn']: row for row in rows}
    rows = current_types(folder)
    current = {row['fqcn']: row for row in rows}
    canonical = baseline['1.21.1-multiloader']
    graphics = baseline['26.1']
    delta = [type_delta(name, canonical.get(name), current.get(name), graphics.get(name))
             for name in sorted(canonical.keys() | current.keys())]
    output = subprocess.check_output(['java', str(ROOT / 'docs/porting/tools/ExtractBinaryApi.java'), *map(str, jars)], text=True)
    binaries = [json.loads(line) for line in output.splitlines()]
    by_name = {}
    for row in binaries:
        assert row['major'] == 69
        by_name.setdefault(row['binaryName'], []).append(row)
    mappings = {}
    for row in rows:
        name = binary_name(row, folder)
        assert name in by_name, ('Named source type absent from release JARs', row['fqcn'], name)
        mappings[row['fqcn']] = name
    resource_members = {}
    model_id = 'Lnet/minecraft/resources/Identifier;'
    resource_manager = 'Lnet/minecraft/server/packs/resources/ResourceManager;'
    restored_descriptors = {
        'modelResourceCandidates': '(' + model_id + ')Ljava/util/List;',
        'loadModel': '(Ljava/util/concurrent/Executor;' + resource_manager + model_id + ')Ljava/util/concurrent/CompletableFuture;',
    }
    for name in ['data.PModelLoader', 'data.gecko.PGeckoModelLoader', 'data.gltf.PGltfModelLoader',
                 'content.renderer.modelData.PModelData']:
        fqcn = PREFIX + name
        row = next(item for item in delta if item['fqcn'] == fqcn)
        assert not row['missingMembers'], (fqcn, row['missingMembers'])
        compiled = by_name[mappings[fqcn]][0]
        for member in canonical[fqcn]['members']:
            matches = [m for m in compiled['fields' if member['kind'] in ['field', 'enum_constant'] else 'methods']
                       if m['name'] == member['name']]
            assert matches, (fqcn, member)
        for copy in by_name[mappings[fqcn]]:
            if name == 'content.renderer.modelData.PModelData':
                matches = [m for m in copy['fields'] if m['name'] == 'DEFAULT_MODEL_LOADER_ID']
                assert len(matches) == 1 and matches[0]['descriptor'] == model_id and matches[0]['flags'] & 0x19 == 0x19
            else:
                for method, descriptor in restored_descriptors.items():
                    matches = [m for m in copy['methods'] if m['name'] == method and m['descriptor'] == descriptor]
                    assert len(matches) == 1 and matches[0]['flags'] & 1
                    assert ('PLoadedModel;' if method == 'loadModel' else 'Identifier;') in matches[0]['signature']
        resource_members[fqcn] = {'canonicalMembers': len(canonical[fqcn]['members']),
                                 'binaryName': mappings[fqcn], 'restoredDescriptorsVerifiedInEveryLoaderCopy': True}
    binary_duplicates = 0
    for name, copies in by_name.items():
        contracts = [{k: v for k, v in row.items() if k != 'jar'} for row in copies]
        assert all(contract == contracts[0] for contract in contracts), ('Cross-loader binary contract mismatch', name)
        binary_duplicates += len(copies) - 1
    production = {str(p.relative_to(folder)): hashlib.sha256(p.read_bytes()).hexdigest()
                  for module in ['common', 'fabric', 'neoforge']
                  for p in sorted((folder / module / 'src/main/java').rglob('*.java'))}
    summary = {
        'checkpoint': '7.2a', 'status': 'AUDIT_COMPLETE_WITH_REVIEW_DEBT', 'sources': SOURCES,
        'canonicalNamedTypes': len(canonical), 'canonicalAccessibleTypes': sum(r['api'] for r in canonical.values()),
        'canonicalAccessibleMembers': sum(len(r['members']) for r in canonical.values() if r['api']),
        'currentNamedTypes': len(current), 'currentAccessibleTypes': sum(r['api'] for r in rows),
        'typeStatuses': dict(Counter(r['status'] for r in delta)),
        'canonicalAccessibleTypeStatuses': dict(Counter(r['status'] for r in delta if r['canonicalApi'])),
        'namedSourceTypesFoundInReleaseJars': len(mappings),
        'binaryClasses': len(binaries), 'uniqueBinaryClasses': len(by_name), 'matchingSharedCopies': binary_duplicates,
        'restoredResourceContracts': resource_members,
        'productionSourceSha256': production,
        'jars': {str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in jars},
        'reviewTypes': [r['fqcn'] for r in delta if r['canonicalApi'] and r['status'] in ['changed-review', 'removed-review']],
        'coverage': [
            'All named source FQCNs from frozen canonical/current trees, including implementation and nested types.',
            'Declared public/protected member signatures, type headers, referenced imports, annotations and field initializers.',
            'All current release class-file descriptors, generic signatures, flags, ConstantValue attributes and declared exceptions without class loading.',
            'All current named source types map to release class files. Shared class contracts agree across module JARs.',
        ],
        'remaining': [
            'Review each changed/removed canonical declaration and restore feasible GPU/material/metadata facades through the existing native backend.',
            'Complete inherited member resolution, compiler-resolved source/type equivalence and exhaustive source-to-descriptor compatibility checks.',
            'Consolidate existing runtime/GPU evidence and evaluate performance measurements within their recorded limits.',
        ],
    }
    return summary, delta, binaries


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-root', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--jars', type=Path, nargs='+', required=True)
    args = parser.parse_args()
    with tempfile.TemporaryDirectory(prefix='pulselib-current-api-') as temporary:
        folder = args.source_root.resolve() if args.source_root else Path(temporary)
        if not args.source_root:
            with tarfile.open(fileobj=io.BytesIO(git('archive', 'HEAD'))) as archive:
                archive.extractall(folder, filter='data')
        summary, delta, binaries = build(folder, args.jars)
    args.output.mkdir(parents=True, exist_ok=True)
    (args.output / 'stage-7.2a-api.json').write_text(json.dumps(summary, indent=2) + '\n')
    (args.output / 'stage-7.2a-api.ndjson').write_text(''.join(json.dumps(row, ensure_ascii=False) + '\n' for row in delta))
    with (args.output / 'stage-7.2a-compiled.ndjson.gz').open('wb') as stream:
        with gzip.GzipFile(filename='', mode='wb', fileobj=stream, mtime=0) as packed:
            packed.write(''.join(json.dumps(row) + '\n' for row in binaries).encode())
    print('API AUDIT COMPLETE', summary['canonicalNamedTypes'], 'canonical named types,',
          summary['currentNamedTypes'], 'current named types,', len(binaries), 'class files')
    print('REVIEW DEBT', summary['canonicalAccessibleTypeStatuses'])


if __name__ == '__main__':
    main()
