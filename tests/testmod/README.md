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


## Smoke resource lifecycle (7.1b.2b.1)

`--lifecycle-regression` on `run_demo.py` adds `-PpulseLifecycleRegression` to
both demo source/JAR profiles on both loaders. Each client first completes the
normal asset/scene/reload checks, then populates the actual queue/backend with
CPU/GPU deformation and explicit ORDINARY/OIT on the existing material type.
It checks deletion after explicit cleanup, recreates resources and verifies the
native loader disconnect callback. Capture checks live GL handles; deletion is
checked inside the callback before other allocations can reuse their names.
Managers, caches, mappings, queues, buffer/texture wrappers and fences must clear.
The dedicated server must keep all client harness classes unloaded.

```bash
python3 tests/testmod/tools/run_demo.py --animation-regression --lifecycle-regression --eula /path/to/your/accepted/eula.txt --output build/reports/lifecycle-UNIQUE
python3 tests/testmod/tools/inventory_lifecycle.py
python3 tests/testmod/tools/inventory_harnesses.py
```

The animation flag is optional. Its retention phase now waits for two actual
maintenance callbacks as well as the original 21-second/two-boundary criteria;
the 90-second deadline and all assertions are retained. The ordinary demo without
these flags keeps its short asset/scene/reload run. COMPLETE is emitted once.

`inventory_lifecycle.py` checks all 11 canonical SmokeResources guard argument
trees plus the natural-hook guard. The only guard representation adaptation is
`geometryResources` to the native registry's `meshResources`. The native resource
inspector derives from the recorded 26.1 Stage42c fixture; its extra guards cover
all current OIT owners, UBO rings, VAOs, fences and close listeners. The full
canonical Smoke graphics/pixel/camera suite remains open, as do identity/player/
render/performance consolidation and the guarded full canonical flags.

For an explicit negative control after packaging, use a new run ID and
`-PpulseDemoJar -PpulseLifecycleRegression -PpulseLifecycleSkipHook` with either
loader's `runClient`. The test-only Mixin suppresses the native disconnect body;
the unchanged canonical resource assertion must report a leak (Java 255 / Gradle
1), without NATURAL DISCONNECT PASS or CLIENT COMPLETE. The positive runner never
sets this flag. Profile prerequisites reject invalid flags before game startup.

See journal section 66, [assertion proof](../../docs/porting/evidence/stage-7.1b.2b.1-inventory.json)
and [recorded evidence](../../docs/porting/evidence/stage-7.1b.2b.1.json).


## Smoke world/graphics orchestration (7.1b.2b.2a)

`--smoke-regression` adds `-PpulseSmokeRegression` to the existing demo
source/JAR profiles. Clients run the canonical Smoke plugin/model/atlas guards,
prepare the original marked-world platform and equipment, lock the camera,
wait 30 player ticks and check block/entity/item/armor/layer/animated callbacks.
A loaded Gecko controller must start and evaluate its clip. The original item
pyramid draws eight GPU bend instances into each 64x64 solid/OIT target.
Visible pixels, partial OIT alpha and indexed blend/depth/scissor restoration
retain the original thresholds and use the existing Common backend/pipelines.

```bash
python3 tests/testmod/tools/run_demo.py --smoke-regression --animation-regression --lifecycle-regression --eula /path/to/your/accepted/eula.txt --output build/reports/smoke-UNIQUE
python3 tests/testmod/tools/inventory_smoke.py
```

After Smoke finishes, its transient block and actor are removed through native
server operations. Client actor removal is observed before L25 selects owners;
the next demo actor must pass native `noCollision` before spawning. Independent
demo/retention owners then complete the two normal resource epochs in that same
area. There is no teleport/world replacement between retention and removal.
Animation/lifecycle options are independent and optional.

The Smoke automation window is hidden while preserving its GL context and
framebuffer; the standard demo profile stays visible. The native stage observer
checks block → particles → final world composition, including the Fabulous
route through `endWorldFrame`. Fixed cameras run before extraction, graphics at
render TAIL. After Smoke, content epochs lock yaw/pitch to 0/8 degrees. Native
Camera yaw is checked after the original 30-tick Smoke wait (0/180 degrees,
tolerance 0.001). Destructive animation removal checks follow graphics. Native
GL debug errors are retained synchronously and checked outside the callback.

The inventory proves all 28 original Smoke guard argument trees after three
explicit adaptations, plus all three stage-order throw expressions. Extra
Smoke guards cover its deadline and actual native yaw; the demo adds a
collision-free spawn prerequisite. The earlier lifecycle proof stays separate.
Full RenderRegression/World/H14, identity/player/performance suites and
canonical full flags remain open.

Two intentionally failing controls require a Smoke profile and a new run ID.
Invoke either loader's `runClient` with `-PpulseDemoJar -PpulseSmokeRegression`
and one of:

- `-PpulseSmokeSkipDraw`: skip only the eight test submissions. The original
  `Original demo geometry must produce visible pixels` guard must fail.
