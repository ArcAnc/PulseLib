#!/usr/bin/env python3
"""Run only the residual legacy attachment clip/format pixel fixture on both loaders.
ArcAnc, created 11.11.2026, copyright 2026.
Arc's License of Common Sense. See LICENSE.txt at repository root.
"""
import argparse
import hashlib
import itertools
import json
from pathlib import Path
import sys
import uuid
import zipfile

sys.dont_write_bytecode = True
from run_bootstrap import ROOT, run


def verify(stdout, directory, mode):
    result = json.loads((directory / 'demo-results.json').read_text())
    assert result['status'] == 'PASS' and result['fixture'] == 'legacy-clips'
    assert result['cases'] == 48 and result['epochs'] == 2 and result['providers'] == 48 and result['resolvers'] >= 48
    expected = set(itertools.product(range(2), ['gecko', 'gltf'], ['global', 'item'], ['move', 'shape'], [0, 10, 20]))
    actual = {(row['epoch'], row['format'], row['source'], row['clip'], row['ticks']) for row in result['records']}
    assert len(result['records']) == len(actual) == 48 and actual == expected
    assert all(row['inside'] >= 32 and row['outside'] > 15000 for row in result['records'])
    for row in result['records']:
        previous = next(value for value in result['records'] if value['epoch'] == 0 and all(value[k] == row[k] for k in ['format', 'source', 'clip', 'ticks']))
        assert row['inside'] == previous['inside'] and row['outside'] == previous['outside']
    assert ('classes/java/demo' if mode == 'source' else 'pulselib-testmod-demo') in result['origin']
    assert (directory / '.pulselib-test-run').read_text() == 'PulseLib disposable test run\n'
    world = directory / 'saves/pulselib-bootstrap'
    assert (world / '.pulselib-disposable').read_text() == 'PulseLib disposable test world\n' and (world / 'level.dat').is_file()
    assert len(list((directory / 'h14-offscreen').glob('legacy-*.png'))) == 16
    for marker in ['LEGACY CLIPS PASS', 'LEGACY CLIPS SHUTDOWN PASS', 'PULSE BOOTSTRAP VERIFIED', 'BUILD SUCCESSFUL']:
        assert stdout.count(marker) == 1, marker
    assert not any(marker in stdout for marker in ['PLAYER MOTION M-P2 PASS', 'RENDER REGRESSION PASS', 'H14 PIXEL PASS', 'NATIVE WORLD PASS', 'PULSE TEST SMOKE COMPLETE'])
    return {'exitCode': 0, 'runDirectory': str(directory.relative_to(ROOT)), 'result': result}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--gradle-init', type=Path, action='append', default=[])
    parser.add_argument('--cases', nargs='+', choices=['fabric-jar', 'neoforge-source'], default=['fabric-jar', 'neoforge-source'])
    parser.add_argument('--negative-control', action='store_true')
    parser.add_argument('--timeout', type=int, default=600)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    base = [str(ROOT / 'gradlew'), '--offline', '--no-daemon', '--console=plain', '-I', str(ROOT / 'tests/testmod/tools/legacy-clips.gradle')]
    for init in args.gradle_init:
        base += ['-I', str(init.resolve())]
    result = {'status': 'RUNNING', 'runId': 'legacy-clips-' + uuid.uuid4().hex, 'negativeControl': args.negative_control, 'cases': {}, 'jars': {}}

    def save():
        (output / 'results.json').write_text(json.dumps(result, indent=2) + '\n')

    save()
    try:
        run(base + [':fabric:demoJar', ':neoforge:demoJar'], output / 'package.log', args.timeout)
        assets = json.loads((ROOT / 'tests/testmod/demo/common/resources/pulselib-legacy-clips-assets.json').read_text())['files']
        for module in ['fabric', 'neoforge']:
            jar = ROOT / module / f'build/libs/pulselib-testmod-demo-26.1.2-1.1.5.3-{module}.jar'
            with zipfile.ZipFile(jar) as archive:
                assert len(archive.namelist()) == len(set(archive.namelist()))
                assert 'com/arcanc/pulselib/test/LegacyClipHarness.class' in archive.namelist()
                for name, digest in assets.items():
                    assert hashlib.sha256(archive.read(name)).hexdigest() == digest
            result['jars'][module] = {'path': str(jar.relative_to(ROOT)), 'sha256': hashlib.sha256(jar.read_bytes()).hexdigest()}
        for selection in args.cases:
            module, mode = selection.split('-')
            result['activeCase'] = selection
            save()
            directory = ROOT / module / 'build/pulselib-tests' / result['runId'] / 'client'
            command = base + [f':{module}:runClient', '-P' + ('pulseDemoJar' if mode == 'jar' else 'pulseDemoContent'), '-PpulseLegacyClips', '-PpulseBootstrapRunId=' + result['runId']]
            if args.negative_control:
                command.append('-PpulseLegacySkipControllers')
            log = output / (selection + '.log')
            failed = False
            try:
                stdout = run(command, log, args.timeout)
            except RuntimeError:
                failed = True
                stdout = log.read_text()
            if args.negative_control:
                guards = [message for message in ['Legacy animated pixels:', 'No unrequested/static/late-mutated mesh pixels outside the authored footprint'] if message in stdout]
                assert failed and len(guards) == 1
                assert 'finished with non-zero exit value 255' in stdout and 'BUILD FAILED' in stdout
                assert 'LEGACY CLIPS PASS' not in stdout and not (directory / 'demo-results.json').exists()
                result['cases'][selection] = {'expectedFailure': guards[0], 'javaExit': 255, 'gradleExit': 1, 'runDirectory': str(directory.relative_to(ROOT))}
            else:
                assert not failed, log
                result['cases'][selection] = verify(stdout, directory, mode)
            save()
            print('LEGACY CLIP CONTROL PASS:' if args.negative_control else 'LEGACY CLIPS PASS:', selection, flush=True)
        result['status'] = 'PASS'
        result.pop('activeCase', None)
        save()
    except BaseException as error:
        result.update(status='FAIL', error=str(error))
        save()
        raise


if __name__ == '__main__':
    main()
