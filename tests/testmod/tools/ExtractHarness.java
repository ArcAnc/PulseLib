/**
 * @author ArcAnc
 * Created at: 06.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parse-only inventory: guards/throws, property reads and native injection annotations. */
public final class ExtractHarness
{
	private static Map<String, Object> object(Object... pairs)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		for (int index = 0; index < pairs.length; index += 2)
			result.put((String)pairs[index], pairs[index + 1]);
		return result;
	}

	private static String json(Object value)
	{
		if (value == null)
			return "null";
		if (value instanceof Map<?, ?> map)
			return "{" + String.join(",", map.entrySet().stream().
					map(entry -> json(entry.getKey()) + ":" + json(entry.getValue())).toList()) + "}";
		if (value instanceof Iterable<?> values)
		{
			List<String> items = new ArrayList<>();
			values.forEach(item -> items.add(json(item)));
			return "[" + String.join(",", items) + "]";
		}
		if (value instanceof Boolean || value instanceof Number)
			return value.toString();
		return "\"" + value.toString().replace("\\", "\\\\").replace("\"", "\\\"").
				replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
	}

	public static void main(String[] args) throws Exception
	{
		Path root = Path.of(args[0]).toAbsolutePath().normalize();
		List<Path> files;
		try (var paths = Files.walk(root))
		{
			files = paths.filter(p -> p.toString().endsWith(".java")).sorted().toList();
		}
		var compiler = ToolProvider.getSystemJavaCompiler();
		var diagnostics = new DiagnosticCollector<JavaFileObject>();
		try (var manager = compiler.getStandardFileManager(diagnostics, null, null))
		{
			var task = (JavacTask)compiler.getTask(null, manager, diagnostics, List.of("-proc:none"), null,
					manager.getJavaFileObjectsFromPaths(files));
			var units = task.parse();
			var trees = Trees.instance(task);
			for (var unit : units)
			{
				String path = root.relativize(Path.of(unit.getSourceFile().toUri())).toString().replace('\\', '/');
				new TreePathScanner<Void, Void>()
				{
					String owner = "";
					String method = "";

					void emit(String kind, Tree tree, Object... details)
					{
						long offset = trees.getSourcePositions().getStartPosition(unit, tree);
						var result = object("path", path, "owner", owner, "method", method, "kind", kind,
								"line", unit.getLineMap().getLineNumber(offset), "offset", offset);
						result.putAll(object(details));
						System.out.println(json(result));
					}

					@Override
					public Void visitClass(ClassTree node, Void unused)
					{
						String before = owner;
						owner += "." + node.getSimpleName();
						emit("type", node, "name", owner);
						super.visitClass(node, unused);
						owner = before;
						return null;
					}

					@Override
					public Void visitMethod(MethodTree node, Void unused)
					{
						String before = method;
						method = node.getName() + "(" + node.getParameters() + ")";
						emit("method", node, "signature", method);
						super.visitMethod(node, unused);
						method = before;
						return null;
					}

					@Override
					public Void visitMethodInvocation(MethodInvocationTree node, Void unused)
					{
						String select = node.getMethodSelect().toString();
						String name = select.substring(select.lastIndexOf('.') + 1);
						if (name.startsWith("check") || name.startsWith("assert") || name.startsWith("require"))
							emit("guard", node, "callee", select, "arguments", node.getArguments().stream().map(Object::toString).toList());
						if ((name.equals("getBoolean") || name.equals("getProperty") || name.equals("getInteger")) && !node.getArguments().isEmpty())
							emit("property", node, "callee", select, "arguments", node.getArguments().stream().map(Object::toString).toList());
						return super.visitMethodInvocation(node, unused);
					}

					@Override
					public Void visitThrow(ThrowTree node, Void unused)
					{
						emit("throw", node, "expression", node.getExpression().toString());
						return super.visitThrow(node, unused);
					}

					@Override
					public Void visitAssert(AssertTree node, Void unused)
					{
						emit("assert", node, "condition", node.getCondition().toString());
						return super.visitAssert(node, unused);
					}

					@Override
					public Void visitAnnotation(AnnotationTree node, Void unused)
					{
						emit("annotation", node, "annotation", node.toString());
						return super.visitAnnotation(node, unused);
					}
				}.scan(unit, null);
			}
			if (diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR))
				throw new AssertionError(diagnostics.getDiagnostics().toString());
		}
	}
}
