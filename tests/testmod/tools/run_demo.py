#!/usr/bin/env python3
"""Verify canonical demo content in JAR/source modes on both loaders.
ArcAnc, created 06.11.2026, copyright 2026.
Arc's License of Common Sense; see LICENSE.txt at repository root.
"""
import argparse
import hashlib
import json
import sys
from pathlib import Path
import uuid
import zipfile

sys.dont_write_bytecode = True

from run_bootstrap import ROOT, run


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--eula', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--gradle-init', type=Path, action='append', default=[])
    parser.add_argument('--modes', nargs='+', choices=['jar', 'source'], default=['jar', 'source'])
    parser.add_argument('--animation-regression', action='store_true')
    parser.add_argument('--lifecycle-regression', action='store_true')
    parser.add_argument('--timeout', type=int, default=900)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    assert args.eula.is_file() and 'eula=true' in args.eula.read_text().splitlines()
    base = [str(ROOT / 'gradlew'), '--offline', '--no-daemon', '--console=plain']
    for script in args.gradle_init:
        base.extend(['-I', str(script.resolve())])
    result = {'status': 'RUNNING', 'runId': 'demo-' + uuid.uuid4().hex, 'cases': {}, 'jars': {}}
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
                assert len(names) == len(set(names))
                assert all(info.date_time == (1980, 2, 1, 0, 0, 0) for info in archive.infolist())
                assert all(name.startswith(('com/arcanc/pulselib/test/', 'com/arcanc/pulselib/content/registration/')) for name in names if name.endswith('.class'))
                for resource, digest in manifest.items():
                    namespace, path = resource.split(':', 1)
                    assert hashlib.sha256(archive.read(f'assets/{namespace}/{path}')).hexdigest() == digest
            result['jars'][module] = {'path': str(jar.relative_to(ROOT)), 'sha256': hashlib.sha256(jar.read_bytes()).hexdigest()}
        for mode in args.modes:
            run_id = result['runId'] + '-' + mode
            for side in ['client', 'server']:
                for module in ['fabric', 'neoforge']:
                    key = f'{mode}-{module}-{side}'
                    result['activeCase'] = key
                    save()
                    command = base + [f':{module}:run{side.capitalize()}', '-P' + ('pulseDemoJar' if mode == 'jar' else 'pulseDemoContent'), '-PpulseBootstrapRunId=' + run_id]
                    if args.animation_regression:
                        command += ['-PpulseAnimationRegression']
                    if args.lifecycle_regression:
                        command += ['-PpulseLifecycleRegression']
                    if side == 'server':
                        command += ['-PpulseTestEulaFile=' + str(args.eula.resolve())]
                    stdout = run(command, output / (key + '.log'), args.timeout)
                    for marker in ['PULSE DEMO PLUGINS PASS', f'PULSE DEMO {side.upper()} COMPLETE', f'PULSE DEMO {side.upper()} SHUTDOWN PASS', 'PULSE BOOTSTRAP VERIFIED', 'BUILD SUCCESSFUL']:
                        assert marker in stdout, (key, marker)
                    directory = ROOT / module / 'build/pulselib-tests' / run_id / side
                    case = json.loads((directory / 'demo-results.json').read_text())
                    assert case['status'] == 'PASS' and case['candidates'] == 3 and case['loaded'] == 2
                    assert ('classes/java/demo' if mode == 'source' else 'pulselib-testmod-demo') in case['origin']
                    world = directory / ('saves/pulselib-bootstrap' if side == 'client' else 'pulselib-bootstrap')
                    assert (world / 'level.dat').is_file()
                    if side == 'client':
                        assert case['assetChecks'] == 2 * len(manifest) and case['modelEpochs'] == 2
                        assert stdout.count('PULSE DEMO SCENE PASS') == 2
                        assert stdout.count('PULSE DEMO CLIENT COMPLETE') == 1
                        assert all(count > 20 for count in case['renders'])
                        if args.animation_regression:
                            assert case['animation']['enabled'] and case['animation']['assertions'] > 6000 and case['animation']['maintenance'] >= 2
                            for marker in ['PULSE TEST L1/L4 PASS', 'PULSE TEST M-A3/M-A4/H2/H1 PASS']:
                                assert stdout.count(marker) == 2, (key, marker)
                            for marker in ['PULSE TEST L25 WORLD PASS', 'PULSE TEST L25 REMOVAL PASS', 'PULSE ANIMATION COMPLETE']:
                                assert marker in stdout, (key, marker)
                        if args.lifecycle_regression:
                            assert case['lifecycle']['completed'] and case['lifecycle']['hooks'] > 0
                            for marker in ['PULSE TEST EXPLICIT CLEANUP PASS', 'PULSE TEST NATURAL RESOURCES PASS', 'PULSE TEST NATURAL DISCONNECT PASS']:
                                assert stdout.count(marker) == 1, (key, marker)
                            assert stdout.count('PULSE TEST LIFECYCLE WORKLOAD PASS') == 2
                    else:
                        assert 'PULSE DEMO CONTENT PASS' in stdout
                        trace = (directory / 'classloading.log').read_text()
                        for prefix in ['net.minecraft.client.', 'com.mojang.blaze3d.', 'com.arcanc.pulselib.test.ClientTestPlugin ', 'com.arcanc.pulselib.test.DemoProbe ', 'com.arcanc.pulselib.test.DemoClient ', 'com.arcanc.pulselib.test.AnimationSamplingHarness ', 'com.arcanc.pulselib.test.AnimationLifecycleHarness ', 'com.arcanc.pulselib.test.mixin.AnimationMaintenanceMixin ', 'com.arcanc.pulselib.test.SmokeLifecycleHarness ', 'com.arcanc.pulselib.test.SmokeLifecycleWorkload ', 'com.arcanc.pulselib.test.SmokeResources ', 'com.arcanc.pulselib.test.RegressionResources ', 'com.arcanc.pulselib.test.mixin.SmokeLifecycleMixin ']:
                            assert '] ' + prefix not in trace, (key, prefix)
                    result['cases'][key] = {'exitCode': 0, 'runDirectory': str(directory.relative_to(ROOT)), 'result': case}
                    save()
                    print('DEMO PASS:', key, flush=True)
        result['status'] = 'PASS'
        result.pop('activeCase', None)
        save()
    except BaseException as error:
        result.update(status='FAIL', error=str(error))
        save()
        raise


if __name__ == '__main__':
    main()
