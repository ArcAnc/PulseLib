#!/usr/bin/env python3
"""Run the full canonical GPU performance harness in isolated disposable runs on both loaders.
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
    assert case['status'] == 'PASS' and case['fixture'] == 'gpu-performance'
    source_mode = mode
    kind = case['kind']
    assert case['configurations'] == (24 if kind == 'offscreen' else 6)
    assert case['warmup'] == (32 if kind == 'offscreen' else 64) and case['samples'] == 80
    queries = case['queries']
    assert queries['created'] == queries['deleted'] and queries['created'] > 0
    assert queries['textureAllocations'] > 0 and case['calibrationStorageEvents'] >= 4
    assert (directory / '.pulselib-test-run').read_text() == 'PulseLib disposable test run\n'
    world = directory / 'saves/pulselib-bootstrap'
    if kind == 'world':
        assert (world / '.pulselib-disposable').read_text() == 'PulseLib disposable test world\n'
        assert (world / 'level.dat').is_file()
    else:
        assert not world.exists()
    name = 'gpu-performance.csv' if kind == 'offscreen' else 'world-gpu-performance.csv'
    import csv
    import math
    with (directory / 'h14-results' / name).open() as stream:
        rows = list(csv.DictReader(stream))
    assert len(rows) == case['configurations']
    if kind == 'offscreen':
        expected = [(mode, width, height, quads) for width, height in [(1920,1080),(2560,1440),(3840,2160)] for mode in ['Fancy','Fabulous'] for quads in [0,1,16,128]]
    else:
        expected = [(mode,1920,1080,quads) for mode,quads in [('Fancy',0),('Fancy',1),('Fabulous',0),('Fabulous',1),('Fabulous',16),('Fabulous',128)]]
    for row, (mode, width, height, quads) in zip(rows, expected):
        assert row['mode'] == mode and int(row['width']) == width and int(row['height']) == height
        assert int(row['quads']) == quads and int(row['samples']) == 80
        assert int(row['oit_fullscreen_draws']) == (0 if quads == 0 else 4 if mode == 'Fancy' else 8)
        assert int(row['steady_texture_allocations']) == 0
        assert all(math.isfinite(float(v)) for k,v in row.items() if k != 'mode')
        assert float(row['frame_mean_ms']) > 0 and float(row['frame_p95_ms']) > 0
        for field in ['depth_ms','accumulation_ms','compositor_ms']:
            assert float(row[field]) > 0 if quads > 0 else float(row[field]) == 0
        assert float(row['metadata_ms']) > 0 if quads > 0 and mode == 'Fabulous' else float(row['metadata_ms']) == 0
    assert ('classes/java/demo' if source_mode == 'source' else 'pulselib-testmod-demo') in case['origin']
    for marker in ['PULSE DEMO PLUGINS PASS', 'GPU STORAGE CALIBRATION PASS', 'GPU PERFORMANCE SHUTDOWN PASS', 'PULSE BOOTSTRAP VERIFIED', 'BUILD SUCCESSFUL']:
        assert stdout.count(marker) == 1, (directory, marker)
    assert stdout.count('H14 GPU PERFORMANCE PASS' if kind == 'offscreen' else 'H14 WORLD GPU PERFORMANCE PASS') == 1
    assert stdout.count('H14 GPU ROW ' if kind == 'offscreen' else 'H14 WORLD GPU ROW ') == len(rows)
    assert all(marker not in stdout for marker in ['PLAYER MOTION M-P2 PASS','ITEM IDENTITY CLIENT PASS', 'VISIBILITY PASS', 'RENDER REGRESSION PASS', 'NATIVE WORLD PASS', 'H14 PIXEL PASS', 'PULSE TEST SMOKE COMPLETE'])
    return {'exitCode': 0, 'runDirectory': str(directory.relative_to(ROOT)), 'result': case, 'csv': rows}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--gradle-init', type=Path, action='append', default=[])
    parser.add_argument('--cases', nargs='+', choices=['fabric-jar', 'neoforge-source'], default=['fabric-jar', 'neoforge-source'])
    parser.add_argument('--negative-control', action='store_true', help='Skip the actual offscreen composite; require the original fullscreen draw-count guard to fail.')
    parser.add_argument('--timeout', type=int, default=900)
    parser.add_argument('--kind', choices=['offscreen','world'], default='offscreen')
    args = parser.parse_args()
    if args.negative_control and args.kind == 'world':
        parser.error('The skip-composite control is offscreen-only')
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    base = [str(ROOT / 'gradlew'), '--offline', '--no-daemon', '--console=plain']
    for script in args.gradle_init:
        base.extend(['-I', str(script.resolve())])
    result = {'status': 'RUNNING', 'runId': 'gpu-perf-' + uuid.uuid4().hex, 'negativeControl': args.negative_control, 'cases': {}, 'jars': {}}
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
                assert len(names) == len(set(names)) and 'com/arcanc/pulselib/test/PerfRun.class' in names
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
            command = base + [f':{module}:runClient', '-P' + ('pulseDemoJar' if mode == 'jar' else 'pulseDemoContent'), '-PpulseOitPerf', '-PpulseBootstrapRunId=' + result['runId']]
            if args.kind == 'world':
                command += ['-PpulsePerfWorld']
            if args.negative_control:
                command += ['-PpulsePerfSkipComposite']
            log = output / (selection + '.log')
            failed = False
            try:
                stdout = run(command, log, args.timeout)
            except RuntimeError:
                failed = True
                stdout = log.read_text()
            if args.negative_control:
                assert failed and 'PulseLib smoke test: Unexpected OIT fullscreen draw count' in stdout, log
                assert 'finished with non-zero exit value 255' in stdout and 'BUILD FAILED' in stdout
                assert 'H14 GPU PERFORMANCE PASS' not in stdout and 'H14 WORLD GPU PERFORMANCE PASS' not in stdout
                assert not (directory / 'demo-results.json').exists()
                result['cases'][selection] = {'expectedFailure': 'Unexpected OIT fullscreen draw count', 'javaExit': 255, 'gradleExit': 1, 'runDirectory': str(directory.relative_to(ROOT))}
            else:
                assert not failed, log
                result['cases'][selection] = verify_positive(stdout, directory, mode)
            save()
            print('GPU PROFILE CONTROL PASS:' if args.negative_control else 'GPU PROFILE PASS:', selection, flush=True)
        result['status'] = 'PASS'
        result.pop('activeCase', None)
        save()
    except BaseException as error:
        result.update(status='FAIL', error=str(error))
        save()
        raise


if __name__ == '__main__':
    main()
