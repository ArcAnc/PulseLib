#!/usr/bin/env python3
"""Audit canonical launch flags and composed driver boundaries.
ArcAnc, created 11.11.2026, copyright 2026.
Arc's License of Common Sense. See LICENSE.txt at repository root.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import sys

sys.dont_write_bytecode = True

from inventory_harnesses import ROOT, PIN, git

REPORT = ROOT / 'docs/porting/evidence/stage-7.1b.2b.6-inventory.json'
CONVENTION = 'buildSrc/src/main/groovy/multiloader-testmod.gradle'
DEMO = 'tests/testmod/demo/common/java/com/arcanc/pulselib/'
ORIGINAL = 'tests/testmod/src/main/java/com/arcanc/pulselib/'
FLAGS = {
    'pulseTestMod': 'external demo source profile',
    'pulseTestJar': 'external packaged demo profile',
    'pulseSmokeAuto': 'automatic smoke and animation/lifecycle composition',
    'pulseSmokeWorld': 'fresh marked integrated world',
    'pulseOitTest': 'native offscreen OIT and optional world composition',
    'pulseOitPerf': 'existing native GPU performance driver',
    'pulsePlayerMotionTest': 'existing full player motion driver',
    'pulseItemIdentityTest': 'existing full item identity driver',
    'pulseGeckoDemo': 'existing entity and armor Gecko parser selection',
    'pulseMotionPhase': 'existing motion phase selector',
    'pulseOitBaseline': 'existing H14 baseline writer',
    'pulseSmokeReproduce': 'existing camera visibility negative control',
    'pulseSmokeSkipHook': 'actual disconnect hook negative control',
}


def build():
    original = git('show', PIN + ':' + CONVENTION)
    flags = set(re.findall(r"'(pulse\w+)'", original.decode()))
    assert flags == set(FLAGS), ('Frozen launch flag inventory changed', flags)
    convention = (ROOT / CONVENTION).read_text()
    assert set(FLAGS) <= set(re.findall(r"'(pulse\w+)'", convention))
    dispatch_path = ORIGINAL + 'test/mixin/SmokeRenderMixin.java'
    dispatch = git('show', PIN + ':' + dispatch_path)
    priority = ['itemIdentity', 'playerMotion', 'oitPerf', 'oit', 'auto']
    body = dispatch.decode().split('pulselibtest$checkResources', 1)[1]
    assert re.findall(r'Boolean.getBoolean\("pulselib.test.(\w+)"\)', body) == priority
    route = convention.split("if (canonicalMode) {", 1)[1].split('def canonicalSmoke', 1)[0]
    assert re.findall(r"canonicalRoute = '(\w+)'", route) == ['manual', 'identity', 'motion', 'perf']
    assert "canonicalBaseline ? 'baseline' : 'oit'" in route
    assert "canonicalWorld ? 'visibility' : 'smoke'" in route
    assert "testmodJar.configure { dependsOn demoJar }" in convention
    assert "tasks.register('remapTestmodJar')" in convention
    assert convention.count("if (geckoDemo)") == 2
    assert convention.count('pulselib.test.geckoEntity') == 2
    sources = {}
    for name in ['TestEntityRender', 'PTestArmor']:
        path = DEMO + 'content/registration/entity/renderer/' + name + '.java'
        data = (ROOT / path).read_bytes()
        # Renderer/parser bodies were accepted in the earlier content checkpoint.
        assert data == git('show', '6549600cf31d063c1ef46f9d8bf9d2b27499a67b:' + path)
        sources[path] = hashlib.sha256(data).hexdigest()
    entity = (ROOT / DEMO / 'content/registration/entity/renderer/TestEntityRender.java').read_text()
    armor = (ROOT / DEMO / 'content/registration/entity/renderer/PTestArmor.java').read_text()
    assert 'Boolean.getBoolean("pulselib.test.geckoEntity") ? PGeckoModelLoader.INSTANCE.id() : PGltfModelLoader.INSTANCE.id()' in entity
    assert 'TestEntityRender.MODEL_DATA.getModelLoaderId()' in armor
    invariants = {
        'CanonicalRun.java': [
            'RenderRegressionHarness.run();', 'SmokeLifecycleHarness.explicitOnly();',
            'WorldRegressionRun.beginExistingWorld(setup);', 'DemoProbe.regressionsComplete();',
            'H14OffscreenHarness.baseline();', 'finally\n\t\t{\n\t\t\trenderProbe = false;',
        ],
        'DemoChecks.java': [
            'CanonicalProfile.smokeWorld() && !result.containsKey("combined")',
            'CanonicalProfile.oitWorld() && !"canonical-oit".equals(result.get("fixture"))',
        ],
        'WorldRegressionRun.java': ['ClientTestPlugin.models == initialModelEpochs + 1'],
        'SmokeStages.java': ['CanonicalRun.renderProbeActive()'],
    }
    for name, fragments in invariants.items():
        path = DEMO + 'test/' + name
        text = (ROOT / path).read_text()
        for fragment in fragments:
            assert fragment in text, (name, fragment)
        sources[path] = hashlib.sha256(text.encode()).hexdigest()
    return {
        'checkpoint': '7.1b.2b.6', 'pin': PIN, 'flags': FLAGS,
        'canonicalConventionSha256': hashlib.sha256(original).hexdigest(),
        'canonicalDispatchSha256': hashlib.sha256(dispatch).hexdigest(),
        'canonicalPriority': priority, 'currentSourceSha256': sources,
        'parserBodiesUnchangedSince': '6549600cf31d063c1ef46f9d8bf9d2b27499a67b',
        'compositionInvariants': invariants,
        'proofScope': 'Frozen flags/dispatch, current wiring, accepted parser source bytes and composition boundaries. Actual configured properties and runtime oracles are recorded separately.',
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write', action='store_true')
    args = parser.parse_args()
    rendered = json.dumps(build(), indent=2, ensure_ascii=False) + '\n'
    if args.write:
        REPORT.write_text(rendered)
    else:
        assert REPORT.read_text() == rendered, 'Canonical launch inventory drift'
    print('CANONICAL LAUNCH PROOF PASS, 13 frozen flags and original dispatch order')


if __name__ == '__main__':
    main()
