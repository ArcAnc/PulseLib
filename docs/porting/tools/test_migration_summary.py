#!/usr/bin/env python3
"""Reject corrupt or mis-scoped measurements in the consolidated report.
ArcAnc, created 11.11.2026, copyright 2026.
Arc's License of Common Sense. See LICENSE.txt at repository root.
"""
import copy
import csv
import io
import sys
import unittest

sys.dont_write_bytecode = True

import summarize_migration as report


class MigrationSummaryTest(unittest.TestCase):
    def test_real_csv_coverage_and_empty_fabulous_scope(self):
        summaries = []
        for path in (report.ROOT / 'docs/porting/evidence').glob('stage-7.1b.2b.5-*.csv'):
            summaries.append(report.csv_summary(path.read_text(), 'offscreen' if 'offscreen' in path.name else 'world'))
        self.assertEqual(len(summaries), 4)
        self.assertEqual(sum(row['rows'] for row in summaries), 60)
        self.assertEqual(sum(row['measuredSamples'] for row in summaries), 4800)
        path = report.ROOT / 'docs/porting/evidence/stage-7.1b.2b.5-fabric-offscreen.csv'
        row = next(row for row in csv.DictReader(io.StringIO(path.read_text())) if row['mode'] == 'Fabulous' and row['quads'] == '0')
        self.assertEqual(row['oit_fullscreen_draws'], '0')
        self.assertEqual(row['vanilla_framegraph_passes'], '2')

    def test_corrupt_rows_fail(self):
        path = report.ROOT / 'docs/porting/evidence/stage-7.1b.2b.5-fabric-offscreen.csv'
        original = list(csv.DictReader(io.StringIO(path.read_text())))

        def encoded(rows):
            output = io.StringIO()
            writer = csv.DictWriter(output, fieldnames=list(original[0]))
            writer.writeheader()
            writer.writerows(rows)
            return output.getvalue()

        for field, value, message in [('frame_mean_ms', 'NaN', 'Non-finite'), ('oit_fullscreen_draws', '8', 'draw count'),
                                      ('steady_texture_allocations', '1', 'Steady texture')]:
            rows = copy.deepcopy(original)
            rows[0][field] = value
            with self.subTest(field=field), self.assertRaisesRegex(AssertionError, message):
                report.csv_summary(encoded(rows), 'offscreen')
        with self.assertRaisesRegex(AssertionError, 'Duplicate'):
            report.csv_summary(encoded(original + [original[0]]), 'offscreen')
        with self.assertRaisesRegex(AssertionError, 'coverage'):
            report.csv_summary(encoded(original[:-1]), 'offscreen')

    def test_query_leak_and_missing_calibration_fail(self):
        result = {'status': 'PASS', 'calibrationStorageEvents': 8,
                  'queries': {'created': 10, 'deleted': 10, 'textureAllocations': 2, 'bufferStorageEvents': 8}}
        self.assertEqual(report.query_lifetime(result)['deleted'], 10)
        leaking = copy.deepcopy(result)
        leaking['queries']['deleted'] = 9
        with self.assertRaisesRegex(AssertionError, 'lifetime'):
            report.query_lifetime(leaking)
        result['calibrationStorageEvents'] = 0
        with self.assertRaisesRegex(AssertionError, 'calibration'):
            report.query_lifetime(result)


if __name__ == '__main__':
    unittest.main()
