#!/usr/bin/env python3
"""Reconcile accepted migration evidence without rerunning the game or benchmarks.
ArcAnc, created 11.11.2026, copyright 2026.
Arc's License of Common Sense. See LICENSE.txt at repository root.
"""
import argparse
import csv
from decimal import Decimal
import hashlib
import io
import itertools
import json
from pathlib import Path
import subprocess
import sys

sys.dont_write_bytecode = True

from inventory import ROOT, SOURCES, git

PIN = '3e7dab33dd5f984d1368da5dbd3b2102f3b66400'
PERF_COMMIT = '6549600cf31d063c1ef46f9d8bf9d2b27499a67b'
EVIDENCE = 'docs/porting/evidence/'
HISTORY = ['stage-5.2b.4c.1', 'stage-5.2b.4c.2a', 'stage-5.2b.4c.2b.2', 'stage-6.2d.2b']
CONTEXT = ['stage-7.1a', 'stage-7.1b.1', 'stage-6.2c.1', 'stage-6.2d.2a', 'player-occlusion-fix']
RUNTIME = {
    'stage-7.1b.2b.1': ('matrix/cases', 8),
    'stage-7.1b.2b.2a': ('acceptedMatrix/cases', 8),
    'stage-7.1b.2b.2b.1': ('acceptedClients', 2),
    'stage-7.1b.2b.2b.2': ('acceptedMatrix/cases', 2),
    'stage-7.1b.2b.2b.3': ('acceptedMatrix/cases', 2),
    'stage-7.1b.2b.2c': ('acceptedMatrix/cases', 2),
    'stage-7.1b.2b.3': ('positive/cases', 2),
    'stage-7.1b.2b.4': ('positive/cases', 2),
    'stage-7.1b.2b.6': ('acceptedMatrix/cases', 11),
    'stage-7.2b': ('acceptedRuntime/cases', 2),
    'stage-7.2c': ('runtime/cases', 2),
    'stage-7.2d': ('runtime/accepted/cases', 4),
    'stage-7.2e': ('runtime/accepted/cases', 2),
}
SCOPES = HISTORY + CONTEXT + list(RUNTIME) + ['stage-7.1b.2b.5', 'stage-7.2a', 'stage-7.2f']


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def tree(ref):
    return {path: meta.split()[2] for meta, path in (line.split('\t', 1) for line in git('ls-tree', '-r', ref).decode().splitlines())}


def production(path):
    return (path.split('/')[0] in {'common', 'fabric', 'neoforge'} and ('/src/main/' in path or path.endswith('.gradle'))) or path.startswith('buildSrc/') or path in {'build.gradle', 'settings.gradle', 'gradle.properties'}


def csv_summary(text, kind):
    rows = list(csv.DictReader(io.StringIO(text)))
    require(kind in {'offscreen', 'world'}, 'Unknown measurement scope')
    resolutions = [(1920, 1080), (2560, 1440), (3840, 2160)] if kind == 'offscreen' else [(1920, 1080)]
    expected = set(itertools.product(['Fancy', 'Fabulous'], resolutions, [0, 1, 16, 128])) if kind == 'offscreen' else {
        ('Fancy', (1920, 1080), 0), ('Fancy', (1920, 1080), 1),
        ('Fabulous', (1920, 1080), 0), ('Fabulous', (1920, 1080), 1),
        ('Fabulous', (1920, 1080), 16), ('Fabulous', (1920, 1080), 128)}
    seen = set()
    for row in rows:
        require(all(value is not None for value in row.values()), 'Malformed CSV row')
        key = (row['mode'], (int(row['width']), int(row['height'])), int(row['quads']))
        require(key not in seen, 'Duplicate configuration')
        seen.add(key)
        values = {name: Decimal(value) for name, value in row.items() if name != 'mode'}
        require(all(value.is_finite() for value in values.values()), 'Non-finite measurement')
        require(all(value >= 0 for name, value in values.items() if name != 'driver_vram_delta_mib'), 'Negative measurement')
        require(values['samples'] == 80 and values['frame_mean_ms'] > 0, 'Wrong sample count or empty GPU interval')
        nonempty, fabulous = key[2] != 0, key[0] == 'Fabulous'
        draws = (8 if fabulous else 4) if nonempty else 0
        require(values['oit_fullscreen_draws'] == draws, 'Wrong OIT draw count')
        require(values['steady_texture_allocations'] == 0, 'Steady texture allocation')
        for span in ['depth_ms', 'accumulation_ms', 'compositor_ms']:
            require(values[span] > 0 if nonempty else values[span] == 0, 'Wrong GPU span: ' + span)
        require(values['metadata_ms'] > 0 if nonempty and fabulous else values['metadata_ms'] == 0, 'Wrong metadata span')
        if kind == 'offscreen':
            require(values['vanilla_framegraph_passes'] == (2 if fabulous else 0), 'Wrong native vanilla pass count')
    require(seen == expected, 'Incomplete or unexpected configuration coverage')
    maximum = max(rows, key=lambda row: Decimal(row['frame_mean_ms']))
    return {'rows': len(rows), 'measuredSamples': sum(int(row['samples']) for row in rows),
            'maxConfigurationMean': {key: maximum[key] for key in ['mode', 'width', 'height', 'quads', 'frame_mean_ms', 'frame_p95_ms']},
            'peakNativeOitMiB': str(max(Decimal(row['oit_texture_mib']) for row in rows)),
            'peakMetadataMiBIncludedInOitTotal': str(max(Decimal(row['metadata_mib']) for row in rows)),
            'steadyTextureAllocations': sorted({row['steady_texture_allocations'] for row in rows}),
            'observedBufferStorageEventsPerSample': sorted({row['buffer_data_calls'] for row in rows}),
            'configurations': [{key: row[key] for key in ['mode', 'width', 'height', 'quads', 'samples']} for row in rows]}