- `-PpulseSmokeFirstPerson`: FIRST_PERSON/yaw 180 must fail an unchanged world
  prerequisite: Fabric `Loader custom armor adapter must be invoked`, NeoForge
  `Original block entity renderer must run`.

Require Java 255 / Gradle 1, without SMOKE or CLIENT COMPLETE. The runner's
`--smoke-first-person` invokes the same deliberately failing control; it writes
FAIL and raises. The positive runner sets neither control. Missing profile
prerequisites are rejected before a game directory is created.

At native yaw 180, Fabric still executes BER callbacks while NeoForge does not.
A portable callback count therefore cannot serve as a block-visibility oracle.
The canonical `pulseSmokeReproduce` flag remains guarded pending native
GPU/frustum visibility evidence. This scoped first-person control does not
claim that equivalence or first-person rendering correctness.

See journal section 67, [assertion proof](../../docs/porting/evidence/stage-7.1b.2b.2a-inventory.json)
and [recorded evidence](../../docs/porting/evidence/stage-7.1b.2b.2a.json).

## Canonical H14 offscreen suite (7.1b.2b.2b.1)

`-PpulseH14Offscreen` requires `-PpulseDemoJar` or `-PpulseDemoContent` and
excludes animation/Smoke/lifecycle orchestration. It runs all 38 canonical
H14 offscreen cases on the title screen, with no world/server or EULA file.
The hidden window retains its GL context and actually resizes across a native
resource reload. Each client writes 48 PNGs and a result JSON into its fresh
marked run directory. No existing run directory is accepted.

```bash
python3 tests/testmod/tools/run_h14.py --output build/reports/h14-UNIQUE
python3 tests/testmod/tools/inventory_h14.py
python3 tests/testmod/tools/run_h14.py --negative-control --output build/reports/h14-negative-UNIQUE
```

The positive runner packages both testmods and runs Fabric JAR + NeoForge
sources. `--cases fabric-jar` or `--cases neoforge-source` selects one case;
`--gradle-init` supports the documented HEAD projection of a dirty workspace.
Default timeout is 240 seconds per Gradle process. It requires 38 cases,
36 native transparency chains, two epochs, actual framebuffer resize, normal
shutdown, all 48 PNG dimensions and no `level.dat`.

The separate negative control sets `-PpulseH14SkipResolve` on Fabric only.
It expects the unchanged first Fancy/Fabulous parity guard to fail with
Java 255 / Gradle 1, no result JSON and no PIXEL/SHUTDOWN PASS. Its report says
PASS only when that expected failure is observed. Neither control changes the
pixel oracle or production files.

The canonical suite, ordered-alpha reference, quad, color conversion and RGB
assertion have matching javac structural fingerprints. Only the quad copy's
UVs map the original white texel into the 26.1 atlas; the white sprite is added
to an existing demo model exclusively in this profile. No new production
pipeline/shader/render type is introduced. Full native world/weather/terrain,
baseline and RenderRegression/World bundles remain open, as do the canonical
full flags. See journal section 68 and
[recorded evidence](../../docs/porting/evidence/stage-7.1b.2b.2b.1.json).

## Native H13/H14 world suites (7.1b.2b.2b.2)

`-PpulseWorldRegression` requires an exclusive demo JAR/source profile. It
creates one fresh marked integrated world, waits for the loading screen to
close and the client spectator/30-tick prerequisite, then runs all 12 canonical
WorldRenderRegression cases and 15 H14 world captures. Real water/glass,
early opaque RGB/depth, stone/sky occlusion, live rain, the native five-target
transparency chain, reload and actual framebuffer resize use the existing
Common backend. The completed offscreen/Smoke/animation suites do not run.

```bash
python3 tests/testmod/tools/run_world.py --output build/reports/world-UNIQUE
python3 tests/testmod/tools/inventory_world.py
python3 tests/testmod/tools/run_world.py --negative-control --output build/reports/world-negative-UNIQUE
```

Default cases are Fabric JAR and NeoForge sources. `--cases fabric-jar` or
`--cases neoforge-source` selects one; `--gradle-init` accepts the documented
HEAD projection for a dirty workspace. The 240-second timeout applies to each
Gradle process. No dedicated server/EULA file is needed. All existing run/world
markers and refusal of old paths remain enforced. Fifteen world-* PNGs per
client use the shared `h14-offscreen/` output directory (960x640, then 992x656).

The separate Fabric negative control sets `-PpulseWorldSkipOpaque`. It skips
only the test opaque submission and must fail the unchanged early H13 RGB
guard with Java 255 / Gradle 1, no WORLD/SHUTDOWN PASS and no result JSON.
Its report says PASS only when that expected failure is observed.

