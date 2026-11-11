/**
 * @author ArcAnc
 * Created at: 11.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

import com.sun.source.tree.ClassTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Attributed source surface, including generic substitutions on inherited members.
 * Arguments: release, classpath text file, class output directory, source directories.
 * Emits nothing on attribution errors; annotation processors are deliberately disabled.
 */
public final class ExtractResolvedApi
{
	private ExtractResolvedApi()
	{
	}

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

	private static boolean visible(Element element)
	{
		return element.getModifiers().contains(Modifier.PUBLIC) || element.getModifiers().contains(Modifier.PROTECTED);
	}

	private static boolean accessible(TypeElement type)
	{
		return visible(type) && (!(type.getEnclosingElement() instanceof TypeElement outer) || accessible(outer));
	}

	private static boolean named(TypeElement type)
	{
		return (type.getNestingKind() == NestingKind.TOP_LEVEL || type.getNestingKind() == NestingKind.MEMBER) &&
				(!(type.getEnclosingElement() instanceof TypeElement outer) || named(outer));
	}

	private static List<String> flags(Element element)
	{
		return element.getModifiers().stream().map(Modifier::toString).sorted().toList();
	}

	private static String type(TypeMirror mirror)
	{
		return switch (mirror.getKind())
		{
			case ERROR -> throw new IllegalStateException("Unresolved type: " + mirror);
			case DECLARED ->
			{
				DeclaredType declared = (DeclaredType)mirror;
				String owner = declared.getEnclosingType().getKind() == javax.lang.model.type.TypeKind.DECLARED
						? type(declared.getEnclosingType()) + "." + declared.asElement().getSimpleName()
						: ((TypeElement)declared.asElement()).getQualifiedName().toString();
				yield owner + (declared.getTypeArguments().isEmpty() ? "" : "<" + String.join(",", declared.getTypeArguments().stream().map(ExtractResolvedApi::type).toList()) + ">");
			}
			case ARRAY -> type(((ArrayType)mirror).getComponentType()) + "[]";
			case TYPEVAR -> ((TypeVariable)mirror).asElement().getSimpleName().toString();
			case WILDCARD ->
			{
				WildcardType wildcard = (WildcardType)mirror;
				yield wildcard.getExtendsBound() != null ? "? extends " + type(wildcard.getExtendsBound())
						: wildcard.getSuperBound() != null ? "? super " + type(wildcard.getSuperBound()) : "?";
			}
			case INTERSECTION -> String.join("&", ((IntersectionType)mirror).getBounds().stream().map(ExtractResolvedApi::type).toList());
			default -> mirror.getKind().name().toLowerCase(java.util.Locale.ROOT);
		};
	}

	private static String descriptor(TypeMirror mirror, Types types, Elements elements)
	{
		TypeMirror erased = types.erasure(mirror);
		return switch (erased.getKind())
		{
			case BOOLEAN -> "Z";
			case BYTE -> "B";
			case SHORT -> "S";
			case INT -> "I";
			case LONG -> "J";
			case CHAR -> "C";
			case FLOAT -> "F";
			case DOUBLE -> "D";
			case VOID -> "V";
			case ARRAY -> "[" + descriptor(((ArrayType)erased).getComponentType(), types, elements);
			case DECLARED -> "L" + elements.getBinaryName((TypeElement)((DeclaredType)erased).asElement()).toString().replace('.', '/') + ";";
			case EXECUTABLE ->
			{
				ExecutableType executable = (ExecutableType)erased;
				yield "(" + String.join("", executable.getParameterTypes().stream().map(parameter -> descriptor(parameter, types, elements)).toList()) + ")" + descriptor(executable.getReturnType(), types, elements);
			}
			default -> throw new IllegalStateException("Cannot erase " + mirror);
		};
	}

	private static List<Object> parameters(List<? extends TypeParameterElement> parameters)
	{
		return parameters.stream().map(parameter -> (Object)object("name", parameter.getSimpleName().toString(),
				"bounds", parameter.getBounds().stream().map(ExtractResolvedApi::type).toList())).toList();
	}