def query_lifetime(result):
    query = result['queries']
    require(result['status'] == 'PASS' and query['created'] == query['deleted'] > 0, 'Query lifetime mismatch')
    require(result['calibrationStorageEvents'] == 8 and query['textureAllocations'] > 0, 'Missing allocation calibration/warmup')
    return {'created': query['created'], 'deleted': query['deleted'], 'calibrationStorageEvents': result['calibrationStorageEvents'],
            'lifetimeTextureAllocations': query['textureAllocations'], 'lifetimeBufferStorageEvents': query['bufferStorageEvents']}


def build():
    pinned = tree(PIN)
    head = tree('HEAD')
    require({p: h for p, h in pinned.items() if production(p)} == {p: h for p, h in head.items() if production(p)}, 'Production/build inputs changed since API acceptance')
    for ref, expected in SOURCES.items():
        require(git('rev-parse', ref).decode().strip() == expected, 'Frozen source ref moved: ' + ref)
    subprocess.run(['git', '-C', str(ROOT), 'merge-base', '--is-ancestor', SOURCES['26.1'], PIN], check=True)
    inputs = {}

    def file(path):
        data = (ROOT / path).read_bytes()
        require(path in pinned and hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest() == pinned[path], 'Evidence differs from pinned Git: ' + path)
        inputs[path] = {'sha256': sha(data), 'gitBlob': pinned[path]}
        return data

    for path in sorted(pinned):
        if path.startswith(EVIDENCE) and any(path[len(EVIDENCE):].startswith(scope + suffix) for scope in SCOPES for suffix in ['.', '-']):
            file(path)

    def load(name):
        return json.loads(file(EVIDENCE + name + '.json'))

    runtime = {}
    for name, (pointer, count) in RUNTIME.items():
        document = load(name)
        require(str(document['status']).startswith(('PASS', 'COMPLETE')), 'Unaccepted checkpoint: ' + name)
        cases = document
        for part in pointer.split('/'):
            cases = cases[part]
        require(len(cases) == count, 'Wrong accepted matrix size: ' + name)
        for case in cases.values():
            require(case.get('exitCode', case.get('gradleExit')) == 0 and case['result']['status'] == 'PASS', 'Unaccepted runtime case: ' + name)
        runtime[name] = {'source': EVIDENCE + name + '.json', 'casePointer': pointer,
                         'acceptedCases': list(cases), 'historicalEvidenceOnly': True}

    perf = load('stage-7.1b.2b.5')
    datasets = {}
    for key, record in perf['rawCsvs'].items():
        data = file(record['path'])
        require(sha(data) == record['sha256'], 'Performance CSV hash differs from accepted run')
        module, kind = key.split('/')
        matrix = perf['accepted'][kind]
        case = matrix['cases']['fabric-jar' if module == 'fabric' else 'neoforge-source']
        require(matrix['status'] == 'PASS' and case['exitCode'] == 0, 'Unaccepted performance case')
        summary = csv_summary(data.decode(), kind)
        require(summary['rows'] == record['rows'] == case['result']['configurations'], 'CSV/runtime count mismatch')
        require(case['result']['samples'] == 80 and case['result']['warmup'] == (32 if kind == 'offscreen' else 64), 'Wrong sampling contract')
        datasets[key] = dict(summary, source=record['path'], sha256=record['sha256'], runId=matrix['runId'],
                             fixtureJarSha256=matrix['jars'][module]['sha256'], queries=query_lifetime(case['result']))
    require(sum(d['rows'] for d in datasets.values()) == 60 and sum(d['measuredSamples'] for d in datasets.values()) == 4800, 'Wrong main matrix size')
    require(sum(d['queries']['deleted'] for d in datasets.values()) == 167156, 'Wrong lifetime query total')
    negative = perf['negative']
    require(negative['status'] == 'PASS' and len(negative['cases']) == 2, 'Missing performance controls')
    for case in negative['cases'].values():
        require(case['javaExit'] == 255 and case['gradleExit'] == 1 and case['expectedFailure'] == 'Unexpected OIT fullscreen draw count', 'Invalid performance negative control')

    final = load('stage-7.2f')
    api = load('stage-7.2f-api')
    for path, expected in final['evidenceSha256'].items():
        require(sha(file(EVIDENCE + path)) == expected, 'Final API evidence hash mismatch')
    objects = list(api['sourceProof']['current']['sourceSha256'])
    raw = subprocess.check_output(['git', '-C', str(ROOT), 'cat-file', '--batch'], input=''.join(pinned[path] + '\n' for path in objects).encode())
    offset = 0
    for path in objects:
        end = raw.index(b'\n', offset)
        size = int(raw[offset:end].split()[2])
        data = raw[end + 1:end + 1 + size]
        require(sha(data) == api['sourceProof']['current']['sourceSha256'][path], 'Current production Java differs from compiler proof: ' + path)
        offset = end + 2 + size
    require(len(objects) == 414 and final['reviewedExceptionContracts'] == 130 and final['sourceErasureChecks'] == 17614, 'Incomplete API acceptance')
    jars = {}
    for module, record in final['production']['production'].items():
        data = (ROOT / record['jar']).read_bytes()
        require(sha(data) == record['finalJarSha256'], 'Current production JAR differs from accepted API build: ' + module)
        jars[module] = {'path': record['jar'], 'sha256': sha(data), 'classCopies': record['classCopies']}
    approved = json.loads(file('docs/porting/approved-api-changes.json'))
    require(len(approved['removedTypes']) == 1 and approved['removedTypes'][0]['type'].endswith('.PItemAnimatable'), 'Approved removal set changed')
    gpu = load('stage-7.2b-gpu-api')
    body_proof = gpu['nativeBodyProof']
    require(sum(len(row['preservedBodies']) for row in body_proof.values()) == 148, 'Incomplete GPU body proof')
    gpu_changes = {path: row['enumeratedChanges'] for path, row in body_proof.items() if row['enumeratedChanges']}
    require(sum(map(len, gpu_changes.values())) == 6, 'Wrong GPU adaptation inventory')
    changed_since_perf = git('diff', '--name-only', PERF_COMMIT, PIN, '--', 'common/src/main/java').decode().splitlines()
    return {'checkpoint': '7.2g', 'status': 'REPORT_COMPLETE_WITH_EXPLICIT_LIMITATIONS', 'parent': PIN,
            'author': 'ArcAnc', 'date': '2026-11-11', 'copyright': 2026, 'license': "Arc's License of Common Sense",
            'commitMessage': 'docs: consolidate GPU runtime and performance evidence (stage 7.2g)',
            'sourceRefs': SOURCES, 'inputEvidence': inputs, 'runtimeEvidence': runtime,
            'apiAcceptance': {'source': EVIDENCE + 'stage-7.2f-api.json', 'canonicalNamedTypes': 458, 'currentNamedTypes': 564,
                              'canonicalAccessibleTypes': 373, 'currentAccessibleTypes': 463, 'compilerConfigurations': 6,
                              'erasureChecks': 17614, 'reviewedExactContracts': 130, 'moduleExceptionCopies': 366,
                              'currentProductionJavaUnitsVerified': len(objects), 'unchangedMinecraft1211AbiClaimed': False},
            'production': {'jars': jars, 'classCopies': sum(j['classCopies'] for j in jars.values()),
                           'unchangedFromStage72f': True, 'physicalWipCompiled': False,
                           'previousJUnit': final['checks']['freshJUnit'], 'newBuilds': 0, 'newGameRuns': 0},
            'graphicsFoundation': {'source': SOURCES['26.1'], 'singleNativeBackend': True,
                                   'sourceProof': EVIDENCE + 'stage-7.2b-gpu-api.json', 'preservedBodiesInStage72b': 148,
                                   'enumeratedStage72bChanges': gpu_changes, 'latestGpuClassCopiesPreservedInStage72f': 144,
                                   'scope': 'Previous body, native lifetime/pixel, material, hook and compiler proofs; no new GPU execution.'},
            'performance': {'verdict': 'HISTORICAL_MEASUREMENTS_ONLY_NO_CURRENT_BUILD_BENCHMARK_ACCEPTANCE',
                            'evidenceCommit': PERF_COMMIT, 'datasets': datasets, 'mainMeasuredSamples': 4800,
                            'lifetimeQueryNamesCreatedAndDeleted': 167156, 'negativeControls': 2,
                            'measuredProductionJars': perf['buildAndProduction']['production'],
                            'productionFilesChangedSinceMeasurements': changed_since_perf,
                            'metadataAlreadyIncludedInOitMiB': True, 'rawPerFrameSamplesAvailableInCsv': False,
                            'emptyOffscreenScope': 'Fabulous runs the native vanilla framegraph even with zero Pulse quads; Fancy has no vanilla chain. Their empty frame intervals differ in workload.',
                            'percentilePolicy': 'Keep each 80-sample configuration p95; do not average p95s or reconstruct a pooled p95 from summary rows.',
                            'device': 'Recorded offscreen: NVIDIA GeForce GTX 1660 Ti / NVIDIA 615.78.08 / OpenGL 3.3',
                            'routeAliases': {'Fancy': 'Improved Transparency=false', 'Fabulous': 'Improved Transparency=true'},
                            'allocationScope': 'Observed steady-state OIT texture reuse and storage-event counters; no zero-allocation CPU/JVM or whole-process memory claim.',
                            'unproven': ['Improvement over frozen 26.1', 'Cross-loader speed ranking', 'Current release performance after stages 7.2a-f', 'General VRAM budget or leak-free process', 'Pooled p95 from unavailable individual samples']},
            'remaining': [{'id': '5.2b.4d', 'status': 'OPEN_EXISTING_COVERAGE_DEBT',
                           'scope': ['Arbitrary legacy mesh-controller clips', 'Attachment mesh formats beyond the previously static Gecko attachment', 'First-person held-item/map pixel combinations'],
                           'evidence': [EVIDENCE + name + '.json' for name in HISTORY],
                           'notClosedBy': 'PlayerMotion matrix/callback assertions or deformation parity do not supply the missing pixel cross-product.'}],
            'stageDisposition': {'7.2': 'COMPLETE_AS_API_AND_EVIDENCE_REPORT_WITH_LIMITATIONS', '5': 'PARTIAL_EXISTING_5.2b.4_COVERAGE_DEBT',
                                 'migration': 'NOT_DECLARED_COMPLETE', 'nextCheckpoint': '5.2b.4d residual legacy mesh/held-item pixel acceptance; preserve closed suites'},
            'limits': ['All runtime cases are historical accepted runs at their recorded inputs, not new runs on the current release.',
                       'The final full physical user WIP is outside the committed HEAD acceptance.',
                       'Two actual native transparency routes; historical Fast/Fancy labels do not establish three distinct modes.',
                       'No blanket coverage of arbitrary GPU strategies, driver/platform combinations, survival death UI or network dimension transfer is claimed.',
                       'No push, publication or release approval is included.']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=ROOT / 'docs/porting/evidence/stage-7.2g.json')
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    data = json.dumps(build(), indent=2, ensure_ascii=False) + '\n'
    if args.check:
        require(args.output.read_text() == data, 'Consolidated report differs from reproducible inputs')
    else:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(data)
    print('MIGRATION REPORT PASS: evidence hashes, accepted cases, 60 CSV rows / 4800 samples, 167156 queries, current API/JAR identity; residual 5.2b.4 remains open')


if __name__ == '__main__':
    main()
