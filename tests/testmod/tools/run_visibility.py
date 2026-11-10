#!/usr/bin/env python3
"""Verify native front-away-front visibility and canonical camera-away reproduction on both loaders.
ArcAnc, created 10.11.2026, copyright 2026.
Arc's License of Common Sense; see LICENSE.txt at repository root.
"""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import struct
import sys
import uuid
import zipfile

sys.dont_write_bytecode = True
from run_bootstrap import ROOT, run


def verify_positive(stdout, directory, mode):
    case = json.loads((directory / 'demo-results.json').read_text())
    assert case['status'] == 'PASS' and case['fixture'] == 'visibility' and case['phases'] == 6
    assert (directory / '.pulselib-test-run').read_text() == 'PulseLib disposable test run\n'
    world = directory / 'saves/pulselib-bootstrap'
    assert (world / '.pulselib-disposable').read_text() == 'PulseLib disposable test world\n' and (world / 'level.dat').is_file()
    assert ('classes/java/demo' if mode == 'source' else 'pulselib-testmod-demo') in case['origin']
    records = case['records']
    assert len(records) == 24 and Counter((r['epoch'], r['phase']) for r in records) == Counter({(e, p): 4 for e in range(2) for p in range(3)})
    for record in records:
        visible = record['phase'] != 1
        assert record['queries'] == 2 and record['frustum'] == visible
        assert abs(record['yaw'] - (0 if visible else 180)) < 0.001
        assert record['samples'] > 0 if visible else record['samples'] == 0
        if visible:
            assert record['owners'] == 1 and record['callbacks'] > 0
    for marker in ['PULSE DEMO PLUGINS PASS', 'VISIBILITY PASS', 'VISIBILITY SHUTDOWN PASS', 'PULSE BOOTSTRAP VERIFIED', 'BUILD SUCCESSFUL']:
        assert stdout.count(marker) == 1, (str(directory), marker)
    assert stdout.count('VISIBILITY SAMPLE') == 24
    assert all(marker not in stdout for marker in ['RENDER REGRESSION PASS', 'NATIVE WORLD PASS', 'H14 PIXEL PASS', 'PULSE TEST SMOKE COMPLETE'])
    images = list((directory / 'h14-offscreen').glob('*.png'))
    assert {image.name for image in images} == {f'visibility-{epoch}-{phase}.png' for epoch in range(2) for phase in range(3)}
    for image in images:
        data = image.read_bytes()
        assert data[:8] == b'\x89PNG\r\n\x1a\n' and struct.unpack('>II', data[16:24]) == (960, 640)
    return {'exitCode': 0, 'runDirectory': str(directory.relative_to(ROOT)), 'result': case, 'screenshots': len(images)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--gradle-init', type=Path, action='append', default=[])
    parser.add_argument('--cases', nargs='+', choices=['fabric-jar', 'neoforge-source'], default=['fabric-jar', 'neoforge-source'])
    parser.add_argument('--negative-control', action='store_true', help='Pass canonical pulseSmokeReproduce; require yaw=180/frustum=false/GPU=0 and the original visibility error.')
    parser.add_argument('--timeout', type=int, default=240)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    base = [str(ROOT / 'gradlew'), '--offline', '--no-daemon', '--console=plain']
    for script in args.gradle_init:
        base.extend(['-I', str(script.resolve())])
    result = {'status': 'RUNNING', 'runId': 'visibility-' + uuid.uuid4().hex, 'negativeControl': args.negative_control, 'cases': {}, 'jars': {}}
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
                assert len(names) == len(set(names)) and 'com/arcanc/pulselib/test/VisibilityHarness.class' in names
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
            command = base + [f':{module}:runClient', '-P' + ('pulseDemoJar' if mode == 'jar' else 'pulseDemoContent'), '-PpulseVisibilityRegression', '-PpulseBootstrapRunId=' + result['runId']]
            if args.negative_control:
                command += ['-PpulseSmokeReproduce']
            log = output / (selection + '.log')
            failed = False
            try:
                stdout = run(command, log, args.timeout)
            except RuntimeError:
                failed = True
                stdout = log.read_text()
            if args.negative_control:
                assert failed and 'PulseLib smoke test: Original block entity renderer must run' in stdout
                assert 'VISIBILITY SAMPLE epoch=0 phase=0 yaw=180.0 frustum=false samples=0' in stdout
                assert 'queries=2' in stdout and 'finished with non-zero exit value 255' in stdout and 'BUILD FAILED' in stdout
                assert 'VISIBILITY PASS' not in stdout and 'VISIBILITY SHUTDOWN PASS' not in stdout
                assert not (directory / 'demo-results.json').exists()
                result['cases'][selection] = {'expectedFailure': 'Original block entity renderer must run', 'nativeYaw': 180, 'frustum': False, 'gpuSamples': 0, 'javaExit': 255, 'gradleExit': 1, 'runDirectory': str(directory.relative_to(ROOT))}
            else:
                assert not failed, log
                result['cases'][selection] = verify_positive(stdout, directory, mode)
            save()
            print('VISIBILITY CONTROL PASS:' if args.negative_control else 'VISIBILITY PASS:', selection, flush=True)
        result['status'] = 'PASS'
        result.pop('activeCase', None)
        save()
    except BaseException as error:
        result.update(status='FAIL', error=str(error))
        save()
        raise


if __name__ == '__main__':
    main()
