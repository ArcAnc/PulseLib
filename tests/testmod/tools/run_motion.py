#!/usr/bin/env python3
"""Run the full canonical player motion matrix/physics/camera harness in fresh disposable worlds on both loaders.
ArcAnc, created 10.11.2026, copyright 2026.
Arc's License of Common Sense; see LICENSE.txt at repository root.
"""
import argparse
import hashlib
import json
from pathlib import Path
import sys
import uuid
import zipfile

sys.dont_write_bytecode = True
from run_bootstrap import ROOT, run


def verify_positive(stdout, directory, mode):
    case = json.loads((directory / 'demo-results.json').read_text())
    assert case['status'] == 'PASS' and case['fixture'] == 'player-motion'
    full = case['phase'] != 'mp2'
    assert case['stages'] == ([420, 2, 4, 2] if full else [240, 2, 4, 0])
    assert case['modelEpochs'] == 2 and case['reloaded'] and case['observers'] == 2
    assert case['checked'] >= (436 if full else 252)
    assert case['cameraChecks'] >= 222 if full else case['cameraChecks'] == 0
    assert all(value > 0 for value in case['callbacks'])
    assert (directory / '.pulselib-test-run').read_text() == 'PulseLib disposable test run\n'
    world = directory / 'saves/pulselib-bootstrap'
    assert (world / '.pulselib-disposable').read_text() == 'PulseLib disposable test world\n'
    assert (world / 'level.dat').is_file()
    assert ('classes/java/demo' if mode == 'source' else 'pulselib-testmod-demo') in case['origin']
    for marker in ['PULSE DEMO PLUGINS PASS', 'PLAYER MOTION M-P2 PASS', 'PLAYER MOTION SHUTDOWN PASS', 'PULSE BOOTSTRAP VERIFIED', 'BUILD SUCCESSFUL']:
        assert stdout.count(marker) == 1, (directory, marker)
    assert stdout.count('PLAYER MOTION M-P3 PASS') == (1 if full else 0)
    assert all(marker not in stdout for marker in ['ITEM IDENTITY CLIENT PASS', 'VISIBILITY PASS', 'RENDER REGRESSION PASS', 'NATIVE WORLD PASS', 'H14 PIXEL PASS', 'PULSE TEST SMOKE COMPLETE'])
    return {'exitCode': 0, 'runDirectory': str(directory.relative_to(ROOT)), 'result': case}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--gradle-init', type=Path, action='append', default=[])
    parser.add_argument('--cases', nargs='+', choices=['fabric-jar', 'neoforge-source'], default=['fabric-jar', 'neoforge-source'])
    parser.add_argument('--negative-control', action='store_true', help='Bypass the actual queue submission during a matrix probe; require its original guard to fail.')
    parser.add_argument('--timeout', type=int, default=600)
    parser.add_argument('--phase', default='all')
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    base = [str(ROOT / 'gradlew'), '--offline', '--no-daemon', '--console=plain']
    for script in args.gradle_init:
        base.extend(['-I', str(script.resolve())])
    result = {'status': 'RUNNING', 'runId': 'motion-' + uuid.uuid4().hex, 'negativeControl': args.negative_control, 'cases': {}, 'jars': {}}
    report = output / 'results.json'

    def save():
        report.write_text(json.dumps(result, indent=2) + '\n')

    save()
    try:
        run(base + [':fabric:demoJar', ':neoforge:demoJar'], output / 'package.log', args.timeout)
        manifest = json.loads((ROOT / 'tests/testmod/demo/common/resources/pulselib-demo-assets.json').read_text())
        for module in ['fabric', 'neoforge']:
            jar = ROOT / module / 'build/libs' / f'pulselib-testmod-demo-26.1.2-1.1.5.3-{module}.jar'
            with zipfile.ZipFile(jar) as archive:
                names = archive.namelist()
                assert len(names) == len(set(names)) and 'com/arcanc/pulselib/test/PlayerMotionHarness.class' in names
                assert all(info.date_time == (1980, 2, 1, 0, 0, 0) for info in archive.infolist())
                for resource, digest in manifest.items():
                    namespace, path = resource.split(':', 1)
                    assert hashlib.sha256(archive.read(f'assets/{namespace}/{path}')).hexdigest() == digest
            result['jars'][module] = {'path': str(jar.relative_to(ROOT)), 'sha256': hashlib.sha256(jar.read_bytes()).hexdigest()}
        for selection in args.cases:
            module, mode = selection.split('-')
            result['activeCase'] = selection
            save()
            directory = ROOT / module / 'build/pulselib-tests' / result['runId'] / 'client'
            assert not directory.exists(), 'Use a new disposable run directory'
            command = base + [f':{module}:runClient', '-P' + ('pulseDemoJar' if mode == 'jar' else 'pulseDemoContent'), '-PpulsePlayerMotionTest', '-PpulseMotionPhase=' + args.phase, '-PpulseBootstrapRunId=' + result['runId']]
            if args.negative_control:
                command += ['-PpulseMotionSkipSubmission']
            log = output / (selection + '.log')
            failed = False
            try:
                stdout = run(command, log, args.timeout)
            except RuntimeError:
                failed = True
                stdout = log.read_text()
            if args.negative_control:
                assert failed and 'PulseLib smoke test: real third-person mesh submission' in stdout, log
                assert 'finished with non-zero exit value 255' in stdout and 'BUILD FAILED' in stdout
                assert 'PLAYER MOTION M-P2 PASS' not in stdout and 'PLAYER MOTION M-P3 PASS' not in stdout
                assert not (directory / 'demo-results.json').exists()
                result['cases'][selection] = {'expectedFailure': 'real third-person mesh submission', 'javaExit': 255, 'gradleExit': 1, 'runDirectory': str(directory.relative_to(ROOT))}
            else:
                assert not failed, log
                result['cases'][selection] = verify_positive(stdout, directory, mode)
            save()
            print('MOTION CONTROL PASS:' if args.negative_control else 'MOTION PASS:', selection, flush=True)
        result['status'] = 'PASS'
        result.pop('activeCase', None)
        save()
    except BaseException as error:
        result.update(status='FAIL', error=str(error))
        save()
        raise


if __name__ == '__main__':
    main()
