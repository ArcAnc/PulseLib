#!/usr/bin/env python3
"""Verify canonical GPU facade declarations and native backend body preservation.
ArcAnc, created 11.11.2026, copyright 2026.
Arc's License of Common Sense. See LICENSE.txt at repository root.
"""
import argparse
import base64
import hashlib
import io
import json
from pathlib import Path
import re
import subprocess
import sys
import tarfile
import tempfile

sys.dont_write_bytecode = True

from inventory import ROOT, git

PARENT = 'f11b5436291044ab5cda9fd13510c383cd2e50bc'
PREFIX = 'com.arcanc.pulselib.'
GPU = 'content.model.deformer.gpu.'
LEGACY = 'content.renderer.legacy.'
SELECTED = [GPU + name for name in ['PDeformerStream', 'PFrameStreams', 'PGpuDeformerBuffers',
            'PGpuDeformerBuffers.Submission', 'PGpuDeformerStack', 'PPoseStream']]
SELECTED += [LEGACY + name for name in ['GlCapabilityProbe', 'GlDrawExecutor', 'GlDynamicGeometry',
             'GlFrameArena', 'GlGeometryArena', 'GlGeometryArena.Slice', 'GlResourceRegistry',
             'GlStreamStorage', 'GlWeightedBlendedOit', 'McLegacyGlHostBridge']]
BODY_TOOL = r'''
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

class NativeBodies
{
	public static void main(String[] args) throws Exception
	{
		try (var files = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null))
		{
			var task = (JavacTask)ToolProvider.getSystemJavaCompiler().getTask(null, files, null,
					List.of("-proc:none"), null, files.getJavaFileObjects(args));
			for (var unit : task.parse())
				new TreeScanner<Void, Void>()
				{
					private final List<String> owners = new ArrayList<>();

					@Override
					public Void visitClass(ClassTree tree, Void unused)
					{
						this.owners.add(tree.getSimpleName().toString());
						super.visitClass(tree, unused);
						this.owners.removeLast();
						return null;
					}

					@Override
					public Void visitMethod(MethodTree tree, Void unused)
					{
						if (tree.getBody() != null)
						{
							String key = String.join(".", this.owners) + "#" + tree.getName() + "(" +
									String.join(",", tree.getParameters().stream().map(p -> p.getType().toString()).toList()) + ")";
							String body = tree.getBody().toString().replaceAll("\\s+", " ").trim();
							System.out.println(key + "\t" + Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8)));
						}
						return null;
					}
				}.scan(unit, null);
		}
	}
}
'''


def bodies(tool, path):
    text = subprocess.check_output(['java', str(tool), str(path)], text=True)
    return {key: base64.b64decode(body).decode() for key, body in (line.split('\t') for line in text.splitlines())}


def split_types(text):
    result, start, depth = [], 0, 0
    for index, character in enumerate(text):
        depth += (character == '<') - (character == '>')
        if character == ',' and depth == 0:
            result.append(text[start:index].strip())
            start = index + 1
    if text.strip():
        result.append(text[start:].strip())
    return result


def binary_name(fqcn, row):
    outer = Path(row['source']).stem
    package, nested = fqcn.split('.' + outer, 1)
    return package.replace('.', '/') + '/' + outer + nested.replace('.', '$')


def jvm_type(text, owner, current, imports, generic=False):
    text = re.sub(r'@[\w.]+\s*', '', text).strip()
    if text.endswith('[]'):
        return '[' + jvm_type(text[:-2], owner, current, imports, generic)
    primitives = {'void': 'V', 'boolean': 'Z', 'byte': 'B', 'short': 'S', 'int': 'I',
                  'long': 'J', 'float': 'F', 'double': 'D', 'char': 'C'}
    if text in primitives:
        return primitives[text]
    name = text.split('<', 1)[0]
    first, _, suffix = name.partition('.')
    package = owner.rsplit('.' + Path(current[owner]['source']).stem, 1)[0]
    candidates = [owner + '.' + name, package + '.' + name]
    candidates += [item + name for item in imports if item.endswith('.')]
    candidates += [item + ('.' + suffix if suffix else '') for item in imports if item.rsplit('.', 1)[-1] == first]
    fqcn = next((candidate for candidate in candidates if candidate in current), None)
    if fqcn:
        resolved = binary_name(fqcn, current[fqcn])
    elif name in ['Object', 'String', 'Float']:
        resolved = 'java/lang/' + name
    elif name in ['List', 'Optional'] and ('java.util.' in imports or 'java.util.' + name in imports):
        resolved = 'java/util/' + name
    else:
        fqcn = next((item for item in imports if item.rsplit('.', 1)[-1] == first), None)
        assert fqcn is not None, (owner, text, 'Unresolved descriptor type')
        resolved = fqcn.replace('.', '/') + ('$' + suffix.replace('.', '$') if suffix else '')
    parameters = ''
    if generic and '<' in text:
        parameters = '<' + ''.join(jvm_type(part, owner, current, imports, True)
                                   for part in split_types(text[text.index('<') + 1:-1])) + '>'
    return 'L' + resolved + parameters + ';'


