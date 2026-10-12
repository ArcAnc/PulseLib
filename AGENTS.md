# PulseLib development rules

- Follow the original Java style in branch `1.21.1`; preserve the Common/Fabric/NeoForge separation.
- Use tabs for indentation and put opening braces on a new line.
- Wrap call chains with the dot at the end of the previous line.
- Expand methods, constructors and conditional blocks over multiple lines; put method annotations on separate lines.
- Add the project's author, creation date, copyright and `Arc's License of Common Sense` header. Preserve existing authors/dates; use verified Git history for migrated files.
- Check each new package for `package-info.java` with `ParametersAreNonnullByDefault`, `MethodsReturnNonnullByDefault` and `FieldsAreNonnullByDefault`. Declare real nullable values with `@Nullable`; avoid duplicate package descriptions when Common is included in loader builds.
- Preserve useful Javadoc, annotations and logical import groups; do not replace imports with wildcards just for formatting.
- Formatting tasks must preserve logic, public API and runtime behavior.
