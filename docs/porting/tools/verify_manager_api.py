#!/usr/bin/env python3
"""Check stage 3.1 declarations against the immutable stage 0 API baseline.
ArcAnc, created 21.10.2026, copyright 2026. Arc's License of Common Sense; see LICENSE.txt.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
PREFIX = 'com.arcanc.pulselib.'


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
    names = ['content.animatable.' + n for n in ['PAnimatable', 'PAnimationManager', 'AnimManagerKey',
             'PItemAnimatable', 'PItemStackAnimationData', 'PItemStackAnimationIds',
             'instance.InstanceAnimationManager', 'singleton.SingletonAnimationManager']]
    names += ['content.mixin.ItemStackMixin', 'util.helpers.PLibHelper']
    selected = sorted(n for n in baseline if any(n == PREFIX + value or n.startswith(PREFIX + value + '.') for value in names))
    contracts = ['kind', 'modifiers', 'annotations', 'type_parameters', 'extends', 'implements', 'permits', 'record_components']
    results = []
    for name in selected:
        assert name in current, f'Missing type: {name}'
        assert sum(row['fqcn'] == name for row in rows) == 1, f'Duplicate type: {name}'
        before, after = baseline[name], current[name]
        assert normalized({k: before[k] for k in contracts}) == normalized({k: after[k] for k in contracts}), f'Type changed: {name}'
        expected = json.loads(json.dumps(before['members']))
        actual = after['members']
        changes, deferred, extras = [], [], []
        if name == PREFIX + 'content.animatable.PItemAnimatable':
            # BEWLR was removed by Minecraft; the nullable default retains its original semantics.
            for member in expected:
                if member['name'] == 'getPulseItemRenderer':
                    member['signature'] = member['signature'].replace('BlockEntityWithoutLevelRenderer', 'SpecialModelRenderer<?>')
                    member['annotations'] = ['@Nullable']
            changes.append('getPulseItemRenderer: removed Minecraft BEWLR -> @Nullable SpecialModelRenderer<?>')
        if name == PREFIX + 'util.helpers.PLibHelper':
            deferred = [m['signature'] for m in expected if m['name'] == 'renderModelInGui']
            expected = [m for m in expected if m['name'] != 'renderModelInGui']
            extras = [m['signature'] for m in actual if m['name'] in ['livingRenderState', 'entityRenderState']]
            actual = [m for m in actual if m['name'] not in ['livingRenderState', 'entityRenderState']]
        assert sorted(map(normalized, expected)) == sorted(map(normalized, actual)), f'Members changed: {name}'
        results.append({'type': name, 'preservedMembers': len(expected), 'minecraftAdaptations': changes,
                        'deferredToStage4': deferred, 'additional26_1Members': extras})
    report = {'status': 'PASS_WITH_EXPLICIT_STAGE4_DEBT', 'baselineCommit': '7ce6f024b307bb821ce543344d3896c8e1fcf26b',
              'baselineChecksums': 'unchanged', 'types': results,
              'scope': 'Stage 3.1 source declarations; full compiled/behavioral API verification remains stage 7'}
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2) + '\n')
    print(f'MANAGER API PASS: {len(results)} canonical types; one MC renderer adaptation, one GUI method deferred to stage 4')


if __name__ == '__main__':
    main()
