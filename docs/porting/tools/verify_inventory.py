#!/usr/bin/env python3
"""Verify the API inventory against javac/javap and pinned source coverage.
ArcAnc, created 21.10.2026, copyright 2026.
Licensed under Arc's License of Common Sense; see LICENSE.txt at repository root.
"""

import csv
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile

sys.dont_write_bytecode = True

from inventory import OUTPUT, ROOT, SOURCES, git


def check(condition, message):
    if not condition:
        raise AssertionError(message)


def declarations(path):
    return [json.loads(line) for line in path.read_text().splitlines()]


def fixture():
    with tempfile.TemporaryDirectory(prefix='pulselib-api-fixture-') as tmp:
        root = Path(tmp)
        sources = root / 'src/main/java/sample'
        sources.mkdir(parents=True)
        (sources / 'Api.java').write_text('''package sample;
import java.io.IOException;
import java.util.List;
public class Api<T extends Number>
{
	public static final int FIRST = 7, SECOND = 8;
	protected Api(int value) {}
	public <U extends T> List<? super U[]> convert(U... values) throws IOException { return null; }
	public class Member {}
	protected static class Extensible {}
	private static class Secret { public void hidden() {} }
	public record Value(int number, String label) {}
	public record Explicit(int number) { public Explicit {} public int number() { return number; } }
	public enum Choice { A, B; }
	public interface Contract
	{
		int CONSTANT = 9;
		String call();
		default String supplied() { return ""; }
		private void hidden() {}
		class Nested {}
	}
}
''')
        rows = declarations_from_output(subprocess.check_output(
            ['java', str(ROOT / 'docs/porting/tools/ExtractApi.java'), str(root)], text=True))
        by_name = {row['fqcn']: row for row in rows}
        check(by_name['sample.Api.Contract.Nested']['api'], 'Implicitly public interface member type missing')
        check(by_name['sample.Api.Extensible']['api'], 'Protected nested API type missing')
        check(not by_name['sample.Api.Secret']['api'], 'Private nested type leaked into API')
        signatures = {row['fqcn']: {member['signature'] for member in row['members']} for row in rows}
        check('public <init>()' in signatures['sample.Api.Contract.Nested'], 'Implicit interface member constructor missing')
        check('public <init>()' in signatures['sample.Api.Member'], 'Implicit member constructor missing')
        check('protected <init>()' in signatures['sample.Api.Extensible'], 'Protected default constructor missing')
        check('public <init>(int, String)' in signatures['sample.Api.Value'], 'Record canonical constructor missing')
        check('public int number()' in signatures['sample.Api.Value'], 'Record accessor missing')
        check(sum(member['name'] == 'number' for member in by_name['sample.Api.Explicit']['members']) == 1, 'Explicit record accessor duplicated')
        check(sum(member['kind'] == 'constructor' for member in by_name['sample.Api.Explicit']['members']) == 1, 'Compact constructor duplicated')
        check('public static Choice[] values()' in signatures['sample.Api.Choice'], 'Enum values() missing')
        check('public static Choice valueOf(String)' in signatures['sample.Api.Choice'], 'Enum valueOf() missing')
        check(not any(member['name'] == 'hidden' for member in by_name['sample.Api.Contract']['members']), 'Private interface method leaked')
        check(any('convert(U...) throws IOException' in item for item in signatures['sample.Api']), 'Generic varargs/throws lost')
        check({'FIRST', 'SECOND'} <= {member['name'] for member in by_name['sample.Api']['members']}, 'Multi-declarator field lost')
        classes = root / 'classes'
        result = subprocess.run(['javac', '-d', str(classes), str(sources / 'Api.java')], capture_output=True, text=True)
        check(result.returncode == 0, 'Independent javac fixture failed: ' + result.stderr)
        for name in ['sample.Api', 'sample.Api$Value', 'sample.Api$Explicit', 'sample.Api$Choice',
                     'sample.Api$Contract', 'sample.Api$Contract$Nested', 'sample.Api$Extensible']:
            output = subprocess.check_output(['javap', '-protected', '-classpath', str(classes), name], text=True)
            bytecode_members = [line for line in output.splitlines() if line.startswith('  ') and line.rstrip().endswith(';')]
            source_row = by_name[name.replace('$', '.')]
            check(len(bytecode_members) == len(source_row['members']),
                  f'javap member count mismatch for {name}: {len(bytecode_members)} != {len(source_row["members"])}')
        print('PASS: Java AST fixture and independent javac/javap counts for seven types')


def declarations_from_output(output):
    return [json.loads(line) for line in output.splitlines()]


def coverage():
    manifest = json.loads((OUTPUT / 'source-manifest.json').read_text())
    table = list(csv.DictReader((OUTPUT / 'class-migration-map.tsv').open(), delimiter='\t'))
    dependencies = declarations(OUTPUT / 'source-dependencies.ndjson')
    for source in manifest['sources']:
        label = source['branch']
        check(source['commit'] == SOURCES[label], 'Source commit drift')
        api = declarations(OUTPUT / ('api-' + label + '.ndjson'))
        check(len(api) == source['accessible_types'], 'Accessible type count drift')
        check(sum(len(row['members']) for row in api) == source['accessible_members'], 'Member count drift')
        units = [unit for unit in dependencies if unit['branch'] == label]
        paths = {unit['source'] for unit in units}
        tracked = git('ls-tree', '-r', '--name-only', source['commit']).decode().splitlines()
        expected = {path for path in tracked if '/src/main/java/' in '/' + path and not path.startswith('tests/') and path.endswith('.java')}
        check(paths == expected, 'Uncovered production Java source')
        check(all(row['source'] in paths for row in api), 'API source context missing')
        field = '26.1_source' if label == '26.1' else 'multiloader_source'
        check(sum(bool(row[field]) for row in table) == source['named_types'], 'Migration map named-type coverage gap')
        check({row['fqcn'] for row in api} <= {row['fqcn'] for row in table}, 'Public type absent from migration map')
        check(all(row['stage'] and row['target'] and row['decision'] and row['adaptation'] for row in table), 'Incomplete migration policy')
    required = ['PAnimatable', 'PAnimationController', 'PAnimationManager', 'AnimManagerKey', 'PLibHelper',
                'PAnimation', 'PAnimationGraphRuntime', 'PAnimationPoseResolver', 'PAnimationRuntime',
                'PModel', 'PBakedModel', 'PRenderer', 'PRenderQueue', 'PFrameCompiler', 'PRenderPlan',
                'PLibRegistration', 'PPlayerAnimations', 'PulsePluginManager', 'PulsePluginLifecycle', 'IPulsePlatform']
    modern = declarations(OUTPUT / 'api-1.21.1-multiloader.ndjson')
    check(set(required) <= {row['fqcn'].split('.')[-1] for row in modern}, 'Required canonical API missing')
    for line in (OUTPUT / 'checksums.sha256').read_text().splitlines():
        digest, name = line.split('  ', 1)
        check(hashlib.sha256((OUTPUT / name).read_bytes()).hexdigest() == digest, 'Checksum mismatch: ' + name)
    print('PASS: pinned source coverage, all migration policies, required APIs and artifact checksums')


if __name__ == '__main__':
    fixture()
    coverage()
