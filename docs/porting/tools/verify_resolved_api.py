#!/usr/bin/env python3
"""Attribute both version-specific source surfaces and check release JVM contracts.
ArcAnc, created 11.11.2026, copyright 2026.
Arc's License of Common Sense. See LICENSE.txt at repository root.

The accepted report pins individual exceptions, never an entire exempt package/type.
No annotation processors, game launches, downloads or production source writes.
"""
import argparse
from collections import Counter
import gzip
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
import zipfile

sys.dont_write_bytecode = True

from inventory import ROOT, SOURCES, git

MODULES = ('common', 'fabric', 'neoforge')
PREFIX = 'com.arcanc.pulselib.'
TYPE_KINDS = {'CLASS', 'INTERFACE', 'ENUM', 'RECORD', 'ANNOTATION_TYPE'}
RENAMES = {
    'net.minecraft.resources.ResourceLocation': 'net.minecraft.resources.Identifier',
    'net.minecraft.client.renderer.RenderType': 'net.minecraft.client.renderer.rendertype.RenderType',
    'net.minecraft.client.model.PlayerModel': 'net.minecraft.client.model.player.PlayerModel',
}
MEMBER_KEYS = ('kind', 'name', 'flags', 'parameters', 'returns', 'throws', 'typeParameters', 'varargs', 'type', 'descriptor', 'resolvedDescriptor')
HEADER_KEYS = ('api', 'kind', 'flags', 'typeParameters', 'superclass', 'interfaces')


def encoded(value):
    return json.dumps(value, sort_keys=True, separators=(',', ':'), ensure_ascii=False)


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def normalize(value):
    if isinstance(value, str):
        value = re.sub(r'net\.minecraft\.client\.model\.PlayerModel<[^<>]+>', RENAMES['net.minecraft.client.model.PlayerModel'], value)
        for old, new in RENAMES.items():
            value = value.replace(old, new).replace(old.replace('.', '/'), new.replace('.', '/'))
        return value
    if isinstance(value, list):
        return [normalize(item) for item in value]
    if isinstance(value, dict):
        return {key: normalize(item) for key, item in value.items()}
    return value


def contract(member):
    return {key: member[key] for key in MEMBER_KEYS if key in member}


def java(tool, *arguments):
    return subprocess.check_output(['java', '-XX:-UsePerfData', '-Xmx3G', str(ROOT / 'docs/porting/tools' / tool), *map(str, arguments)], text=True)


def source_proof(root, ref):
    expected = {}
    for line in git('ls-tree', '-r', ref).decode().splitlines():
        meta, path = line.split('\t')
        if path.split('/')[0] in MODULES and '/src/main/java/' in path and path.endswith('.java'):
            expected[path] = meta.split()[2]
    actual = {str(path.relative_to(root)): path for module in MODULES for path in (root / module / 'src/main/java').rglob('*.java')}
    if actual.keys() != expected.keys():
        raise AssertionError('Production source path set differs from ' + ref)
    hashes = {}
    for path, file in actual.items():
        data = file.read_bytes()
        if hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest() != expected[path]:
            raise AssertionError('Source differs from Git object: ' + path)
        hashes[path] = hashlib.sha256(data).hexdigest()
    return {'ref': ref, 'units': len(hashes), 'sourceSha256': hashes}


def classpath(project, module, output):
    entries = json.loads((project / module / 'build/resolved-api-classpath.json').read_text())
    # Source includes Common explicitly; stale Common output must never supply types.
    entries = list(dict.fromkeys(path for path in entries if not path.endswith('/build/classes/java/main')))
    for path in entries:
        if not Path(path).is_file():
            raise AssertionError('Missing dependency: ' + path)
        with zipfile.ZipFile(path) as jar:
            if any(name.startswith('com/arcanc/pulselib/') and name.endswith('.class') for name in jar.namelist()):
                raise AssertionError('A dependency shadows PulseLib sources: ' + path)
    output.write_text(':'.join(entries))
    return [{'path': path, 'sha256': digest(Path(path))} for path in entries]


