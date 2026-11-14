<!--
@author ArcAnc
Created at: 12.11.2026
Copyright (c) 2026
This code is licensed under "Arc's License of Common Sense" (LICENSE.txt).
-->

# Minecraft 26.2 component checks

Full production Fabric client bootstrap can also be checked explicitly:

```bash
./gradlew --init-script tests/porting/client-check.gradle :fabric:runClient -PportingClientBackend=opengl --no-parallel --no-daemon --console=plain
./gradlew --init-script tests/porting/client-check.gradle :fabric:runClient -PportingClientBackend=vulkan --no-parallel --no-daemon --console=plain
```

Each invocation creates a fresh directory under `fabric/build/porting-client`, uses all production
sources/resources/Mixins and opens no existing save. Quit the test client normally within three minutes;
a timeout is a failed run. This launches the actual game, without a fixture mod or relaxed injections.
Verify the actual backend in the log (Minecraft may fall back), the visible screen and resource-loading
errors. Exit code zero alone is insufficient: native shader loading can fail before a normal shutdown.
Reaching the title screen does not validate world models, player animation, GUI previews or OIT.

These fixtures compile explicit, current production components against the actual Minecraft/loader
dependencies. They never replace `compileJava`, change the main source sets, package a production mod, remove
renderer classes or use old PulseLib classfiles to satisfy missing types. Outputs live under each
module's ignored `build/porting-core` directory. No world or Minecraft save is opened.

```bash
./gradlew --init-script tests/porting/core-check.gradle :common:checkPortingCore --no-parallel --no-daemon
./gradlew --init-script tests/porting/core-check.gradle :fabric:checkPortingDiscovery :neoforge:checkPortingDiscovery --no-parallel --no-daemon
./gradlew :fabric:validateAccessWidener --no-parallel --no-daemon
./gradlew --init-script tests/porting/core-check.gradle :common:checkPortingIntegration --no-parallel --no-daemon
./gradlew --init-script tests/porting/core-check.gradle --init-script tests/porting/fabric-native-check.gradle :fabric:checkPortingNativeItemStack --no-parallel --no-daemon
python3 tests/porting/diagnose.py
```

The final command exports the real inputs of all three production JavaCompile tasks, removes stale
project class outputs from their diagnostic classpaths, and invokes Java 25 on **every** production
source. It returns nonzero when compilation fails. `-XDshould-stop.ifError=FLOW` is a JDK 25 diagnostic
option to reveal attribution errors beyond missing graphics declarations; it is not applied to the
normal build and its output is never used as a test runtime or shipping artifact. Annotation processing
is disabled only in this diagnostic invocation, so it does not validate transformed Mixin behavior.
The ordinary `:common:compileJava` result is recorded independently in the migration journal.

Core checks cover:

- Real vanilla Minecraft 26.2 bootstrap, with a fresh classloader rejecting Minecraft client,
  Blaze3D and PulseLib backend classes.
- The common ServiceLoader contract, using an explicitly test-only server provider.
- Java 25 ASM plugin annotation scanning from streams, directories and archives, including a
  client-only superclass that must not be resolved by scanning.
- Plugin lifecycle phase ordering, one-time phases, failed registration and reload retry.
- Atomic extension registry validation, Identifier/DFU codecs and UUID stream round trips.
- Molang context/variable behavior and four-point Catmull-Rom anchors.
- Production ItemStack identity transfer rules, partial/full splits and concurrent allocation.
  Fixture identity holders implement the real interface; they are not transformed Minecraft ItemStacks.
- Portable geometry ownership, index widths, draw offsets and copied instance transforms.

Discovery checks compile and instantiate the actual Fabric/NeoForge platform and finder classes
against their own loader dependencies, with the same client-class rejection. They do **not** create a
running mod list, launch a dedicated server, execute `PulsePluginManager.registerCommon()` or test a
complete mod artifact. The Common fixture uses ASM 9.10.1, as provided by both selected 26.2 loaders;
Common's standalone runtime graph is not itself a loader runtime.

The two test-package nullability annotations use JSR-305 `@Nonnull`/`@TypeQualifierDefault` because
Minecraft 26.2 removed its former annotations with those names. They are test metadata, have no
Minecraft namespace, and are never included in production.

