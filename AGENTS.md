# PulseLib development rules

- Follow the original Java style in branch `1.21.1`; preserve the Common/Fabric/NeoForge separation.
- Use tabs for indentation and put opening braces on a new line.
- Wrap call chains with the dot at the end of the previous line.
- Expand methods and constructors over multiple lines, with opening braces on a new line. Put method annotations on separate lines.
- **Do not use braces for single-statement control flow bodies.** This applies to `if`, `else`, `for`, `while` and similar constructs.
- Always place the single statement on the next line, indented with a tab.
- Use braces only when the body contains multiple statements or when required to preserve correct control flow (e.g., avoiding dangling `else` ambiguity).
- Never add braces to an existing single-statement control flow body unless strictly necessary for correctness. When modifying code, remove unnecessary braces from affected control flow statements.
- This rule takes precedence over general Java formatting conventions and standard formatter preferences.
Example: 
```java
if (condition)
    doSomething();

for (Item item : items)
    process(item);

if (condition)
    doSomething();
else
    doSomethingElse();

if (condition)
{
    doSomething();
    doSomethingElse();
}
```
- Add the project's author, creation date, copyright and `Arc's License of Common Sense` header. Preserve existing authors/dates; use verified Git history for migrated files.
- Check each new package for `package-info.java` with `ParametersAreNonnullByDefault`, `MethodsReturnNonnullByDefault` and `FieldsAreNonnullByDefault`. Declare real nullable values with `@Nullable`; avoid duplicate package descriptions when Common is included in loader builds.
- Preserve useful Javadoc, annotations and logical import groups; do not replace imports with wildcards just for formatting.
- Formatting tasks must preserve logic, public API and runtime behavior.
