#!/usr/bin/env python3
"""Check canonical renderer/layer declarations with explicit Minecraft 26.1 adaptations.
ArcAnc, created 23.10.2026, copyright 2026. Arc's License of Common Sense; see LICENSE.txt.
"""
import argparse
import copy
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
PREFIX = 'com.arcanc.pulselib.content.renderer.'
CONTRACTS = ['kind', 'modifiers', 'annotations', 'type_parameters', 'extends', 'implements', 'permits', 'record_components']
BOUNDS = {'PRenderer': 'RS extends PRenderState<T>',
          'PBlockRenderer': 'RS extends BlockEntityRenderState & PBlockRenderState<T>',
          'PEntityRenderer': 'RS extends EntityRenderState & PEntityRenderState<T>',
          'PItemRenderer': 'RS extends PItemRenderState<T>',
          'PEntityRenderLayer': 'RS extends EntityRenderState & PEntityRenderState<T>'}


def normalized(value):
    value = copy.deepcopy(value)
    if isinstance(value, dict) and 'signature' in value:
        # JSpecify TYPE_USE annotations on a qualified nested type are legal after the qualifier.
        value['signature'] = value['signature'].replace('PEntityRenderer.@Nullable HeadRotation', 'PEntityRenderer.HeadRotation')
    result = json.dumps(value, sort_keys=True).replace('ResourceLocation', 'Identifier')
    for name in ['PRenderer', 'PEntityRenderer', 'PEntityRenderLayer', 'BlockEntityRenderer', 'EntityRenderer']:
        result = result.replace(name + '<T>', name + '<T, RS>')
    return result.replace('PEntityRenderer.@Nullable HeadRotation', '@Nullable PEntityRenderer.HeadRotation')