The integration suite compiles selected real API, plugin, animation, deformer and parser sources
with ordinary Java 25 `javac --release 25 -proc:none -implicit:none`. Source dependencies are resolved
from the current production source tree; only explicitly selected sources are emitted. The task
clears its own fixture output first. It does not use any classfiles from failed compilation, old
PulseLib artifacts or stand-in APIs. A command-line compiler is intentional: in this environment
Gradle's JavaCompile path attributes additional implicit graphics bodies, whereas standalone javac
on the same inputs succeeds. Failures of either invocation are recorded in the migration journal;
production `compileJava` is unchanged. Argument/source lists are under `build/porting-core`.

Integration checks initialize the actual built-in registries and exercise `PulsePluginManager` with
test candidates: side filtering before class loading, deterministic ordering, deduplication, common
phase order, applied registrations, closed/wrong-phase scopes, duplicate failure retention and client
registration rejection. They also test actual manager/controller lazy initialization and owner isolation
without a bound model. The fresh classloader still rejects Minecraft client and Blaze3D classes.

The native Fabric fixture is an **opt-in separate test mod**, not PulseLib's distributable artifact.
Its JAR contains selected core implementations, the unchanged ItemStackMixin, actual Fabric entrypoint,
platform and finder sources, and test-only item/plugin content. It runs KnotServer with the real
Minecraft 26.2 / Fabric Loader / Fabric API dependencies in a fresh directory under `fabric/build`.
The actual Fabric finder scans loaded mod roots; the actual manager performs common registration.
The production SERVER_STARTING callback is not dispatched: the fixture calls `registerCommon()`
explicitly and exits before a server/world is created. A strict success marker and absence of world
state are checked; an exception or silent server exit cannot pass the task.

A fixture-only Mixin config applies the **unchanged** ItemStackMixin on the server for this test.
Production configs keep it client-only. Real Minecraft ItemStacks test copy identity, partial/full
splits, nonanimated and EMPTY behavior. Animated fixture items throw if manager/controller creation
is attempted. Minecraft 26.2 binds item prototype components later during resource loading; the test
explicitly binds components only for its own two items so datapack/world startup is unnecessary.
Vanilla item prototypes and user saves are not modified. The fixture has no publishing task and is
never added to production source sets, standard build, CI, jarJar or include.

Successful fixtures do not establish a complete production mod build, full dedicated server startup,
NeoForge FML integration, client-only Mixin loading, Animation Graph playback against baked models,
resource loading, or graphics correctness. Those remain separate required checks.

NeoForge dedicated bootstrap (requires the core init script first):

```bash
./gradlew --init-script tests/porting/core-check.gradle --init-script tests/porting/neoforge-bootstrap-check.gradle :neoforge:checkPortingBootstrap --no-parallel --no-daemon --console=plain
```

This separate selected-core fixture runs real FML 11.0.16 dedicated startup and the real
`ServerModLoader.load(false)`. FML instantiates the production `PLib` constructor, delivers common,
dedicated and load-complete events, and executes the original `CommonEvents` deferred queue.
The actual NeoForge finder scans real mod annotation data, including a Java 25 CLIENT candidate
with a Minecraft client superclass that must never load on a server. The actual manager registers
the SERVER fixture plugin once. No fake ModList, lifecycle event, event bus or registry is supplied.

Production Common/NeoForge Mixin configs, AT and service descriptors are included unchanged in the
fixture. Its extra mod metadata and assertions are test-only; this JAR is not a distribution.
The launcher uses FML's dedicated content loader, then closes it before MinecraftServer/world
creation. Success requires a marker in the real log, absence of world state, positive loading of
production bootstrap classes, and zero Minecraft client, Blaze3D or PulseLib GL/legacy backend
classes in the JVM class-loading log. Only a fresh directory under `neoforge/build/porting-core`
is used, with a two-minute timeout. EULA acceptance, world ticks, datapack loading and graphics are
outside this check.

The production NeoForge entrypoint now resolves an internal client bootstrap service only inside
its existing physical-client branch. That provider calls the same `ClientEvents.registerClientEvents`
method as before; its resource descriptor is mandatory and uses the existing strict PulseServices
contract. The server fixture intentionally does not package the client implementation, so a server
attempt to resolve it would fail. Production source sets still include it. Actual client execution
of the provider remains part of client/render integration validation.

