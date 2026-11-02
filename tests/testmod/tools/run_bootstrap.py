#!/usr/bin/env python3
"""Build and run the packaged 7.1a bootstrap on both loaders in fresh marked directories.
ArcAnc, created 03.11.2026, copyright 2026.
Arc's License of Common Sense; see LICENSE.txt at repository root.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import signal
import subprocess
import time
import uuid
import zipfile

ROOT = Path(__file__).resolve().parents[3]


def run(command, log, timeout):
    with log.open('w') as output:
        process = subprocess.Popen(command, cwd=ROOT, stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
        try:
            code = process.wait(timeout=timeout)
        except BaseException:
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
                process.wait()
            raise
    if code != 0:
        raise RuntimeError(f'Gradle exit {code}; see {log}')
    return log.read_text()


def inspect_jar(module):
    properties = dict(line.split('=', 1) for line in (ROOT / 'gradle.properties').read_text().splitlines() if '=' in line)
    jar = ROOT / module / 'build/libs' / f"pulselib-testmod-bootstrap-{properties['minecraft_version']}-{properties['mod_version']}-{module}.jar"
    manifest = json.loads((ROOT / 'tests/testmod/bootstrap/common/resources/pulselib-bootstrap-assets.json').read_text())
    with zipfile.ZipFile(jar) as archive:
        entries = archive.namelist()
        assert len(entries) == len(set(entries)), 'Duplicate testmod JAR entries'
        classes = [name for name in entries if name.endswith('.class')]
        assert classes and all(name.startswith('com/arcanc/pulselib/test/bootstrap/') for name in classes), 'Library/demo classes leaked into bootstrap JAR'
        assert ('fabric.mod.json' if module == 'fabric' else 'META-INF/neoforge.mods.toml') in entries
        assert ('META-INF/neoforge.mods.toml' if module == 'fabric' else 'fabric.mod.json') not in entries
        for resource, digest in manifest.items():
            namespace, path = resource.split(':', 1)
            assert hashlib.sha256(archive.read(f'assets/{namespace}/{path}')).hexdigest() == digest
    return {'path': str(jar.relative_to(ROOT)), 'sha256': hashlib.sha256(jar.read_bytes()).hexdigest(), 'classes': len(classes), 'canonicalAssets': len(manifest)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--eula', type=Path, required=True, help='Your previously accepted eula.txt; this script never accepts EULA')
    parser.add_argument('--output', type=Path)
    parser.add_argument('--gradle-init', type=Path, action='append', default=[])
    parser.add_argument('--timeout', type=int, default=900)
    args = parser.parse_args()
    run_id = 'bootstrap-' + uuid.uuid4().hex
    output = (args.output or ROOT / 'build/reports/testmod-bootstrap' / run_id).resolve()
    output.mkdir(parents=True, exist_ok=False)
    eula = args.eula.resolve()
    if not eula.is_file() or 'eula=true' not in eula.read_text().splitlines():
        raise ValueError('A previously accepted eula.txt is required')
    base = [str(ROOT / 'gradlew'), '--offline', '--no-daemon', '--console=plain']
    for script in args.gradle_init:
        base.extend(['-I', str(script.resolve())])
    progress = {'status': 'RUNNING', 'runId': run_id, 'cases': {}}
    progress_file = output / 'results.json'
    def save():
        progress_file.write_text(json.dumps(progress, indent=2) + '\n')
    save()
    try:
        run(base + [':fabric:testmodJar', ':neoforge:testmodJar'], output / 'build.log', args.timeout)
        progress['jars'] = {module: inspect_jar(module) for module in ['fabric', 'neoforge']}
        save()
        for side in ['server', 'client']:
            for module in ['fabric', 'neoforge']:
                key = module + '-' + side
                progress['activeCase'] = key
                save()
                directory = ROOT / module / 'build/pulselib-tests' / run_id / side
                command = base + [f':{module}:run{side.capitalize()}', '-PpulseTestJarBootstrap', f'-PpulseBootstrapRunId={run_id}', f'-PpulseTestEulaFile={eula}']
                started = time.monotonic()
                text = run(command, output / (key + '.log'), args.timeout)
                for marker in ['PULSE BOOTSTRAP PLUGINS PASS', f'PULSE BOOTSTRAP {side.upper()} COMPLETE', f'PULSE BOOTSTRAP {side.upper()} SHUTDOWN PASS', 'PULSE BOOTSTRAP VERIFIED', 'BUILD SUCCESSFUL']:
                    assert marker in text, f'Missing {marker}: {key}'
                result = json.loads((directory / 'bootstrap-results.json').read_text())
                assert result['status'] == 'PASS' and result['side'] == side and result['candidates'] == 3 and result['loaded'] == 2
                assert '.jar' in result['origin'] and 'pulselib-testmod-bootstrap' in result['origin']
                world = directory / ('saves/pulselib-bootstrap' if side == 'client' else 'pulselib-bootstrap')
                assert (world / 'level.dat').is_file(), 'Native world was not saved'
                assert (directory / '.pulselib-test-run').read_text() == 'PulseLib disposable test run\n'
                assert (world / '.pulselib-disposable').read_text() == 'PulseLib disposable test world\n'
                if side == 'client':
                    assert result['assetChecks'] == 12 and result['modelEpochs'] == 2 and result['gltfBones'] > 0 and result['geckoBones'] > 0 and result['geckoAnimations'] > 0
                else:
                    loaded = (directory / 'classloading.log').read_text()
                    assert '] net.minecraft.client.' not in loaded and '] com.mojang.blaze3d.' not in loaded
                    assert '] com.arcanc.pulselib.test.bootstrap.ClientTestPlugin ' not in loaded
                    assert '] com.arcanc.pulselib.test.bootstrap.BootstrapClient ' not in loaded
                progress['cases'][key] = {'exitCode': 0, 'seconds': round(time.monotonic() - started, 2), 'runDirectory': str(directory.relative_to(ROOT)), 'result': result}
                save()
                print('PACKAGED BOOTSTRAP PASS:', key, flush=True)
        progress['status'] = 'PASS'
        progress.pop('activeCase', None)
        save()
        print('PACKAGED BOOTSTRAP COMPLETE:', progress_file, flush=True)
    except BaseException as error:
        progress['status'] = 'FAIL'
        progress['error'] = str(error)
        save()
        raise


if __name__ == '__main__':
    main()
