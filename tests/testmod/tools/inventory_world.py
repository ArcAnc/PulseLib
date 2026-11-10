#!/usr/bin/env python3
"""Prove native H13/H14 guard and ordered-alpha oracle preservation.
ArcAnc, created 10.11.2026, copyright 2026.
Arc's License of Common Sense; see LICENSE.txt at repository root.
"""
import argparse
import base64
from collections import Counter
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile

sys.dont_write_bytecode = True
from inventory_harnesses import ROOT, PIN, git, scan
from inventory_h14 import BODY_TOOL

REPORT = ROOT / 'docs/porting/evidence/stage-7.1b.2b.2b.2-inventory.json'


def build():
    sources = {}
    proofs = {}
    with tempfile.TemporaryDirectory(prefix='pulselib-world-') as temporary:
        base = Path(temporary)
        for name in ['WorldRenderRegressionHarness', 'H14Harness']:
            path = 'tests/testmod/src/main/java/com/arcanc/pulselib/test/' + name + '.java'
            data = git('show', PIN + ':' + path)
            (base / (name + '.java')).write_bytes(data)
            sources[path] = hashlib.sha256(data).hexdigest()
        old = scan(base)
        directory = ROOT / 'tests/testmod/demo/common/java/com/arcanc/pulselib/test'
        new = scan(directory)
        for old_name, new_name, methods in [('WorldRenderRegressionHarness', 'WorldRenderRegressionHarness', None), ('H14Harness', 'H14WorldHarness', {'worldChecks'})]:
            def guards(records, name):
                return [r for r in records if Path(r['path']).stem == name and r['kind'] == 'guard' and r['callee'] == 'check' and (methods is None or r['method'].split('(')[0] in methods)]
            def key(record):
                return json.dumps(record['arguments'], ensure_ascii=False).replace('\\"pulselib-smoke\\"', 'DemoChecks.WORLD')
            wanted = Counter(map(key, guards(old, old_name)))
            current = Counter(map(key, guards(new, new_name)))
            assert not wanted - current, (old_name, wanted - current)
            proofs[old_name] = {'canonicalCheckSites': sum(wanted.values()), 'preservedSites': sum(wanted.values()), 'exactGuardTreesAfterDeclaredAdaptation': True}
        # Compare complete same-frame compositing oracles, including ordering/depth/alpha math.
        tool = base / 'H14Bodies.java'
        tool.write_text(BODY_TOOL.replace('Set.of("suite", "expected", "color", "quad", "assertRgb")', 'Set.of("fancyWorldComposite", "fabulousWorldComposite")'))
        def bodies(path, adapted):
            text = path.read_text()
            if adapted:
                text = text.replace('WorldRegressionRun.h14Active()', 'Boolean.getBoolean("pulselib.test.oit")').replace('H14OffscreenHarness.color(', 'color(')
            normalized = base / ('New.java' if adapted else 'Old.java')
            normalized.write_text(text)
            rows = subprocess.check_output(['java', str(tool), str(normalized)], text=True).splitlines()
            return {name: base64.b64decode(body) for name, body in (r.split('\t') for r in rows)}
        before, after = bodies(base / 'H14Harness.java', False), bodies(directory / 'H14WorldHarness.java', True)
        assert len(before) == 2 and before == after, 'H14 world compositing oracle drift'
    return {'checkpoint': '7.1b.2b.2b.2', 'pin': PIN, 'sources': sources, 'guardProofs': proofs,
            'oracleBodyProof': {name: hashlib.sha256(body).hexdigest() for name, body in before.items()},
            'guardAdaptations': ['Only the world filename literal pulselib-smoke becomes DemoChecks.WORLD (pulselib-bootstrap), retaining the same normalized run-owned save check and original message.'],
            'bodyAdaptations': ['OIT system-property gate becomes the scoped active H14 world phase; shared canonical color helper is qualified. Redundant one-statement blocks are unwrapped by the same structural fingerprint as the offscreen proof.'],
            'nativeAdapters': ['GraphicsStatus Fast/Fancy both map to MC26 Improved Transparency=false; Fabulous maps to true. Twelve H13 scenarios retained, with two distinct native routes.', 'ProjectionMatrixBuffer CPU mirror via PGlRenderHost, native camera position(), GlTexture read FBO with pack-state restoration, existing white atlas quad.', 'WeatherData replaces ServerLevel weather parameters; native time command replaces removed setDayTime.', 'Real Common stage HEAD submit / TAIL early-depth; existing queue HEAD composite observers read live native inputs.'],
            'remaining': ['RenderRegressionHarness local/entity/manager consumers', 'Legacy H14 baseline routing', 'Canonical pulseSmokeReproduce visibility oracle', 'Full canonical flags, identity/player/performance bundles and final 7.2 report']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write', action='store_true')
    args = parser.parse_args()
    result = build()
    rendered = json.dumps(result, indent=2, ensure_ascii=False) + '\n'
    if args.write:
        REPORT.write_text(rendered)
    else:
        assert REPORT.read_text() == rendered, 'Native world proof drift'
    print('CANONICAL WORLD PROOF PASS:', result['guardProofs'], 'two exact compositing oracle bodies')


if __name__ == '__main__':
    main()
