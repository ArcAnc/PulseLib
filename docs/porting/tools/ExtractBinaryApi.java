/**
 * @author ArcAnc
 * Created at: 11.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

import java.lang.classfile.AttributedElement;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

/** Reads class-file contracts directly without loading Minecraft or mod classes. */
public final class ExtractBinaryApi
{
	private ExtractBinaryApi()
	{
	}

	private static Map<String, Object> object(Object... pairs)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		for (int index = 0; index < pairs.length; index += 2)
			result.put((String)pairs[index], pairs[index + 1]);
		return result;
	}

	private static String signature(AttributedElement element)
	{
		return element.findAttribute(Attributes.signature()).
				map(attribute -> attribute.signature().stringValue()).orElse("");
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
		for (String argument : args)
			try (var zip = new ZipFile(Path.of(argument).toFile()))
			{
				for (var entry : zip.stream().filter(item -> item.getName().endsWith(".class")).
						sorted(java.util.Comparator.comparing(java.util.zip.ZipEntry::getName)).toList())
				{
					var model = ClassFile.of().parse(zip.getInputStream(entry).readAllBytes());
					List<Object> fields = new ArrayList<>();
					for (var field : model.fields())
						fields.add(object("name", field.fieldName().stringValue(), "descriptor", field.fieldType().stringValue(),
								"flags", field.flags().flagsMask(), "signature", signature(field),
								"constant", field.findAttribute(Attributes.constantValue()).
										map(attribute -> attribute.constant().constantValue().toString()).orElse(null)));
					List<Object> methods = new ArrayList<>();
					for (var method : model.methods())
						methods.add(object("name", method.methodName().stringValue(), "descriptor", method.methodType().stringValue(),
								"flags", method.flags().flagsMask(), "signature", signature(method),
								"exceptions", method.findAttribute(Attributes.exceptions()).
										map(attribute -> attribute.exceptions().stream().map(type -> type.asInternalName()).toList()).orElse(List.of())));
					System.out.println(json(object("jar", argument, "binaryName", model.thisClass().asInternalName(),
							"major", model.majorVersion(), "flags", model.flags().flagsMask(), "signature", signature(model),
							"superclass", model.superclass().map(type -> type.asInternalName()).orElse(""),
							"interfaces", model.interfaces().stream().map(type -> type.asInternalName()).toList(),
							"fields", fields, "methods", methods)));
				}
			}
	}
}
