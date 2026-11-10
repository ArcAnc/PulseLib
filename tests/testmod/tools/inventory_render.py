#!/usr/bin/env python3
"""Prove the canonical RenderRegression and legacy H14 baseline guards/oracles.
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

REPORT = ROOT / 'docs/porting/evidence/stage-7.1b.2b.2b.3-inventory.json'


def build():
    source_hashes = {}
    proofs = {}
    with tempfile.TemporaryDirectory(prefix='pulselib-render-') as temporary:
        base = Path(temporary)
        for name in ['RenderRegressionHarness', 'H14Harness']:
            path = 'tests/testmod/src/main/java/com/arcanc/pulselib/test/' + name + '.java'
            data = git('show', PIN + ':' + path)
            (base / (name + '.java')).write_bytes(data)
            source_hashes[path] = hashlib.sha256(data).hexdigest()
        old = scan(base)
        directory = ROOT / 'tests/testmod/demo/common/java/com/arcanc/pulselib/test'
        new = scan(directory)
        for old_name, new_name, method in [('RenderRegressionHarness', 'RenderRegressionHarness', None), ('H14Harness', 'H14OffscreenHarness', 'baseline')]:
            def guards(records, name):
                return [r for r in records if Path(r['path']).stem == name and r['kind'] == 'guard' and r['callee'] == 'check' and (method is None or r['method'].split('(')[0] == method)]
            def key(record):
                return json.dumps(record['arguments'], ensure_ascii=False).replace('H14Harness.', 'H14OffscreenHarness.').replace('RenderSystem.getShader()', 'GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM)').replace('PGpuDeformerBuffers.streams().deformers().values()', 'PGpuDeformerBuffers.values()')
            wanted, current = Counter(map(key, guards(old, old_name))), Counter(map(key, guards(new, new_name)))
            assert not wanted - current, (old_name, wanted - current)
            proofs[old_name] = {'canonicalCheckSites': sum(wanted.values()), 'preservedSites': sum(wanted.values()), 'exactArgumentTreesAfterDeclaredAdaptations': True}
        tool = base / 'H14Bodies.java'
        tool.write_text(BODY_TOOL.replace('Set.of("suite", "expected", "color", "quad", "assertRgb")', 'Set.of("rgb", "baseline")'))
        def body(path):
            return {name: base64.b64decode(value) for name, value in (row.split('\t') for row in subprocess.check_output(['java', str(tool), str(path)], text=True).splitlines())}
        before = body(base / 'RenderRegressionHarness.java') | body(base / 'H14Harness.java')
        after = body(directory / 'RenderRegressionHarness.java') | body(directory / 'H14OffscreenHarness.java')
        assert len(before) == 2 and before == after, 'Canonical RGB/baseline oracle body drift'
    return {'checkpoint': '7.1b.2b.2b.3', 'pin': PIN, 'sources': source_hashes, 'guardProofs': proofs,
            'oracleBodyProof': {name: hashlib.sha256(value).hexdigest() for name, value in before.items()},
            'guardAdaptations': ['Shared H14 helper qualifier becomes H14OffscreenHarness; helper guard/geometry/readback semantics preserved.', 'Removed ShaderInstance global getter -> linked native GL_CURRENT_PROGRAM identity; same equality predicate/message.', 'Removed streams().deformers().values() -> 26.1 PGpuDeformerBuffers.values(); same active deformation-values emptiness predicate/message.'],
            'nativeAdapters': ['Legacy GraphicsStatus -> existing test WorldGraphics; Fast/Fancy share Improved Transparency=false, Fabulous=true.', 'Legacy raw targets/projection -> native attachment views/ProjectionMatrixBuffer; reuse exact white atlas quad and GL readback/state helpers.', 'Ad-hoc sorted-alpha RenderType -> existing emissive translucent material plus explicit ORDINARY policy, preserving source stages and shared sorting.', 'Dispatcher.render -> extractEntity/submit/renderAllFeatures inside an explicit local execution boundary, required by MC26 deferred submission.', 'InventoryScreen.renderEntityInInventory -> actual InventoryScreen extraction and native GuiEntityRenderer.prepare; GPU-copy actual native PiP output to independent 64x64 target.', 'Partial-submission failure uses the real native PiP caller; additionally require every alpha byte of its internal target to remain zero.', 'Legacy H14 baseline composites OIT into weather before the real native transparency chain, retaining the exact original discriminating oracle.'],
            'remaining': ['Canonical pulseSmokeReproduce visibility oracle', 'Full ItemStackIdentity, PlayerMotion, GPU timing/allocation and full canonical flags', '7.2 full semantic/compiled API and GPU/performance report']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write', action='store_true')
    args = parser.parse_args()
    result = build()
    rendered = json.dumps(result, indent=2, ensure_ascii=False) + '\n'
    if args.write:
        REPORT.write_text(rendered)
    else:
        assert REPORT.read_text() == rendered, 'Render proof drift'
    print('CANONICAL RENDER PROOF PASS:', result['guardProofs'], 'two exact RGB/baseline oracle bodies')


if __name__ == '__main__':
    main()