def compiled_contracts(folder, current, results, jars):
    output = subprocess.check_output(['java', str(ROOT / 'docs/porting/tools/ExtractBinaryApi.java'), *map(str, jars)], text=True)
    classes = [json.loads(line) for line in output.splitlines()]
    proof = {}
    for row in results:
        owner = row['type']
        source = current[owner]
        imports = re.findall(r'^import\s+([\w.*]+)\s*;', (folder / source['source']).read_text(), re.M)
        imports = [item[:-1] if item.endswith('*') else item for item in imports]
        copies = [item for item in classes if item['binaryName'] == binary_name(owner, source)]
        assert len(copies) == len(jars), owner
        members = []
        for signature in row['verifiedNativeSignatures']:
            member = next(item for item in source['members'] if item['signature'] == signature)
            declaration = re.sub(r'^(?:(?:public|protected|private|static|final|abstract|default)\s+)+', '', signature)
            if member['kind'] in ['field', 'enum_constant']:
                type_text = declaration.rsplit(' ', 1)[0]
                descriptor = jvm_type(type_text, owner, current, imports)
                generic = jvm_type(type_text, owner, current, imports, True) if '<' in type_text else ''
                category = 'fields'
            else:
                prefix, parameters = declaration.split('(', 1)
                parameters = split_types(parameters.split(')', 1)[0])
                result = 'void' if member['name'] == '<init>' else prefix.rsplit(' ', 1)[0]
                descriptor = '(' + ''.join(jvm_type(part, owner, current, imports) for part in parameters) + ')' + jvm_type(result, owner, current, imports)
                generic = '(' + ''.join(jvm_type(part, owner, current, imports, True) for part in parameters) + ')' + jvm_type(result, owner, current, imports, True)
                if generic == descriptor:
                    generic = ''
                category = 'methods'
            flags = sum(bit for word, bit in [('public', 1), ('protected', 4), ('static', 8), ('final', 16), ('abstract', 1024)]
                        if word in signature.split())
            for copy in copies:
                matches = [item for item in copy[category] if item['name'] == member['name'] and item['descriptor'] == descriptor]
                assert len(matches) == 1 and matches[0]['signature'] == generic, (owner, signature, descriptor, generic, matches)
                assert matches[0]['flags'] & flags == flags, (owner, signature, 'access flags')
                if signature in row['canonicalConstants']:
                    assert matches[0]['constant'] == row['canonicalConstants'][signature], (owner, signature, 'ConstantValue')
            members.append({'source': signature, 'descriptor': descriptor, 'genericSignature': generic})
        proof[owner] = {'jarCopies': len(copies), 'members': members}
    return proof


