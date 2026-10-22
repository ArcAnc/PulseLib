#!/usr/bin/env python3
"""Compare canonical render-plan declarations with the frozen stage 0 API.
ArcAnc, created 22.10.2026, copyright 2026. Arc's License of Common Sense; see LICENSE.txt.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
PREFIX = 'com.arcanc.pulselib.content.renderer.plan.'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    directory = ROOT / 'docs/porting/26.1.2-multiloader'
    for line in (directory / 'checksums.sha256').read_text().splitlines():
        digest, name = line.split()
        assert hashlib.sha256((directory / name).read_bytes()).hexdigest() == digest, name
    all_baseline = {r['fqcn']: r for r in map(json.loads, (directory / 'api-1.21.1-multiloader.ndjson').read_text().splitlines())}
    baseline = {name: row for name, row in all_baseline.items() if name.startswith(PREFIX)}
    with tempfile.TemporaryFile(mode='w+') as output:
        subprocess.run(['java', str(ROOT / 'docs/porting/tools/ExtractApi.java'), str(ROOT)], stdout=output, check=True, cwd=ROOT)
        output.seek(0)
        rows = [json.loads(line) for line in output]
    current = {r['fqcn']: r for r in rows}
    keys = ['kind', 'modifiers', 'annotations', 'type_parameters', 'extends', 'implements', 'permits', 'record_components', 'members']
    results = []
    for name, before in sorted(baseline.items()):
        assert name in current, f'Missing render-plan type: {name}'
        assert sum(row['fqcn'] == name for row in rows) == 1, f'Duplicate render-plan type: {name}'
        after = current[name]
        assert {k: before[k] for k in keys} == {k: after[k] for k in keys}, f'Render-plan contract changed: {name}'
        results.append({'type': name, 'preservedMembers': len(before['members'])})
    queue_debt = []
    for name in ['com.arcanc.pulselib.content.renderer.PRenderQueue', 'com.arcanc.pulselib.content.renderer.PRenderQueue.RenderStage']:
        before, after = all_baseline[name], current[name]
        expected = {json.dumps(member, sort_keys=True): member for member in before['members']}
        actual = {json.dumps(member, sort_keys=True): member for member in after['members']}
        queue_debt.append({'type': name, 'preservedMembers': len(expected.keys() & actual.keys()),
                           'deferredMembers': [expected[key] for key in sorted(expected.keys() - actual.keys())],
                           'temporary26_1FacadeMembers': [actual[key] for key in sorted(actual.keys() - expected.keys())],
                           'deferredTypeChanges': {key: {'baseline': before[key], 'current': after[key]}
                                                   for key in keys if key != 'members' and before[key] != after[key]}})
    report = {'status': 'PASS', 'baselineCommit': '7ce6f024b307bb821ce543344d3896c8e1fcf26b',
              'baselineChecksums': 'unchanged', 'types': results,
              'scope': 'All public/protected render.plan types, including nested enums; source declarations, not the final whole-library compiled audit',
              'minecraftAdaptations': [], 'annotationAdaptation': 'org.jetbrains Nullable -> JSpecify Nullable; existing @NullMarked package convention retained',
              'queueBoundaryDebtOutsidePlanScope': queue_debt,
              'remainingStage4Work': ['Baked/model and renderer API, canonical queue geometry overloads',
                                      'Render stage hooks, OIT integration and controlled pixel regressions',
                                      'GUI/offscreen local scopes and PLibHelper.renderModelInGui']}
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2) + '\n')
    print(f"RENDER PLAN API PASS: {len(results)} canonical types / {sum(r['preservedMembers'] for r in results)} declared members")


if __name__ == '__main__':
    main()
