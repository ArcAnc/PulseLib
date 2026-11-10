#!/usr/bin/env python3
"""Audit full canonical PlayerMotion guards, registrations and independent matrix/camera/physics oracles.
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

REPORT = ROOT / 'docs/porting/evidence/stage-7.1b.2b.4-inventory.json'
SOURCE = 'tests/testmod/src/main/java/com/arcanc/pulselib/test/PlayerMotionHarness.java'


def build():
    original = git('show', PIN + ':' + SOURCE)
    target = ROOT / 'tests/testmod/demo/common/java/com/arcanc/pulselib/test/PlayerMotionHarness.java'
    adaptations = [('ResourceLocation', 'Identifier'), ('SmokeHarness.check', 'RegressionChecks.check'),
                   ('SmokeHarness.field', 'RegressionChecks.field'), ('getPosition()', 'position()')]
    def adapt(text):
        for old, new in adaptations:
            text = text.replace(old, new)
        return text
    with tempfile.TemporaryDirectory(prefix='pulselib-motion-') as temporary:
        base = Path(temporary)
        source = base / 'PlayerMotionHarness.java'
        source.write_text(adapt(original.decode()))
        before, after = scan(base), scan(target.parent)
        guards = lambda records: [r for r in records if Path(r['path']).name == source.name and r['kind'] == 'guard' and r['callee'].endswith('.check')]
        key = lambda row: json.dumps(row['arguments'], ensure_ascii=False)
        expected, actual = Counter(map(key, guards(before))), Counter(map(key, guards(after)))
        assert not expected - actual, ('Canonical player motion guard changed/lost', expected - actual)
        methods = ['registerModels', 'registerAnimations', 'registerAttachments', 'submitted', 'createObservers', 'stopFixtures',
                   'verifyTransition', 'verifyWeightedRoot', 'verifyMultipleDefinitions', 'beforeCamera', 'cameraBaseline',
                   'verifyNativeCamera', 'sequentialRotation', 'rotation', 'sequential', 'matrix', 'check']
        tool = base / 'H14Bodies.java'
        tool.write_text(BODY_TOOL.replace('Set.of("suite", "expected", "color", "quad", "assertRgb")',
                                        'Set.of(' + ', '.join(json.dumps(m) for m in methods) + ')'))
        def bodies(path):
            rows = subprocess.check_output(['java', str(tool), str(path)], text=True).splitlines()
            return {name: base64.b64decode(body) for name, body in (row.split('\t') for row in rows)}
        old, new = bodies(source), bodies(target)
        assert set(old) == set(new) == set(methods)
        assert old == new, ('Canonical oracle/registration body changed', [m for m in old if old[m] != new[m]])
    convention = (ROOT / 'buildSrc/src/main/groovy/multiloader-testmod.gradle').read_text()
    for flag in ['pulsePlayerMotionTest', 'pulseMotionPhase']:
        assert flag in convention
    assert '-Dpulselib.test.playerMotion=true' in convention and "run.systemProperty 'pulselib.test.playerMotion', 'true'" in convention
    return {'checkpoint': '7.1b.2b.4', 'pin': PIN, 'source': SOURCE, 'sourceSha256': hashlib.sha256(original).hexdigest(),
            'guardProof': {'canonicalSites': sum(expected.values()), 'preservedSites': sum(expected.values()),
                           'additionalSites': sum((actual - expected).values()), 'exactArgumentsMessagesTolerancesAfterApiRenames': True},
            'oracleBodyProof': {name: hashlib.sha256(body).hexdigest() for name, body in sorted(old.items())},
            'bodyPolicy': 'Seventeen complete registration/oracle/helper bodies match after declared API renames; javac structural fingerprints only unwrap redundant single-statement blocks.',
            'provenance': {'author': 'ArcAnc', 'preservedCreationDate': '13.10.2026', 'lastCanonicalCommit': '5b5961705240de348f1d31926dc24311dc44dbf2'},
            'nativeAdapters': ['ResourceLocation -> Identifier, Camera.getPosition -> position, JetBrains Nullable -> JSpecify Nullable; shared RegressionChecks preserves canonical check/field helpers.',
                               'Camera.setup -> actual Camera.alignWithEntity. Wrap calls original once; native setPosition(DDD) AFTER captures baseline before production TAIL animation integration; detached argument comes from current Options camera mode because native Camera.detached is stale at that boundary.',
                               'RenderType package moved; original three-argument submitEntityMesh/submitFirstPersonMesh HEAD observes unchanged PInstanceHeader.transform.',
                               'Attachment callback buffer argument -> SubmitNodeStorage, matching native deferred submit API.',
                               'Direct matrix probes use an existing OFFSCREEN local compiler, observe real submissions and discard only their local work in finally; do not append post-frame work to the native world queue.',
                               'Standalone fresh-world driver replaces old Smoke world prerequisites; frame driver runs at actual GameRenderer.render TAIL. Full original phases/weights/waits/reload/server isolation/respawn retained.',
                               'Canonical playerMotion and motionPhase properties retained; only mp2 disables camera/F/G/H/VANILLA, exactly as in source. Other values retain full CAMERA path.',
                               'Test-only skip-submission bypasses actual third-person queue call during direct probe, preserving every oracle and native world render.'],
            'expectedMainCases': {'all': 420, 'mp2': 240, 'formats': 2, 'blendModes': 5, 'weights': [0.25, 0.6, 1], 'epochs': 2},
            'remaining': ['Native GPU timing/allocation hooks and remaining full canonical flags/Gecko', '7.2 final semantic/compiled API and GPU/performance report']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write', action='store_true')
    args = parser.parse_args()
    result = build()
    rendered = json.dumps(result, indent=2, ensure_ascii=False) + '\n'
    if args.write:
        REPORT.write_text(rendered)
    else:
        assert REPORT.read_text() == rendered, 'Player motion inventory drift'
    print('CANONICAL PLAYER MOTION PROOF PASS:', result['guardProof'], '; seventeen full bodies preserved')


if __name__ == '__main__':
    main()
