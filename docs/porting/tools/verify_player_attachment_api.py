#!/usr/bin/env python3
"""Verify canonical player attachment declarations for checkpoint 5.2a.
ArcAnc, created 26.10.2026, copyright 2026. Arc's License of Common Sense; see LICENSE.txt.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
PREFIX = 'com.arcanc.pulselib.content.player.animation.attachment.'
TYPES = ['PPlayerAnimatedAttachmentContext', 'PPlayerAnimatedAttachmentRenderer',
         'PPlayerAnimatedAttachments', 'PPlayerAnimationMeshAttachmentPose', 'PPlayerAutomaticMeshAttachments']
HEADERS = ['kind', 'modifiers', 'annotations', 'type_parameters', 'extends', 'implements', 'permits', 'record_components']


def normalize(value):
    return json.dumps(value, sort_keys=True).replace('ResourceLocation', 'Identifier').replace('MultiBufferSource', 'SubmitNodeCollector')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    directory = ROOT / 'docs/porting/26.1.2-multiloader'
    for line in (directory / 'checksums.sha256').read_text().splitlines():
        digest, name = line.split()
        assert hashlib.sha256((directory / name).read_bytes()).hexdigest() == digest, name
    baseline = {row['fqcn']: row for row in map(json.loads, (directory / 'api-1.21.1-multiloader.ndjson').read_text().splitlines())}
    with tempfile.TemporaryFile(mode='w+') as output:
        subprocess.run(['java', str(ROOT / 'docs/porting/tools/ExtractApi.java'), str(ROOT)], stdout=output, check=True, cwd=ROOT)
        output.seek(0)
        rows = [json.loads(line) for line in output]
    results = []
    for short in TYPES:
        name = PREFIX + short
        matches = [row for row in rows if row['fqcn'] == name]
        assert len(matches) == 1, f'Missing/duplicate attachment type: {name}'
        before, after = baseline[name], matches[0]
        assert normalize({k: before[k] for k in HEADERS}) == normalize({k: after[k] for k in HEADERS}), f'Attachment type contract: {name}'
        actual = {normalize(member) for member in after['members']}
        for member in before['members']:
            assert normalize(member) in actual, f'Canonical attachment member missing/changed: {name}: {member}'
        results.append({'type': name, 'preservedMembers': len(before['members'])})
    context = next(row for row in rows if row['fqcn'] == PREFIX + 'PPlayerAnimatedAttachmentContext')
    assert 'public SubmitNodeCollector submitNodeCollector()' in {member['signature'] for member in context['members']}
    report = {'status': 'PASS', 'baselineCommit': '7ce6f024b307bb821ce543344d3896c8e1fcf26b', 'types': results,
              'minecraftAdaptations': ['ResourceLocation -> Identifier', 'MultiBufferSource -> SubmitNodeCollector'],
              'retainedNativeAdapters': ['PPlayerAnimatedAttachmentContext.submitNodeCollector()'],
              'limits': 'Source declaration audit for attachment APIs. Native RenderLayer, rig/item/camera/armor/deformer hooks remain the next 5.2 checkpoint; full compiled API audit remains stage 7.'}
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2) + '\n')
    print(f"PLAYER ATTACHMENT API PASS: {len(results)} canonical types / {sum(row['preservedMembers'] for row in results)} declared members")


if __name__ == '__main__':
    main()