def attribute(root, project, label, module, output):
    cp = output / (label + '-' + module + '.classpath')
    dependencies = classpath(project, module, cp)
    classes = output / (label + '-' + module + '-classes')
    sources = [root / 'common/src/main/java']
    if module != 'common':
        sources.append(root / module / 'src/main/java')
    text = java('ExtractResolvedApi.java', 21 if label == 'canonical' else 25, cp, classes, *sources)
    rows = [json.loads(line) for line in text.splitlines()]
    for row in rows:
        row['source'] = str(Path(row['source']).relative_to(root))
    jar_path = output / (label + '-' + module + '.jar')
    with zipfile.ZipFile(jar_path, 'w', zipfile.ZIP_DEFLATED) as jar:
        for path in sorted(classes.rglob('*.class')):
            jar.writestr(zipfile.ZipInfo(str(path.relative_to(classes)), (1980, 1, 1, 0, 0, 0)), path.read_bytes())
    binaries = [json.loads(line) for line in java('ExtractBinaryApi.java', jar_path).splitlines()]
    for row in binaries:
        row.pop('jar')
    return rows, binaries, dependencies


def callers(label, module, output):
    """Compile real calls to the portable inherited contracts on both native classpaths."""
    source = '''// ArcAnc, 11.11.2026, copyright 2026; Arc's License of Common Sense.
import com.arcanc.pulselib.content.model.baked.AtlasBufferBuilder;
import com.arcanc.pulselib.content.renderer.PEntityRenderer;
import com.arcanc.pulselib.content.renderer.PItemRenderer;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.function.Function;
final class CanonicalInheritedCaller
{
	static VertexConsumer vertices(AtlasBufferBuilder consumer, PoseStack.Pose pose, Matrix4f matrix, Vector3f vector)
	{
		consumer.addVertex(pose, vector);
		consumer.addVertex(matrix, 1, 2, 3);
		consumer.addVertex(vector);
		return consumer.setWhiteAlpha(127);
	}
	static String name(PItemRenderer renderer)
	{
		return renderer.getName();
	}
	static int leashSteps()
	{
		return PEntityRenderer.LEASH_RENDER_STEPS;
	}
	abstract static class EntityCaller extends PEntityRenderer
	{
		EntityCaller(EntityRendererProvider.Context context, PModelData model, Function<ResourceLocation, RenderType> material)
		{
			super(context, model, material);
		}
		static float nameScale()
		{
			return NAMETAG_SCALE;
		}
	}
}
'''
    if module == 'neoforge':
        source += '''final class CanonicalOverlayCaller
{
	static void overlay(com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorClientExtensions extension,
			net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.player.Player player)
	{
		extension.renderHelmetOverlay(stack, player, 320, 240, 0.5F);
	}
}
'''
    if label == 'current':
        source = normalize(source).replace('ResourceLocation', 'Identifier')
    directory = output / (label + '-' + module + '-callers')
    directory.mkdir()
    path = directory / 'CanonicalInheritedCaller.java'
    path.write_text(source)
    cp = str(output / (label + '-' + module + '.jar')) + ':' + (output / (label + '-' + module + '.classpath')).read_text()
    result = subprocess.run(['javac', '-J-XX:-UsePerfData', '--release', '21' if label == 'canonical' else '25', '-proc:none',
                             '-Xlint:none', '-classpath', cp, '-sourcepath', '', '-d', str(directory), str(path)], capture_output=True, text=True)
    (directory / 'compiler.txt').write_text(result.stdout + result.stderr)
    if result.returncode:
        raise AssertionError('Canonical inherited caller failed: ' + result.stderr)
    return {'sourceSha256': digest(path), 'classFiles': len(list(directory.glob('*.class'))), 'exitCode': result.returncode}


def compare(before, after):
    """Return every loss/change, including external inherited members. Additions are safe surface additions here."""
    current = {row['type']: row for row in after}
    changes = []
    counts = Counter()
    for old in before:
        if not old['api']:
            continue
        name = old['type']
        new = current.get(name)
        if new is None:
            changes.append({'kind': 'removed-type', 'type': name, 'canonical': {key: old[key] for key in HEADER_KEYS}, 'current': None})
            continue
        header = {key: {'canonical': old[key], 'current': new[key]} for key in HEADER_KEYS if normalize(old[key]) != normalize(new[key])}
        if header:
            changes.append({'kind': 'header', 'type': name, 'changes': header})
        candidates = [member for member in new['members'] if member['kind'] not in TYPE_KINDS]
        for member in old['members']:
            if member['kind'] in TYPE_KINDS:
                continue  # Each accessible nested declaration has its own type row.
            shape = contract(member)
            exact = [candidate for candidate in candidates if contract(candidate) == shape]
            adapted = [candidate for candidate in candidates if normalize(contract(candidate)) == normalize(shape)]
            if exact or adapted:
                counts['exact' if exact else 'native-name-adaptation'] += 1
                counts['inherited-preserved' if not member['declared'] else 'declared-preserved'] += 1
                continue
            changes.append({'kind': 'member', 'type': name, 'owner': member['owner'], 'inherited': not member['declared'],
                            'canonical': shape, 'currentCandidates': [contract(candidate) for candidate in candidates if candidate['name'] == member['name']]})
    return changes, dict(counts)


