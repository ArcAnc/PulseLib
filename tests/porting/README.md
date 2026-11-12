<!--
@author ArcAnc
Created at: 12.11.2026
Copyright (c) 2026
This code is licensed under "Arc's License of Common Sense" (LICENSE.txt).
-->

# Minecraft 26.2 component checks

These fixtures compile explicit, current production components against the actual Minecraft/loader
dependencies. They never replace `compileJava`, change the main source sets, package a mod, remove
renderer classes or use old PulseLib classfiles to satisfy missing types. Outputs live under each
module's ignored `build/porting-core` directory. No world or Minecraft save is opened.

```bash
./gradlew --init-script tests/porting/core-check.gradle :common:checkPortingCore --no-parallel --no-daemon
./gradlew --init-script tests/porting/core-check.gradle :fabric:checkPortingDiscovery :neoforge:checkPortingDiscovery --no-parallel --no-daemon
./gradlew :fabric:validateAccessWidener --no-parallel --no-daemon
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

Do not treat a successful component check as a successful mod build or loader bootstrap. Native
ItemStack Mixin integration, full plugin discovery/registration, manager/controller/Animation Graph
integration, dedicated servers and renderer validation remain separate required checks.
