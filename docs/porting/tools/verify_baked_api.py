#!/usr/bin/env python3
"""Check canonical baked/model declarations and inventory the remaining renderer boundary.
ArcAnc, created 23.10.2026, copyright 2026. Arc's License of Common Sense; see LICENSE.txt.
"""
import argparse
import copy
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
PREFIX = 'com.arcanc.pulselib.content.'
CONTRACTS = ['kind', 'modifiers', 'annotations', 'type_parameters', 'extends', 'implements', 'permits', 'record_components']


def normalized(value):
    return json.dumps(value, sort_keys=True).replace('ResourceLocation', 'Identifier')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    directory = ROOT / 'docs/porting/26.1.2-multiloader'
    for line in (directory / 'checksums.sha256').read_text().splitlines():
        digest, name = line.split()
        assert hashlib.sha256((directory / name).read_bytes()).hexdigest() == digest, name
    baseline = {r['fqcn']: r for r in map(json.loads, (directory / 'api-1.21.1-multiloader.ndjson').read_text().splitlines())}
    with tempfile.TemporaryFile(mode='w+') as output:
        subprocess.run(['java', str(ROOT / 'docs/porting/tools/ExtractApi.java'), str(ROOT)], stdout=output, check=True, cwd=ROOT)
        output.seek(0)
        rows = [json.loads(line) for line in output]
    current = {r['fqcn']: r for r in rows}
    selected = sorted(name for name in baseline if name.startswith(PREFIX + 'model.baked.') or name in [
        PREFIX + 'model.PModel', PREFIX + 'renderer.legacy.GlDynamicGeometry', PREFIX + 'renderer.legacy.GlGeometryDataFactory'])
    results = []
    for name in selected:
        assert name in current, f'Missing canonical type: {name}'
        assert sum(row['fqcn'] == name for row in rows) == 1, f'Duplicate type: {name}'
        before, after = baseline[name], current[name]
        assert normalized({k: before[k] for k in CONTRACTS}) == normalized({k: after[k] for k in CONTRACTS}), f'Type changed: {name}'
        expected = copy.deepcopy(before['members'])
        adaptations = []
        for member in expected:
            if name.endswith('PBakedBone.PBakedBoneBuilder') and member['name'] == 'parent':
                member['annotations'] = ['@Nullable']
                adaptations.append('Builder root parent is actually nullable; explicit JSpecify annotation')
            if name.endswith('PBakedModel') and member['name'] == 'compiledAnimation':
                member['annotations'] = ['@Nullable']
                adaptations.append('Missing compiled clip actually returns null; explicit JSpecify annotation')
            if name.endswith('PDeformedMeshBuffers') and member['name'] == 'resolve':
                member['parameters'] = ['PBakedMesh mesh', '@Nullable PMeshDeformation deformation']
                adaptations.append('Existing no-deformation branch accepts null; explicit JSpecify annotation')
            if name.endswith('GlDynamicGeometry') and member['name'] == 'vertexBuffer':
                member['signature'] = 'public PGlDynamicGeometry vertexBuffer()'
                adaptations.append('Minecraft removed VertexBuffer; returns the sole MC26 dynamic geometry source for uploads/close')
        expected_set = {normalized(m) for m in expected}
        actual_set = {normalized(m) for m in after['members']}
        assert expected_set <= actual_set, f'Missing/changed members: {name}: {expected_set - actual_set}'
        results.append({'type': name, 'preservedMembers': len(expected), 'adaptations': adaptations,
                        'additional26_1Members': [m for m in after['members'] if normalized(m) not in expected_set]})
    renderer_debt = []
    for name in sorted(name for name in baseline if any(name == PREFIX + 'renderer.' + simple or name.startswith(PREFIX + 'renderer.' + simple + '.')
                       for simple in ['PRenderer', 'PBlockRenderer', 'PEntityRenderer', 'PItemRenderer'])):
        before, after = baseline[name], current.get(name)
        expected = {normalized(m): m for m in before['members']}
        actual = {} if after is None else {normalized(m): m for m in after['members']}
        if after is not None and name.rsplit('.', 1)[-1] in ['PRenderer', 'PBlockRenderer', 'PEntityRenderer', 'PItemRenderer']:
            owner_members = {normalized(m) for m in before['members'] if m['name'] in ['getModelData', 'getModel', 'getRenderType']}
            assert owner_members <= actual.keys(), f'Canonical owner model/texture selection changed: {name}'
        renderer_debt.append({'type': name, 'preservedDeclaredMembers': len(expected.keys() & actual.keys()),
                              'missingOrChangedMembers': [expected[k] for k in sorted(expected.keys() - actual.keys())],
                              'additional26_1Members': [actual[k] for k in sorted(actual.keys() - expected.keys())],
                              'typeChanges': {k: {'canonical': before[k], 'current': after[k] if after else None} for k in CONTRACTS
                                              if after is None or normalized(before[k]) != normalized(after[k])}})
    report = {'status': 'PASS_BAKED_RENDERER_AUDIT_SEPARATE', 'baselineCommit': '7ce6f024b307bb821ce543344d3896c8e1fcf26b',
              'baselineChecksums': 'unchanged', 'types': results,
              'minecraftAdaptations': ['ResourceLocation -> Identifier', 'RenderType package -> renderer.rendertype',
                                       'org.jetbrains.Nullable -> JSpecify.Nullable under @NullMarked', 'Removed VertexBuffer -> PGlDynamicGeometry upload source'],
              'scope': 'All canonical model.baked types + PModel + geometry facade/factory; source declarations, not whole-library compiled audit',
              'rendererOwnerLookup': 'PASS: all four renderer types preserve getModelData(T), getModel(T), getRenderType(Identifier)',
              'rendererBoundaryInventory': renderer_debt,
              'strictRendererAudit': 'verify_renderer_api.py classifies Minecraft generics/signature adaptations; raw declaration differences below are not unimplemented members',
              'remainingWork': ['World stages/OIT 4.2', 'GUI/offscreen scopes and renderModelInGui 4.3', 'Player integration 5, Fabric runtime 6, compiled whole-library API audit 7']}
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2) + '\n')
    print(f"BAKED API PASS: {len(results)} canonical types / {sum(r['preservedMembers'] for r in results)} members; renderer owner lookup preserved; raw renderer boundary inventoried; strict renderer audit is separate")


if __name__ == '__main__':
    main()
