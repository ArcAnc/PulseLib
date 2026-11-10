#!/usr/bin/env python3
"""Audit canonical GPU timer and performance guards against native PGl adapters.
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

REPORT = ROOT / 'docs/porting/evidence/stage-7.1b.2b.5-inventory.json'
PREFIX = 'tests/testmod/src/main/java/com/arcanc/pulselib/test/'


def build():
    sources = ['GpuTiming.java', 'OitPerformanceHarness.java', 'WorldOitPerformanceHarness.java']
    digests = {}
    proof = {}
    with tempfile.TemporaryDirectory(prefix='pulselib-perf-') as temporary:
        base = Path(temporary)
        for name in sources:
            original = git('show', PIN + ':' + PREFIX + name)
            digests[PREFIX + name] = hashlib.sha256(original).hexdigest()
            (base / name).write_bytes(original)
        before, after = scan(base), scan(ROOT / 'tests/testmod/demo/common/java/com/arcanc/pulselib/test')
        for name in sources:
            guards = lambda records: [r for r in records if Path(r['path']).name == name and r['kind'] == 'guard' and r['callee'] in ['check', 'SmokeHarness.check', 'RegressionChecks.check']]
            key = lambda r: json.dumps(r['arguments'], ensure_ascii=False)
            expected, actual = Counter(map(key, guards(before))), Counter(map(key, guards(after)))
            assert not expected - actual, (name, 'Canonical GPU guard changed/lost', expected - actual)
            proof[name] = {'canonicalSites': sum(expected.values()), 'preservedSites': sum(expected.values()), 'additionalSites': sum((actual-expected).values()), 'exactArgumentsMessagesAndThresholds': True}
        groups = {'GpuTiming.java': ['start','begin','end','fullscreen','allocation','bufferReallocation','finish','drain'],
                  'OitPerformanceHarness.java': ['fabulous','quads','mean','textureBytes','availableVram'],
                  'WorldOitPerformanceHarness.java': ['quads','mean','beginFrame','endFrame']}
        bodies_proof = {}
        for name, methods in groups.items():
            tool = base / 'H14Bodies.java'
            tool.write_text(BODY_TOOL.replace('Set.of("suite", "expected", "color", "quad", "assertRgb")', 'Set.of('+', '.join(json.dumps(m) for m in methods)+')'))
            def body(path):
                rows = subprocess.check_output(['java',str(tool),str(path)],text=True).splitlines()
                return {n:base64.b64decode(v) for n,v in (row.split('\t') for row in rows)}
            old = base / name
            old.write_text(old.read_text().replace('SmokeHarness.check','RegressionChecks.check'))
            target = ROOT / 'tests/testmod/demo/common/java/com/arcanc/pulselib/test' / name
            current = base / 'adapted' / name
            current.parent.mkdir(exist_ok=True)
            text = target.read_text()
            for statement in ['created += 2;', 'deleted += 2;', 'observedAllocations++;', 'observedBufferReallocations++;']:
                text = text.replace(statement, '')
            current.write_text(text)
            a,b = body(old),body(current)
            assert a == b, (name, 'Canonical timer/statistics body changed', [m for m in a if a[m] != b[m]])
            bodies_proof[name] = {n:hashlib.sha256(v).hexdigest() for n,v in sorted(a.items())}
    return {'checkpoint':'7.1b.2b.5','pin':PIN,'sourceSha256':digests,'guardProof':proof,'bodyProof':bodies_proof,
            'bodyPolicy':'Seventeen full timer/statistics/world interval bodies match. Only redundant single-statement blocks, shared helper owner name, and four lifetime-counter statements are normalized.',
            'sampling':{'offscreen':{'configurations':24,'warmup':32,'samples':80,'resolutions':[[1920,1080],[2560,1440],[3840,2160]],'quads':[0,1,16,128]},'world':{'configurations':6,'warmup':64,'samples':80,'resolution':[1920,1080],'quads':[0,1,0,1,16,128]}},
            'nativeAdapters':['Legacy GlWeightedBlendedOit -> native per-target PGlWeightedBlendedOit owners; real RenderPass.draw calls supply compositor/metadata spans and fullscreen counts. Depth/accumulation spans retain actual native pass boundaries.',
                              'Instance/indirect fallback glBufferData calls use current stream uploadHeaders/upload return-value callbacks; deformer stream counts physical buffers from actual MappableRingBuffer construction. Ring storage events are not per-write counts.',
                              'Removed PostChain constructor/temp-targets/process -> actual ShaderManager transparency + FrameGraphBuilder with persistent five inputs and pooled internal targets. Native vanilla_framegraph_passes replaces legacy hard-coded vanilla_fullscreen_draws; no draw-count equivalence claimed.',
                              'Typed native GlTexture owners and real byte queries replace legacy int texture arrays; resource stability covers native owner maps/geometry/streams/rings, excludes intentionally rotating sync fences.',
                              'Original quad positions/colors/depth distribution retained; existing atlasQuad supplies white atlas UVs. Explicit OIT on existing type, no new pipeline/backend/shader.',
                              'GraphicsStatus -> two Improved Transparency routes; original Fancy/Fabulous CSV labels remain aliases. WeatherData/time command/teleport/window handles use native APIs.',
                              'Standalone exclusive pulseOitPerf demo profile; test-only pulsePerfWorld supplies existing world property and creates a fresh marked integrated save. Whole GameRenderer interval wraps original call exactly once; row writes/drain occur after interval.',
                              'Calibration uses genuine fallback uploads and physical GPU deformer ring construction; real warmup OIT texture allocations must be observed. Query lifetime counters prove all names retired/deleted.',
                              'Offscreen skip-composite control bypasses actual compositor, retaining original draw-count oracle.'],
            'remaining':['Remaining full canonical flags/Gecko and combined orchestration','7.2 final semantic/compiled API and GPU/performance report; these samples do not establish performance acceptance']}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write',action='store_true')
    args=parser.parse_args()
    result=build();rendered=json.dumps(result,indent=2,ensure_ascii=False)+'\n'
    if args.write:
        REPORT.write_text(rendered)
    else:
        assert REPORT.read_text()==rendered,'GPU performance inventory drift'
    print('CANONICAL GPU PERFORMANCE PROOF PASS:',result['guardProof'],'; seventeen full bodies retained')


if __name__=='__main__':
    main()
