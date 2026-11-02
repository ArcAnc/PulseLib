# External testmod migration

Stage **7.1a** provides a packaged bootstrap, ID `pulselibtest_bootstrap`, for
Minecraft 26.1.2 on Fabric and NeoForge. The complete canonical `pulselibtest`
demo (blocks/entities/items/renderers/player demos), source mode and consolidation
of the existing stage harnesses remain **7.1b**. `pulseTestMod`, `pulseTestJar`
and the canonical content/harness flags remain unsupported until that migration.

The `testmod` source set packages only `bootstrap/common` and the current loader's
`bootstrap/{fabric,neoforge}` sources/resources. Runtime uses the resulting JAR
as an ordinary dependency; testmod source-set outputs are absent from the run
classpath. Production JARs contain neither the bootstrap nor its assets.

Build first, then run in a separate Gradle invocation:

```bash
./gradlew :fabric:testmodJar :neoforge:testmodJar --offline --no-daemon --console=plain
python3 tests/testmod/tools/run_bootstrap.py --eula /absolute/path/to/your/accepted/eula.txt
python3 tests/testmod/tools/check_guards.py --output build/reports/bootstrap-guards-UNIQUE
```

The runner creates a unique run ID, starts both dedicated servers and both
visible clients sequentially, and requires COMPLETE, SHUTDOWN PASS, Gradle
verification, saved worlds and exit 0. JSON results and stdout are retained under
`build/reports/testmod-bootstrap/<runId>`; `--output` selects another **new**
directory. `--gradle-init` may be repeated, for example for the HEAD source
projection of a dirty workspace documented in migration journal section 63.
`--timeout` bounds each Gradle process (default 900 seconds).

Each game directory is created under the loader's
`build/pulselib-tests/<runId>/{server,client}`. Any existing target directory,
including a previously marked one, is rejected. Symlink ancestors and path-like
run IDs are rejected. `.pulselib-test-run` and the world's `.pulselib-disposable`
must have their exact generated contents. The server copies an explicitly
provided, previously accepted EULA; no tool accepts it automatically. Servers
bind to loopback with port 0. Each client creates its own flat creative/peaceful
integrated world, performs an actual resource reload and stops automatically.
Old saves are never adopted. Failed and successful run directories remain for
inspection; rerunning creates fresh ones.

For one case after building both JARs:

```bash
./gradlew :fabric:runServer -PpulseTestJarBootstrap -PpulseBootstrapRunId=UNIQUE -PpulseTestEulaFile=/absolute/path/to/your/accepted/eula.txt --offline --no-daemon --console=plain
./gradlew :neoforge:runClient -PpulseTestJarBootstrap -PpulseBootstrapRunId=ANOTHER_UNIQUE --offline --no-daemon --console=plain
```

`pulseBootstrapJar` is an explicit JAR override for negative controls; it must
still contain the bootstrap mod. Normal runs use the loader's `testmodJar`.
Packaging does not require any launch flags, EULA or existing game directory.

Checks cover JAR CodeSource, three annotated candidates (BOTH/CLIENT/SERVER),
exactly two compatible loaded plugins, common lifecycle callbacks exactly once,
and absence of client classes on the dedicated server. Clients check actual
resource-pack origin and SHA-256 for six canonical assets, baked GLB meshes,
Gecko bones/animation, atlas texture and model replacement after reload.
The bootstrap does not claim full demo rendering or the final GPU/performance
matrix. Existing stage harnesses and their evidence remain available separately.

Assets are byte-for-byte copies of `tests/testmod/src/main/resources` at frozen
`1.21.1-multiloader` SHA `7ce6f024b307bb821ce543344d3896c8e1fcf26b`:
`item/test_block.glb`, its pyramid/circle textures, `player_flip` Gecko model and
animation, and `entity/test_entity/0.png`. The resource manifest records hashes.
`CommonTestPlugin` and the packaging convention preserve creation date
12.10.2026 (Git `e591523b9f8dfdd0fbe17402616d700e57cf14cd`);
`DisposableTestWorlds` preserves 13.10.2026
(`4b22a67bcf3e1091de92dc5f8e2c94de9ac48eaf`). Other authored bootstrap code is
dated 03.11.2026. All retain ArcAnc / Arc's License of Common Sense attribution.

See [migration journal](../../docs/porting/26.1.2-multiloader.md) section 63 and
[recorded evidence](../../docs/porting/evidence/stage-7.1a.json).