All five H13 and nine H14 world guard sites are preserved. The two complete
same-frame ordered-alpha oracle methods match by javac structural fingerprint.
MC26 merges legacy Fast/Fancy into Improved Transparency=false; Fabulous uses
true. All 12 canonical H13 scenarios remain, across two distinct native routes.
Native world H13/H14 is complete in these bounds; RenderRegression local,
entity/manager consumers, legacy baseline and full canonical flags remain open.
See journal section 69 and
[recorded evidence](../../docs/porting/evidence/stage-7.1b.2b.2b.2.json).

## RenderRegression and legacy H14 baseline (7.1b.2b.2b.3)

`-PpulseRenderRegression` requires an exclusive demo JAR/source profile. It
creates a fresh marked integrated world, waits for the 30-tick spectator and
closed loading-screen prerequisite, then runs all 41 canonical RenderRegression
cases and the exact two-capture legacy H14 baseline. Earlier world/offscreen,
Smoke, animation and lifecycle suites do not run. No dedicated server/EULA file
is required.

```bash
python3 tests/testmod/tools/run_render.py --output build/reports/render-UNIQUE
python3 tests/testmod/tools/inventory_render.py
python3 tests/testmod/tools/run_render.py --negative-control --output build/reports/render-negative-UNIQUE
```

Default cases are Fabric JAR + NeoForge sources. `--cases` selects one;
`--gradle-init` supports the documented HEAD projection for a dirty workspace.
The runner requires all 41 cases, H18 manager/identity and H15 entity markers,
original baseline reproduction, saved world and normal shutdown. Two baseline
PNGs (64x32) use the shared `h14-offscreen/` output directory. Legacy Fast/Fancy
share Improved Transparency=false; Fabulous uses true.

The actual dispatcher uses MC26 extract/submit/deferred feature execution
within an explicit local boundary. InventoryScreen extracts the real entity
PiP; the native GuiEntityRenderer prepares it, and its actual output is copied
to the independent target for the original colored-pixel oracle. Failure goes
through the native PiP/dispatcher boundary, including production scope discard;
all alpha bytes of the internal target must remain zero after partial submit.
The adapter does not claim that unscoped native submit is an immediate draw.

`--negative-control` sets only `-PpulseRenderSkipMesh` on Fabric, requiring the
unchanged early opaque pixel guard to fail with Java 255 / Gradle 1 and no
render/baseline/shutdown PASS or result JSON. Its report says PASS only for
that expected failure. The ordinary baseline intentionally reproduces old
weather-before-native-chain occlusion; it does not change production routing.

All 31 RenderRegression and one baseline check sites retain exact argument
trees after the declared MC26 adaptations; complete RGB/baseline bodies match.
Full canonical flags remain guarded pending the remaining visibility,
identity/player/performance consolidation. See journal section 70 and
[recorded evidence](../../docs/porting/evidence/stage-7.1b.2b.2b.3.json).

## Native camera visibility and scoped reproduction (7.1b.2b.2c)

`-PpulseVisibilityRegression` requires an exclusive demo source/JAR profile.
It creates a fresh marked integrated world with the original test block at
(11,140,14), player at (10.5,140,10.5), and FIRST_PERSON yaw 0/180, pitch 0.
Each phase waits 30 player ticks, then measures four actual world frames.
Front-away-front repeats after a real resource reload with Improved
Transparency true instead of false. Other regression suites do not run.

```bash
python3 tests/testmod/tools/run_visibility.py --output build/reports/visibility-UNIQUE
python3 tests/testmod/tools/run_visibility.py --negative-control --output build/reports/visibility-negative-UNIQUE
python3 tests/testmod/tools/inventory_visibility.py
```

Default cases are Fabric JAR and NeoForge sources; `--cases` selects one and
`--gradle-init` accepts the documented HEAD projection. The runner requires
24 native yaw/frustum/GPU observations, 48 query scopes, six 960x640 PNGs,
normal shutdown and a saved world per client. Query scopes cover only actual
Pulse mesh flushes; native terrain and fullscreen resolves are excluded. No
foreign samples query may be active. Renderer callbacks are diagnostics.
GPU samples include overdraw/OIT passes and are not unique pixels or timing.

`--negative-control` passes the actual canonical `-PpulseSmokeReproduce` flag
on both loaders. It must observe yaw=180, native frustum=false and GPU=0 in
the first expected-visible phase, then fail with the original
`Original block entity renderer must run` message, Java 255 / Gradle 1 and no
visibility/shutdown PASS or result JSON. Its report says PASS only when this
expected failure occurs. No geometry/guard suppression is used.

The canonical flag is supported only with this visibility profile plus a demo
source/JAR profile. Outside it, the flag still fails before launch. Other full
canonical flags retain their guards. This supersedes the deferred visibility
boundary of the earlier Smoke section; `pulseSmokeFirstPerson` remains a
separate world-prerequisite control. Visibility uses a declared input change
from callback count to actual GPU samples, retaining the original >0, message,
throw body and property mapping. See journal section 71 and
[recorded evidence](../../docs/porting/evidence/stage-7.1b.2b.2c.json).
