# External testmod migration

Stage **7.1a** provides a packaged bootstrap, ID `pulselibtest_bootstrap`, for
Minecraft 26.1.2 on Fabric and NeoForge. The complete canonical `pulselibtest`
demo content (blocks/entities/items/renderers/player demos) and source/JAR
profiles are migrated in **7.1b.1** below. Consolidation of the complete canonical
harness bundle remains **7.1b.2**. `pulseTestMod`, `pulseTestJar`
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


## Canonical demo content (7.1b.1)

`demo/common`, `demo/fabric` and `demo/neoforge` provide the canonical
`pulselibtest` game content on Minecraft 26.1.2: block/block entity, entity,
animated block item, three armor pieces, tail attachment and ball/acrobatic
player demos. Each loader keeps its native registry/event wiring; renderer
callbacks run through the existing Common/PGl backend. The special item codec
is `pulselibtest:test_block`. Key mappings remain V (ball) / B (acrobatic).

Build `:fabric:demoJar :neoforge:demoJar` first. `-PpulseDemoJar` uses the packaged
JAR, `-PpulseDemoContent` uses the demo source-set outputs/mod roots. Select one
profile per invocation. The bootstrap profile above remains separate. Full
`pulseTestMod`/`pulseTestJar` and canonical harness flags stay guarded until
**7.1b.2**; demo source/JAR profiles do not claim that complete bundle.

```bash
./gradlew :fabric:demoJar :neoforge:demoJar --offline --no-daemon --console=plain
python3 tests/testmod/tools/run_demo.py --eula /absolute/path/to/your/accepted/eula.txt --output build/reports/demo-UNIQUE
```

The runner checks eight client/server cases across both loaders and both modes;
`--modes jar` or `--modes source` restricts modes. `--gradle-init` accepts the
source projection described in journal section 64. Fresh-directory, symlink,
accepted-EULA and marked-world contracts are shared with bootstrap; directories
are never adopted or reused. Runs finish automatically and preserve their JSON,
logs, classloading traces and saved worlds. Individual launches use the selected
profile, `-PpulseBootstrapRunId=UNIQUE` and, on dedicated servers,
`-PpulseTestEulaFile=/absolute/path/to/your/accepted/eula.txt`.

Each client creates a canonical demo scene, requires new entity/block/item
submissions in two resource epochs, starts both player demos through canonical
handles, and verifies native armor setup, ball socket and tail sampling.
Actual resource bytes and mod-pack origin are checked against the 50-resource
manifest twice. Forty-five original assets are canonical byte-for-byte copies;
five MC26 item definitions are generated adaptations. Servers verify registry
IDs, entity attributes, five items and three equippable armor components without
loading Minecraft client classes. Complete regression harness consolidation and
new GPU pixel/performance claims remain the following checkpoints.

See journal section 64 and
[recorded evidence](../../docs/porting/evidence/stage-7.1b.1.json).


## Canonical animation harness consolidation (7.1b.2a)

`--animation-regression` on `run_demo.py` adds `-PpulseAnimationRegression` to
all eight source/JAR client/server cases. Clients execute the original loaded
GLB overlay/snapshot suite and Gecko sampling/Molang suite before and after
resource reload. They retain active entity/block controllers for at least 21
seconds across two scheduled maintenance boundaries, observe the actual
production maintenance calls, then require immediate removal through native
entity/chunk unload. The production loader subscriptions missing from this
path are restored on both loaders; the Common animation/GPU core is unchanged.

```bash
python3 tests/testmod/tools/run_demo.py --animation-regression --eula /path/to/your/accepted/eula.txt --output build/reports/animation-UNIQUE
python3 tests/testmod/tools/inventory_harnesses.py
```

`inventory_harnesses.py` extracts only frozen canonical Git objects, parses all
62 Java files with javac and checks the committed call-site/flag/Mixin inventory.
It also proves that all 25 canonical animation `check` argument trees are still
present with unchanged messages/tolerances. `--write` explicitly regenerates
the inventory after review. Deferred entries remain open; old stage evidence
links do not claim complete assertion equivalence. Full canonical regression
flags remain guarded for **7.1b.2b**.

The explicit negative control `-PpulseAnimationSkipRemoval` requires the
animation regression profile and a demo profile. It cancels only the production
removeManager body through a test-only client Mixin; the unchanged canonical
L25 removal assertion must fail. Do not count this as normal shutdown/PASS.
The positive runner never sets the negative flag. Temporary debugger runs and
incomplete runs are excluded from accepted results.

See journal section 65 and
[recorded evidence](../../docs/porting/evidence/stage-7.1b.2a.json).
