#!/usr/bin/env python3
"""Prove the canonical H14 offscreen slice without closing native-world bundles.
ArcAnc, created 10.11.2026, copyright 2026.
Arc's License of Common Sense; see LICENSE.txt at repository root.
"""
import argparse
import base64
from collections import Counter
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile

sys.dont_write_bytecode = True
from inventory_harnesses import ROOT, PIN, git, scan

REPORT = ROOT / 'docs/porting/evidence/stage-7.1b.2b.2b.1-inventory.json'
SOURCE = 'tests/testmod/src/main/java/com/arcanc/pulselib/test/H14Harness.java'
BODY_TOOL = r'''
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import javax.tools.ToolProvider;

public class H14Bodies
{
	private static final Set<String> NAMES = Set.of("suite", "expected", "color", "quad", "assertRgb");

	public static void main(String[] args) throws Exception
	{
		var compiler = ToolProvider.getSystemJavaCompiler();
		try (var files = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8))
		{
			var task = (JavacTask)compiler.getTask(null, files, null, List.of("-proc:none"), null,
					files.getJavaFileObjects(args[0]));
			for (var unit : task.parse())
				new TreeScanner<Void, Void>()
				{
					@Override
					public Void visitMethod(MethodTree method, Void unused)
					{
						if (NAMES.contains(method.getName().toString()))
						{
							var out = new StringBuilder();
							new TreeScanner<Void, Void>()
							{
								@Override
								public Void scan(Tree tree, Void unused)
								{
									if (tree == null)
										return null;
									if (tree instanceof BlockTree block && block.getStatements().size() == 1)
										return scan(block.getStatements().getFirst(), unused);
									out.append('(').append(tree.getKind()).append(':');
									if (tree instanceof IdentifierTree name)
										out.append(name.getName());
									if (tree instanceof MemberSelectTree member)
										out.append(member.getIdentifier());
									if (tree instanceof LiteralTree literal)
										out.append(literal);
									if (tree instanceof VariableTree variable)
										out.append(variable.getName());
									super.scan(tree, unused);
									out.append(')');
									return null;
								}
							}.scan(method.getBody(), null);
							System.out.println(method.getName() + "\t" + Base64.getEncoder().encodeToString(out.toString().getBytes(StandardCharsets.UTF_8)));
						}
						return super.visitMethod(method, unused);
					}
				}.scan(unit, null);
		}
	}
}
'''


def build():
    with tempfile.TemporaryDirectory(prefix='pulselib-h14-') as temporary:
        base = Path(temporary)
        original = git('show', PIN + ':' + SOURCE)
        source = base / 'H14Harness.java'
        source.write_bytes(original)
        before = scan(base)
        target = ROOT / 'tests/testmod/demo/common/java/com/arcanc/pulselib/test/H14OffscreenHarness.java'
        after = scan(target.parent)
        guards = lambda records, name: [r for r in records if Path(r['path']).name == name and r['kind'] == 'guard' and r['callee'] == 'check']
        scoped = {'scene', 'verify', 'exceptionState', 'assertRgb'}
        expected = [r for r in guards(before, 'H14Harness.java') if r['method'].split('(')[0] in scoped or r['arguments'][-1] == '"Actual window framebuffer must resize"']
        current = [r for r in guards(after, 'H14OffscreenHarness.java') if r['method'].split('(')[0] != 'baseline']
        key = lambda r: json.dumps(r['arguments'], ensure_ascii=False).replace('RenderSystem.getShader()', 'GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)')
        wanted, actual = Counter(map(key, expected)), Counter(map(key, current))
        assert not wanted - actual, ('Canonical H14 guard changed/lost', wanted - actual)
        tool = base / 'H14Bodies.java'
        tool.write_text(BODY_TOOL)

        def bodies(path):
            rows = subprocess.check_output(['java', str(tool), str(path)], text=True).splitlines()
            return {name: base64.b64decode(body) for name, body in (row.split('\t') for row in rows)}

        old, new = bodies(source), bodies(target)
        assert old.keys() == new.keys() and len(old) == 5
        assert old == new, ('Canonical suite/oracle body changed', [name for name in old if old[name] != new[name]])
    return {
        'pin': PIN, 'checkpoint': '7.1b.2b.2b.1', 'source': SOURCE, 'sourceSha256': hashlib.sha256(original).hexdigest(),
        'scope': 'All 38 canonical H14 offscreen cases, native five-target postchain, state/depth/exception/reload/resize. Native world/weather/baseline and RenderRegression/World bundles remain open.',
        'guardProof': {'canonicalSites': len(expected), 'preservedSites': sum(wanted.values()), 'additionalSites': sum((actual - wanted).values()), 'exactArgumentTreesAfterDeclaredAdaptation': True},
        'oracleBodyProof': {name: hashlib.sha256(body).hexdigest() for name, body in sorted(old.items())},
        'oracleBodyPolicy': 'Javac structural fingerprints retain node kinds, tree nesting, variable/member/identifier names and literals; only redundant single-statement BlockTree nodes are unwrapped. All five full method bodies match.',
        'guardAdaptations': {'RenderSystem.getShader()': 'GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM): MC26 removed ShaderInstance global pointer; native linked program identity is retained. Caller program is the already registered vanilla GUI pipeline.'},
        'nativeAdapters': ['ResourceLocation -> Identifier; GlStateManager cache-aware GL wrappers.', 'PostChain constructor/process/getTempTarget -> ShaderManager transparency + FrameGraphBuilder/LevelTargetBundle; same five independent live inputs.', 'Legacy framebuffer ids -> temporary native GlTexture framebuffer attachments; preserve read/write framebuffer, views and pixel-pack state.', 'Original screenshot ABGR -> NativeImage.setPixelABGR; output restricted to the generated marked run root.', 'Explicit OIT on the existing material types and independent test stage; no new backend/pipeline/shader.', 'Canonical quad body remains exact; atlasQuad changes only its eight UV floats from standalone white texel to the registered white atlas sprite center. New test-only 1x1 RGBA white PNG; model count unchanged.', 'Production private createTexture raw bind/restore supports caller texture unit 15 without indexing the twelve-slot Minecraft cache.', 'Both production composite paths use the existing GlRenderState scope to restore caller program, VAO/buffers, active texture and bindings in addition to framebuffer/depth/blend state.'],
        'remaining': ['Native H14 world terrain/cloud/rain pixels and queue/stage injections', 'RenderRegressionHarness and WorldRenderRegressionHarness', 'Legacy baseline routing and canonical pulseSmokeReproduce visibility oracle', 'Full canonical flags/Gecko option, identity/player/performance consolidation', '7.2 full API/GPU/performance report'],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write', action='store_true')
    args = parser.parse_args()
    result = build()
    rendered = json.dumps(result, indent=2, ensure_ascii=False) + '\n'
    if args.write:
        REPORT.write_text(rendered)
    else:
        assert REPORT.read_text() == rendered, 'H14 proof drift; review before --write'
    print('CANONICAL H14 OFFSCREEN PROOF PASS:', result['guardProof'], 'five exact suite/oracle bodies')


if __name__ == '__main__':
    main()
