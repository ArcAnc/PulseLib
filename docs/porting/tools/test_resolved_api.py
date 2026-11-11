#!/usr/bin/env python3
"""Compiler audit regression fixtures, not Minecraft/game tests.
ArcAnc, created 11.11.2026, copyright 2026.
Arc's License of Common Sense. See LICENSE.txt at repository root.
"""
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import zipfile

sys.dont_write_bytecode = True

import verify_resolved_api as audit


class ResolvedApiTest(unittest.TestCase):
    def test_attribution_and_generated_linkage(self):
        with tempfile.TemporaryDirectory(prefix='pulselib-resolved-fixture-') as temporary:
            root = Path(temporary)
            source = root / 'source'
            source.mkdir()
            (source / 'Child.java').write_text('''package sample;
import java.util.*;
interface Provider<T> { T value(); }
class Base<T> { public List<? extends T> inherited() { return null; }
 public <U extends T> U bounded(U value) { return value; } }
public class Child extends Base<String> implements Provider<String> {
 public String value() { return ""; }
 public class Inner { public Inner(int value) {} }
 public void local() { class Local { class AlsoLocal {} } Object ignored = new Object() { class AlsoAnonymous {} }; }
}''')
            cp = root / 'classpath'
            cp.write_text('')
            classes = root / 'classes'
            rows = [json.loads(line) for line in audit.java('ExtractResolvedApi.java', 25, cp, classes, source).splitlines()]
            by_name = {row['type']: row for row in rows}
            self.assertEqual(set(by_name), {'sample.Provider', 'sample.Base', 'sample.Child', 'sample.Child.Inner'})
            members = by_name['sample.Child']['members']
            inherited = next(member for member in members if member['name'] == 'inherited')
            self.assertFalse(inherited['declared'])
            self.assertEqual(inherited['returns'], 'java.util.List<? extends java.lang.String>')
            bounded = next(member for member in members if member['name'] == 'bounded')
            self.assertEqual(bounded['typeParameters'], [{'name': 'U', 'bounds': ['java.lang.String']}])
            self.assertEqual(bounded['descriptor'], '(Ljava/lang/Object;)Ljava/lang/Object;')
            self.assertEqual(bounded['resolvedDescriptor'], '(Ljava/lang/String;)Ljava/lang/String;')
            inner = next(member for member in by_name['sample.Child.Inner']['members'] if member['name'] == '<init>')
            self.assertEqual(inner['descriptor'], '(Lsample/Child;I)V')
            jar = root / 'classes.jar'
            with zipfile.ZipFile(jar, 'w') as output:
                for path in classes.rglob('*.class'):
                    output.write(path, path.relative_to(classes))
            binary = [json.loads(line) for line in audit.java('ExtractBinaryApi.java', jar).splitlines()]
            child = next(row for row in binary if row['binaryName'] == 'sample/Child')
            bridges = [member for member in child['methods'] if member['flags'] & 0x40]
            self.assertEqual({(member['name'], member['descriptor']) for member in bridges},
                             {('value', '()Ljava/lang/Object;'), ('bounded', '(Ljava/lang/Object;)Ljava/lang/Object;'), ('inherited', '()Ljava/util/List;')})
            self.assertTrue(all(member['flags'] & 0x1000 for member in bridges))

    def test_unresolved_type_rejects_inventory(self):
        with tempfile.TemporaryDirectory(prefix='pulselib-resolved-error-') as temporary:
            root = Path(temporary)
            source = root / 'source'
            source.mkdir()
            (source / 'Broken.java').write_text('public class Broken { public Missing unresolved; }')
            cp = root / 'classpath'
            cp.write_text('')
            result = subprocess.run(['java', '-XX:-UsePerfData', str(audit.ROOT / 'docs/porting/tools/ExtractResolvedApi.java'),
                                     '25', str(cp), str(root / 'classes'), str(source)], capture_output=True, text=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertEqual(result.stdout, '')
            self.assertIn('Compiler errors:', result.stderr)

    def test_unreviewed_loss_and_stale_exception_fail(self):
        canonical = {'api': True, 'kind': 'CLASS', 'flags': ['public'], 'typeParameters': [], 'superclass': 'java.lang.Object',
                     'interfaces': [], 'type': 'sample.Api', 'members': [{'kind': 'METHOD', 'name': 'value', 'owner': 'sample.Api',
                     'declared': True, 'flags': ['public'], 'returns': 'java.lang.String', 'parameters': [], 'throws': []}]}
        current = dict(canonical, members=[])
        changes, _ = audit.compare([canonical], [current])
        self.assertEqual(len(changes), 1)
        changes[0]['module'] = 'common'
        with self.assertRaisesRegex(AssertionError, 'Unreviewed'):
            audit.acceptance(changes, {'reviewedExceptions': []})
        contract = {key: value for key, value in changes[0].items() if key != 'module'}
        accepted = {'reviewedExceptions': [{'contract': contract, 'modules': ['common'], 'decision': {'status': 'fixture'}}]}
        self.assertEqual(len(audit.acceptance(changes, accepted)), 1)
        with self.assertRaisesRegex(AssertionError, 'stale'):
            audit.acceptance([], accepted)
        with self.assertRaisesRegex(AssertionError, 'loader coverage'):
            audit.acceptance([dict(changes[0], module='fabric')], accepted)


if __name__ == '__main__':
    unittest.main()
