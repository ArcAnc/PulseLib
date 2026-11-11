#!/usr/bin/env python3
"""Classify canonical Mixin/lifecycle contracts and verify real Minecraft 26.1 targets.
ArcAnc, created 11.11.2026, copyright 2026.
Arc's License of Common Sense. See LICENSE.txt at repository root.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path
import subprocess
import sys
import tarfile
import tempfile
import zipfile
sys.dont_write_bytecode = True

import verify_gpu_facade_api as gpu
from inventory import ROOT, git

PARENT = '6fd45746c72794a3bb992d4f60c2dc638e8c9e54'
CANONICAL = '7ce6f024b307bb821ce543344d3896c8e1fcf26b'
PREFIX = 'com.arcanc.pulselib.'
COMMON = 'common/src/main/java/com/arcanc/pulselib/'
FABRIC = 'fabric/src/main/java/com/arcanc/pulselib/fabric/'
NEO = 'neoforge/src/main/java/com/arcanc/pulselib/'
RESTORED = ['content.mixin.' + name for name in ['BlockEntityMixin', 'ItemInHandRendererAccessor', 'RenderTypeAccessor',
             'RenderTypeAccessor.CompositeRenderTypeAccessor', 'RenderTypeAccessor.CompositeStateAccessor']]
RESTORED += ['fabric.mixin.MinecraftMixin', 'fabric.mixin.SpriteSourcesAccessor', 'neoforge.NeoForgeClientLifecycle']
# These are concrete implementation relocations or removed Minecraft dependencies, not empty alias classes.
RELOCATED = {
 'content.mixin.EntityRenderDispatcherMixin': ('PiP submission now occurs after native target/projection selection. The old immediate dispatcher wrapper and its abstract constructor are replaced by scoped native PiP execution and failure discard.',
     [COMMON+'content/mixin/PictureInPictureRendererMixin.java', COMMON+'content/renderer/PModelGuiRenderer.java'], [58, 70]),
 'content.mixin.InventoryScreenMixin': ('Inventory previews use native PictureInPictureRenderer.prepare/renderToTexture and the same GUI scope. The old immediate inventory injection and abstract constructor are replaced.',
     [COMMON+'content/mixin/PictureInPictureRendererMixin.java'], [58, 70]),
 'content.mixin.LevelRendererContextMixin': ('WORLD enters at native GameRenderer.renderLevel. The native framegraph pass guarantees endWorldFrame in finally after the transparency inputs are consumed.',
     [COMMON+'content/mixin/GameRendererMixin.java', COMMON+'content/renderer/PWorldRenderPass.java'], [39, 56, 57]),
 'content.mixin.LevelRendererTransparencyMixin': ('PostChain.process(float) is replaced by native framegraph transparency declarations. LevelRenderer retains five targets and PWorldRenderPass composites before their retirement.',
     [COMMON+'content/mixin/LevelRendererMixin.java', COMMON+'content/renderer/PWorldRenderPass.java'], [39, 57]),
 'content.mixin.LivingEntityRendererMixin': ('The existing name now targets PlayerModel.setupAnim(AvatarRenderState). Its old protected EntityModel<T> shadow and two type parameters cannot describe the extracted-state model. Native LivingEntityRenderer.getModel remains the real renderer model accessor; extraction, rig setup and deferred submit own the old apply/restore work.',
     [COMMON+'content/mixin/LivingEntityRendererMixin.java', COMMON+'content/mixin/EntityRendererExtractionMixin.java', COMMON+'content/mixin/PlayerRigSubmitMixin.java', COMMON+'content/mixin/PlayerRootTransformMixin.java'], [34, 35, 36, 41]),
 'content.mixin.VertexBufferAccessor': ('VertexBuffer and its six mutable index/upload accessors were removed. Native geometry is replaced atomically with PGlDynamicGeometry.update(PGeometryData), and native arena pages/rings own GPU storage. No independently mutable fake index fields are introduced.',
     [COMMON+'content/renderer/gl/PGlDynamicGeometry.java', COMMON+'content/renderer/gl/PGlGeometryArena.java', COMMON+'content/renderer/legacy/GlDynamicGeometry.java'], [23, 77]),
 'fabric.mixin.HumanoidArmorLayerMixin': ('Shared extraction/feature/submit hooks own armor provider selection, visibility, animation and frozen geometry on both loaders; the old immediate Fabric-only injection and abstract constructor are replaced.',
     [COMMON+'content/mixin/ArmorProviderStateMixin.java', COMMON+'content/mixin/ArmorProviderLayerMixin.java', COMMON+'content/mixin/ArmorProviderSubmitMixin.java', COMMON+'content/mixin/ArmorProviderFeatureMixin.java'], [61, 62]),
 'fabric.mixin.LevelRendererMixin': ('Fabric LevelRenderEvents route opaque/translucent/particle boundaries; shared framegraph integration resolves the actual weather/Fabulous targets. The old immediate stage injection and abstract constructor are replaced.',
     [FABRIC+'PulseLibFabricClient.java', COMMON+'content/renderer/PWorldRenderPass.java'], [56, 57]),
 'fabric.mixin.ModelManagerMixin': ('AtlasRegistry.register(AtlasConfig) replaces mutation of the removed AtlasSet constructor mapping. Both typed metadata records are registered with the native atlas.',
     [FABRIC+'PulseLibFabricClient.java'], [54, 78]),
}
original_jvm_type = gpu.jvm_type


def jvm_type(text, owner, current, imports, generic=False):
    if text.startswith('? extends '):
        assert generic
        return '+' + jvm_type(text.removeprefix('? extends '), owner, current, imports, True)
    return original_jvm_type(text, owner, current, imports, generic)


def native_signature(signature):
    return signature.replace('MultiBufferSource', 'SubmitNodeCollector').replace('RenderType.CompositeState', 'RenderSetup').replace(
        'RenderStateShard.TransparencyStateShard', 'RenderPipeline').replace('BiMap<ResourceLocation, SpriteSourceType>',
        'ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpriteSource>>')


def native_targets(jar):
    checks = [
        ('net/minecraft/world/level/block/entity/BlockEntity', 'methods', 'setRemoved', '()V'),
        ('net/minecraft/client/renderer/ItemInHandRenderer', 'methods', 'renderPlayerArm', '(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IFFLnet/minecraft/world/entity/HumanoidArm;)V'),
        ('net/minecraft/client/renderer/rendertype/RenderType', 'fields', 'state', 'Lnet/minecraft/client/renderer/rendertype/RenderSetup;'),
        ('net/minecraft/client/renderer/rendertype/RenderSetup', 'fields', 'pipeline', 'Lcom/mojang/blaze3d/pipeline/RenderPipeline;'),
        ('net/minecraft/client/renderer/texture/atlas/SpriteSources', 'fields', 'ID_MAPPER', 'Lnet/minecraft/util/ExtraCodecs$LateBoundIdMapper;'),
        ('net/minecraft/client/Minecraft', 'fields', 'level', 'Lnet/minecraft/client/multiplayer/ClientLevel;'),
        ('net/minecraft/client/Minecraft', 'methods', 'setLevel', '(Lnet/minecraft/client/multiplayer/ClientLevel;)V'),
        ('net/minecraft/client/renderer/entity/LivingEntityRenderer', 'fields', 'model', 'Lnet/minecraft/client/model/EntityModel;'),
        ('net/minecraft/client/renderer/entity/LivingEntityRenderer', 'methods', 'getModel', '()Lnet/minecraft/client/model/EntityModel;'),
    ]
    with tempfile.TemporaryDirectory(prefix='pulselib-hook-targets-') as temporary, zipfile.ZipFile(jar) as source:
        removed = ['com/mojang/blaze3d/vertex/VertexBuffer', 'net/minecraft/client/renderer/texture/atlas/SpriteSourceType',
                   'net/minecraft/client/renderer/rendertype/RenderType$CompositeState', 'net/minecraft/client/renderer/RenderStateShard$TransparencyStateShard']
        assert all(name+'.class' not in source.namelist() for name in removed)
        archive = Path(temporary)/'targets.jar'
        with zipfile.ZipFile(archive, 'w') as output:
            for owner in sorted({row[0] for row in checks}):
                output.writestr(owner+'.class', source.read(owner+'.class'))
        models = {row['binaryName']: row for row in map(json.loads, subprocess.check_output(
            ['java', str(ROOT/'docs/porting/tools/ExtractBinaryApi.java'), str(archive)], text=True).splitlines())}
        verified = []
        for owner, category, name, descriptor in checks:
            matches = [m for m in models[owner][category] if m['name'] == name and m['descriptor'] == descriptor]
            assert len(matches) == 1, (owner, name, descriptor)
            verified.append({'owner': owner, 'category': category, **matches[0]})
    return {'jarSha256': hashlib.sha256(jar.read_bytes()).hexdigest(), 'verifiedTargets': verified, 'removedMinecraftClasses': removed}


def verify(folder, jars, minecraft):
    baseline = {row['fqcn']: row for row in map(json.loads, (ROOT/'docs/porting/26.1.2-multiloader/api-1.21.1-multiloader.ndjson').read_text().splitlines())}
    current = {row['fqcn']: row for row in map(json.loads, subprocess.check_output(['java', str(ROOT/'docs/porting/tools/ExtractApi.java'), str(folder)], text=True).splitlines())}
    expected = {row['type'] for row in json.loads((ROOT/'docs/porting/evidence/stage-7.2c.json').read_text())['nextCheckpoint']['types']}
    assert {PREFIX+name for name in RESTORED+list(RELOCATED)} == expected and len(expected) == 17
    restored, relocations, preserved = [], [], {}
    for name in RESTORED:
        fqcn = PREFIX+name
        before, after = baseline[fqcn], current[fqcn]
        for key in ['api', 'kind', 'modifiers', 'type_parameters', 'extends', 'implements', 'permits']:
            assert before[key] == after[key], (fqcn, key)
        signatures = {m['signature'] for m in after['members']}
        verified = [native_signature(m['signature']) for m in before['members']]
        assert set(verified) <= signatures, (fqcn, verified, signatures)
        restored.append({'type': fqcn, 'canonicalMembers': len(verified), 'verifiedNativeSignatures': verified,
                         'canonicalConstants': {}, 'nativeAdapters': [{'canonical': m['signature'], 'native': native_signature(m['signature'])}
                            for m in before['members'] if native_signature(m['signature']) != m['signature']]})
    for name, (reason, sources, sections) in RELOCATED.items():
        fqcn = PREFIX+name
        relocations.append({'type': fqcn, 'status': 'REMOVED_MC_DEPENDENCY' if name.endswith('VertexBufferAccessor') else 'NATIVE_IMPLEMENTATION_RELOCATION',
                            'canonicalDeclarations': [m['signature'] for m in baseline[fqcn]['members']],
                            'reason': reason, 'nativeSources': sources, 'priorJournalSections': sections})
        for path in sources:
            actual = (folder/path).read_bytes()
            assert actual == git('show', PARENT+':'+path), (path, 'Accepted native implementation changed')
            preserved[path] = hashlib.sha256(actual).hexdigest()
    neo = NEO+'neoforge/NeoForgeClientLifecycle.java'
    assert (folder/neo).read_text() == git('show', PARENT+':'+neo).decode().replace('private NeoForgeClientLifecycle()', 'public NeoForgeClientLifecycle()')
    canonical_bodies = {}
    with tempfile.TemporaryDirectory(prefix='pulselib-hook-bodies-') as temporary:
        tool = Path(temporary)/'NativeBodies.java';tool.write_text(gpu.BODY_TOOL)
        for path, key in [(COMMON+'content/mixin/BlockEntityMixin.java', 'BlockEntityMixin#pulselib$removeAnimationManager(CallbackInfo)'),
                          (FABRIC+'mixin/MinecraftMixin.java', 'MinecraftMixin#pulselib$worldChanged(@Nullable ClientLevel,ReceivingLevelScreen.Reason,CallbackInfo)')]:
            original = Path(temporary)/Path(path).name;original.write_bytes(git('show', CANONICAL+':'+path))
            before = gpu.bodies(tool, original)
            old_key = next(k for k in before if k.split('#')[1].split('(')[0] == key.split('#')[1].split('(')[0])
            after = gpu.bodies(tool, folder/path)
            new_key = next(k for k in after if k.split('#')[1].split('(')[0] == old_key.split('#')[1].split('(')[0])
            expected_body = before[old_key].replace('if (level != null && level.isClientSide()) { InstanceAnimationManager.removeManager(this); }', 'if (level != null && level.isClientSide()) InstanceAnimationManager.removeManager(this);')
            assert expected_body == after[new_key], (path, old_key, new_key)
            canonical_bodies[path] = {'canonical': old_key, 'native': new_key, 'bodySha256': hashlib.sha256(expected_body.encode()).hexdigest()}
    common_config = json.loads((folder/'common/src/main/resources/pulselib.mixins.json').read_text())
    fabric_config = json.loads((folder/'fabric/src/main/resources/pulselib-fabric.mixins.json').read_text())
    new_mixins = ['BlockEntityMixin', 'ItemInHandRendererAccessor', 'RenderTypeAccessor$CompositeStateAccessor', 'RenderTypeAccessor$CompositeRenderTypeAccessor']
    for entry in new_mixins:
        assert common_config['client'].count(entry) == 1 and entry not in common_config['mixins']
    for entry in ['MinecraftMixin', 'SpriteSourcesAccessor']:
        assert fabric_config['client'].count(entry) == 1 and entry not in fabric_config.get('mixins', [])
    gpu.jvm_type = jvm_type
    compiled = {}
    if jars:
        for module in ['common', 'fabric', 'neoforge']:
            rows = [r for r in restored if baseline[r['type']]['source'].startswith(module+'/')]
            selected = jars if module == 'common' else [j for j in jars if '-'+module+'-' in j.name]
            assert selected and rows
            compiled.update(gpu.compiled_contracts(folder, current, rows, selected))
    return {'checkpoint': '7.2d', 'status': 'PASS_WITH_ENUMERATED_NATIVE_RELOCATIONS', 'parent': PARENT,
            'canonicalSource': CANONICAL, 'reviewTypes': 17, 'canonicalMembersReviewed': sum(len(baseline[n]['members']) for n in expected),
            'restoredTypes': restored, 'restoredTypeCount': len(restored), 'restoredCanonicalMembers': sum(r['canonicalMembers'] for r in restored),
            'classifiedNativeRelocations': relocations, 'preservedAcceptedSources': preserved, 'canonicalHookBodies': canonical_bodies,
            'nativeTargets': native_targets(minecraft), 'compiledContracts': compiled, 'newClientOnlyMixins': new_mixins+['fabric.MinecraftMixin', 'fabric.SpriteSourcesAccessor'],
            'limitations': ['Nine implementation/dependency types have enumerated source/binary incompatibilities. Their old FQCNs, abstract constructors, generic shadow field or VertexBuffer setters are not claimed restored.',
                            'The hand invoker is checked for native target resolution/application. Existing native hand consumers and accepted arm/armor pixels remain prior behavioral evidence.',
                            'Fabric world replacement is checked on a fresh transient native ClientLevel in a disposable integrated world. This is not a new network dimension-transfer acceptance.'],
            'remaining': ['17 renderer/player/GUI/reload review types', 'Whole-library inherited/compiler-resolved acceptance and GPU/performance report']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-root', type=Path)
    parser.add_argument('--report', type=Path, required=True)
    parser.add_argument('--jars', type=Path, nargs='+')
    parser.add_argument('--minecraft-jar', type=Path, default=Path.home()/'.gradle/caches/fabric-loom/26.1.2/minecraft-merged.jar')
    args = parser.parse_args()
    with tempfile.TemporaryDirectory(prefix='pulselib-hook-api-') as temporary:
        folder = args.source_root.resolve() if args.source_root else Path(temporary)
        if not args.source_root:
            with tarfile.open(fileobj=io.BytesIO(git('archive', 'HEAD'))) as archive:
                archive.extractall(folder, filter='data')
        result = verify(folder, args.jars, args.minecraft_jar)
    args.report.parent.mkdir(parents=True, exist_ok=True);args.report.write_text(json.dumps(result, indent=2)+'\n')
    print('HOOK API PASS', result['reviewTypes'], 'review types,', result['restoredTypeCount'], 'restored types,', result['restoredCanonicalMembers'], 'restored members')


if __name__ == '__main__':
    main()
