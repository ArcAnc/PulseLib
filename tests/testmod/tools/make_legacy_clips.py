#!/usr/bin/env python3
"""Reproduce the authored two-format legacy attachment fixture and its asset hashes.
ArcAnc, created 11.11.2026, copyright 2026.
Arc's License of Common Sense. See LICENSE.txt at repository root.
"""
import argparse
import base64
import hashlib
import json
from pathlib import Path
import struct

ROOT = Path(__file__).resolve().parents[3]
RESOURCES = ROOT / 'tests/testmod/demo/common/resources'


def assets():
    gecko = {'format_version': '1.12.0', 'minecraft:geometry': [{'description': {'identifier': 'geometry.legacy_clips', 'texture_width': 16, 'texture_height': 16},
             'bones': [{'name': 'root', 'pivot': [0, 0, 0]}, {'name': 'mesh', 'parent': 'root', 'pivot': [0, 0, 0],
                        'cubes': [{'origin': [-2, -1, -0.5], 'size': [4, 2, 1], 'uv': [0, 0]}]}]}]}
    animations = {'format_version': '1.8.0', 'animations': {
        'move': {'loop': True, 'animation_length': 2, 'bones': {'root': {'position': {'0': [-8, 0, 0], '1': [8, 0, 0], '2': [8, 0, 0]}}}},
        'shape': {'loop': True, 'animation_length': 2, 'bones': {'mesh': {'rotation': {'0': [0, 0, 0], '1': [0, 0, 90], '2': [0, 0, 90]},
                                                                         'scale': {'0': [1, 1, 1], '1': [2, 1, 1], '2': [2, 1, 1]}}}}}}
    binary = bytearray()
    views, accessors = [], []

    def accessor(values, kind, components=3, integer=False):
        while len(binary) % 4:
            binary.append(0)
        offset = len(binary)
        binary.extend(struct.pack('<' + ('H' if integer else 'f') * len(values), *values))
        views.append({'buffer': 0, 'byteOffset': offset, 'byteLength': len(binary) - offset})
        accessors.append({'bufferView': len(views) - 1, 'componentType': 5123 if integer else 5126, 'count': len(values) // components, 'type': kind})
        if kind == 'VEC3':
            accessors[-1].update(min=[min(values[i::3]) for i in range(3)], max=[max(values[i::3]) for i in range(3)])
        return len(accessors) - 1

    x, y, z = 0.125, 0.0625, 0.03125
    faces = [((0, 0, 1), [(-x, -y, z), (x, -y, z), (x, y, z), (-x, y, z)]),
             ((0, 0, -1), [(x, -y, -z), (-x, -y, -z), (-x, y, -z), (x, y, -z)]),
             ((1, 0, 0), [(x, -y, z), (x, -y, -z), (x, y, -z), (x, y, z)]),
             ((-1, 0, 0), [(-x, -y, -z), (-x, -y, z), (-x, y, z), (-x, y, -z)]),
             ((0, 1, 0), [(-x, y, z), (x, y, z), (x, y, -z), (-x, y, -z)]),
             ((0, -1, 0), [(-x, -y, -z), (x, -y, -z), (x, -y, z), (-x, -y, z)])]
    positions = [value for _, vertices in faces for vertex in vertices for value in vertex]
    normals = [value for normal, _ in faces for _ in range(4) for value in normal]
    indices = [face * 4 + index for face in range(6) for index in [0, 1, 2, 2, 3, 0]]
    primitive = {'attributes': {'POSITION': accessor(positions, 'VEC3'), 'NORMAL': accessor(normals, 'VEC3'), 'TEXCOORD_0': accessor([0.5, 0.5] * 24, 'VEC2', 2)},
                 'indices': accessor(indices, 'SCALAR', 1, True), 'material': 0}
    times = accessor([0, 1, 2], 'SCALAR', 1)
    move = accessor([-0.5, 0, 0, 0.5, 0, 0, 0.5, 0, 0], 'VEC3')
    rotation = accessor([0, 0, 0, 1, 0, 0, 2 ** -0.5, 2 ** -0.5, 0, 0, 2 ** -0.5, 2 ** -0.5], 'VEC4', 4)
    scale = accessor([1, 1, 1, 2, 1, 1, 2, 1, 1], 'VEC3')
    gltf = {'asset': {'version': '2.0', 'generator': 'ArcAnc legacy clip fixture, 11.11.2026', 'copyright': "Arc's License of Common Sense"},
            'scene': 0, 'scenes': [{'nodes': [0]}], 'nodes': [{'name': 'root', 'children': [1]}, {'name': 'mesh', 'mesh': 0}],
            'meshes': [{'primitives': [primitive]}], 'materials': [{'name': '0', 'doubleSided': True, 'pbrMetallicRoughness': {'baseColorTexture': {'index': 0}}}],
            'images': [{'name': '0', 'uri': 'data:image/png;base64,' + base64.b64encode((RESOURCES / 'assets/pulselibtest/textures/h14_white.png').read_bytes()).decode()}],
            'textures': [{'name': '0', 'source': 0}],
            'animations': [{'name': 'move', 'samplers': [{'input': times, 'output': move, 'interpolation': 'LINEAR'}], 'channels': [{'sampler': 0, 'target': {'node': 0, 'path': 'translation'}}]},
                           {'name': 'shape', 'samplers': [{'input': times, 'output': rotation, 'interpolation': 'LINEAR'}, {'input': times, 'output': scale, 'interpolation': 'LINEAR'}],
                            'channels': [{'sampler': 0, 'target': {'node': 1, 'path': 'rotation'}}, {'sampler': 1, 'target': {'node': 1, 'path': 'scale'}}]}],
            'buffers': [{'uri': 'data:application/octet-stream;base64,' + base64.b64encode(binary).decode(), 'byteLength': len(binary)}], 'bufferViews': views, 'accessors': accessors}
    return {path: (json.dumps(data, indent=2) + '\n').encode() for path, data in {
        'assets/pulselibtest/geckolib/models/regression/legacy_clips.geo.json': gecko,
        'assets/pulselibtest/geckolib/animations/regression/legacy_clips.animation.json': animations,
        'assets/pulselibtest/glmodels/regression/legacy_clips_gltf.gltf': gltf}.items()}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    files = assets()
    manifest = {'author': 'ArcAnc', 'created': '2026-11-11', 'copyright': 2026, 'license': "Arc's License of Common Sense",
                'files': {path: hashlib.sha256(data).hexdigest() for path, data in files.items()}}
    files['pulselib-legacy-clips-assets.json'] = (json.dumps(manifest, indent=2) + '\n').encode()
    for path, data in files.items():
        file = RESOURCES / path
        if args.check:
            assert file.read_bytes() == data, path
        else:
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_bytes(data)
    print('LEGACY CLIP ASSETS PASS: three reproducible authored resources')


if __name__ == '__main__':
    main()