## Opt-in RHI checks (stage 3)

```bash
./gradlew --init-script tests/porting/rhi-check.gradle :common:checkPortingRhiLayout --no-parallel --no-daemon --console=plain
./gradlew --init-script tests/porting/rhi-check.gradle :neoforge:checkPortingRhiOpenGL :neoforge:checkPortingRhiVulkan --no-parallel --no-daemon --console=plain
```

These tasks compile the canonical render-plan sources, the Common RHI package, the actual canonical
`PGlDynamicGeometry`/`GlDynamicGeometry` CPU sources and test-only fixtures against real 26.2
dependencies, without stale production class outputs. They do
not alter production compilation or join the normal CI/build/publishing task graph. The GPU fixture
uses a real FML client content loader and a hidden GLFW window; it never constructs a Minecraft
instance, loads a world or accesses user saves. Each invocation gets a new directory under
`neoforge/build/porting-rhi` and a two-minute process timeout.

The CPU check covers std140 bytes, light/overlay/deformer fields, full-block padding, zero and
511/512/513/1025 boundaries, independent packed ranges, multiple-group order and 64/256 alignment.
GPU checks execute both explicit packed plans and actual `PFrameCompiler` output, read back 1025
unique pixels, and verify nonzero `firstInstance`/`firstIndex`, negative valid `baseVertex`, frame
matrices, three-slot upload reuse, cutout textures/samplers, ushort/uint indices, dynamic updates,
static caching and resource ownership. Invalid ranges and unsupported OIT fail explicitly.

The optional `PGeometrySource` contract joins existing CPU deformation sources to RHI without changing
the `PDynamicGeometry` marker. Canonical facade/native aliases share one GPU allocation; native
`RenderType` identities retain independent ordinary/OIT policy bindings. Checks cover canonical source
updates through `PFrameCompiler`, failed close listeners, multiple registries, value-equivalent release
handles and stale native bindings after cleanup. Listener failures are intentional assertions and must
not prevent unrelated GPU owners from releasing buffers. These checks still do not execute production
deformation algorithms or select RHI in PRenderQueue.

`PRhiNativeMaterial` now provides a frame-driven native material adapter. The GPU fixture supplies real
`PreparedRenderType` records and GPU resources to its explicit offscreen preparation/output callbacks;
it does not call `RenderType.prepare()` in a Minecraft game instance. It verifies one immutable pass
snapshot per pipeline binding/execution across 1025 instances, Sampler0/1/2, scissor, target/texture
switches, native Fog/Lighting/Globals buffer changes, and missing/duplicate texture rejection. The
default game preparation/output resolver is compiled but awaits in-game validation. Model-view and
projection remain owned by `PRenderFrame`; vanilla layering/animated texture transforms are outside
this PulseLib adapter's contract. No production pipelines/shaders or queue selection are replaced.

The RHI fixture also compiles and executes all four production `PRhiPipelines` profiles (lit/emissive,
solid/cutout) using actual production shader resources, the native GLSL import preprocessor and real
`PBakedMesh`/`PMeshPrimitive` records. A 24-byte vertex layout feeds 1025 instances through the canonical
compiler and renderer. Readback verifies UVs, packed color/light/overlay, FORCE_OPAQUE versus cutoff,
normal changes, back-facing triangles and uint dynamic geometry. Unsupported nonzero GPU deformer
counts and shader/CPU capacity mismatches fail explicitly; full deformer/shader families remain pending.

For these tests only, the fixture JAR includes unmodified vanilla shader imports from the resolved
Minecraft compile artifact: FML class loading does not provide the game's ResourceManager asset view.
Production and vanilla asset bytes are checked against their sources. The normal loader resource
tasks package the new production vertex shader and unchanged canonical fragment shader for both
loaders. These GPU runs do not execute complete production client initialization or in-game world rendering.

The production client lifecycle now registers the shared `PRhiPipelines.portable()` definitions through
loader-local helpers. To check the Fabric helper in the actual Knot CLIENT game loader, with the
unmodified production access widener, run:

```bash
./gradlew --init-script tests/porting/rhi-check.gradle --init-script tests/porting/rhi-loader-check.gradle :fabric:checkPortingRhiRegistration :fabric:validateAccessWidener --no-parallel --no-daemon --console=plain
```