	private static Map<String, Object> member(TypeElement receiver, Element element, Types types, Elements elements)
	{
		TypeElement owner = (TypeElement)element.getEnclosingElement();
		TypeMirror resolved = types.asMemberOf((DeclaredType)receiver.asType(), element);
		Map<String, Object> row = object("kind", element.getKind().name(), "name", element.getSimpleName().toString(),
				"owner", owner.getQualifiedName().toString(), "ownerBinary", elements.getBinaryName(owner).toString(),
				"declared", owner.equals(receiver), "flags", flags(element));
		if (element instanceof ExecutableElement method)
		{
			ExecutableType executable = (ExecutableType)resolved;
			String erasedDescriptor = descriptor(method.asType(), types, elements);
			if (element.getKind() == ElementKind.CONSTRUCTOR && owner.getNestingKind() == NestingKind.MEMBER &&
					!owner.getModifiers().contains(Modifier.STATIC))
				erasedDescriptor = "(" + descriptor(owner.getEnclosingElement().asType(), types, elements) + erasedDescriptor.substring(1);
			row.putAll(object("parameters", executable.getParameterTypes().stream().map(ExtractResolvedApi::type).toList(),
					"returns", type(executable.getReturnType()), "throws", executable.getThrownTypes().stream().map(ExtractResolvedApi::type).toList(),
					"typeParameters", executable.getTypeVariables().stream().map(variable -> object("name", type(variable),
							"bounds", variable.getUpperBound() instanceof IntersectionType intersection ? intersection.getBounds().stream().map(ExtractResolvedApi::type).toList()
									: List.of(type(variable.getUpperBound())))).toList(), "varargs", method.isVarArgs(),
					"descriptor", erasedDescriptor, "resolvedDescriptor", descriptor(resolved, types, elements)));
		}
		else if (element instanceof TypeElement nested)
			row.put("type", nested.getQualifiedName().toString());
		else
			row.putAll(object("type", type(resolved), "descriptor", descriptor(element.asType(), types, elements)));
		return row;
	}

	public static void main(String[] args) throws Exception
	{
		if (args.length < 4)
			throw new IllegalArgumentException("Expected release, classpath file, class output, source directories");
		List<Path> sources = new ArrayList<>();
		for (int index = 3; index < args.length; index++)
			try (var paths = Files.walk(Path.of(args[index])))
			{
				sources.addAll(paths.filter(path -> path.toString().endsWith(".java")).sorted().toList());
			}
		var diagnostics = new DiagnosticCollector<JavaFileObject>();
		var compiler = ToolProvider.getSystemJavaCompiler();
		try (var files = compiler.getStandardFileManager(diagnostics, null, java.nio.charset.StandardCharsets.UTF_8))
		{
			Files.createDirectories(Path.of(args[2]));
			var task = (JavacTask)compiler.getTask(null, files, diagnostics,
					List.of("--release", args[0], "-proc:none", "-encoding", "UTF-8", "-Xlint:none", "-classpath", Files.readString(Path.of(args[1])).strip(),
							"-sourcepath", "", "-d", args[2]), null, files.getJavaFileObjectsFromPaths(sources));
			var units = task.parse();
			task.analyze();
			check(diagnostics);
			var trees = Trees.instance(task);
			var elements = task.getElements();
			var types = task.getTypes();
			List<Map<String, Object>> rows = new ArrayList<>();
			for (var unit : units)
				new TreePathScanner<Void, Void>()
				{
					@Override
					public Void visitClass(ClassTree tree, Void unused)
					{
						if (trees.getElement(getCurrentPath()) instanceof TypeElement element && named(element))
						{
							List<Map<String, Object>> members = new ArrayList<>();
							for (var candidate : elements.getAllMembers(element))
								if (visible(candidate) && (candidate.getKind() != ElementKind.CONSTRUCTOR || candidate.getEnclosingElement().equals(element)))
									members.add(member(element, candidate, types, elements));
							members.sort(Comparator.comparing(ExtractResolvedApi::json));
							rows.add(object("type", element.getQualifiedName().toString(), "binaryName", elements.getBinaryName(element).toString(),
									"source", Path.of(unit.getSourceFile().toUri()).toString(), "api", accessible(element), "kind", element.getKind().name(),
									"flags", flags(element), "typeParameters", parameters(element.getTypeParameters()), "superclass", type(element.getSuperclass()),
									"interfaces", element.getInterfaces().stream().map(ExtractResolvedApi::type).toList(), "members", members));
						}
						return super.visitClass(tree, unused);
					}
					}.scan(unit, null);
			rows.sort(Comparator.comparing(row -> (String)row.get("type")));
			task.generate();
			check(diagnostics);
			rows.forEach(row -> System.out.println(json(row)));
		}
	}

	private static void check(DiagnosticCollector<JavaFileObject> diagnostics)
	{
		var errors = diagnostics.getDiagnostics().stream().filter(item -> item.getKind() == Diagnostic.Kind.ERROR).toList();
		if (!errors.isEmpty())
		{
			errors.forEach(System.err::println);
			throw new IllegalStateException("Compiler errors: " + errors.size());
		}
	}
}