def adaptation(type_name, member):
    name = member['name']
    if name in ['preSubmit', 'trueSubmit', 'postSubmit']:
        signature = ('abstract public ' if type_name == 'PRenderer' else 'public ') + 'void ' + name + '(PoseStack, RS, CameraRenderState, SubmitNodeCollector)'
        return signature, 'Minecraft extraction/submission replaces live owner + MultiBufferSource: RS retains owner, partial tick, light/overlay and ItemStack/display context; virtual getRenderType supplies the pipeline; collector and camera are explicit. Hooks remain invoked by native submit.'
    replacements = {
        ('PBlockRenderer', 'render'): ('public void submit(RS, PoseStack, SubmitNodeCollector, CameraRenderState)', 'Vanilla BlockEntityRenderer.render was replaced by extractRenderState/submit. Light and facing are captured; per-bone context overrides supply custom light/overlay/materials.'),
        ('PEntityRenderer', 'render'): ('public void submit(RS, PoseStack, SubmitNodeCollector, CameraRenderState)', 'Vanilla EntityRenderer now extracts RS before submit. Entity position/body/head/light values are snapshots; collector replaces old immediate buffer source.'),
        ('PItemRenderer', 'renderByItem'): ('public void submit(RS, PoseStack, SubmitNodeCollector, int, int, boolean, int)', 'Minecraft removed BEWLR.renderByItem. SpecialModelRenderer.extractArgument captures the stack; submit receives light/overlay/foil/outline and a deferred collector. Display context is retained in ItemStackRenderState.'),
        ('PEntityRenderer', 'applyPositioning'): ('protected HeadRotation applyPositioning(LivingEntityRenderState, PoseStack)', 'Body/head/scale inputs are captured in Minecraft LivingEntityRenderState; submit does not resample live entity interpolation. Canonical HeadRotation record and its traversal contract are preserved.'),
        ('PEntityRenderer', 'setupRotations'): ('protected void setupRotations(LivingEntityRenderState, PoseStack, float, float)', 'Minecraft living rotations use captured age/death/pose/body-yaw/scale. The partial tick and bob were folded into extraction, and the native state hook remains invoked by applyPositioning.')}
    return replacements.get((type_name, name))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    directory = ROOT / 'docs/porting/26.1.2-multiloader'
    for line in (directory / 'checksums.sha256').read_text().splitlines():
        digest, name = line.split()
        assert hashlib.sha256((directory / name).read_bytes()).hexdigest() == digest, name
    baseline = {row['fqcn']: row for row in map(json.loads, (directory / 'api-1.21.1-multiloader.ndjson').read_text().splitlines())}
    with tempfile.TemporaryFile(mode='w+') as output:
        subprocess.run(['java', str(ROOT / 'docs/porting/tools/ExtractApi.java'), str(ROOT)], stdout=output, check=True, cwd=ROOT)
        output.seek(0)
        rows = [json.loads(line) for line in output]
    current = {row['fqcn']: row for row in rows}
    selected = sorted(name for name in baseline if any(name == PREFIX + simple or name.startswith(PREFIX + simple + '.') for simple in BOUNDS))
    results = []
    for name in selected:
        assert name in current, f'Missing renderer type: {name}'
        assert sum(row['fqcn'] == name for row in rows) == 1, f'Duplicate renderer type: {name}'
        before, after = baseline[name], current[name]
        expected = copy.deepcopy(before)
        simple = name.removeprefix(PREFIX)
        header_adaptations = []
        if simple in BOUNDS:
            expected['type_parameters'].append(BOUNDS[simple])
            header_adaptations.append('Additional RS generic: ' + BOUNDS[simple])
        if simple == 'PItemRenderer':
            expected['extends'] = ''
            expected['implements'] = ['SpecialModelRenderer<RS>', 'PRenderer<T, RS>']
            header_adaptations.append('Removed Minecraft BlockEntityWithoutLevelRenderer -> SpecialModelRenderer<RS>; canonical four-argument constructor remains as a real adapter')
        assert normalized({k: expected[k] for k in CONTRACTS}) == normalized({k: after[k] for k in CONTRACTS}), f'Renderer type contract changed: {name}'
        actual = {normalized(member): member for member in after['members']}
        changes, preserved = [], []
        for member in expected['members']:
            canonical_member = copy.deepcopy(member)
            if simple == 'PEntityRenderer' and member['name'] == 'getTextureLocation':
                # The Minecraft superclass no longer declares this method. Keep the nullable per-mesh texture contract.
                member['annotations'].remove('@Override')
                changes.append({'canonical': canonical_member,
                                'adapted': member['signature'].replace('ResourceLocation', 'Identifier'),
                                'reason': 'Removed only source @Override annotation because MC26 superclass has no getTextureLocation; method still returns null for per-mesh texture selection'})
            if normalized(member) in actual:
                preserved.append(member['signature'])
                continue
            change = adaptation(simple, member)
            assert change is not None, f'Unclassified missing/changed renderer member: {name}: {member}'
            signature, reason = change
            matches = [m for m in after['members'] if m['signature'] == signature]
            assert len(matches) == 1, f'Minecraft adapter missing/ambiguous: {name}: {signature}'
            changes.append({'canonical': member, 'adapted': matches[0], 'reason': reason})
        results.append({'type': name, 'canonicalMembers': len(expected['members']), 'preservedMembers': preserved,
                        'minecraftTypeAdaptations': header_adaptations, 'memberAdaptations': changes,
                        'unclassifiedOrDeferredMembers': []})
    report = {'status': 'PASS_WITH_ENUMERATED_MINECRAFT_ADAPTATIONS', 'baselineCommit': '7ce6f024b307bb821ce543344d3896c8e1fcf26b',
              'baselineChecksums': 'unchanged', 'types': results,
              'scope': 'Selected canonical PRenderer/PBlockRenderer/PEntityRenderer/PItemRenderer/PEntityRenderLayer types and their public/protected nested types; source declarations plus runtime probes, not the final compiled whole-library audit of other public GPU facade types',
              'sharedAdaptations': ['Identifier and renderer.rendertype.RenderType', 'JSpecify nullable type-use placement', 'MC26 RS generic propagated to renderer/layer references'],
              'remainingOutsideThisCheckpoint': ['World stages/H13/L18/OIT/Fabulous and full GPU lifecycle 4.2', 'GUI/offscreen local scopes and GUI helper 4.3', 'Player 5, Fabric runtime 6, compiled whole-library compatibility 7']}
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2) + '\n')
    print(f"RENDERER API PASS: {len(results)} types / {sum(r['canonicalMembers'] for r in results)} canonical members classified; no unexplained or deferred renderer members")


if __name__ == '__main__':
    main()