This isolated fixture checks four new entries in the real native RenderPipelines registry, shared
object identity, and game-class-loader ownership. It stops after Knot initialization, before launching
Minecraft. The normal RHI GPU fixture also posts a real NeoForge RegisterRenderPipelinesEvent through
a native mod event bus to the production helper. Neither fixture executes the complete production
client entrypoint. Use `-PportingRhiOpenGLCapacity=128` to run OpenGL readback with the same shared
portable definitions used by registration; Vulkan's default 128 already uses those definitions.

The default block holds 128 records of 112 bytes (14336 bytes), below the portable 16 KiB uniform
block limit. OpenGL's fixture queries its actual block limit and tests 512 records. Native 26.2
`GpuDevice` exposes uniform offset alignment, but not the maximum uniform buffer range. Vulkan's
fixture therefore defaults to the portable limit/capacity. On hardware whose range has been
independently verified (for example with `vulkaninfo`), also run:

```bash
./gradlew --init-script tests/porting/rhi-check.gradle :neoforge:checkPortingRhiVulkan -PportingRhiVulkanCapacity=512 -PportingRhiVulkanUniformLimit=65536 --no-parallel --no-daemon --console=plain
```

LWJGL is confined to fixture device/window bootstrap and the OpenGL limit query. The production RHI
package uses native Blaze3D abstractions only. Both backends were tested on one GTX 1660 Ti; this
is not a claim of full Minecraft/PulseLib Vulkan support. Remaining shader families, world/GUI integration,
deformers, four-layer OIT/Fabulous and Fabric runtime remain pending. Vulkan validation layers and
long-running GPU memory profiling were not enabled. See the migration journal and stage 3 evidence.

The automated production scene launches a separate test mod alongside the complete PulseLib client:

```bash
./gradlew --init-script tests/porting/scene-check.gradle :fabric:runClient --no-parallel --no-daemon --console=plain
./gradlew --init-script tests/porting/scene-check.gradle :neoforge:runClient -PportingSceneLoader=neoforge --no-parallel --no-daemon --console=plain
```

Use a clean checkout for NeoForge if local draft package metadata duplicates Common. The init script
changes no production source set or Mixin requirement. It creates a fresh `build/porting-scene/client-*`
directory, installs only the separate scene JAR and requires an explicit `PASS` result after normal
Minecraft shutdown; exit code zero without a result fails. No existing save is opened or created.
The default backend is OpenGL; `-PportingSceneBackend=vulkan` requests and requires actual Vulkan.
The test waits for native resource loading and renders offscreen
inside GameRenderer with native TextureManager, lighting/overlay/UI lightmap and default RenderType
preparation. Production queue/compiler/host/backend are called without material/output callbacks.

For each lit/emissive solid/cutout profile, the scene checks 1025 mixed portable/baked-mesh instance
pixels, visible/discarded alpha, dynamic geometry updates and close, queue cleanup/recreation and
borrowed target lifetime. Non-cancelling test-only Mixins require 20 RHI executions and zero legacy
executions. PNG readbacks and `pulselib-scene-result.txt` remain in the test directory. This is a basic
renderer acceptance test; it does not validate world stages, OIT/Fabulous, GPU deformation, GUI scopes,
player integration or full Vulkan support. Scene content is never packaged in production Common.

Canonical shader and deformer checks can be added explicitly:

```bash
./gradlew --init-script tests/porting/scene-check.gradle :fabric:runClient -PportingSceneShaders=true -PportingSceneBackend=opengl --no-parallel --no-daemon --console=plain
./gradlew --init-script tests/porting/scene-check.gradle :fabric:runClient -PportingSceneShaders=true -PportingSceneBackend=vulkan --no-parallel --no-daemon --console=plain
```

