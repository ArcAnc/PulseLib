#!/usr/bin/env python3
"""Inventory the frozen canonical testmod and verify migrated animation assertions with javac AST.
ArcAnc, created 06.11.2026, copyright 2026.
Arc's License of Common Sense; see LICENSE.txt at repository root.
"""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import re
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
PIN = '7ce6f024b307bb821ce543344d3896c8e1fcf26b'
MIGRATED = {'AnimationLifecycleHarness.java', 'AnimationSamplingHarness.java'}
REPORT = ROOT / 'docs/porting/evidence/stage-7.1b.2a-inventory.json'


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT)


def scan(folder):
    output = subprocess.check_output(['java', str(ROOT / 'tests/testmod/tools/ExtractHarness.java'), str(folder)], cwd=ROOT, text=True)
    return [json.loads(line) for line in output.splitlines()]


def build():
    with tempfile.TemporaryDirectory(prefix='pulselib-harness-') as temporary:
        base = Path(temporary)
        files = git('ls-tree', '-r', '--name-only', PIN, 'tests/testmod').decode().splitlines()
        java = {}
        for name in files:
            if name.endswith('.java'):
                data = git('show', PIN + ':' + name)
                relative = str(Path(name).relative_to('tests/testmod'))
                target = base / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(data)
                java[relative] = hashlib.sha256(data).hexdigest()
        canonical = scan(base)
    current = scan(ROOT / 'tests/testmod/demo')
    # javac prints expression trees consistently: whitespace inside literals remains significant.
    proof = {}
    for name in sorted(MIGRATED):
        before = [entry for entry in canonical if Path(entry['path']).name == name and entry['kind'] == 'guard' and entry['callee'] == 'check']
        after = [entry for entry in current if Path(entry['path']).name == name and entry['kind'] == 'guard' and entry['callee'] == 'check']
        key = lambda entry: json.dumps(entry['arguments'], ensure_ascii=False)
        expected, actual = Counter(map(key, before)), Counter(map(key, after))
        assert not expected - actual, (name, 'Canonical assertion changed/lost', expected - actual)
        proof[name] = {'canonicalCheckSites': sum(expected.values()), 'preservedSites': sum(expected.values()), 'additionalCheckSites': sum((actual - expected).values()), 'tolerancesAndMessagesUnchanged': True}
    for entry in canonical:
        name = Path(entry['path']).name
        if name in MIGRATED:
            entry['disposition'] = 'migrated-animation-suite'
            entry['target'] = 'tests/testmod/demo/common/java/com/arcanc/pulselib/test/' + name
        elif '/content/registration/' in entry['path'] or name in {'DemoContent.java', 'DemoClient.java', 'CommonTestPlugin.java', 'ClientTestPlugin.java'}:
            entry['disposition'] = 'content-port-7.1b.1; residual legacy harness calls remain deferred'
            entry['target'] = 'docs/porting/26.1.2-multiloader.md#64-этап-71b1--canonical-demo-content-и-sourcejar-modes-06112026'
        else:
            entry['disposition'] = 'deferred-consolidation; no assertion equivalence or new runtime PASS claimed'
            entry['target'] = '7.1b.2b'
    convention = git('show', PIN + ':buildSrc/src/main/groovy/multiloader-testmod.gradle').decode()
    readme = git('show', PIN + ':tests/testmod/README.md').decode()
    flags = sorted(set(re.findall(r'\bpulse[A-Z][A-Za-z0-9_]*', convention + '\n' + readme)))
    config = json.loads(git('show', PIN + ':tests/testmod/src/main/resources/pulselib-test.mixins.json'))
    groups = {
        'ItemStackIdentityHarness': {'existingEvidence': ['stage-6.2c.2.json'], 'remaining': 'Reconnect/reopen/drop/pickup/GC/server hurtAndBreak and complete canonical menu state machine.'},
        'SmokeHarness': {'existingEvidence': ['stage-2.json', 'stage-4.2c.json', 'stage-6.1b.2b.json'], 'remaining': 'Consolidate plugin, camera, loaded-resource and natural GPU lifecycle assertions; do not silently replace canonical tests with old PASS markers.'},
        'PlayerMotionHarness': {'existingEvidence': ['stage-5.2b.5a.json', 'stage-5.2b.5b.json'], 'remaining': 'Native camera/first-person presentation and complete full-mode orchestration.'},
        'RenderRegressionHarness/WorldRenderRegressionHarness/H14Harness': {'existingEvidence': ['stage-6.2b.json', 'stage-6.2c.2.json'], 'remaining': 'Adapt native 26.1.2 targets/injections and consolidate pixel expectations, recovery and mode flags.'},
        'OitPerformanceHarness/WorldOitPerformanceHarness/GpuTiming/allocation Mixins': {'existingEvidence': [], 'remaining': 'Wire real GPU timer/allocator probes to current backend; measurements belong to 7.2. No CPU timing substitute.'},
    }
    return {'pin': PIN, 'scope': 'Parse-only complete canonical Java inventory; static call sites are not dynamic assertion execution totals. Only the two animation suites are newly consolidated here.', 'javaSourceSha256': java, 'javaUnits': len(java), 'coveragePolicy': 'Every guard/throw/property/annotation call site is indexed. Existing evidence links are context, not claims of assertion-equivalent consolidation.', 'recordCounts': dict(sorted(Counter(entry['kind'] for entry in canonical).items())), 'canonicalGradleConventionSha256': hashlib.sha256(convention.encode()).hexdigest(), 'canonicalFlags': flags, 'flagDisposition': {flag: 'guarded; full canonical wiring remains 7.1b.2b' for flag in flags}, 'canonicalMixinConfig': config, 'migrationProof': proof, 'remainingGroups': groups, 'records': canonical}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write', action='store_true')
    args = parser.parse_args()
    result = build()
    text = json.dumps(result, indent=2, ensure_ascii=False) + '\n'
    if args.write:
        REPORT.write_text(text)
    else:
        assert REPORT.read_text() == text, 'Inventory drift; review pinned sources/coverage and regenerate explicitly'
    print('CANONICAL HARNESS INVENTORY PASS:', result['javaUnits'], 'Java units;', result['recordCounts'])
    print('ANIMATION ASSERTION PROOF:', result['migrationProof'])


if __name__ == '__main__':
    main()
