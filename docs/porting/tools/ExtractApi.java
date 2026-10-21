/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;

import javax.lang.model.element.Modifier;
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
import java.util.Set;

/** Parse-only source inventory. Deliberately uses no Minecraft/loader classpath. */
public final class ExtractApi
{
	private static String normalized(Object value)
	{
		return value == null ? "" : value.toString().replaceAll("\\s+", " ").trim();
	}

	private static Map<String, Object> object(Object... pairs)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		for (int index = 0; index < pairs.length; index += 2)
		{
			result.put((String)pairs[index], pairs[index + 1]);
		}
		return result;
	}

	private static String json(Object value)
	{
		if (value == null)
		{
			return "null";
		}
		if (value instanceof Map<?, ?> map)
		{
			return "{" + String.join(",", map.entrySet().stream().
					map(entry -> json(entry.getKey()) + ":" + json(entry.getValue())).toList()) + "}";
		}
		if (value instanceof Iterable<?> values)
		{
			List<String> items = new ArrayList<>();
			values.forEach(item -> items.add(json(item)));
			return "[" + String.join(",", items) + "]";
		}
		if (value instanceof Boolean || value instanceof Number)
		{
			return value.toString();
		}
		return "\"" + value.toString().replace("\\", "\\\\").replace("\"", "\\\"").
				replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
	}

	private static boolean visible(Set<Modifier> flags)
	{
		return flags.contains(Modifier.PUBLIC) || flags.contains(Modifier.PROTECTED);
	}

	private static List<String> flags(ModifiersTree modifiers, String... implicit)
	{
		var result = new java.util.TreeSet<String>();
		modifiers.getFlags().forEach(flag -> result.add(flag.toString()));
		result.addAll(List.of(implicit));
		return new ArrayList<>(result);
	}

	private static List<String> annotations(ModifiersTree modifiers)
	{
		return modifiers.getAnnotations().stream().map(ExtractApi::normalized).toList();
	}

	private static Map<String, Object> synthetic(String kind, String name, String signature)
	{
		return object("kind", kind, "name", name, "signature", signature, "implicit", true);
	}

	private static void type(CompilationUnitTree unit, Trees trees, String path, ClassTree declaration,
	                         String owner, boolean outerVisible, boolean interfaceOwner)
	{
		String name = declaration.getSimpleName().toString();
		String fqcn = owner + "." + name;
		var modifiers = declaration.getModifiers();
		boolean api = outerVisible && (visible(modifiers.getFlags()) || interfaceOwner);
		boolean isInterface = declaration.getKind() == Tree.Kind.INTERFACE ||
				declaration.getKind() == Tree.Kind.ANNOTATION_TYPE;
		List<Map<String, Object>> members = new ArrayList<>();
		List<VariableTree> components = new ArrayList<>();
		boolean hasConstructor = false;
		for (Tree member : declaration.getMembers())
		{
			if (member instanceof VariableTree variable)
			{
				if (declaration.getKind() == Tree.Kind.RECORD && !variable.getModifiers().getFlags().contains(Modifier.STATIC))
				{
					components.add(variable);
				}
				boolean constant = declaration.getKind() == Tree.Kind.ENUM && variable.getInitializer() != null &&
						variable.getInitializer().getKind() == Tree.Kind.NEW_CLASS && normalized(variable.getType()).equals(name);
				if (visible(variable.getModifiers().getFlags()) || isInterface || constant)
				{
					List<String> memberFlags = flags(variable.getModifiers(),
							isInterface || constant ? new String[]{"public", "static", "final"} : new String[]{});
					String signature = String.join(" ", memberFlags) + " " + normalized(variable.getType()) + " " + variable.getName();
					members.add(object("kind", constant ? "enum_constant" : "field", "name", variable.getName().toString(),
							"signature", signature, "annotations", annotations(variable.getModifiers()),
							"initializer", normalized(variable.getInitializer()), "implicit", false));
				}
			}
			else if (member instanceof MethodTree method)
			{
				boolean constructor = method.getReturnType() == null;
				hasConstructor |= constructor;
				var methodFlags = method.getModifiers().getFlags();
				if (!visible(methodFlags) && !(isInterface && !methodFlags.contains(Modifier.PRIVATE)))
				{
					continue;
				}
				List<String> effectiveFlags = flags(method.getModifiers(), isInterface ? new String[]{"public"} : new String[]{});
				if (isInterface && method.getBody() == null)
				{
					effectiveFlags.add("abstract");
					effectiveFlags = effectiveFlags.stream().distinct().sorted().toList();
				}
				String methodName = constructor ? "<init>" : method.getName().toString();
				List<String> parameters = new ArrayList<>();
				for (VariableTree parameter : method.getParameters())
				{
					String parameterType = normalized(parameter.getType());
					if (parameter.toString().contains("..."))
					{
						parameterType = parameterType.replaceFirst("\\[\\]$", "...");
					}
					parameters.add(parameterType);
				}
				String signature = String.join(" ", effectiveFlags) + " " +
						(method.getTypeParameters().isEmpty() ? "" : "<" + String.join(", ", method.getTypeParameters().stream().map(ExtractApi::normalized).toList()) + "> ") +
						(constructor ? "" : normalized(method.getReturnType()) + " ") + methodName + "(" + String.join(", ", parameters) + ")" +
						(method.getThrows().isEmpty() ? "" : " throws " + String.join(", ", method.getThrows().stream().map(ExtractApi::normalized).toList()));
				members.add(object("kind", constructor ? "constructor" : "method", "name", methodName,
						"signature", signature, "parameters", method.getParameters().stream().map(ExtractApi::normalized).toList(),
						"receiver", normalized(method.getReceiverParameter()), "annotations", annotations(method.getModifiers()),
						"default", normalized(method.getDefaultValue()), "implicit", false));
			}
		}
		if (declaration.getKind() == Tree.Kind.RECORD)
		{
			String args = String.join(", ", components.stream().map(component -> normalized(component.getType())).toList());
			String canonical = "public <init>(" + args + ")";
			if (members.stream().noneMatch(member -> member.get("signature").equals(canonical)))
			{
				members.add(synthetic("constructor", "<init>", canonical));
			}
			for (VariableTree component : components)
			{
				String signature = "public " + normalized(component.getType()) + " " + component.getName() + "()";
				if (members.stream().noneMatch(member -> member.get("name").equals(component.getName().toString()) &&
						member.get("kind").equals("method") && member.get("signature").toString().endsWith(component.getName() + "()")))
				{
					members.add(synthetic("method", component.getName().toString(), signature));
				}
			}
			for (String[] standard : List.of(new String[]{"equals", "public final boolean equals(Object)"},
					new String[]{"hashCode", "public final int hashCode()"}, new String[]{"toString", "public final String toString()"}))
			{
				if (members.stream().noneMatch(member -> member.get("kind").equals("method") &&
						member.get("signature").toString().replaceAll("@[\\w.]+\\s*", "").matches(".*\\b" + standard[0] +
						(standard[0].equals("equals") ? "\\((?:java.lang.)?Object\\)" : "\\(\\)"))))
				{
					members.add(synthetic("method", standard[0], standard[1]));
				}
			}
		}
		else if (declaration.getKind() == Tree.Kind.ENUM)
		{
			members.add(synthetic("method", "values", "public static " + name + "[] values()"));
			members.add(synthetic("method", "valueOf", "public static " + name + " valueOf(String)"));
		}
		else if (declaration.getKind() == Tree.Kind.CLASS && !hasConstructor && (visible(modifiers.getFlags()) || interfaceOwner))
		{
			members.add(synthetic("constructor", "<init>",
					(modifiers.getFlags().contains(Modifier.PROTECTED) ? "protected" : "public") + " <init>()"));
		}
		long position = trees.getSourcePositions().getStartPosition(unit, declaration);
		System.out.println(json(object("fqcn", fqcn, "source", path, "line", unit.getLineMap().getLineNumber(position),
				"api", api, "kind", declaration.getKind().toString(), "modifiers", flags(modifiers, interfaceOwner ? new String[]{"public", "static"} : new String[]{}),
				"annotations", annotations(modifiers), "type_parameters", declaration.getTypeParameters().stream().map(ExtractApi::normalized).toList(),
				"extends", normalized(declaration.getExtendsClause()), "implements", declaration.getImplementsClause().stream().map(ExtractApi::normalized).toList(),
				"permits", declaration.getPermitsClause().stream().map(ExtractApi::normalized).toList(),
				"record_components", components.stream().map(component -> object("name", component.getName().toString(),
						"type", normalized(component.getType()), "annotations", annotations(component.getModifiers()))).toList(),
				"members", members)));
		for (Tree member : declaration.getMembers())
		{
			if (member instanceof ClassTree nested)
			{
				type(unit, trees, path, nested, fqcn, api, isInterface);
			}
		}
	}

	public static void main(String[] args) throws Exception
	{
		Path root = Path.of(args[0]).toAbsolutePath();
		List<Path> paths;
		try (var walk = Files.walk(root))
		{
			paths = walk.filter(path -> path.toString().endsWith(".java") &&
					path.toString().contains("/src/main/java/") && !root.relativize(path).startsWith("tests/")).sorted().toList();
		}
		var compiler = ToolProvider.getSystemJavaCompiler();
		var diagnostics = new DiagnosticCollector<JavaFileObject>();
		try (var manager = compiler.getStandardFileManager(diagnostics, null, java.nio.charset.StandardCharsets.UTF_8))
		{
			JavacTask task = (JavacTask)compiler.getTask(null, manager, diagnostics,
					List.of("-proc:none", "--release", "25"), null, manager.getJavaFileObjectsFromPaths(paths));
			Trees trees = Trees.instance(task);
			for (CompilationUnitTree unit : task.parse())
			{
				String path = root.relativize(Path.of(unit.getSourceFile().toUri())).toString();
				for (Tree declaration : unit.getTypeDecls())
				{
					if (declaration instanceof ClassTree named)
					{
						type(unit, trees, path, named, unit.getPackageName().toString(), true, false);
					}
				}
			}
		}
		for (var diagnostic : diagnostics.getDiagnostics())
		{
			if (diagnostic.getKind() == Diagnostic.Kind.ERROR)
			{
				throw new IllegalStateException(diagnostic.toString());
			}
		}
	}
}
