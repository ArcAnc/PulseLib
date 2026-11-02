#!/usr/bin/env python3
"""Reject pre-existing runs, path traversal, symlinks and missing EULA without launching Minecraft.
ArcAnc, created 03.11.2026, copyright 2026.
Arc's License of Common Sense; see LICENSE.txt at repository root.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import uuid

ROOT = Path(__file__).resolve().parents[3]


def fingerprints(directory):
    return {path.name: hashlib.sha256(path.read_bytes()).hexdigest() for path in directory.iterdir()}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--gradle-init', type=Path, action='append', default=[])
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    prefix = 'guards-' + uuid.uuid4().hex
    runs = ROOT / 'fabric/build/pulselib-tests'
    base = [str(ROOT / 'gradlew'), ':fabric:prepareTestmodServer', '-PpulseTestJarBootstrap', '--offline', '--no-daemon', '--console=plain']
    for script in args.gradle_init:
        base.extend(['-I', str(script.resolve())])
    cases = []

    def reject(name, run_id, message):
        result = subprocess.run(base + ['-PpulseBootstrapRunId=' + run_id], cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=180)
        (output / (name + '.log')).write_text(result.stdout)
        assert result.returncode != 0 and message in result.stdout, f'Expected rejection: {name}'
        record = {'case': name, 'exit': result.returncode}
        cases.append(record)
        return record

    for name, marked in [('existing-unmarked', False), ('existing-marked', True)]:
        run_id = prefix + '-' + name
        directory = runs / run_id / 'server'
        directory.mkdir(parents=True, exist_ok=False)
        (directory / 'sentinel.txt').write_text('Owned guard-test data; must not change\n')
        if marked:
            (directory / '.pulselib-test-run').write_text('PulseLib disposable test run\n')
        before = fingerprints(directory)
        record = reject(name, run_id, 'Refusing every existing disposable run directory')
        assert fingerprints(directory) == before
        record['filesUnchanged'] = True

    reject('traversal', '../' + prefix, 'simple directory name, never a path')
    target = output / 'symlink-target'
    target.mkdir()
    (runs / (prefix + '-symlink')).symlink_to(target, target_is_directory=True)
    record = reject('symlink', prefix + '-symlink', 'Refusing symlink in disposable run path')
    assert not list(target.iterdir())
    record['targetUnchanged'] = True
    record = reject('no-eula', prefix + '-no-eula', 'no EULA is accepted automatically')
    assert not (runs / (prefix + '-no-eula') / 'server').exists()
    record['runNotCreated'] = True
    (output / 'results.json').write_text(json.dumps({'status': 'PASS', 'runPrefix': prefix, 'cases': cases}, indent=2) + '\n')
    print('DISPOSABLE GUARDS PASS:', len(cases), 'rejections; existing data unchanged')


if __name__ == '__main__':
    main()