def binary_proof(surface, binaries):
    by_name = {row['binaryName'].replace('/', '.'): row for row in binaries}
    checks = 0
    for row in surface:
        if row['binaryName'] not in by_name:
            raise AssertionError('Named source type absent from bytecode: ' + row['type'])
        for member in row['members']:
            if not member['owner'].startswith(PREFIX) or member['kind'] in TYPE_KINDS:
                continue
            owner = by_name[member['ownerBinary']]
            candidates = owner['methods' if member['kind'] in {'METHOD', 'CONSTRUCTOR'} else 'fields']
            if not any(item['name'] == member['name'] and item['descriptor'] == member['descriptor'] for item in candidates):
                raise AssertionError('Source erasure absent from bytecode: ' + encoded(member))
            checks += 1
    return checks


def bridge_changes(before, after):
    current = {row['binaryName']: row for row in after}
    changes, preserved = [], 0
    for row in before:
        for bridge in row['methods']:
            if not bridge['flags'] & 0x40:
                continue
            candidates = current.get(row['binaryName'], {}).get('methods', [])
            if any(normalize(bridge) == normalize(member) for member in candidates):
                preserved += 1
            else:
                changes.append({'kind': 'bridge', 'type': row['binaryName'].replace('/', '.').replace('$', '.'),
                                'canonical': bridge, 'currentCandidates': [member for member in candidates if member['flags'] & 0x40 and member['name'] == bridge['name']]})
    return changes, preserved


def write_gzip(path, rows):
    with path.open('wb') as file:
        with gzip.GzipFile(filename='', mode='wb', fileobj=file, mtime=0) as zipped:
            zipped.write(''.join(encoded(row) + '\n' for row in rows).encode())


