#!/usr/bin/env python3
"""Verify canonical player core and camera math declarations for stage 5.1.
ArcAnc, created 26.10.2026, copyright 2026. Arc's License of Common Sense; see LICENSE.txt.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
PREFIX = 'com.arcanc.pulselib.content.player.animation.'
HEADERS = ['kind', 'modifiers', 'annotations', 'type_parameters', 'extends', 'implements', 'permits', 'record_components']


def normalize(value):
    # Section 50 isolates client descriptors using four constant-return policies.
    policies = {
        'final public static ItemRenderPolicy RENDER': (
            'new ItemRenderPolicy(){ @Override public boolean hide(LocalPlayer player, InteractionHand hand, ItemStack stack) { return false; } }',
            '(player,hand,stack)->false'),
        'final public static ItemRenderPolicy HIDE': (
            'new ItemRenderPolicy(){ @Override public boolean hide(LocalPlayer player, InteractionHand hand, ItemStack stack) { return true; } }',
            '(player,hand,stack)->true'),
        'final public static ItemVisibilityPolicy VISIBLE': (
            'new ItemVisibilityPolicy(){ @Override public ItemVisibility visibility(LocalPlayer player, InteractionHand hand, float animationTime, ItemStack stack) { return ItemVisibility.VISIBLE; } }',
            '(player,hand,animationTime,stack)->ItemVisibility.VISIBLE'),
        'final public static ItemVisibilityPolicy HIDDEN': (
            'new ItemVisibilityPolicy(){ @Override public ItemVisibility visibility(LocalPlayer player, InteractionHand hand, float animationTime, ItemStack stack) { return ItemVisibility.HIDDEN; } }',
            '(player,hand,animationTime,stack)->ItemVisibility.HIDDEN'),
    }
    policy = policies.get(value.get('signature'))
    if policy and ''.join(value.get('initializer', '').split()) == ''.join(policy[0].split()):
        value = dict(value, initializer=policy[1])
    return json.dumps(value, sort_keys=True).replace('ResourceLocation', 'Identifier').replace('PlayerModel<?>', 'PlayerModel')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    directory = ROOT / 'docs/porting/26.1.2-multiloader'
    for line in (directory / 'checksums.sha256').read_text().splitlines():
        digest, name = line.split()
        assert hashlib.sha256((directory / name).read_bytes()).hexdigest() == digest, name
    baseline = {row['fqcn']: row for row in map(json.loads, (directory / 'api-1.21.1-multiloader.ndjson').read_text().splitlines())}
    def selected(name):
        if not name.startswith(PREFIX):
            return False
        rest = name.removeprefix(PREFIX)
        return not rest.startswith(('attachment.', 'firstPerson.')) or rest == 'firstPerson.PFirstPersonCameraSpace'
    with tempfile.TemporaryFile(mode='w+') as output:
        subprocess.run(['java', str(ROOT / 'docs/porting/tools/ExtractApi.java'), str(ROOT)], stdout=output, check=True, cwd=ROOT)
        output.seek(0)
        rows = [json.loads(line) for line in output]
    results = []
    for name, before in sorted(baseline.items()):
        if not selected(name):
            continue
        matches = [row for row in rows if row['fqcn'] == name]
        assert len(matches) == 1, f'Missing/duplicate player type: {name}'
        after = matches[0]
        assert normalize({k: before[k] for k in HEADERS}) == normalize({k: after[k] for k in HEADERS}), f'Player type contract: {name}'
        actual = {normalize(member) for member in after['members']}
        for member in before['members']:
            assert normalize(member) in actual, f'Canonical player member missing/changed: {name}: {member}'
        results.append({'type': name, 'preservedMembers': len(before['members'])})
    assert len(results) > 20
    # Explicitly retain the old native query names as real adapters, in addition to canonical entrypoints.
    native = next(row for row in rows if row['fqcn'] == PREFIX + 'PPlayerAnimations')
    declarations = {member['signature'] for member in native['members']}
    assert 'public static List<PPlayerAnimationMeshAttachmentPose> automaticMeshAttachmentPoses(Player, float)' in declarations
    assert 'public static PFirstPersonRenderPresentation firstPersonPresentation(Player, float)' in declarations
    report = {'status': 'PASS', 'baselineCommit': '7ce6f024b307bb821ce543344d3896c8e1fcf26b', 'types': results,
              'minecraftAdaptations': ['ResourceLocation -> Identifier', 'PlayerModel<?> -> non-generic net.minecraft.client.model.player.PlayerModel', 'PPlayerPart.resolve uses the native main ModelParts; 26.1 overlay parts are children, not separately sampled parts', 'Four ItemRenderPolicy/ItemVisibilityPolicy constants use exact constant-return anonymous implementations for server descriptor isolation, section 50'],
              'retainedNativeAdapters': ['automaticMeshAttachmentPoses(Player, float)', 'firstPersonPresentation(Player, float)'],
              'limits': 'Source declaration audit for player core/camera math. Native player/attachment rendering signatures and runtime motion remain stage 5.2; compiled full API audit remains stage 7.'}
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2) + '\n')
    print(f"PLAYER CORE API PASS: {len(results)} canonical types / {sum(row['preservedMembers'] for row in results)} declared members")


if __name__ == '__main__':
    main()
