/**
 * @author ArcAnc
 * Created at: 12.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;

import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Parse-only documentation tool; no Minecraft dependencies or compiler attribution required. */
class ApiBaseline
{
	public static void main(String[] args) throws Exception
	{
		Path root = Path.of(args[0]).toAbsolutePath();
		List<Path> files = new ArrayList<>();
		for (String module : List.of("common", "fabric", "neoforge"))
			try (var paths = Files.walk(root.resolve(module + "/src/main/java")))
			{
				files.addAll(paths.filter(path -> path.toString().endsWith(".java")).sorted().toList());
			}
		var compiler = ToolProvider.getSystemJavaCompiler();
		var diagnostics = new DiagnosticCollector<JavaFileObject>();
		try (var manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8))
		{
			JavacTask task = (JavacTask)compiler.getTask(null, manager, diagnostics, List.of("-proc:none", "--release", "25"),
					null, manager.getJavaFileObjectsFromPaths(files));
			SourcePositions positions = Trees.instance(task).getSourcePositions();
			for (CompilationUnitTree unit : task.parse())
			{
				String source = unit.getSourceFile().getCharContent(true).toString();
				System.out.println("FILE " + root.relativize(Path.of(unit.getSourceFile().toUri())));
				unit.getPackageAnnotations().forEach(annotation -> System.out.println(flat(annotation.toString())));
				System.out.println("package " + unit.getPackageName() + ";");
				unit.getImports().forEach(value -> System.out.println(flat(value.toString())));
				for (Tree type : unit.getTypeDecls())
					if (type instanceof ClassTree declaration)
						emit(declaration, unit, source, positions, "");
				System.out.println();
			}
		}
		for (var diagnostic : diagnostics.getDiagnostics())
			if (diagnostic.getKind() == Diagnostic.Kind.ERROR)
				throw new IllegalStateException(diagnostic.toString());
	}

	private static void emit(ClassTree type, CompilationUnitTree unit, String source, SourcePositions positions, String indent)
	{
		if (type.getModifiers().getFlags().contains(Modifier.PRIVATE))
			return;
		int start = (int)positions.getStartPosition(unit, type);
		int opening = openingBrace(source, start);
		System.out.println(indent + "TYPE " + flat(source.substring(start, opening)));
		boolean implicitPublic = type.getKind() == Tree.Kind.INTERFACE || type.getKind() == Tree.Kind.ANNOTATION_TYPE;
		for (Tree member : type.getMembers())
		{
			if (member instanceof ClassTree nested)
				emit(nested, unit, source, positions, indent + "\t");
			else if (member instanceof MethodTree method && visible(method.getModifiers().getFlags(), implicitPublic))
			{
				int from = (int)positions.getStartPosition(unit, method);
				int to = (int)(method.getBody() == null ? positions.getEndPosition(unit, method) :
						positions.getStartPosition(unit, method.getBody()));
				System.out.println(indent + "\tMETHOD " + flat(source.substring(from, to)));
			}
			else if (member instanceof VariableTree field && visible(field.getModifiers().getFlags(), implicitPublic))
			{
				String value = flat(field.getModifiers() + " " + field.getType() + " " + field.getName());
				if (field.getInitializer() != null && (field.getModifiers().getFlags().contains(Modifier.FINAL) || implicitPublic) &&
						(field.getType().getKind() == Tree.Kind.PRIMITIVE_TYPE || field.getType().toString().equals("String")))
					value += " = " + flat(field.getInitializer().toString());
				System.out.println(indent + "\tFIELD " + value);
			}
		}
	}

	private static boolean visible(Set<Modifier> modifiers, boolean implicitPublic)
	{
		return !modifiers.contains(Modifier.PRIVATE) && (implicitPublic || modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PROTECTED));
	}

	/** Find the body after annotations, generic parameters and record components. */
	private static int openingBrace(String source, int start)
	{
		int parentheses = 0;
		for (int index = start; index < source.length(); index++)
		{
			char next = source.charAt(index);
			if (next == '"' || next == '\'')
			{
				char quote = next;
				while (++index < source.length())
				{
					if (source.charAt(index) == '\\')
						index++;
					else if (source.charAt(index) == quote)
						break;
				}
			}
			else if (next == '/' && index + 1 < source.length() && source.charAt(index + 1) == '*')
				index = source.indexOf("*/", index + 2) + 1;
			else if (next == '/' && index + 1 < source.length() && source.charAt(index + 1) == '/')
				index = source.indexOf('\n', index + 2);
			else if (next == '(')
				parentheses++;
			else if (next == ')')
				parentheses--;
			else if (next == '{' && parentheses == 0)
				return index;
		}
		throw new IllegalArgumentException("Missing type body");
	}

	private static String flat(String value)
	{
		return value.replaceAll("\\s+", " ").trim();
	}
}
