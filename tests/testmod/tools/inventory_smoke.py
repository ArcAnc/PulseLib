#!/usr/bin/env python3
"""Verify the canonical Smoke world/graphics slice; keep the remaining bundles explicit.
ArcAnc, created 10.11.2026, copyright 2026.
Arc's License of Common Sense; see LICENSE.txt at repository root.
"""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import tempfile

from inventory_harnesses import ROOT, PIN, git, scan

REPORT = ROOT / 'docs/porting/evidence/stage-7.1b.2b.2a-inventory.json'
ADAPTATIONS = {
    '!gecko.bones().isEmpty()': 'gecko.boneCount() > 0',
    'submission.enabled()': 'submission.operationCount() > 0',
    'mc.getBlockEntityRenderDispatcher().getRenderer(block) instanceof TestBlockEntityRenderer':
        '(Object)mc.getBlockEntityRenderDispatcher().getRenderer(block) instanceof TestBlockEntityRenderer',
}


def build():
    prefix = 'tests/testmod/src/main/java/com/arcanc/pulselib/test/'
    files = ['SmokeHarness.java', 'mixin/SmokeStagesMixin.java', 'mixin/SmokeRenderMixin.java', 'mixin/SmokeLayerMixin.java', 'mixin/GlDebugMixin.java', 'GlErrors.java', 'DisposableTestWorlds.java']
    digests = {}
    with tempfile.TemporaryDirectory(prefix='pulselib-smoke-') as temporary:
        base = Path(temporary)
        for name in files:
            data = git('show', PIN + ':' + prefix + name)
            target = base / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
            digests[prefix + name] = hashlib.sha256(data).hexdigest()
        canonical = scan(base)
    current = scan(ROOT / 'tests/testmod/demo/common/java/com/arcanc/pulselib/test')

    def guards(entries):
        return [entry for entry in entries if Path(entry['path']).name == 'SmokeHarness.java' and entry['kind'] == 'guard' and entry['callee'] == 'check']

    # Natural-hook AST parity is already proved by inventory_lifecycle.py.
    # The legacy buffer-capture helper is replaced by the native live-handle
    # inspector in section 66; do not claim exact AST parity for that helper here.
    excluded = {'"Natural disconnect did not invoke a loader lifecycle hook"', '"Geometry arena must allocate GPU buffers"'}
    before = [entry for entry in guards(canonical) if entry['arguments'][-1] not in excluded]
    after = guards(current)

    def key(entry):
        args = entry['arguments'].copy()
        for old, new in ADAPTATIONS.items():
            if new not in args[0]:
                args[0] = args[0].replace(old, new)
        return json.dumps(args, ensure_ascii=False)

    expected, actual = Counter(map(key, before)), Counter(map(key, after))
    assert not expected - actual, ('Canonical Smoke assertion changed/lost', expected - actual)
    original_throws = [entry for entry in canonical if Path(entry['path']).name == 'SmokeStagesMixin.java' and entry['kind'] == 'throw']
    migrated_throws = [entry for entry in current if Path(entry['path']).name == 'SmokeStages.java' and entry['kind'] == 'throw']
    # Full throw trees include exactly the original error messages and phase values.
    throw_key = lambda entry: entry['expression']
    assert Counter(map(throw_key, original_throws)) == Counter(map(throw_key, migrated_throws))
    return {
        'pin': PIN, 'checkpoint': '7.1b.2b.2a', 'sourceSha256': digests,
        'scope': 'Smoke own plugin/world/camera/animation-controller/solid+OIT pixel checks and native stage/layer/render/GL-error observers. No full RenderRegression/World/H14/identity/player/performance claim.',
        'assertionProof': {'canonicalCheckSites': len(before), 'preservedSites': sum(expected.values()), 'additionalCheckSites': sum((actual - expected).values()), 'exactArgumentTreesAfterDeclaredAdaptations': True},
        'stageThrowProof': {'canonical': len(original_throws), 'preserved': len(migrated_throws), 'exactExpressionTrees': True},
        'guardAdaptations': ADAPTATIONS,
        'excludedFromThisAstProof': {
            'Natural disconnect did not invoke a loader lifecycle hook': 'Exact argument-tree parity is covered by inventory_lifecycle.py.',
            'Geometry arena must allocate GPU buffers': 'Legacy captureGeometryBuffers helper is replaced by RegressionResources.all/page live GL-handle checks and SmokeResources nonempty-resource prerequisites (section 66); not claimed as exact argument-tree parity.',
        },
        'nativeAdaptations': [
            'Optional Gecko-entity mode stays guarded. Its geometry check argument tree is retained, but the scoped default profile selects the glTF entity; no Gecko-entity GPU-mode coverage is claimed. Loaded Gecko player rig still exercises the real Gecko parser/controller.',
            'Run only inside the existing opt-in marked demo world; no legacy flags enabled. Original 30-tick world wait, fixed platform/equipment, FIRST_PERSON yaw=180 and THIRD_PERSON_BACK yaw=0 retained.',
            'Smoke-only automated window is hidden with GLFW while preserving its GL context, adapting canonical DisposableTestWorlds.prepareAutomationWindow measurement isolation; demo epochs lock yaw=0/pitch=8 before extraction. Manual/default demo profile is unchanged.',
            'Smoke transient block is removed and actor discarded by the server; native noCollision must pass for the next demo actor, and client removal is awaited before L25 selects all TestEntity owners. Smoke world/draw checks run before demo/retention owners are created in the same area; no teleport/world replacement between retention and removal. Destructive AnimationSamplingHarness.verifyRemovalHooks stays after graphics, restoring canonical ordering. All guards remain unchanged.',
            'GameRenderer.update HEAD locks the camera before extraction; render TAIL drives the GPU slice. PLivingAttachmentLayer.render -> submit. Block counter observes the original live owner in resolveBoneRender.',
            'afterBlockEntities / afterParticles phase guards unchanged. Final guard observes PRenderQueue.endWorldFrame after native weather/composition, including the Fabulous path that bypasses afterWeather.',
            'TextureTarget/ProjectionMatrixBuffer/GlTexture and explicit output views replace legacy bindWrite/projection/frameBufferId; native GL read framebuffer and pixel-pack state restored.',
            'Indexed PBakedModel.bone list replaces recursive roots-only traversal without duplicate children.',
            'Existing trianglesEmissiveTranslucent uses explicit OIT; solid uses ORDINARY. Eight instances, original bend and 64x64 target, visible/partial-alpha thresholds and indexed GL state equality retained.',
            'Native camera yaw is checked at the real Camera after 30 ticks (0/180 degrees, tolerance 0.001). First-person native yaw=180 yields BER callbacks on Fabric but not NeoForge, so the new scoped control expects original world-armor (Fabric) / block-renderer (NeoForge) guards; legacy pulseSmokeReproduce equivalence stays deferred.',
            'GlDebug moved to opengl; enableDebugCallback now returns GlDebug and accepts extension set, printDebugLog is an instance method. Error recording remains synchronous and throws outside native callback.',
            'Canonical animation and cleanup suites stay in their existing harnesses. This slice adds the original single Gecko controller start/evaluate/remove check; it does not duplicate full suites.',
        ],
        'remaining': ['Canonical pulseSmokeReproduce block-visibility negative control: Fabric MC26 BER callbacks still occur at native yaw=180 (NeoForge callback count is zero); it requires a native GPU/frustum oracle, not callback-count equivalence. The new first-person control checks existing world prerequisites (Fabric armor / NeoForge block).', '7.1b.2b.2b: full RenderRegressionHarness/WorldRenderRegressionHarness/H14Harness and their native queue/stage injections', 'ItemStackIdentity world/reentry/drop/pickup/GC', 'PlayerMotion', 'GPU timing/allocation hooks', 'Full canonical source/JAR flags and Gecko entity option', '7.2 final API/GPU/residual/performance report'],
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
        assert REPORT.read_text() == rendered, 'Smoke inventory drift; review before --write'
    print('CANONICAL SMOKE ASSERTION PROOF PASS:', result['assertionProof'], result['stageThrowProof'])


if __name__ == '__main__':
    main()
