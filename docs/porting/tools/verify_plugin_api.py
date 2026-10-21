#!/usr/bin/env python3
"""Verify the migrated plugin surface against the frozen stage 0 API, without regenerating it.
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


def normalize(value):
    # The Minecraft type was renamed in 26.1; no PulseLib type names are rewritten.
    return json.dumps(value, sort_keys=True).replace('ResourceLocation', 'Identifier')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    directory = ROOT / 'docs/porting/26.1.2-multiloader'
    for line in (directory / 'checksums.sha256').read_text().splitlines():
        digest, name = line.split()
        assert hashlib.sha256((directory / name).read_bytes()).hexdigest() == digest, name
    baseline = {row['fqcn']: row for row in map(json.loads, (directory / 'api-1.21.1-multiloader.ndjson').read_text().splitlines())}
    # Compiler Tree API compares declared types, generics, members, annotations and record contracts.
    with tempfile.TemporaryFile(mode='w+') as output:
        subprocess.run(['java', str(ROOT / 'docs/porting/tools/ExtractApi.java'), str(ROOT)], stdout=output, check=True, cwd=ROOT)
        output.seek(0)
        rows = [json.loads(line) for line in output]
    current = {row['fqcn']: row for row in rows}
    exact = ['content.registration.PRegistry', 'content.event.PulseRuntime', 'content.event.CommonEvents',
             'content.event.ClientEvents', 'PLib', 'content.mixin.NeoForgeCameraAccessor', 'content.mixin.ResourceReloadMixin']
    selected = sorted(name for name in baseline if name.startswith((PREFIX + 'api.', PREFIX + 'plugin.', PREFIX + 'platform.')) or
                      name in {PREFIX + value for value in exact} or
                      name in {PREFIX + loader + '.' + kind for loader in ['fabric', 'neoforge'] for kind in
                               [('Fabric' if loader == 'fabric' else 'NeoForge') + suffix for suffix in
                                ['CameraService', 'Platform', 'PulsePluginFinder']]} or
                      name in {PREFIX + 'util.PResourceCache', PREFIX + 'content.mixin.CameraAccessor'})
    extras = {
        PREFIX + 'content.mixin.CameraAccessor': {'pulselib$getForwards', 'pulselib$getUp', 'pulselib$getLeft'},
        PREFIX + 'util.PResourceCache': {'getModelResource'},
    }
    keys = ['kind', 'modifiers', 'type_parameters', 'extends', 'implements', 'permits', 'record_components', 'annotations']
    results = []
    for name in selected:
        assert name in current, f'Missing canonical type: {name}'
        assert sum(row['fqcn'] == name for row in rows) == 1, f'Duplicate type: {name}'
        before, after = baseline[name], current[name]
        assert normalize({key: before[key] for key in keys}) == normalize({key: after[key] for key in keys}), f'Type contract changed: {name}'
        allowed = extras.get(name, set())
        actual = [member for member in after['members'] if member['name'] not in allowed]
        assert sorted(map(normalize, before['members'])) == sorted(map(normalize, actual)), f'Member contract changed: {name}'
        added = [member['signature'] for member in after['members'] if member['name'] in allowed]
        results.append({'type': name, 'canonicalMembers': len(before['members']), 'additional26_1Members': added})
    report = {'status': 'PASS', 'baselineCommit': '7ce6f024b307bb821ce543344d3896c8e1fcf26b',
              'baselineChecksums': 'unchanged', 'types': results,
              'allowedRename': 'Minecraft ResourceLocation -> Identifier',
              'scope': 'Declared source API only; full compiled/behavioral compatibility remains stage 7'}
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2) + '\n')
    print(f'PLUGIN API PASS: {len(results)} canonical types; complete declared member contracts preserved')


if __name__ == '__main__':
    main()
