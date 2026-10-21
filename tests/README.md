# Migration regression fixtures

The canonical regression suites and real Gecko/glTF assets are pinned at
`7ce6f024b307bb821ce543344d3896c8e1fcf26b` on `1.21.1-multiloader`.
Port each suite alongside its subsystem as described in
[the migration journal](../docs/porting/26.1.2-multiloader.md).

Stage 1.1 establishes the Gradle modules and preserves the 26.1 source bodies.
The external test mod is not yet migrated. Client/server regression results
must be recorded explicitly; a successful compilation does not establish them.
