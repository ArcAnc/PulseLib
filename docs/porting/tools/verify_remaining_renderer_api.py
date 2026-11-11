#!/usr/bin/env python3
"""Verify remaining canonical renderer/player/GUI/reload contracts and native adapters.
ArcAnc, created 11.11.2026, copyright 2026.
Arc's License of Common Sense. See LICENSE.txt at repository root.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path
import re
import subprocess
import sys
import tarfile
import tempfile
import zipfile
sys.dont_write_bytecode = True

import verify_renderer_api as renderer
import verify_gpu_facade_api as gpu
from inventory import ROOT, git

PARENT = '5d546c3e0ae8d1dfdd54effdf64f47b97adcdf77'
BASE = 'com.arcanc.pulselib.'
REASONS = {
 'PPlayerAnimatedAttachmentContext': 'ResourceLocation/collector type substitutions retain the record and canonical buffers accessor.',
 'PPlayerAnimatedAttachmentLayer': 'Native RenderLayer submits AvatarRenderState with PlayerModel and SubmitNodeCollector; live player render inputs are extracted first.',
 'PPlayerAnimatedAttachments': 'Native collector replaces MultiBufferSource; snapshot/anchor/callback implementations were already accepted.',
 'PPlayerFirstPersonRenderer': 'Native collector replaces MultiBufferSource; existing presentation/hand scope ownership remains unchanged.',
 'PBlockRenderer': 'Native BlockEntityRenderer<T, RS> requires extracted BlockEntityRenderState. submit replaces render; existing virtual material/pose hooks and captured lighting/facing remain active.',
 'PEntityRenderLayer': 'The additional RS generic binds layer replay to the parent renderer snapshot. Existing per-bone context hooks and geometry-only submission remain active.',
 'PEntityRenderer': 'Native EntityRenderer<T, RS> and LivingEntityRenderState own captured model/light/head/body/scale values. Existing submit and per-bone hooks preserve accepted extraction/submit ownership.',
 'PItemRenderer': 'SpecialModelRenderer<RS> replaces removed BEWLR. Native item argument/display/light/foil snapshots and existing renderer adapters own actual submission.',
 'PRenderer': 'RS generic and explicit native camera/collector hooks preserve extraction/submission separation; legacy live-owner varargs cannot replace the native deferred contract.',
 'PModelCache': 'The canonical six-argument reload overload delegates to the unchanged native SharedState pipeline. The two profiler parameters were unused in the frozen canonical body.',
 'PAttachmentAnchorResolvers': 'Resolvers receive the extracted EntityRenderState and native EntityModel state generic, preserving resolver ordering and nullable unresolved anchors.',
 'AnchorResolver': 'EntityRenderState replaces the live LivingEntity argument at the native model replay boundary.',
 'PLivingAttachmentLayer': 'Native RenderLayer<S, M>.submit consumes extracted attachment records and native model parts; constructor generic bounds follow living render state.',
 'PHumanoidAttachmentLayer': 'Native HumanoidRenderState/Model state bounds retain shared living-layer behavior and accepted hand/armor snapshots.',
 'PLibArmorHandler': 'Native EntityRenderer has two type parameters; the same layer registration and extraction implementation remains active.',
 'PLibHelper': 'GuiGraphicsExtractor replaces removed GuiGraphics; the existing helper captures a native PiP pose/target snapshot.',
 'PArmorClientExtensions': 'Canonical base composition and explicit entity/slot helpers are restored. Native callbacks delegate to the supplied base. Native automatic provider extraction remains shared. The removed getCustomRenderer/BEWLR hook moves to native SpecialModelRenderer item registration.',
}
SECTIONS = {
 'PPlayerAnimatedAttachmentContext':[33], 'PPlayerAnimatedAttachmentLayer':[33,60], 'PPlayerAnimatedAttachments':[33,42],
 'PPlayerFirstPersonRenderer':[36,42,47], 'PBlockRenderer':[21,26], 'PEntityRenderLayer':[22,26], 'PEntityRenderer':[22,26,29],
 'PItemRenderer':[23,25,28], 'PRenderer':[21,22,23,31], 'PModelCache':[76], 'PAttachmentAnchorResolvers':[60,62],
 'AnchorResolver':[60,62], 'PLivingAttachmentLayer':[45,60,62], 'PHumanoidAttachmentLayer':[47,60,62],
 'PLibArmorHandler':[60,61,62], 'PLibHelper':[58,70], 'PArmorClientExtensions':[61,62],
}
def norm(s):
 s=re.sub(r'@[\w.]+\s*','',s).replace('ResourceLocation','Identifier').replace('MultiBufferSource','SubmitNodeCollector').replace('GuiGraphics','GuiGraphicsExtractor')
 s=s.replace('GuiGraphicsExtractorExtractor','GuiGraphicsExtractor')
 for n in ['PRenderer','PEntityRenderer','PEntityRenderLayer','BlockEntityRenderer','EntityRenderer']:s=s.replace(n+'<T>',n+'<T, RS>')
 s=s.replace('PlayerModel<AbstractClientPlayer>','PlayerModel')
 return s
manual={
 ('PPlayerAnimatedAttachmentLayer','public <init>(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>)'):'public <init>(RenderLayerParent<AvatarRenderState, PlayerModel>)',
 ('PPlayerAnimatedAttachmentLayer','public void render(PoseStack, MultiBufferSource, int, AbstractClientPlayer, float, float, float, float, float, float)'):'public void submit(PoseStack, SubmitNodeCollector, int, AvatarRenderState, float, float)',
 ('PAttachmentAnchorResolvers','public static <T extends LivingEntity> ModelPart resolve(T, EntityModel<T>, PAttachmentAnchor)'):'public static <S extends EntityRenderState> ModelPart resolve(S, EntityModel<? super S>, PAttachmentAnchor)',
 ('AnchorResolver','abstract public ModelPart resolve(LivingEntity, EntityModel<?>)'):'abstract public ModelPart resolve(EntityRenderState, EntityModel<?>)',
 ('PLivingAttachmentLayer','public <init>(RenderLayerParent<T, M>)'):'public <init>(RenderLayerParent<S, M>)',
 ('PLivingAttachmentLayer','public void render(PoseStack, MultiBufferSource, int, T, float, float, float, float, float, float)'):'public void submit(PoseStack, SubmitNodeCollector, int, S, float, float)',
 ('PHumanoidAttachmentLayer','public <init>(RenderLayerParent<T, M>)'):'public <init>(RenderLayerParent<S, M>)',
 ('PLibArmorHandler','public static void addLayers(EntityRenderer<?>)'):'public static void addLayers(EntityRenderer<?, ?>)',
 ('PArmorClientExtensions','public BlockEntityWithoutLevelRenderer getCustomRenderer()'):None,
}


def read_types(folder):
    text = subprocess.check_output(['java', str(ROOT/'docs/porting/tools/ExtractApi.java'), str(folder)], text=True)
    return {row['fqcn']: row for row in map(json.loads, text.splitlines())}


def verify(folder, jars, neoforge):
    baseline = {row['fqcn']: row for row in map(json.loads, (ROOT/'docs/porting/26.1.2-multiloader/api-1.21.1-multiloader.ndjson').read_text().splitlines())}
    current = read_types(folder)
    selected = [row['type'] for row in json.loads((ROOT/'docs/porting/evidence/stage-7.2d.json').read_text())['nextCheckpoint']['types']]
    results, source_proof = [], {}
    with tempfile.TemporaryDirectory(prefix='pulselib-renderer-review-') as temporary:
        parent = Path(temporary)
        for name in selected:
            path = current[name]['source'];p = parent/path;p.parent.mkdir(parents=True, exist_ok=True)
            p.write_bytes(git('show', PARENT+':'+path))
        parent_types = read_types(parent)
        for name in selected:
            short = name.rsplit('.',1)[-1];before, after = baseline[name], current[name]
            for key in ['kind','modifiers','type_parameters','extends','implements','permits','record_components']:
                assert after[key] == parent_types[name][key], (name,key,'Unexpected native type header change')
            actual = {norm(m['signature']): m['signature'] for m in after['members']}
            entries = []
            for member in before['members']:
                canonical = member['signature'];target = manual.get((short,canonical), norm(canonical))
                if short == 'PArmorClientExtensions' and target:
                    target = target.replace('ArmorMaterial.Layer','EquipmentClientInfo.Layer')
                change = renderer.adaptation(short,member) if short in renderer.BOUNDS else None
                if norm(target or '') not in actual and change:
                    target = change[0]
                native = actual.get(norm(target)) if target else None
                assert target is None or native is not None, (name,canonical,target)
                entries.append({'canonical':canonical,'native':native,'status':'RETIRED_MC_DEPENDENCY' if target is None else
                                'NATIVE_ADAPTER' if native != canonical else 'PRESERVED'})
            path = after['source'];data = (folder/path).read_bytes();source_proof[path] = {'sha256':hashlib.sha256(data).hexdigest(),
                            'parentByteIdentical':data==(parent/path).read_bytes()}
            if not name.endswith(('PModelCache','PArmorClientExtensions')):
                assert source_proof[path]['parentByteIdentical'], path
            results.append({'type':name,'canonicalMembers':len(before['members']),'entries':entries,
                            'canonicalHeader':{k:before[k] for k in ['type_parameters','extends','implements','record_components']},
                            'nativeHeader':{k:after[k] for k in ['type_parameters','extends','implements','record_components']},
                            'reason':REASONS[short],'priorJournalSections':SECTIONS[short]})
        model_path = 'common/src/main/java/com/arcanc/pulselib/util/PModelCache.java'
        tool = parent/'NativeBodies.java';tool.write_text(gpu.BODY_TOOL)
        before, after = gpu.bodies(tool,parent/model_path),gpu.bodies(tool,folder/model_path)
        assert all(after.get(key)==body for key,body in before.items()), 'Existing native reload/bake/cache algorithms changed'
        added = set(after)-set(before);assert len(added)==1
        assert after[next(iter(added))]=='{ return reload(new PreparableReloadListener.SharedState(resourceManager), backgroundExecutor, stage, gameExecutor); }'
        canonical_file = parent/'canonical-PModelCache.java'
        canonical_file.write_bytes(git('show','7ce6f024b307bb821ce543344d3896c8e1fcf26b:'+model_path))
        canonical_body = next(body for key,body in gpu.bodies(tool,canonical_file).items() if '#reload(' in key and 'ProfilerFiller' in key)
        assert 'preparationsProfiler' not in canonical_body and 'reloadProfiler' not in canonical_body
        reload_proof = {'canonicalProfilerParametersUnused':True,'canonicalBodySha256':hashlib.sha256(canonical_body.encode()).hexdigest(),'preservedBodies':{key:hashlib.sha256(body.encode()).hexdigest() for key,body in before.items()},
                        'addedCanonicalAdapter':next(iter(added))}
    compiled = {};targeted = []
    if jars:
        models = [json.loads(line) for line in subprocess.check_output(['java',str(ROOT/'docs/porting/tools/ExtractBinaryApi.java'),*map(str,jars)],text=True).splitlines()]
        for name in selected:
            binary = gpu.binary_name(name,current[name]);copies = [row for row in models if row['binaryName']==binary]
            assert len(copies)==(1 if name.endswith('PArmorClientExtensions') else 3),(name,len(copies))
            comparable = [{k:v for k,v in row.items() if k!='jar'} for row in copies]
            assert all(row==comparable[0] for row in comparable), name
            compiled[name]={'jarCopies':len(copies),'contract':comparable[0]}
        owner = compiled[BASE+'util.PModelCache']['contract']
        desc = '(Lnet/minecraft/server/packs/resources/PreparableReloadListener$PreparationBarrier;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;Lnet/minecraft/util/profiling/ProfilerFiller;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;'
        matches = [m for m in owner['methods'] if m['name']=='reload' and m['descriptor']==desc]
        assert len(matches)==1 and matches[0]['flags']&9==9
        targeted.append({'owner':BASE+'util.PModelCache','member':matches[0],'copies':3})
        armor = compiled[BASE+'util.attachments.humanoid.armor.PArmorClientExtensions']['contract']
        with tempfile.TemporaryDirectory(prefix='pulselib-neoforge-api-') as temporary, zipfile.ZipFile(neoforge) as source:
            path = Path(temporary)/'extension.jar'
            with zipfile.ZipFile(path,'w') as output:
                name = 'net/neoforged/neoforge/client/extensions/common/IClientItemExtensions.class';output.writestr(name,source.read(name))
            interface = json.loads(subprocess.check_output(['java',str(ROOT/'docs/porting/tools/ExtractBinaryApi.java'),str(path)],text=True))
        callbacks = [m for m in interface['methods'] if not m['flags']&8 and not m['name'].startswith('<')]
        assert len(callbacks)==13
        required = [(m['name'],m['descriptor']) for m in callbacks]
        prefix = 'Lnet/neoforged/neoforge/client/extensions/common/IClientItemExtensions;'
        entity = 'Lnet/minecraft/world/entity/LivingEntity;';stack = 'Lnet/minecraft/world/item/ItemStack;'
        slot = 'Lnet/minecraft/world/entity/EquipmentSlot;';humanoid = 'Lnet/minecraft/client/model/HumanoidModel;'
        required += [('<init>','('+prefix+')V'),('buildFor','(Lnet/minecraft/world/item/Item;'+prefix+')'+prefix),
                     ('getHumanoidArmorModel','('+entity+stack+slot+humanoid+')'+humanoid),
                     ('getGenericArmorModel','('+entity+stack+slot+humanoid+')Lnet/minecraft/client/model/Model;'),
                     ('renderHelmetOverlay','('+stack+'Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V'),
                     ('getArmorLayerTintColor','('+stack+entity+'Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;II)I')]
        for name,descriptor in required:
            matches = [m for m in armor['methods'] if m['name']==name and m['descriptor']==descriptor]
            assert len(matches)==1 and matches[0]['flags']&1, (name,descriptor)
            targeted.append({'owner':BASE+'util.attachments.humanoid.armor.PArmorClientExtensions','member':matches[0],'copies':1})
    count = sum(row['canonicalMembers'] for row in results)
    assert len(results)==17 and count==144
    retired = [entry for row in results for entry in row['entries'] if entry['native'] is None]
    assert len(retired)==1 and retired[0]['canonical']=='public BlockEntityWithoutLevelRenderer getCustomRenderer()'
    return {'checkpoint':'7.2e','status':'PASS_WITH_ENUMERATED_NATIVE_ADAPTERS','parent':PARENT,'canonicalSource':'7ce6f024b307bb821ce543344d3896c8e1fcf26b',
            'reviewTypes':17,'canonicalMembers':count,'mappedMembers':count-len(retired),'retiredMinecraftDeclarations':retired,
            'types':results,'sourceProof':source_proof,'nativeReloadBodyProof':reload_proof,'compiledInventories':compiled,
            'targetedCompiledChecks':targeted,'nativeExtensionCallbacks':13,
            'decisions':['The one-shot native item-extension registration still leaves already registered external extensions untouched. Composition is exposed through buildFor(item, base).',
                         'Automatic provider selection/setup remains in the accepted shared extraction hooks. Explicit canonical entity/slot overloads and setupModelAnimations remain callable separately.',
                         'Legacy humanoid queries keep the original humanoid if a native base returns a non-humanoid Model. Generic queries preserve arbitrary native Models.',
                         'Default native tint fallback is preserved; explicit custom-base tint queries delegate, and hidden armor wins.',
                         'Standalone profiler arguments retain the canonical signature; the frozen canonical reload body did not use them.',
                         'Old BEWLR/getCustomRenderer has no native IClientItemExtensions counterpart; use registered native SpecialModelRenderer/PItemRenderer item integration.'],
            'limits':['Source-level scoped mapping and 49 named class-file inventories; whole-library inherited/compiler-resolved acceptance remains the next checkpoint.',
                      'Native RS/collector/gui/anchor/layer generic adaptations are enumerated, not a claim of unchanged 1.21 binary descriptors.'],
            'nextCheckpoint':'7.2f whole-library inherited/compiler-resolved API acceptance'}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-root',type=Path)
    parser.add_argument('--report',type=Path,required=True)
    parser.add_argument('--jars',type=Path,nargs='+')
    parser.add_argument('--neoforge-jar',type=Path,default=next((Path.home()/'.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.1.2.95').rglob('*universal.jar'),None))
    args=parser.parse_args()
    with tempfile.TemporaryDirectory(prefix='pulselib-remaining-renderer-api-') as temporary:
        folder=args.source_root.resolve() if args.source_root else Path(temporary)
        if not args.source_root:
            with tarfile.open(fileobj=io.BytesIO(git('archive','HEAD'))) as archive:archive.extractall(folder,filter='data')
        result=verify(folder,args.jars,args.neoforge_jar)
    args.report.parent.mkdir(parents=True,exist_ok=True);args.report.write_text(json.dumps(result,indent=2)+'\n')
    print('REMAINING RENDERER API PASS',result['reviewTypes'],'types /',result['canonicalMembers'],'members /',result['mappedMembers'],'mapped')


if __name__=='__main__':
    main()
