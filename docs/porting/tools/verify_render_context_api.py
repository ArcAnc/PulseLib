#!/usr/bin/env python3
"""Verify canonical local render scope declarations against the frozen stage 0 API.
ArcAnc, created 25.10.2026, copyright 2026. Arc's License of Common Sense; see LICENSE.txt.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
PREFIX = 'com.arcanc.pulselib.content.renderer.PRenderContexts'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    directory = ROOT / 'docs/porting/26.1.2-multiloader'
    for line in (directory / 'checksums.sha256').read_text().splitlines():
        digest, name = line.split()
        assert hashlib.sha256((directory / name).read_bytes()).hexdigest() == digest, name
    baseline = {row['fqcn']: row for row in map(json.loads, (directory / 'api-1.21.1-multiloader.ndjson').read_text().splitlines())
                if row['fqcn'] == PREFIX or row['fqcn'].startswith(PREFIX + '.')
                or row['fqcn'] == 'com.arcanc.pulselib.content.renderer.legacy.GlRenderState'}
    with tempfile.TemporaryFile(mode='w+') as output:
        subprocess.run(['java', str(ROOT / 'docs/porting/tools/ExtractApi.java'), str(ROOT)], stdout=output, check=True, cwd=ROOT)
        output.seek(0)
        rows = [json.loads(line) for line in output]
    keys = ['kind', 'modifiers', 'annotations', 'type_parameters', 'extends', 'implements', 'permits', 'record_components', 'members']
    results = []
    for name, before in sorted(baseline.items()):
        candidates = [row for row in rows if row['fqcn'] == name]
        assert len(candidates) == 1, f'Missing or duplicate canonical context type: {name}'
        assert {key: before[key] for key in keys} == {key: candidates[0][key] for key in keys}, f'Changed canonical context declaration: {name}'
        results.append({'type': name, 'preservedMembers': len(before['members'])})
    assert len(results) == 4
    report = {'status': 'PASS', 'baselineCommit': '7ce6f024b307bb821ce543344d3896c8e1fcf26b',
              'baselineChecksums': 'unchanged', 'types': results,
              'scope': 'Canonical PRenderContexts, Context, Scope and GlRenderState source declarations; native automatic GUI integration remains stage 4.3b',
              'minecraftAdaptations': ['Invocation scopes use 26.1 attachment overrides rather than the last bound raw GL framebuffer',
                                       'Local scopes capture frame matrices, projection UBO binding and target views at entry',
                                       'GlCommandEncoder program/pipeline caches invalidated after native restoration'],
              'annotationAdaptation': 'org.jetbrains Nullable -> JSpecify Nullable; package remains @NullMarked'}
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2) + '\n')
    print(f"RENDER CONTEXT API PASS: {len(results)} canonical types / {sum(row['preservedMembers'] for row in results)} declared members")


if __name__ == '__main__':
    main()
