#!/usr/bin/env python3
"""Verify the scoped canonical Smoke resource/lifecycle port without completing other suites.
ArcAnc, created 07.11.2026, copyright 2026.
Arc's License of Common Sense; see LICENSE.txt at repository root.
"""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import tempfile

from inventory_harnesses import ROOT, PIN, git, scan

REPORT = ROOT / 'docs/porting/evidence/stage-7.1b.2b.1-inventory.json'


def build():
    prefix = 'tests/testmod/src/main/java/com/arcanc/pulselib/test/'
    sources = ['SmokeResources.java', 'SmokeHarness.java', 'mixin/SmokeLifecycleMixin.java']
    digests = {}
    with tempfile.TemporaryDirectory(prefix='pulselib-lifecycle-') as temporary:
        base = Path(temporary)
        for name in sources:
            data = git('show', PIN + ':' + prefix + name)
            target = base / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
            digests[prefix + name] = hashlib.sha256(data).hexdigest()
        canonical = scan(base)
    current = scan(ROOT / 'tests/testmod/demo/common/java/com/arcanc/pulselib/test')
    guards = lambda entries, name: [e for e in entries if Path(e['path']).name == name and e['kind'] == 'guard' and e['callee'] == 'check']
    before = guards(canonical, 'SmokeResources.java')
    after = guards(current, 'SmokeResources.java')
    # Only one representation adaptation: the MC26 registry owns meshResources.
    key = lambda entry: json.dumps(entry['arguments'], ensure_ascii=False).replace('"geometryResources"', '"meshResources"').replace('\\"geometryResources\\"', '\\"meshResources\\"')
    expected, actual = Counter(map(key, before)), Counter(map(key, after))
    assert not expected - actual, ('Canonical resource assertion changed/lost', expected - actual)
    natural = [e for e in guards(canonical, 'SmokeHarness.java') if e['arguments'][-1] == '"Natural disconnect did not invoke a loader lifecycle hook"']
    migrated = guards(current, 'SmokeLifecycleHarness.java')
    assert len(natural) == 1 and key(natural[0]) in map(key, migrated)
    return {
        'pin': PIN, 'checkpoint': '7.1b.2b.1', 'scope': 'Only SmokeResources and explicit/natural disconnect orchestration. Full Smoke/identity/player/render/performance bundle remains incomplete.',
        'sourceSha256': digests,
        'resourceAssertionProof': {'canonicalCheckSites': len(before), 'preservedSites': sum(expected.values()), 'additionalCheckSites': sum((actual - expected).values()), 'exactArgumentTreesAfterDeclaredAdaptation': True},
        'naturalHookAssertionProof': {'canonicalCheckSites': 1, 'preservedSites': 1, 'exactArgumentTrees': True},
        'adaptations': [
            'geometryResources -> meshResources: native PGlResourceRegistry field; same emptiness guard/message.',
            'Single legacy OIT owner/stream texture IDs -> all native OIT owners, buffers, views, FBOs, VAOs, UBO rings and fences; captured GL names must be live.',
            'Native Minecraft.disconnect(TitleScreen, false); natural deletion guards execute inside PulseRuntime.disconnected TAIL before GL names can be reused. Suppressed-hook control executes the same guards on the next tick.',
            'AnimationSamplingHarness waits for actual maintenance callbacks in addition to the existing 21s/two-boundary prerequisites; all assertions and the 90s deadline remain unchanged.',
            'Smoke graphics draw is not claimed as migrated; a dedicated workload populates CPU/GPU deformation and explicit ORDINARY/OIT on the existing material type. Native generated-primitives query proves draw execution.',
        ],
        'helperProvenance': {'source': 'tests/stage1/neoforge/java/com/arcanc/pulselib/test/stage1/Stage42cResources.java', 'creationCommit': '60a81599436ea544b0f42257551a7d497cd5d66e', 'preservedDate': '25.10.2026'},
        'completed': ['SmokeResources: all canonical guards', 'SmokeHarness.runExplicitCleanup/seedManagers/Dummy lifecycle slice', 'SmokeHarness natural disconnect guard/observation slice', 'SmokeLifecycleMixin disconnect HEAD observation and negative cancellation; added immediate TAIL verification'],
        'remaining': ['Smoke plugin/camera/graphics/world orchestration', 'ItemStackIdentity full suite', 'PlayerMotion full suite', 'RenderRegression/World/H14 native injections', 'GPU timing/allocation hooks', 'Full canonical source/JAR flags', '7.2 final API/GPU/performance report'],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write', action='store_true')
    args = parser.parse_args()
    result = build()
    rendered = json.dumps(result, indent=2, ensure_ascii=False) + '\n'
    if args.write:
        REPORT.write_text(rendered)
    else:
        assert REPORT.read_text() == rendered, 'Lifecycle inventory drift; review before --write'
    print('CANONICAL LIFECYCLE ASSERTION PROOF PASS:', result['resourceAssertionProof'], result['naturalHookAssertionProof'])


if __name__ == '__main__':
    main()
