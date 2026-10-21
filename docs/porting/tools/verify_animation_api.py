#!/usr/bin/env python3
"""Verify the stage 3.2 sampling/event surface against the frozen stage 0 API.
ArcAnc, created 21.10.2026, copyright 2026. Arc's License of Common Sense; see LICENSE.txt.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
PREFIX = 'com.arcanc.pulselib.'


def normalized(value):
    if isinstance(value, dict) and value.get('kind') == 'field':
        value = dict(value)
        initializer = value.get('initializer', '')
        # Executable callback/registry initializer bodies are implementation, as method bodies are.
        # Retain literal constant values; report changed nonconstant bodies separately below.
        if initializer and not re.fullmatch(r'"(?:[^"\\]|\\.)*"|true|false|[-+]?[0-9][0-9a-fA-FxX.pPeE+\-]*[fFdDlL]?', initializer):
            value.pop('initializer', None)
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
    names = ['content.model.animation.' + n for n in ['PAnimation', 'PAnimationChannel', 'PAnimationChannelType',
             'PAnimationDecodeContext', 'PAnimationEvaluationContext', 'PAnimationEvent', 'PAnimationEventContext',
             'PAnimationEventType', 'PAnimationEventTypes', 'PAnimationFormatDecoder', 'PAnimationTrack',
             'PAnimationType', 'PAnimationValue', 'PAnimationVisibilityTrack', 'PBoneAnimation', 'PCompiledAnimation',
             'PEventSide', 'PInterpolation', 'PInterpolationType', 'PKeyframe', 'PVectorConversion']]
    names += ['data.gecko.' + n for n in ['MolangParser', 'PExpressionDependency', 'PExpressionEvaluator',
              'PMolangVectorValue', 'PMolangEulerRotationValue', 'PGeckoDecodeContext', 'PGeckoChannelDecoder',
              'PGeckoModelParser', 'PGeckoAnimationEventParser']]
    names += ['data.gltf.' + n for n in ['PGltfDecodeContext', 'PGltfChannelDecoder', 'PGltfModelParser', 'PGltfAnimationEventSidecarParser']]
    names += ['data.PAnimationSidecarParser', 'content.animatable.PAnimationEventCallbacks', 'content.animatable.PAnimationEventDispatcher']
    # These owners are completed in 3.3. Verify every present member and expose the precise missing API.
    partial = {PREFIX + 'content.animatable.PAnimationController', PREFIX + 'content.model.animation.PAnimationGraphRuntime',
               PREFIX + 'content.model.animation.PPose'}
    selected = sorted(n for n in baseline if any(n == PREFIX + v or n.startswith(PREFIX + v + '.') for v in names) or
                      any(n == v or n.startswith(v + '.') for v in partial))
    contract_keys = ['kind', 'modifiers', 'annotations', 'type_parameters', 'extends', 'implements', 'permits', 'record_components']
    results, missing_types, initializers = [], [], []
    for name in selected:
        is_partial = any(name == v or name.startswith(v + '.') for v in partial)
        if name not in current:
            assert is_partial, f'Missing sampling type: {name}'
            missing_types.append(name)
            continue
        assert sum(row['fqcn'] == name for row in rows) == 1, f'Duplicate type: {name}'
        before, after = baseline[name], current[name]
        assert normalized({k: before[k] for k in contract_keys}) == normalized({k: after[k] for k in contract_keys}), f'Type changed: {name}'
        for member in after['members']:
            if member['kind'] == 'field':
                previous = next((m for m in before['members'] if m['name'] == member['name']), {})
                old = previous.get('initializer', '').replace('ResourceLocation', 'Identifier')
                new = member.get('initializer', '')
                if old != new:
                    initializers.append({'type': name, 'field': member['name'],
                                         'baselineBodySha256': hashlib.sha256(old.encode()).hexdigest(),
                                         'currentBodySha256': hashlib.sha256(new.encode()).hexdigest()})
        expected = {normalized(m): m for m in before['members']}
        actual = {normalized(m): m for m in after['members']}
        assert not actual.keys() - expected.keys(), f'Noncanonical/changed members: {name}'
        missing = [expected[k] for k in sorted(expected.keys() - actual.keys())]
        assert is_partial or not missing, f'Missing sampling members: {name}'
        results.append({'type': name, 'preservedMembers': len(actual), 'deferredToStage33': missing})
    report = {'status': 'PASS_WITH_EXPLICIT_STAGE33_DEBT', 'baselineCommit': '7ce6f024b307bb821ce543344d3896c8e1fcf26b',
              'baselineChecksums': 'unchanged', 'types': results, 'typesDeferredToStage33': missing_types,
              'allowedRename': 'Minecraft ResourceLocation -> Identifier',
              'nonConstantInitializerChanges': initializers,
              'initializerPolicy': 'Literal constants compared; executable callback/registry bodies excluded from API declarations and changed body hashes reported',
              'scope': 'Source declarations for sampling/events plus explicit partial graph/controller/pose API; full compatibility remains stage 7'}
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2) + '\n')
    print(f'ANIMATION API PASS: {len(results)} canonical types; graph/controller/pose debt explicitly listed for 3.3')


if __name__ == '__main__':
    main()
