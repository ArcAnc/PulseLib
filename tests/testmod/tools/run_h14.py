#!/usr/bin/env python3
"""Run canonical H14 offscreen pixels on Fabric JAR and NeoForge sources.
ArcAnc, created 10.11.2026, copyright 2026.
Arc's License of Common Sense; see LICENSE.txt at repository root.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import sys
import uuid
import zipfile

sys.dont_write_bytecode = True
from run_bootstrap import ROOT, run


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--gradle-init', type=Path, action='append', default=[])
    parser.add_argument('--cases', nargs='+', choices=['fabric-jar', 'neoforge-source'], default=['fabric-jar', 'neoforge-source'])
    parser.add_argument('--negative-control', action='store_true', help='Expect the original parity guard to reject a skipped Fabulous resolve on Fabric.')
    parser.add_argument('--timeout', type=int, default=240)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    base = [str(ROOT / 'gradlew'), '--offline', '--no-daemon', '--console=plain']
    for script in args.gradle_init:
        base.extend(['-I', str(script.resolve())])
    result = {'status': 'RUNNING', 'runId': 'h14-' + uuid.uuid4().hex, 'negativeControl': args.negative_control, 'cases': {}, 'jars': {}}
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
                assert len(names) == len(set(names)), 'Duplicate fixture JAR entry'
                assert 'com/arcanc/pulselib/test/H14OffscreenHarness.class' in names
                assert all(info.date_time == (1980, 2, 1, 0, 0, 0) for info in archive.infolist())
                for resource, digest in manifest.items():
                    namespace, path = resource.split(':', 1)
                    assert hashlib.sha256(archive.read(f'assets/{namespace}/{path}')).hexdigest() == digest
                white = archive.read('assets/pulselibtest/textures/h14_white.png')
                assert struct.unpack('>II', white[16:24]) == (1, 1)
            result['jars'][module] = {'path': str(jar.relative_to(ROOT)), 'sha256': hashlib.sha256(jar.read_bytes()).hexdigest()}
        cases = [('fabric', 'jar')] if args.negative_control else [tuple(case.split('-')) for case in args.cases]
        for module, mode in cases:
            key = module + '-' + mode
            result['activeCase'] = key
            save()
            directory = ROOT / module / 'build/pulselib-tests' / result['runId'] / 'client'
            assert not directory.exists(), 'Use a fresh run directory'
            command = base + [f':{module}:runClient', '-P' + ('pulseDemoJar' if mode == 'jar' else 'pulseDemoContent'), '-PpulseH14Offscreen', '-PpulseBootstrapRunId=' + result['runId']]
            if args.negative_control:
                command += ['-PpulseH14SkipResolve']
            log = output / (key + '.log')
            failed = False
            try:
                stdout = run(command, log, args.timeout)
            except RuntimeError:
                failed = True
                stdout = log.read_text()
            assert not list(directory.rglob('level.dat')), 'Offscreen fixture must not open/create a world'
            assert (directory / '.pulselib-test-run').read_text() == 'PulseLib disposable test run\n'
            if args.negative_control:
                assert failed and 'H14: initial-front Fancy/Fabulous parity:' in stdout
                assert 'finished with non-zero exit value 255' in stdout and 'BUILD FAILED' in stdout
                assert 'H14 PIXEL PASS' not in stdout and 'H14 SHUTDOWN PASS' not in stdout
                assert not (directory / 'demo-results.json').exists()
                result['cases'][key] = {'expectedFailure': 'original Fancy/Fabulous parity', 'gradleExit': 1, 'javaExit': 255, 'runDirectory': str(directory.relative_to(ROOT))}
            else:
                assert not failed, log
                case = json.loads((directory / 'demo-results.json').read_text())
                assert case['status'] == 'PASS' and case['fixture'] == 'h14-offscreen'
                assert case['cases'] == 38 and case['epochs'] == 2 and case['nativeChains'] == 36 and case['framebufferResized']
                assert ('classes/java/demo' if mode == 'source' else 'pulselib-testmod-demo') in case['origin']
                for marker in ['PULSE DEMO PLUGINS PASS', 'H14 PIXEL PASS', 'H14 SHUTDOWN PASS', 'PULSE BOOTSTRAP VERIFIED', 'BUILD SUCCESSFUL']:
                    assert stdout.count(marker) == 1, (key, marker)
                assert stdout.count('H14 CASE PASS') == 36
                images = sorted((directory / 'h14-offscreen').glob('*.png'))
                assert len(images) == 48
                for image in images:
                    data = image.read_bytes()
                    assert data[:8] == b'\x89PNG\r\n\x1a\n'
                    assert struct.unpack('>II', data[16:24]) == ((64, 32) if image.name.startswith('initial-') else (96, 48))
                result['cases'][key] = {'exitCode': 0, 'runDirectory': str(directory.relative_to(ROOT)), 'result': case, 'screenshots': len(images)}
            save()
            print('H14 CONTROL PASS:' if args.negative_control else 'H14 PASS:', key, flush=True)
        result['status'] = 'PASS'
        result.pop('activeCase', None)
        save()
    except BaseException as error:
        result.update(status='FAIL', error=str(error))
        save()
        raise


if __name__ == '__main__':
    main()
