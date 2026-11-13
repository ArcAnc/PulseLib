<!--
@author ArcAnc
Created at: 12.11.2026
Copyright (c) 2026
This code is licensed under "Arc's License of Common Sense" (LICENSE.txt).
-->

# Minecraft 26.2 component checks

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
