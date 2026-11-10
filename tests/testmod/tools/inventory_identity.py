#!/usr/bin/env python3
"""Audit every canonical identity guard and the complete core regression method bodies.
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

REPORT = ROOT / 'docs/porting/evidence/stage-7.1b.2b.3-inventory.json'
SOURCE = 'tests/testmod/src/main/java/com/arcanc/pulselib/test/ItemStackIdentityHarness.java'


def build():
    original = git('show', PIN + ':' + SOURCE)
    target = ROOT / 'tests/testmod/demo/common/java/com/arcanc/pulselib/test/ItemStackIdentityHarness.java'
    with tempfile.TemporaryDirectory(prefix='pulselib-identity-') as temporary:
        base = Path(temporary)
        source = base / 'ItemStackIdentityHarness.java'
        source.write_bytes(original)
        before, after = scan(base), scan(target.parent)
        guards = lambda records: [r for r in records if Path(r['path']).name == source.name and r['kind'] == 'guard' and r['callee'] == 'check']
        key = lambda row: json.dumps(row['arguments'], ensure_ascii=False)
        expected, actual = Counter(map(key, guards(before))), Counter(map(key, guards(after)))
        assert not expected - actual, ('Canonical identity guard changed/lost', expected - actual)
        # MC26 removed ItemStack.parse; use its registry-aware CODEC directly, preserving the round-trip.
        source.write_text(original.decode().replace('ItemStack.parse(registries, saved).orElseThrow()',
                                                  'ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), saved).getOrThrow()'))
        tool = base / 'H14Bodies.java'
        methods = ['regressions', 'concurrentAccess', 'serialization', 'verifyManagers', 'totalAnimated', 'animated', 'data', 'id', 'managers', 'pass']
        tool.write_text(BODY_TOOL.replace('Set.of("suite", "expected", "color", "quad", "assertRgb")', 'Set.of(' + ', '.join(json.dumps(m) for m in methods) + ')'))

        def bodies(path):
            rows = subprocess.check_output(['java', str(tool), str(path)], text=True).splitlines()
            return {name: base64.b64decode(body) for name, body in (row.split('\t') for row in rows)}

        old, new = bodies(source), bodies(target)
        assert set(old) == set(new) == set(methods)
        assert old == new, ('Core identity regression body changed', [m for m in old if old[m] != new[m]])
    convention = (ROOT / 'buildSrc/src/main/groovy/multiloader-testmod.gradle').read_text()
    original_convention = git('show', PIN + ':buildSrc/src/main/groovy/multiloader-testmod.gradle').decode()
    assert "testProperties['pulselib.test.itemIdentity'] = 'true'" in original_convention
    assert '-Dpulselib.test.itemIdentity=true' in convention and "run.systemProperty 'pulselib.test.itemIdentity', 'true'" in convention
    assert "!(it == 'pulseItemIdentityTest' && itemIdentity)" in convention
    return {'checkpoint': '7.1b.2b.3', 'pin': PIN, 'source': SOURCE, 'sourceSha256': hashlib.sha256(original).hexdigest(),
            'guardProof': {'canonicalSites': sum(expected.values()), 'preservedSites': sum(expected.values()),
                           'additionalSites': sum((actual - expected).values()), 'exactArgumentTreesIncludingMessagesAndThresholds': True},
            'coreBodyProof': {name: hashlib.sha256(body).hexdigest() for name, body in sorted(old.items())},
            'bodyPolicy': 'Javac structural fingerprints retain nesting, node kinds, identifiers, members, variables and literals. Only redundant single-statement blocks are unwrapped. Ten entire core/helper bodies match after the sole declared NBT parse API adaptation.',
            'provenance': {'author': 'ArcAnc', 'preservedCreationDate': '14.10.2026', 'lastCanonicalCommit': '5ae02648f36cae466c52486c97aa6890e5630f78'},
            'adapters': ['ClickType -> ContainerInput; handleInventoryMouseClick -> handleContainerInput: same native prediction and ordinary server packet.',
                         'MultiPlayerGameMode.hasInfiniteItems -> Player.hasInfiniteMaterials; selected -> setSelectedSlot; serverLevel -> level; teleportTo adds empty relative set and reset-camera argument.',
                         'Registry-aware ItemStack.CODEC.parse replaces removed ItemStack.parse, same NBT data and fresh runtime ID guards.',
                         'Minecraft.disconnect(TitleScreen,false) replaces old level.disconnect + disconnect; no explicit Pulse cleanup call. Existing runtime mixin observes the real loader hook.',
                         'Standalone tick driver opens one fresh marked world, waits for native readiness, then runs the original phases with original tick waits/deadlines. Reentry requires this JVM to have created that exact marked save.',
                         'Full pulseItemIdentityTest -> pulselib.test.itemIdentity mapping retained within exclusive demo source/JAR; integrated server exercises real hurtAndBreak. No dedicated server required.',
                         'Test-only pulseIdentitySkipHook cancels the actual PulseRuntime.disconnected callback; same unchanged natural cleanup assertion must fail.'],
            'completed': ['16 core regressions including 100000 copies, eight concurrent workers, server hurtAndBreak, NBT/network, independent managers/controllers/phases',
                          'Inventory pickup/hotbar/reopen/split/merge/offhand/chest quick move, authoritative sync, drop/pickup and 18-count recovery',
                          'Natural disconnect cleanup, retained survivor ID, weak-reference collection, saved-world reentry and new deserialized IDs, controller restart, final disconnect'],
            'remaining': ['PlayerMotion full canonical orchestration', 'GPU timing/allocation and remaining canonical full flags/Gecko', '7.2 semantic/compiled API and GPU/performance report']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write', action='store_true')
    args = parser.parse_args()
    result = build()
    rendered = json.dumps(result, indent=2, ensure_ascii=False) + '\n'
    if args.write:
        REPORT.write_text(rendered)
    else:
        assert REPORT.read_text() == rendered, 'Identity inventory drift; review before --write'
    print('CANONICAL IDENTITY PROOF PASS:', result['guardProof'], '; ten full core bodies preserved')


if __name__ == '__main__':
    main()
