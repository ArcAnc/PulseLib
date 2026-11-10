#!/usr/bin/env python3
"""Verify canonical source/JAR launch dispatch and composed client lifecycles.
ArcAnc, created 11.11.2026, copyright 2026.
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

CASES = {
    'fabric-jar-menu': ('fabric', 'jar', 'menu', False),
    'neoforge-source-menu-gecko': ('neoforge', 'source', 'menu', True),
    'fabric-jar-world': ('fabric', 'jar', 'world', False),
    'fabric-source-world-gecko': ('fabric', 'source', 'world', True),
    'neoforge-source-world': ('neoforge', 'source', 'world', False),
    'neoforge-jar-world-gecko': ('neoforge', 'jar', 'world', True),
    'fabric-source-oit-world': ('fabric', 'source', 'oit-world', False),
    'neoforge-jar-baseline': ('neoforge', 'jar', 'baseline', False),
    'fabric-jar-baseline-world': ('fabric', 'jar', 'baseline-world', False),
    'fabric-source-server': ('fabric', 'source', 'server', False),
    'neoforge-jar-server-gecko': ('neoforge', 'jar', 'server', True),
}


def verify(stdout, directory, selection):
    module, mode, kind, gecko = CASES[selection]
    result = json.loads((directory / 'demo-results.json').read_text())
    assert result['status'] == 'PASS'
    assert (directory / '.pulselib-test-run').read_text() == 'PulseLib disposable test run\n'
    assert ('classes/java/demo' if mode == 'source' else 'pulselib-testmod-demo') in result['origin']
    assert stdout.count('PULSE BOOTSTRAP VERIFIED') == 1 and 'BUILD SUCCESSFUL' in stdout
    if kind in ['world', 'oit-world', 'baseline-world', 'server']:
        world = directory / ('pulselib-bootstrap' if kind == 'server' else 'saves/pulselib-bootstrap')
        assert (world / '.pulselib-disposable').read_text() == 'PulseLib disposable test world\n'
        assert (world / 'level.dat').is_file()
    else:
        assert not (directory / 'saves/pulselib-bootstrap').exists()
    if kind == 'world':
        assert result['assetChecks'] == 100 and result['modelEpochs'] == 2
        assert result['animation']['enabled'] and result['lifecycle']['completed']
        combined = result['combined']
        assert (
            combined['renderCases'], combined['h13Cases'],
            combined['h14Cases'], combined['modelRegistrations']
        ) == (41, 12, 15, 3)
        assert combined['format'] == ('pulselib:gecko' if gecko else 'pulselib:gltf')
        for marker in [
            'PULSE TEST L1/L4 PASS', 'PULSE TEST L25 WORLD PASS',
            'PULSE ANIMATION COMPLETE', 'H13 WORLD PIXEL PASS',
            'NATIVE WORLD PASS', 'PULSE TEST EXPLICIT CLEANUP PASS',
            'PULSE TEST NATURAL DISCONNECT PASS', 'PULSE TEST CLIENT PASS',
            'PULSE DEMO CLIENT SHUTDOWN PASS',
        ]:
            assert marker in stdout, (selection, marker)
    elif kind == 'menu':
        assert result['fixture'] == 'canonical-smoke-menu' and result['renderCases'] == 36 and (result['modelEpochs'] == 1)
        assert result['format'] == ('pulselib:gecko' if gecko else 'pulselib:gltf')
        assert result['smoke']['completed'] and len(result['smoke']['draws']) == 2
        assert 'PULSE TEST EXPLICIT CLEANUP PASS' in stdout and 'CANONICAL SHUTDOWN PASS' in stdout
    elif kind == 'server':
        assert result['side'] == 'server' and result['candidates'] == 3 and (result['loaded'] == 2)
        assert 'PULSE DEMO SERVER SHUTDOWN PASS' in stdout
        trace = (directory / 'classloading.log').read_text()
        for prefix in [
            'net.minecraft.client.', 'com.mojang.blaze3d.',
            'com.arcanc.pulselib.test.CanonicalRun ',
            'com.arcanc.pulselib.test.ClientTestPlugin ',
            'com.arcanc.pulselib.test.DemoProbe ',
        ]:
            assert '] ' + prefix not in trace, (selection, prefix)
    else:
        assert result['fixture'] == 'canonical-oit'
        assert result['world'] == (kind != 'baseline') and result['baseline'] == ('baseline' in kind)
        assert result['offscreenCases'] == (38 if kind == 'oit-world' else 0)
        assert result['h13Cases'] == (0 if kind == 'baseline' else 12)
        assert result['h14Cases'] == (0 if kind == 'baseline' else 15)
        assert result['modelEpochs'] == (3 if kind == 'oit-world' else 1 if kind == 'baseline' else 2)
        assert 'CANONICAL OIT PASS' in stdout and 'CANONICAL SHUTDOWN PASS' in stdout
    return {'exitCode': 0, 'runDirectory': str(directory.relative_to(ROOT)), 'result': result}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--gradle-init', type=Path, action='append', default=[])
    parser.add_argument('--cases', nargs='+', choices=CASES, default=list(CASES))
    parser.add_argument('--eula-file', type=Path)
    parser.add_argument('--adopt', type=Path, help='Read prior successful cases from this invocation, validating their logs/results against the same fixture JARs.')
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    base = [str(ROOT / 'gradlew'), '--offline', '--no-daemon', '--console=plain']
    for script in args.gradle_init:
        base += ['-I', str(script.resolve())]
    report = {'status': 'RUNNING', 'runId': 'canonical-' + uuid.uuid4().hex, 'cases': {}, 'jars': {}}

    def save():
        (output / 'results.json').write_text(json.dumps(report, indent=2) + '\n')
    save()
    try:
        run(base + [':fabric:demoJar', ':neoforge:demoJar'], output / 'package.log', 180)
        manifest = json.loads((ROOT / 'tests/testmod/demo/common/resources/pulselib-demo-assets.json').read_text())
        for module in ['fabric', 'neoforge']:
            jar = ROOT / module / 'build/libs' / f'pulselib-testmod-demo-26.1.2-1.1.5.3-{module}.jar'
            with zipfile.ZipFile(jar) as archive:
                assert len(archive.namelist()) == len(set(archive.namelist()))
                assert 'com/arcanc/pulselib/test/CanonicalRun.class' in archive.namelist()
                for resource, digest in manifest.items():
                    namespace, path = resource.split(':', 1)
                    assert hashlib.sha256(archive.read(f'assets/{namespace}/{path}')).hexdigest() == digest
            report['jars'][module] = {'path': str(jar.relative_to(ROOT)), 'sha256': hashlib.sha256(jar.read_bytes()).hexdigest()}
        adopted = json.loads(args.adopt.read_text()) if args.adopt else {}
        for selection in args.cases:
            module, mode, kind, gecko = CASES[selection]
            report['activeCase'] = selection
            save()
            if selection in adopted:
                case = adopted[selection]
                assert case['jarSha256'] == report['jars'][module]['sha256']
                stdout = Path(case['log']).read_text()
                directory = ROOT / case['runDirectory']
                (output / (selection + '.log')).write_text(stdout)
            else:
                side = 'server' if kind == 'server' else 'client'
                run_id = report['runId'] + '-' + selection
                directory = ROOT / module / 'build/pulselib-tests' / run_id / side
                command = base + [f':{module}:run{side.capitalize()}', '-P' + ('pulseTestMod' if mode == 'source' else 'pulseTestJar'), '-PpulseBootstrapRunId=' + run_id]
                if kind in ['menu', 'world', 'server']:
                    command += ['-PpulseSmokeAuto']
                else:
                    command += ['-PpulseOitTest']
                if kind in ['world', 'oit-world', 'baseline-world']:
                    command += ['-PpulseSmokeWorld']
                if 'baseline' in kind:
                    command += ['-PpulseOitBaseline']
                if gecko:
                    command += ['-PpulseGeckoDemo']
                if side == 'server':
                    assert args.eula_file and args.eula_file.is_file(), 'Supply the existing accepted EULA file for dedicated cases'
                    command += ['-PpulseTestEulaFile=' + str(args.eula_file.resolve())]
                stdout = run(command, output / (selection + '.log'), 360)
            report['cases'][selection] = verify(stdout, directory, selection)
            save()
            print('CANONICAL PASS', selection, flush=True)
        report['status'] = 'PASS'
        report.pop('activeCase', None)
        save()
    except BaseException as error:
        report.update(status='FAIL', error=str(error))
        save()
        raise

if __name__ == '__main__':
    main()