def verify(folder, jars):
    baseline = {row['fqcn']: row for row in map(json.loads, (ROOT / 'docs/porting/26.1.2-multiloader/api-1.21.1-multiloader.ndjson').read_text().splitlines())}
    text = subprocess.check_output(['java', str(ROOT / 'docs/porting/tools/ExtractApi.java'), str(folder)], text=True)
    current = {row['fqcn']: row for row in map(json.loads, text.splitlines())}
    results = []
    for name in SELECTED:
        fqcn = PREFIX + name
        before, after = baseline[fqcn], current[fqcn]
        for key in ['api', 'kind', 'modifiers', 'type_parameters', 'extends', 'implements', 'permits', 'record_components']:
            assert before[key] == after[key], (fqcn, key)
        actual = {member['signature'] for member in after['members']}
        adapted, verified = [], []
        for member in before['members']:
            signature = member['signature']
            native = signature.replace('VertexBuffer', 'PGlDynamicGeometry').replace('ShaderInstance', 'RenderPass')
            assert native in actual, (fqcn, signature)
            verified.append(native)
            if native != signature:
                adapted.append({'canonical': signature, 'native': native})
        results.append({'type': fqcn, 'canonicalMembers': len(before['members']), 'nativeTypeAdapters': adapted,
                        'verifiedNativeSignatures': verified,
                        'canonicalConstants': {m['signature']: str(int(m['initializer'].replace('_', '')))
                                               for m in before['members'] if m['kind'] == 'field' and
                                               re.fullmatch(r'[0-9_]+', m.get('initializer', ''))}})
    allowed = {
        'PGlFrameArena#<init>(int)': 'Retain the canonical positive-slot argument guard.',
        'PGlFrameArena#finish(int)': 'Retain the canonical pending-slot double-submission guard.',
        'PGlMultiDrawExecutor#<init>(PGlResourceRegistry)': 'Delegate to the new constructor sharing the registry arena.',
        'PGlMultiDrawExecutor#cleanup()': 'Default native cleanup delegates to cleanup(true). The facade uses cleanup(false) to retain registry ownership.',
        'PGpuDeformerBuffers#finishFrame()': 'Reset canonical pose records together with native deformation values.',
        'PGpuDeformerBuffers#cleanup()': 'Clear canonical pose records together with native deformation definitions.',
    }
    native_sources = ['content/renderer/gl/' + name + '.java' for name in
                      ['PGlFrameArena', 'PGlGeometryArena', 'PGlMultiDrawExecutor', 'PGlResourceRegistry', 'PGlWeightedBlendedOit']]
    native_sources += ['content/model/deformer/gpu/' + name + '.java' for name in
                       ['PDeformerStream', 'PGpuDeformerStack', 'PGpuDeformerBuffers']]
    proof = {}
    with tempfile.TemporaryDirectory(prefix='pulselib-gpu-bodies-') as temporary:
        tool = Path(temporary) / 'NativeBodies.java'
        tool.write_text(BODY_TOOL)
        for relative in native_sources:
            path = 'common/src/main/java/com/arcanc/pulselib/' + relative
            original = Path(temporary) / Path(path).name
            original.write_bytes(git('show', PARENT + ':' + path))
            old, new = bodies(tool, original), bodies(tool, folder / path)
            preserved, exceptions = {}, {}
            for key, body in old.items():
                assert key in new, (path, key, 'Native method disappeared')
                adjusted = new[key]
                if key == 'PGpuDeformerBuffers.UploadBuffers#upload()':
                    adjusted = adjusted.replace('this.stream.', 'PGpuDeformerBuffers.')
                if adjusted == body:
                    preserved[key] = hashlib.sha256(body.encode()).hexdigest()
                else:
                    assert key in allowed, (path, key, 'Unexpected native body change')
                    if key == 'PGlMultiDrawExecutor#cleanup()':
                        assert new[key] == '{ cleanup(true); }'
                        restored = new['PGlMultiDrawExecutor#cleanup(boolean)']
                        restored = restored.replace('if (clearGeometry) this.geometry.clear();', 'this.geometry.clear();')
                        restored = restored.replace('if (clearGeometry) this.resources.clear(); else this.frameArena.reset();', 'this.resources.clear();')
                        assert restored == body, 'Default native cleanup must retain its original body and order'
                    exceptions[key] = allowed[key]
            proof[path] = {'preservedBodies': preserved, 'enumeratedChanges': exceptions,
                           'additionalBodies': sorted(new.keys() - old.keys())}
    for name in ['PFrameStreams', 'PPoseStream']:
        path = 'common/src/main/java/com/arcanc/pulselib/content/model/deformer/gpu/' + name + '.java'
        assert (folder / path).read_bytes() == git('show', '1.21.1-multiloader:' + path)
    return {'checkpoint': '7.2b', 'status': 'PASS_WITH_ENUMERATED_NATIVE_ADAPTERS', 'parent': PARENT,
            'canonicalSource': '7ce6f024b307bb821ce543344d3896c8e1fcf26b',
            'types': results, 'canonicalTypeCount': len(results),
            'canonicalMembers': sum(row['canonicalMembers'] for row in results),
            'nativeBodyProof': proof,
            'compiledContracts': compiled_contracts(folder, current, results, jars) if jars else {},
            'uploadBodyNormalization': 'The existing uploader reads its injected PDeformerStream. Native global calls inject the same stream returned by streams().deformers().',
            'targetDependencies': ['Removed VertexBuffer is represented by the actual PGlDynamicGeometry source.',
                                   'Removed ShaderInstance is represented by RenderPass. prepare(streams) uploads before the pass opens, bind(pass, streams) binds prepared native uniforms.',
                                   'Geometry follows the existing native POSITION_TEX_NORMAL layout. Canonical page-addressed commands are converted to native mesh-local offsets.',
                                   'Frame arenas, geometry pages and per-target OIT owners use the same native implementation as the main renderer.'],
            'remaining': ['Whole-library inherited/compiler-resolved API classification and material/atlas/native hooks review',
                          'Final consolidated GPU/performance report']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-root', type=Path)
    parser.add_argument('--report', type=Path, required=True)
    parser.add_argument('--jars', type=Path, nargs='+')
    args = parser.parse_args()
    with tempfile.TemporaryDirectory(prefix='pulselib-gpu-api-') as temporary:
        folder = args.source_root.resolve() if args.source_root else Path(temporary)
        if not args.source_root:
            with tarfile.open(fileobj=io.BytesIO(git('archive', 'HEAD'))) as archive:
                archive.extractall(folder, filter='data')
        result = verify(folder, args.jars)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(result, indent=2) + '\n')
    print('GPU FACADE API PASS', result['canonicalTypeCount'], 'types /', result['canonicalMembers'], 'canonical members')


if __name__ == '__main__':
    main()
