#!/usr/bin/env python3
"""Check every canonical PLibHelper declaration with explicit Minecraft 26.1 type substitutions.
ArcAnc, created 25.10.2026, copyright 2026. Arc's License of Common Sense; see LICENSE.txt.
"""
import argparse
import copy
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
NAME = 'com.arcanc.pulselib.util.helpers.PLibHelper'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    directory = ROOT / 'docs/porting/26.1.2-multiloader'
    for line in (directory / 'checksums.sha256').read_text().splitlines():
        digest, name = line.split()
        assert hashlib.sha256((directory / name).read_bytes()).hexdigest() == digest, name
    baseline = next(row for row in map(json.loads, (directory / 'api-1.21.1-multiloader.ndjson').read_text().splitlines()) if row['fqcn'] == NAME)
    with tempfile.TemporaryFile(mode='w+') as output:
        subprocess.run(['java', str(ROOT / 'docs/porting/tools/ExtractApi.java'), str(ROOT)], stdout=output, check=True, cwd=ROOT)
        output.seek(0)
        candidates = [row for row in map(json.loads, output) if row['fqcn'] == NAME]
    assert len(candidates) == 1, 'Missing/duplicate PLibHelper'
    current = candidates[0]
    for key in ['kind', 'modifiers', 'annotations', 'type_parameters', 'extends', 'implements', 'permits']:
        assert baseline[key] == current[key], key
    results = []
    for member in baseline['members']:
        expected = copy.deepcopy(member)
        reason = None
        if member['name'] == 'renderModelInGui':
            # Only Minecraft-owned types change. Keep generic bounds, parameter order/names and all Pulse types.
            serialized = json.dumps(expected).replace('GuiGraphics', 'GuiGraphicsExtractor').replace('ResourceLocation', 'Identifier')
            expected = json.loads(serialized)
            reason = 'Minecraft 26.1 GUI extraction replaces immediate GuiGraphics; ResourceLocation became Identifier; RenderType moved to renderer.rendertype. Pose is sampled at invocation and drawn through registered native PIP execution.'
        assert sum(actual == expected for actual in current['members']) == 1, f'Canonical declaration missing or changed: {expected}'
        results.append({'canonical': member['signature'], 'current': expected['signature'], 'minecraftReason': reason})
    imports = (ROOT / 'common/src/main/java/com/arcanc/pulselib/util/helpers/PLibHelper.java').read_text().splitlines()
    for required in ['net.minecraft.client.gui.GuiGraphicsExtractor', 'net.minecraft.resources.Identifier', 'net.minecraft.client.renderer.rendertype.RenderType']:
        assert any(required in item for item in imports), required
    extra = [member['signature'] for member in current['members'] if member['name'] in ['livingRenderState', 'entityRenderState']]
    assert len(extra) == 4
    assert len(current['members']) == len(baseline['members']) + len(extra)
    report = {'status': 'PASS_WITH_ENUMERATED_MINECRAFT_ADAPTATIONS', 'type': NAME,
              'baselineCommit': '7ce6f024b307bb821ce543344d3896c8e1fcf26b',
              'canonicalMembers': len(results), 'members': results, 'additional26_1Factories': extra,
              'baselineChecksums': 'unchanged',
              'scope': 'All declared public/protected PLibHelper source contracts, including parameter metadata; GPU behavior is verified separately. Whole compiled library audit remains stage 7.'}
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2) + '\n')
    print(f'GUI HELPER API PASS: 1 canonical type / {len(results)} declarations; only enumerated Minecraft types changed')


if __name__ == '__main__':
    main()
