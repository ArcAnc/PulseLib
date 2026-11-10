#!/usr/bin/env python3
"""Audit the declared native-input adaptation of the canonical camera-away visibility guard.
ArcAnc, created 10.11.2026, copyright 2026.
Arc's License of Common Sense; see LICENSE.txt at repository root.
"""
import argparse
import base64
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile

sys.dont_write_bytecode = True
from inventory_harnesses import ROOT, PIN, git, scan
from inventory_h14 import BODY_TOOL

REPORT = ROOT / 'docs/porting/evidence/stage-7.1b.2b.2c-inventory.json'


def build():
    source = 'tests/testmod/src/main/java/com/arcanc/pulselib/test/SmokeHarness.java'
    convention = 'buildSrc/src/main/groovy/multiloader-testmod.gradle'
    original = git('show', PIN + ':' + source)
    old_convention = git('show', PIN + ':' + convention)
    current = ROOT / 'tests/testmod/demo/common/java/com/arcanc/pulselib/test/VisibilityHarness.java'
    with tempfile.TemporaryDirectory(prefix='pulselib-visibility-') as temporary:
        base = Path(temporary)
        old_path = base / 'SmokeHarness.java'
        old_path.write_bytes(original)
        before = scan(base)
        after = scan(current.parent)
        message = '"Original block entity renderer must run"'
        def guard(records, name):
            return [r for r in records if Path(r['path']).name == name and r['kind'] == 'guard' and r['callee'] == 'check' and r['arguments'][-1] == message]
        old_guard, new_guard = guard(before, 'SmokeHarness.java'), guard(after, 'VisibilityHarness.java')
        assert len(old_guard) == len(new_guard) == 1
        expected = [arg.replace('blockCalls', 'blockSamples') for arg in old_guard[0]['arguments']]
        assert expected == new_guard[0]['arguments']
        tool = base / 'H14Bodies.java'
        tool.write_text(BODY_TOOL.replace('Set.of("suite", "expected", "color", "quad", "assertRgb")', 'Set.of("check")'))
        def body(path):
            rows = subprocess.check_output(['java', str(tool), str(path)], text=True).splitlines()
            return {name: base64.b64decode(value) for name, value in (r.split('\t') for r in rows)}
        old_body, new_body = body(old_path), body(current)
        assert old_body == new_body and len(old_body) == 1
    compact = lambda text: ''.join(text.split())
    assert 'newBlockPos(11,140,14)' in compact(original.decode()) and 'newBlockPos(11,140,14)' in compact(current.read_text())
    assert '"pulselib.test.reproduceCamera"' in original.decode() and '"pulselib.test.reproduceCamera"' in current.read_text()
    assert "'pulseSmokeReproduce': 'reproduceCamera'" in old_convention.decode()
    target_convention = (ROOT / convention).read_text()
    assert "!(it == 'pulseSmokeReproduce' && visibilityRegression)" in target_convention
    return {'checkpoint': '7.1b.2b.2c', 'pin': PIN, 'sources': {source: hashlib.sha256(original).hexdigest(), convention: hashlib.sha256(old_convention).hexdigest()},
            'guardProof': {'canonicalSites': 1, 'adaptedSites': 1, 'argumentTreesMatchAfterInputRename': True, 'messageAndGreaterThanZeroUnchanged': True,
                           'semanticAdaptation': 'blockCalls (BER callback count) -> blockSamples (real GL_SAMPLES_PASSED around actual native Pulse mesh flushes). This is an intentional visibility-input change, not a claim that callback and sample counts are equivalent.'},
            'checkBodySha256': hashlib.sha256(old_body['check']).hexdigest(),
            'canonicalInputs': {'block': [11, 140, 14], 'player': [10.5, 140, 10.5], 'camera': 'FIRST_PERSON, yaw 0/180, pitch 0; minimum 30 player ticks per phase', 'flag': 'pulseSmokeReproduce -> pulselib.test.reproduceCamera, unchanged mapping'},
            'queryBoundaries': ['Wrap native afterBlockEntities and afterParticles with exception-safe begin/end; call the original flush exactly once.', 'Read native LevelRenderState camera/frustum/owner list at actual LevelRenderer.renderLevel entry.', 'Read queries after native PRenderQueue.endWorldFrame; exclude terrain and fullscreen OIT resolve from query scopes.', 'Isolate the original block owner; reject other Pulse block/entity/item/player sources; no synthetic geometry, shader, render type or backend.'],
            'flagBoundary': 'pulseSmokeReproduce is allowed only with exclusive pulseVisibilityRegression + demo source/JAR. It remains rejected in all unfinished full-testmod modes.',
            'remaining': ['Full ItemStackIdentity world/reentry/drop/pickup/GC', 'PlayerMotion', 'GPU timing/allocation hooks and remaining full canonical flags', '7.2 final compiled/semantic API and GPU/performance report']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write', action='store_true')
    args = parser.parse_args()
    result = build()
    rendered = json.dumps(result, indent=2, ensure_ascii=False) + '\n'
    if args.write:
        REPORT.write_text(rendered)
    else:
        assert REPORT.read_text() == rendered, 'Visibility adaptation proof drift'
    print('NATIVE VISIBILITY ADAPTATION PROOF PASS: one original guard/message/throw body; declared GPU input adaptation')


if __name__ == '__main__':
    main()