These checks also support `:neoforge:runClient -PportingSceneLoader=neoforge`. They use the canonical
solid lit/emissive pipelines with `PRhiNativeMaterial`, `PRhiDrawExecutor` and actual native preparation;
they do not substitute the basic four RHI shader profiles. Production GPU deformer streams supply
stretch/twist/bend operations with nonzero value offsets. CPU positions and finite-difference normals
are packed into reference geometry and compared with GPU image readbacks. Coverage differences must
stay within 8 pixels, RGB differences within 5/255 and mean RGB error within 1/255; undeformed coverage
must differ by more than 50 pixels, so ignoring the operations cannot pass. Two additional 513-instance
CPU/GPU comparisons require byte-identical results across portable 128-record UBO chunks. There are
22 additional RHI executions. OpenGL also calls the isolated compatibility executor directly for four
1025-instance profiles with two meshes, arena offsets, packed instance ranges and a zero-index command,
and requires four additional legacy executions. The default queue test still
requires exactly 20 RHI and zero legacy executions.

This validates shader transport and math through real RHI draws. It does not accept automatic queue
selection for GPU-deformed plans, all seven operations, composed stacks/masks, CPU fallback/subdivision,
immediate rendering, transparent/OIT passes, Fabulous, or full Vulkan compatibility. The additional
production queue suite below checks queue selection, immediate, all operations and subdivision;
OIT/Fabulous and full Vulkan compatibility remain pending. Catalog compilation alone is insufficient.

NeoForge/FML 11.0.16 with its default early window failed Vulkan surface creation in the tested setup:
GLFW requires a window created with `GLFW_NO_API`, while FML's early display creates an OpenGL window.
The init script accepts `-PportingSceneEarlyWindow=false` for a separate run using FML's native
`earlyWindowControl = false` setting. It writes only the new test directory's `config/fml.toml`;
production configuration, source sets and user directories remain unchanged. The backend guard still
requires actual Vulkan. A passing run with that setting does not make the default startup pass.

The remaining stage 4 production queue/immediate checks are enabled with `-PportingSceneQueue=true`:

```bash
./gradlew --init-script tests/porting/scene-check.gradle :fabric:runClient -PportingSceneQueue=true -PportingSceneShaders=true -PportingSceneBackend=opengl --no-parallel --no-daemon --console=plain
./gradlew --init-script tests/porting/scene-check.gradle :fabric:runClient -PportingSceneQueue=true -PportingSceneShaders=true -PportingSceneBackend=vulkan --no-parallel --no-daemon --console=plain
```

For NeoForge use `:neoforge:runClient -PportingSceneLoader=neoforge`; the existing explicit
`-PportingSceneEarlyWindow=false` is required for the tested FML 11.0.16 Vulkan bootstrap.
`QueueChecks` uses public `PRenderQueue.submit/flushOpaque/drawImmediate` only. Native preparation,
canonical pipeline identities and nonzero deformation headers are observed by non-cancelling test
Mixins. It does not construct an executor or supply prepared materials, target callbacks or replacement
shaders. The reference path uses actual `PDeformedMeshBuffers` and the same subdivided source geometry.

There are 44 queue cases: seven deformer operations, a composed stack, the eight-operation GPU limit,
a nine-operation CPU fallback and an unsupported custom deformer CPU fallback, each on four lit/emissive
solid/cutout profiles with subdivision levels 0/1/2. Hinge tests include vertices on both sides of its
mask. Each case compares undeformed, CPU reference and production deformation readbacks and requires
changed coverage, coverage difference <=8 pixels, maximum RGB difference <=5/255 and mean <=1/255.
A further 513-instance test alternates sampled values, verifies distinct per-instance value offsets,
subdivision and portable UBO chunk boundaries, and requires byte-identical CPU/GPU output.

Public drawImmediate covers twelve ordinary/instant lit/emissive solid/cutout/translucent pipelines,
CPU/GPU normal lighting, separate frame/instance transforms, native alpha and transparent-texture
cutout/discard. Additional draws prove that an offscreen instance transform is applied and that a
translucent primitive does not write reversed-Z depth. This adds exactly 185 RHI executions, including
61 plans with nonzero deformation counts, and no legacy execution. With both optional suites enabled,
total dispatch must be RHI=227; OpenGL also has the four explicitly invoked compatibility checks,
while Vulkan requires legacy=0. These are executor entries, not GPU draw-call counts.

This accepts basic shaders, deformation and public immediate submission. It does not accept the
four-layer OIT/Fabulous algorithm, world transparency integration, nested GUI/PIP state restoration,
player animation rendering, stress reload/resize, resource leaks or performance. Those retain their
separate migration stages. Default NeoForge Vulkan early-window startup remains a recorded failure;
the test-only native setting is never written to production or user configuration.
