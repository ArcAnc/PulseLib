#!/usr/bin/env python3
"""Check the canonical player deformer API against the frozen source baseline.
ArcAnc, created 27.10.2026, copyright 2026. Arc's License of Common Sense; see LICENSE.txt.
"""
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
PREFIX = 'com.arcanc.pulselib.content.player.deformer.'
HEADERS = ['kind', 'modifiers', 'annotations', 'type_parameters', 'extends', 'implements', 'permits', 'record_components']


def normalize(value):
    return json.dumps(value, sort_keys=True).replace('ResourceLocation', 'Identifier').replace('PlayerModel<?>', 'PlayerModel')


def main():
    directory = ROOT / 'docs/porting/26.1.2-multiloader'
    for line in (directory / 'checksums.sha256').read_text().splitlines():
        digest, name = line.split()
        assert hashlib.sha256((directory / name).read_bytes()).hexdigest() == digest, name
    baseline = [row for row in map(json.loads, (directory / 'api-1.21.1-multiloader.ndjson').read_text().splitlines())
                if row['fqcn'].startswith(PREFIX)]
    with tempfile.TemporaryFile(mode='w+') as output:
        subprocess.run(['java', str(ROOT / 'docs/porting/tools/ExtractApi.java'), str(ROOT)], stdout=output, check=True, cwd=ROOT)
        output.seek(0)
        actual = [json.loads(line) for line in output]
    for before in baseline:
        matches = [row for row in actual if row['fqcn'] == before['fqcn']]
        assert len(matches) == 1, f"Missing/duplicate deformer type: {before['fqcn']}"
        after = matches[0]
        assert normalize({key: before[key] for key in HEADERS}) == normalize({key: after[key] for key in HEADERS}), before['fqcn']
        members = {normalize(member) for member in after['members']}
        for member in before['members']:
            assert normalize(member) in members, f"Changed canonical deformer member: {before['fqcn']}: {member}"
    assert len(baseline) == 6
    print(f"PLAYER DEFORMER API PASS: {len(baseline)} canonical types / {sum(len(row['members']) for row in baseline)} declared members; Identifier and non-generic PlayerModel adaptations")


if __name__ == '__main__':
    main()