def acceptance(changes, accepted):
    expected = {encoded(row['contract']): row for row in accepted['reviewedExceptions']}
    if len(expected) != len(accepted['reviewedExceptions']):
        raise AssertionError('Duplicate accepted exceptions')
    actual = {}
    for row in changes:
        contract = {key: value for key, value in row.items() if key != 'module'}
        key = encoded(contract)
        actual.setdefault(key, {'contract': contract, 'modules': []})['modules'].append(row['module'])
    if actual.keys() != expected.keys():
        raise AssertionError('Unreviewed or stale exceptions: ' + encoded({'unreviewed': [actual[key] for key in actual.keys() - expected.keys()],
                                                                       'stale': [json.loads(key) for key in expected.keys() - actual.keys()]}))
    for key, row in actual.items():
        if sorted(row['modules']) != expected[key]['modules']:
            raise AssertionError('Exception loader coverage differs: ' + key)
    return [expected[key] for key in sorted(actual)]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-root', type=Path, required=True)
    parser.add_argument('--canonical-root', type=Path, required=True)
    parser.add_argument('--current-classpath-root', type=Path, default=ROOT)
    parser.add_argument('--canonical-classpath-root', type=Path)
    parser.add_argument('--current-ref', default='HEAD', help='Git commit/tree matching the current production Java projection')
    parser.add_argument('--output', type=Path, required=True, help='New directory for isolated generated classes and reports')
    parser.add_argument('--accepted', type=Path, default=ROOT / 'docs/porting/evidence/stage-7.2f-api.json')
    parser.add_argument('--jars', type=Path, nargs=3, required=True, metavar=('COMMON', 'FABRIC', 'NEOFORGE'))
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    canonical = args.canonical_root.resolve()
    current = args.source_root.resolve()
    provenance = {'canonical': source_proof(canonical, SOURCES['1.21.1-multiloader']),
                  'current': source_proof(current, git('rev-parse', args.current_ref).decode().strip())}
    parsed = {label: {row['fqcn'] for row in map(json.loads, java('ExtractApi.java', root).splitlines())}
              for label, root in [('canonical', canonical), ('current', current)]}
    surfaces, bytecode, dependencies = [], [], {}
    changes, modules = [], {}
    for module, release in zip(MODULES, args.jars):
        data = {}
        for label, root, project in [('canonical', canonical, (args.canonical_classpath_root or canonical).resolve()),
                                     ('current', current, args.current_classpath_root.resolve())]:
            rows, binaries, cp = attribute(root, project, label, module, args.output)
            data[label] = (rows, binaries)
            dependencies[label + '-' + module] = cp
            surfaces.extend(dict(row, version=label, module=module) for row in rows)
            bytecode.extend(dict(row, version=label, module=module) for row in binaries)
        old, old_binary = data['canonical']
        new, new_binary = data['current']
        delta, counts = compare(old, new)
        bridges, retained = bridge_changes(old_binary, new_binary)
        changes.extend(dict(row, module=module) for row in delta + bridges)
        released = [json.loads(line) for line in java('ExtractBinaryApi.java', release).splitlines()]
        for row in released:
            row.pop('jar')
        if sorted(released, key=encoded) != sorted(new_binary, key=encoded):
            raise AssertionError('Generated contracts differ from release JAR: ' + str(release))
        modules[module] = {'canonicalTypes': len(old), 'currentTypes': len(new), 'canonicalApiTypes': sum(row['api'] for row in old),
                           'currentApiTypes': sum(row['api'] for row in new), 'memberMatches': counts,
                           'sourceErasureChecks': {'canonical': binary_proof(old, old_binary), 'current': binary_proof(new, new_binary)},
                           'canonicalClassFiles': len(old_binary), 'currentClassFiles': len(new_binary),
                           'canonicalBridges': sum(bool(member['flags'] & 0x40) for row in old_binary for member in row['methods']),
                           'currentBridges': sum(bool(member['flags'] & 0x40) for row in new_binary for member in row['methods']),
                           'retainedCanonicalBridges': retained, 'releaseJar': str(release), 'releaseJarSha256': digest(release),
                           'canonicalCallerProbes': {label: callers(label, module, args.output) for label in ('canonical', 'current')},
                           'implementationOnlyRemovedTypes': [row['type'] for row in old if not row['api'] and row['type'] not in {item['type'] for item in new}]}
        print(module + ': attributed both versions, checked erasures/bridges and all release class contracts', flush=True)
    for label, names in parsed.items():
        if {row['type'] for row in surfaces if row['version'] == label} != names:
            raise AssertionError('Compiler and parse-only named type inventories disagree: ' + label)
    raw = {'modules': modules, 'changes': changes}
    (args.output / 'observed.json').write_text(json.dumps(raw, indent=2) + '\n')
    reviewed = acceptance(changes, json.loads(args.accepted.read_text()))
    write_gzip(args.output / 'resolved-surface.ndjson.gz', surfaces)
    write_gzip(args.output / 'generated-binary.ndjson.gz', bytecode)
    (args.output / 'classpath-provenance.json').write_text(json.dumps(dependencies, indent=2) + '\n')
    report = {'checkpoint': '7.2f', 'status': 'PASS_WITH_ENUMERATED_NATIVE_EXCEPTIONS', 'sourceProof': provenance, 'modules': modules,
              'nameAdaptations': RENAMES, 'playerModelGenericRemoval': True, 'reviewedExceptions': reviewed,
              'artifactSha256': {name: digest(args.output / name) for name in ('resolved-surface.ndjson.gz', 'generated-binary.ndjson.gz', 'classpath-provenance.json')},
              'limits': ['Source surface acceptance uses each version native classpath; Minecraft 1.21.1 binary compatibility is not asserted.',
                         'Exact exceptions include removed/changed dependency-inherited methods; they are not counted as preserved PulseLib methods.',
                         'This does not test arbitrary downstream overload choices, runtime rendering, cross-loader class loading, pixels or performance.']}
    (args.output / 'report.json').write_text(json.dumps(report, indent=2, ensure_ascii=False) + '\n')
    print(report['status'])


if __name__ == '__main__':
    main()
