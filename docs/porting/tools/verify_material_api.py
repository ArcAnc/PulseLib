#!/usr/bin/env python3
"""Verify canonical material/atlas declarations and native implementation preservation.
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

sys.dont_write_bytecode = True

import verify_gpu_facade_api as gpu
from inventory import ROOT, git

PARENT = 'dc2e46b2f81e12af39e69b874b19ec0fcae26fda'
CANONICAL = '7ce6f024b307bb821ce543344d3896c8e1fcf26b'
PREFIX = 'com.arcanc.pulselib.'
SELECTED = ['content.model.textures.atlas.PLibMetadata', 'content.model.textures.atlas.RuntimeLoader',
            'util.PRenderTypes', 'util.PRenderTypes.RenderTypeProvider',
            'util.PRenderTypes.ShaderRegistrar', 'util.PRenderTypes.ShadersProvider']
REGISTRAR = 'abstract public void register(Identifier, VertexFormat, Consumer<RenderPipeline>) throws IOException'
NATIVE_REGISTRAR = 'abstract public void register(RenderPipeline)'

# Resolve the sibling nested registrar, which is implicitly visible inside ShadersProvider.
original_jvm_type = gpu.jvm_type


def jvm_type(text, owner, current, imports, generic=False):
    if text == 'Output' and owner.endswith('.RuntimeLoader'):
        return 'Lnet/minecraft/client/renderer/texture/atlas/SpriteSource$Output;'
    if text == 'ShaderRegistrar':
        return 'Lcom/arcanc/pulselib/util/PRenderTypes$ShaderRegistrar;'
    return original_jvm_type(text, owner, current, imports, generic)


def native_signature(signature):
    result = signature.replace('ResourceLocation', 'Identifier').replace('ShaderInstance', 'RenderPipeline')
    result = result.replace('SpriteSourceType', 'MapCodec<RuntimeLoader>')
    result = result.replace('RenderStateShard.TransparencyStateShard', 'BlendFunction')
    return NATIVE_REGISTRAR if result == REGISTRAR else result


def verify(folder, jars):
    baseline = {row['fqcn']: row for row in map(json.loads, (ROOT / 'docs/porting/26.1.2-multiloader/api-1.21.1-multiloader.ndjson').read_text().splitlines())}
    current = {row['fqcn']: row for row in map(json.loads, subprocess.check_output(['java', str(ROOT / 'docs/porting/tools/ExtractApi.java'), str(folder)], text=True).splitlines())}
    results = []
    for name in SELECTED:
        fqcn = PREFIX + name
        before, after = baseline[fqcn], current[fqcn]
        for key in ['api', 'kind', 'modifiers', 'type_parameters', 'extends', 'implements', 'permits', 'record_components']:
            assert before[key] == after[key], (fqcn, key)
        actual = {member['signature'] for member in after['members']}
        verified, adapters = [], []
        for member in before['members']:
            canonical = member['signature']
            native = native_signature(canonical)
            assert native in actual, (fqcn, canonical, native)
            verified.append(native)
            if native != canonical.replace('ResourceLocation', 'Identifier'):
                adapters.append({'canonical': canonical, 'native': native})
        results.append({'type': fqcn, 'canonicalMembers': len(before['members']), 'verifiedNativeSignatures': verified,
                        'nativeAdapters': adapters, 'canonicalConstants': {m['signature']: json.loads(m['initializer'])
                            for m in before['members'] if m['kind'] == 'field' and m['signature'].startswith('final public static String ')}})
    proof = {}
    paths = ['common/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java',
             'common/src/main/java/com/arcanc/pulselib/content/model/textures/atlas/RuntimeLoader.java',
             'fabric/src/main/java/com/arcanc/pulselib/fabric/PulseLibFabricClient.java',
             'neoforge/src/main/java/com/arcanc/pulselib/neoforge/NeoForgeClientLifecycle.java']
    with tempfile.TemporaryDirectory(prefix='pulselib-material-bodies-') as temporary:
        tool = Path(temporary) / 'NativeBodies.java'
        tool.write_text(gpu.BODY_TOOL)
        for path in paths:
            original = Path(temporary) / Path(path).name
            original.write_bytes(git('show', PARENT + ':' + path))
            before, after = gpu.bodies(tool, original), gpu.bodies(tool, folder / path)
            if path.endswith('/PRenderTypes.java'):
                canonical_file = Path(temporary) / 'canonical-PRenderTypes.java'
                canonical_file.write_bytes(git('show', CANONICAL + ':' + path))
                canonical_body = gpu.bodies(tool, canonical_file)['PRenderTypes.RenderTypeProvider#emissiveVariant(RenderType,ResourceLocation)']
                expected = canonical_body.replace('renderType', 'baseType').replace('location', 'texture')
                for method, pipeline in [('trianglesSolid', 'TRIANGLES_SOLID'), ('trianglesCutout', 'TRIANGLES_CUTOUT'),
                                         ('trianglesTranslucent', 'TRIANGLES_TRANSLUCENT'), ('trianglesGui', 'TRIANGLES_INSTANT_TRANSLUCENT'),
                                         ('trianglesImmediate', 'TRIANGLES_INSTANT_CUTOUT')]:
                    expected = expected.replace('baseType == ' + method + '(texture)', 'baseType.pipeline() == RenderPipelinesProvider.' + pipeline)
                expected = expected.replace('return baseType;', 'if (baseType.pipeline() == RenderPipelinesProvider.TRIANGLES_INSTANT_SOLID) return trianglesInstantEmissiveSolid(texture); return baseType;')
                assert after['PRenderTypes.RenderTypeProvider#emissiveVariant(RenderType,Identifier)'] == expected
            preserved, additions = {}, {}
            for key, body in before.items():
                if key == 'PRenderTypes.RenderTypeProvider#emissiveVariant(RenderType,Identifier)':
                    additions[key] = 'Restore canonical recognized-material selection, GUI ordinary alpha, and unchanged unknown types, with one extra native opaque immediate variant. Exact canonical body transformation verified.'
                    continue
                adjusted = after[key]
                if path.startswith('fabric/'):
                    adjusted = adjusted.replace('Set.of(PLibSpriteMetadata.TYPE, PLibMetadata.TYPE)', 'Set.of(PLibSpriteMetadata.TYPE)')
                elif path.startswith('neoforge/'):
                    adjusted = adjusted.replace(' event.addAdditionalMetadata(PResourceCache.ATLAS_FILE_LOCATION, PLibMetadata.TYPE);', '')
                assert adjusted == body, (path, key, 'Unexpected native method change')
                preserved[key] = hashlib.sha256(body.encode()).hexdigest()
                if after[key] != body:
                    additions[key] = 'Decode both typed views of the same pulselib metadata section in the native atlas.'
            proof[path] = {'preservedNativeBodies': preserved, 'enumeratedChanges': additions,
                           'additionalMethods': sorted(after.keys() - before.keys())}
    native_path = 'common/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java'
    before_text = git('show', PARENT + ':' + native_path).decode()
    after_text = (folder / native_path).read_text()
    before_pipelines = before_text.split('public static class RenderPipelinesProvider', 1)[1].split('public static class RenderTypeProvider', 1)[0]
    after_pipelines = after_text.split('public static class RenderPipelinesProvider', 1)[1].split('public static final class RenderTypeProvider', 1)[0]
    assert before_pipelines == after_pipelines, 'Native pipelines and vertex layout must stay byte-identical'
    # Record the complete public native pipeline catalogue.
    native_baseline = json.loads(next(line for line in subprocess.check_output(['java', str(ROOT / 'docs/porting/tools/ExtractApi.java'), str(folder)], text=True).splitlines() if json.loads(line)['fqcn'] == PREFIX + 'util.PRenderTypes.RenderPipelinesProvider'))
    gpu.jvm_type = jvm_type
    compiled = gpu.compiled_contracts(folder, current, results, jars) if jars else {}
    return {'checkpoint': '7.2c', 'status': 'PASS_WITH_ENUMERATED_NATIVE_ADAPTERS', 'parent': PARENT,
            'canonicalSource': CANONICAL, 'canonicalTypeCount': len(results),
            'canonicalMembers': sum(row['canonicalMembers'] for row in results), 'types': results,
            'compiledContracts': compiled, 'nativeBodyProof': proof,
            'nativePipelineAndVertexSourceSha256': hashlib.sha256(before_pipelines.encode()).hexdigest(),
            'nativePipelines': [m['signature'] for m in native_baseline['members'] if m['kind'] == 'field'],
            'decisions': [
                'PLibMetadata retains its canonical record, constants and codec. Both loaders decode it alongside PLibSpriteMetadata, with the same JSON section and values.',
                'RuntimeLoader.TYPE and type() expose the registered native MapCodec; SpriteSourceType was removed in Minecraft 26.1.',
                'getTransparencyState returns Optional<BlendFunction>; opaque pipelines have no blend function. isTranslucent retains the blending predicate.',
                'emissiveVariant restores canonical recognized-material selection, retains GUI/immediate pipeline ownership, and leaves unknown RenderTypes unchanged.',
                'Six material aliases return existing cached native RenderTypes. Immediate aliases select the native cutout immediate pipeline; GUI aliases select ordinary translucent immediate pipelines.',
                'ShadersProvider returns the immutable RenderPipelines actually used by the native renderer. They exist eagerly, so getters are nonnull.',
                'The emissive flag does not change native depth-only OIT passes; accumulation retains separate lit/emissive pipelines.',
                'ShaderRegistrar receives the complete immutable RenderPipeline. Identifier and vertex format are properties of that pipeline. There is no mutable ShaderInstance setter or IO construction step.',
                'registerShaders uses the same one-shot registration drain as PRenderTypes.register. It is an alternative loader registration entry point, not a second registration pass. The checked IOException remains on registerShaders for canonical callers.'
            ], 'remaining': ['34 canonical native-hook/renderer/player/GUI/reload review types',
                              'Inherited/compiler-resolved API acceptance and final GPU/performance report']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-root', type=Path)
    parser.add_argument('--report', type=Path, required=True)
    parser.add_argument('--jars', type=Path, nargs='+')
    args = parser.parse_args()
    with tempfile.TemporaryDirectory(prefix='pulselib-material-api-') as temporary:
        folder = args.source_root.resolve() if args.source_root else Path(temporary)
        if not args.source_root:
            with tarfile.open(fileobj=io.BytesIO(git('archive', 'HEAD'))) as archive:
                archive.extractall(folder, filter='data')
        result = verify(folder, args.jars)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(result, indent=2) + '\n')
    print('MATERIAL API PASS', result['canonicalTypeCount'], 'types /', result['canonicalMembers'], 'canonical members')


if __name__ == '__main__':
    main()
