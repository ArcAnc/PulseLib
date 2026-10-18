# PulseLib cross-platform demo and smoke tests

This opt-in external mod is built from the original NeoForge examples: TestBlock, TestBlockEntity/Renderer, TestEntity/Render, TestBlockItem/Renderer, TestArmor, TestTailItem, PTestArmor, TestDayTimeColor, PPlayerBallDemo and PPlayerAcrobaticDemo. Their implementations live once in src/main/java; small Fabric/NeoForge adapters handle vanilla registries, renderer factories, attributes and keys. No demo classes/assets are included in release PulseLib JARs.

These game/demo checks are run manually. Standard CI only runs `./gradlew build --no-daemon` for the library and JUnit tests; it does not build or launch this mod, accept Minecraft's EULA or create worlds.

Original GLB/glTF/Gecko assets were unpacked from the library's resource ZIPs without changing their geometry or animation. The acrobatic skin texture was recovered from the existing Blockbench player template. Small vanilla item/blockstate descriptors make the previously disabled game objects usable on 1.21.1. The synthetic triangle/cube/items of stage 1 were removed.

CommonTestPlugin exercises all three type registrars using existing built-in implementations. ClientTestPlugin contributes models, armor/tail attachments, both player demos and an observation callback on the ball's animated hand anchor through @PulsePlugin alone. No PulseLib entrypoints or event registrations are used. Loader initializers only register Minecraft content and ordinary renderer/key/tick callbacks.

## Run in development

```sh
./gradlew :neoforge:runServer -PpulseTestMod -PpulseSmokeAuto
./gradlew :fabric:runServer -PpulseTestMod -PpulseSmokeAuto
./gradlew :neoforge:runClient -PpulseTestMod -PpulseSmokeAuto
./gradlew :fabric:runClient -PpulseTestMod -PpulseSmokeAuto
```

Accept Minecraft's EULA in each disposable server run directory. Server assertions check one construction, channels/events/deformers registered once, one common plugin and no CLIENT class initialization. Clients load original models, sample the animation-only Gecko player rig, draw eight GPU-deformed instances of the original item pyramid, verify solid/OIT pixels, compare indexed blend/depth/scissor state and verify actual GPU buffer deletion. Automatic smoke tests perform one pass and exit after explicit cleanup. With pulseSmokeWorld they additionally disconnect through the actual loader lifecycle and verify cleanup independently.

## Discover an installed JAR

Build first, then launch in a separate Gradle invocation so Loom resolves fresh file contents:

```sh
./gradlew :neoforge:testmodJar :fabric:testmodJar
./gradlew :neoforge:runServer -PpulseTestJar -PpulseSmokeAuto
./gradlew :fabric:runServer -PpulseTestJar -PpulseSmokeAuto
./gradlew :neoforge:runClient -PpulseTestJar -PpulseSmokeAuto
./gradlew :fabric:runClient -PpulseTestJar -PpulseSmokeAuto
```

Production Fabric uses :fabric:remapTestmodJar (pulselib-testmod-intermediary-1.2.0-fabric.jar), the remapped PulseLib JAR and Fabric API. Its regular testmodJar uses named development mappings. NeoForge's fixture JAR needs no remap. The demo mod version in loader metadata is an internal fixture version independent of library publication.

## In-world checks and manual examples

Automated launches use a fresh, marked run directory under `<loader>/build/pulselib-tests/<UUID>`. With `pulseSmokeWorld`, the harness creates a new, seeded `pulselib-smoke` world itself; no manual save preparation or Quick Play is used. Existing saves are never adopted, overwritten, restored or deleted. The client refuses a missing/mismatched run marker, an unexpected world path or a missing disposable-world marker before scene changes. Both `pulseTestMod` and `pulseTestJar` are supported; automation without either fails during Gradle configuration. Normal manual `runClient` and demo launches keep their existing directories and behavior.

```sh
./gradlew :neoforge:runClient -PpulseTestJar -PpulseSmokeAuto -PpulseSmokeWorld
./gradlew :fabric:runClient -PpulseTestJar -PpulseSmokeAuto -PpulseSmokeWorld
```

Both clients exercise the block/entity/item renderers, armor adapter and hiding, living/tail attachments, ball-toss player animation and animated hand callback in a single world. Probe Mixins verify block-entity → particles → weather hook order. The fixture prepares a level platform and fixes the camera, waits for server/client BlockEntity creation, verifies registry/renderer mapping, and counts actual calls for that block. It tests explicit `PulseRuntime.disconnected()` first. It then repopulates managers and GPU resources, performs a real disconnect without calling cleanup itself, and checks geometry/instance/indirect/deformer buffer IDs, OIT textures/FBO, deformation caches and instance/singleton/player managers. Require separate `PULSE TEST EXPLICIT CLEANUP PASS` and `PULSE TEST NATURAL DISCONNECT PASS` markers.

Add -PpulseGeckoDemo to select the existing Gecko entity/armor resources instead of their glTF variants. The item remains glTF, so both parsers still run. Equivalent direct-launch VM properties are pulselib.test.auto, pulselib.test.world and pulselib.test.geckoEntity.

Without pulseSmokeAuto, the mod is a manual demo. Give pulselibtest:test_block, test_hat, test_chest, test_leggings or test_tail; summon pulselibtest:test_entity. Right-click the block to toggle its random animation and the entity to toggle PTestArmor. V plays the original ball toss; B plays acrobatics with knee deformation. Tail controllers are cleared when client worlds change.

PASS markers: PULSE TEST SERVER PASS, PULSE TEST WORLD PASS, PULSE TEST CLIENT PASS. Always require the marker as well as a successful process exit: loaders can catch a server exception while Gradle exits successfully. Diagnostic reflection is confined to the smoke harness; library runtime handlers do no scanning/reflection.

## H14 Fabulous OIT regression

The opt-in GPU fixture renders real PulseLib meshes through the queue/compiler/executor and Minecraft 1.21.1's actual `transparency.json` chain. It checks final RGB pixels against an independently sorted alpha reference, rather than checking only nonempty OIT textures. Its opaque half-plane is at depth 0.5 and sky at 1.0. It covers front/behind occlusion, crossing surfaces, all four OIT layers, lit and emissive meshes, all five vanilla translucent targets on either side, interleaved targets and an opaque particle. It also asserts caller framebuffer/viewport/program/VAO/buffer/texture/depth/indexed blend/scissor state, depth attachments and exception restoration.

```sh
./gradlew :fabric:runClient -PpulseTestMod -PpulseOitTest --no-daemon
./gradlew :neoforge:runClient -PpulseTestMod -PpulseOitTest --no-daemon
```

This mode performs 38 cases, including one explicit resource reload (equivalent to F3+T) and one window resize, then exits. These operations are confined to this regression mode. Ordinary demos and smoke tests do not repeatedly reload resources or switch worlds. Minecraft is never added to normal CI.

For the real `LevelRenderer.renderLevel` integration test, the harness creates a fresh **disposable** save in its isolated run directory. The following commands alter only that marked test save: the harness clears a small volume, builds a stone wall, moves the player to spectator mode, fixes the camera, selects Fancy/Fabulous and enables rain. It compares final world pixels with the same-frame native color/depth targets, saves screenshots with and without front/behind surfaces, and then runs the 38 offscreen cases. It loads only one world. Scene preparation requires the expected path and both run/world markers; it refuses existing or unmarked saves.

```sh
./gradlew :fabric:runClient -PpulseTestMod -PpulseOitTest -PpulseSmokeWorld --no-daemon
./gradlew :neoforge:runClient -PpulseTestMod -PpulseOitTest -PpulseSmokeWorld --no-daemon
```

Require `H14 WORLD PIXEL PASS`, `H14 WEATHER PIXEL PASS`, `H14 PIXEL PASS` and successful exit. Output PNGs are in `<loader>/build/reports/h14`. The harness restores window size, graphics mode, camera mode and HUD settings on successful completion; the disposable scene and weather remain in the test save. Do not open an important save with these flags.

Adding `-PpulseOitBaseline` to the commands **without** `pulseSmokeWorld` deliberately replays the former weather-target routing, asserts the known H14 disappearance and writes `fancy-before.png` / `fabulous-before.png`. It is a diagnostic reproduction, not a successful fix regression; its marker is `H14 BASELINE REPRODUCED`.

Native target fixtures make particle/depth overlap deterministic. The world fixture exercises actual clouds and rain; it does not make a claim about shader mods or every particle renderer. Repeat visual checks with those configurations separately.

## Smoke negative controls

These are intentionally failing opt-in game tests. They must exit unsuccessfully with the expected assertion; they never belong in mandatory CI.

```sh
./gradlew :neoforge:runClient -PpulseTestMod -PpulseSmokeAuto -PpulseSmokeWorld -PpulseSmokeReproduce --no-daemon
./gradlew :fabric:runClient -PpulseTestMod -PpulseSmokeAuto -PpulseSmokeWorld -PpulseSmokeSkipHook --no-daemon
./gradlew :neoforge:runClient -PpulseTestMod -PpulseSmokeAuto -PpulseSmokeWorld -PpulseSmokeSkipHook --no-daemon
```

`pulseSmokeReproduce` points the camera away from the existing, registered fixture block and reproduces `Original block entity renderer must run`. The BER probe reports the block state, renderer, call count, camera and frustum visibility. Normal smoke uses a visible fixed scene.

`pulseSmokeSkipHook` cancels loader cleanup only during natural disconnect. Explicit cleanup remains enabled. The natural resource assertion must fail on retained GPU resources; a genuinely missing callback also fails the hook-observation deadline. This prevents manual cleanup from making broken lifecycle integration pass.

## H14 GPU profiling

```sh
./gradlew :fabric:runClient -PpulseTestMod -PpulseOitPerf --no-daemon
./gradlew :neoforge:runClient -PpulseTestMod -PpulseOitPerf --no-daemon
./gradlew :fabric:runClient -PpulseTestMod -PpulseOitPerf -PpulseSmokeWorld --no-daemon
./gradlew :neoforge:runClient -PpulseTestMod -PpulseOitPerf -PpulseSmokeWorld --no-daemon
```

Profiling hides its disposable window while retaining the live GL context; this prevents manual input from replacing a measured scene. Ordinary manual/demo windows are unaffected. Profiling disables pause-on-lost-focus only in the disposable run.

The offscreen series measures the existing H14 quad/queue/executor and actual vanilla transparency chain at 1920×1080, 2560×1440 and 3840×2160. Fancy/Fabulous each have 0/1/16/128 overlapping quads, with 32 warmup and 80 measured frames per scenario. Resources persist across frames. A following empty frame must execute zero OIT depth/metadata/fullscreen passes, even with retained allocations. `gpu-performance.csv` contains GPU frame mean/p95, depth, accumulation, metadata, compositor and vanilla-chain intervals; texture payloads, driver NVX VRAM delta (or -1 when unsupported), fullscreen counts, texture allocations and buffer-storage calls are separate fields.

The world series uses a fresh marked world, fixed camera/stone fixture, six 1080p scenarios and 64 warmup + 80 measured frames. `world-gpu-performance.csv` measures the entire `GameRenderer.render` GPU interval, including terrain/UI rendering. Higher resolutions are offscreen fixtures, not claims of full world/presentation testing on a larger monitor.

Test-only Mixins issue paired OpenGL/ARB timer timestamps. Nested passes use separate timestamp pairs; results are collected asynchronously only after `GL_QUERY_RESULT_AVAILABLE`. No CPU Java-call duration, glFinish or pixel readback is substituted for GPU execution time. Texture footprint is measured from allocated channel sizes; NVX available-memory delta is a global driver counter and includes allocations other than OIT. Query overhead, CPU submission gaps and background GPU work can affect elapsed timestamps, so compare repeated controlled runs before applying an optimization.

Require `H14 GPU PERFORMANCE PASS` / `H14 WORLD GPU PERFORMANCE PASS` and successful exit. CSV output is in `<loader>/build/reports/h14`. Current results and limitations are recorded in the stabilization report. Functional H14 pixel regressions remain separate and mandatory before any OIT optimization.

## Player visual Root Motion and animated camera (M-P2 / M-P3)

```sh
./gradlew :fabric:runClient -PpulseTestMod -PpulsePlayerMotionTest -PpulseMotionPhase=mp2 --no-daemon
./gradlew :neoforge:runClient -PpulseTestMod -PpulsePlayerMotionTest -PpulseMotionPhase=mp2 --no-daemon
./gradlew :fabric:runClient -PpulseTestMod -PpulsePlayerMotionTest -PpulseMotionPhase=all --no-daemon
./gradlew :neoforge:runClient -PpulseTestMod -PpulsePlayerMotionTest -PpulseMotionPhase=all --no-daemon
```

This opt-in mode creates a fresh marked disposable world and hides its window to prevent manual demo keys from changing a controlled scenario. It loads equivalent Gecko and embedded, indexed glTF group/mesh rigs. Matrix probes observe actual custom-anchor stacks and entity/first-person mesh submissions. A–D run with all five blend modes and three weights, E runs two active definitions, and the suite repeats after one resource reload. ROOT masks, part/bone weights and intermediate crossfade frames are checked. Physics snapshots bracket the animation tick and render/extraction APIs. Two client observer actors exercise separate player instances and native world rendering; an actual integrated-server respawn checks player replacement. These observers are not networked multiplayer clients.

`pulseMotionPhase=all` additionally checks F/G/H against the actual `Camera.setup` position and quaternion, plus I with unchanged VANILLA camera position/rotation. The independent oracle includes the definition's first-person activation during transitions. Gecko/glTF axes, inherited ROOT/pivot, nonzero camera bind offsets and signed roll are included. Mesh/anchor commands remain in the shared camera-relative VIEW frame. `PPlayerCameraTest` separately covers rotated/scaled intermediate camera parents, rotated bind frames and rootless rigs without launching Minecraft.

Require `PLAYER MOTION M-P2 PASS`, plus `PLAYER MOTION M-P3 PASS` for `all`, **and** normal successful process completion. A PASS followed by a stalled JVM shutdown is only assertion evidence, not a fully successful game run. Current verification and remaining limitations are recorded in `docs/reviews/player-motion-camera-regressions-2026-10-13.md`. This mode is never part of mandatory CI.


## ItemStack runtime animation identity

```sh
./gradlew :fabric:runClient -PpulseTestMod -PpulseItemIdentityTest --no-daemon
./gradlew :neoforge:runClient -PpulseTestMod -PpulseItemIdentityTest --no-daemon
```

This opt-in mode uses the existing external mod, creates a marked disposable world and hides its window. It runs all sixteen ItemStack regressions against the actual loader-transformed class, including eight concurrent readers/copy callers, 100,000 copies with unchanged manager count, actual tool damage, NBT/network round trips and vanilla merging. It checks inherited identity before first rendering, the deliberate shared identity of separate copies, separate managers/controllers/phases, and preservation of a temporary copy's phase. JUnit independently checks monotonic/concurrent allocation and exhaustion; it does not replace these transformed-stack tests.

The world fixture uses Survival so the vanilla inventory screen keeps its normal slot numbering. Actual client menu actions and click packets exercise hotbar transfers, both hands, partial/full splitting, merging, inventory reopening, a server-opened chest, throwing and vanilla pickup. It then disconnects through the loader lifecycle, checks an empty manager cache and collectable manager/controllers, and reopens only its own marked disposable save. The runtime ID survives cache cleanup on a retained Java object, while deserialization creates a new identity.

Require all `ITEM IDENTITY REGRESSION 1` through `16 PASS`, `ITEM IDENTITY REGRESSIONS PASS`, `ITEM IDENTITY WORLD OPERATIONS PASS`, `ITEM IDENTITY REENTRY PASS`, `ITEM IDENTITY CLIENT PASS`, and successful process completion. The copy benchmark is diagnostic wall-clock time, not an allocation profile or a comparison to uninstrumented Minecraft. Client prediction is checked at each operation; network replacement can still create a new Java stack/identity. No assertion promises persistent identity across packets or logical copies.

## L1 overlay lifecycle / L4 RESTART snapshot

The existing opt-in client smoke now also samples the original entity rig's `idle` and `walk` clips with a graph interrupted at 40%. It compares every bone's translation, quaternion rotation, nonuniform scale and visibility immediately across RESTART, with forward, reverse and paused overlays. Reverse starts at the loaded clip end and its cursor must decrease. It repeats an overlay trigger 1000 times, checks the runtime instance identity/count, cancels a paused overlay with fade-out, and verifies cleanup. This runs after resources load on the actual client; the model parsers and existing graphics smoke remain in use.

```sh
./gradlew :fabric:runClient -PpulseTestMod -PpulseSmokeAuto --no-daemon
./gradlew :neoforge:runClient -PpulseTestMod -PpulseSmokeAuto --no-daemon
./gradlew :fabric:runClient -PpulseTestMod -PpulseSmokeAuto -PpulseGeckoDemo --no-daemon
./gradlew :neoforge:runClient -PpulseTestMod -PpulseSmokeAuto -PpulseGeckoDemo --no-daemon
```

Require `PULSE TEST L1/L4 PASS`, `PULSE TEST CLIENT PASS` and successful process exit. The default entity uses glTF; `pulseGeckoDemo` selects the equivalent Gecko rig. These checks are independent of the JUnit numeric regressions and stay outside mandatory CI. They sample resolved transformations on loaded game models; they do not assert interruption screenshots or exercise multiplayer.

## H18 manager access / H13 world depth / H15 GUI and offscreen

The existing opt-in smoke now includes these regressions (no new CI or launch flags):

```sh
./gradlew :fabric:runClient -PpulseTestMod -PpulseSmokeAuto -PpulseSmokeWorld --no-daemon
./gradlew :neoforge:runClient -PpulseTestMod -PpulseSmokeAuto -PpulseSmokeWorld --no-daemon
```

After the original world smoke assertions, the harness modifies only its marked disposable save to build a sealed water tank and a blue stained-glass wall. Twelve cases use the real loader stages and actual terrain in Fast/Fancy/Fabulous. They read opaque entity-queue RGB and depth immediately after `afterBlockEntities`, then verify front/behind tinting in the final world pixels. The fixture restores graphics mode, camera mode and HUD settings before cleanup. The tank/wall remain only in the disposable save.

The additional 41 GPU cases cover early opaque depth, 32 instances of one mesh, lit/emissive materials, crossing transparency, combined Entity/BlockEntity alpha sorting, orthographic GUI depth, separate world/GUI queues, a successful nested framebuffer render, nested exceptions, and nondefault indexed blend/depth/scissor state. They also render the original Entity model through actual `EntityRenderDispatcher` and `InventoryScreen` entry points with opaque and translucent materials, and throw from the real renderer after a partial submission to verify discard and restoration. Offscreen/GUI transparency uses sorted alpha and does not touch world OIT accumulation.

H18 checks actual Entity/BlockEntity field initializers, automatic instance/singleton selection, explicit factory overrides, stable instance ownership, initialized controllers and ItemStack helper access with independent runtime IDs and intentional copy identity. JUnit separately checks generic non-Item singleton owners, equal keys across equal/unequal owner types, direct manager constructors and independent graph runtimes. The existing ItemStack identity and Animation Graph regressions retain their expectations.

TestBlockItem, TestEntity and TestBlockEntity use the same owner pattern:

```java
private final PAnimationManager<MyAnimatable> animationManager =
		PLibHelper.createManager(this);

@Override
public PAnimationManager<MyAnimatable> getAnimationManager(AnimManagerKey key)
{
	return this.animationManager.getManager(key);
}
```

The automatic factory selects an instance manager for Entity/BlockEntity and a singleton manager for other owners. Custom owners can choose `PLibHelper.createInstanceManager(this)` or `PLibHelper.createSingletonManager(this)` explicitly; the boolean overload remains available. Creation does not invoke owner callbacks. The keyed getter runs after owner construction and initializes each context once. Singleton contexts stay in the shared owner-identity/key cache, so the item's field does not share one animation state among all stacks. External code uses `PLibHelper.getManager(owner)`, `getManager(owner, key)` or `getManager(stack)`. Existing direct instance getters and `SingletonAnimationManager.getManager(key, owner)` continue to work. A plain custom object with an instance lifetime should use the explicit instance factory.

Require `H13 WORLD PIXEL PASS`, `H18 GAME PASS`, `H15 ENTITY PIXEL PASS`, `H13/H15 PIXEL PASS`, `PULSE TEST EXPLICIT CLEANUP PASS`, `PULSE TEST NATURAL DISCONNECT PASS`, `PULSE TEST CLIENT PASS`, and successful process exit. Without `pulseSmokeWorld`, smoke runs the synthetic GPU cases but skips real-world/Entity checks. H14 remains a separate mandatory regression with its unchanged 38-case expectations.

Automated clients also use synchronous OpenGL debug callbacks and fail on any GL API error at the next safe frame boundary, retaining the original Java call stack. This applies only to disposable automated runs. It prevents the observed NVIDIA shutdown deadlock between a driver callback's JNI detach and VM exit; errors are asserted rather than suppressed. Arena allocation invalidates Minecraft's VAO binding cache before the next vanilla draw. No forced Minecraft exit is added.

## Animation sampling and instance lifetime (M-A3 / M-A4 / H2 / H1 / L25)

`pulseSmokeAuto` checks loaded Gecko 360°/720° intermediate poses, fixed uniform
Catmull-Rom reference samples and independent Molang frame contexts. JUnit also
checks a decoded glTF rotation track against its original quaternion SLERP.

For the visible multi-turn Gecko entity demo and the longer lifecycle assertions:

```bash
./gradlew :fabric:runClient -PpulseTestMod -PpulseSmokeAuto -PpulseSmokeWorld -PpulseGeckoDemo --no-daemon
./gradlew :neoforge:runClient -PpulseTestMod -PpulseSmokeAuto -PpulseSmokeWorld -PpulseGeckoDemo --no-daemon
```

The existing disposable-world harness watches the rendered Entity/BlockEntity
managers for at least 21 seconds and two real scheduled cleanup cycles. It checks
controller identity and persistent Molang state, entity removal, client chunk
unload and world disconnect. Require `L25 WORLD PASS`, `L25 REMOVAL PASS`,
`M-A3/M-A4/H2/H1 PASS`, `NATURAL DISCONNECT PASS` and successful process exit.
No existing saves are opened or modified. Without `pulseSmokeAuto`, use
`-PpulseTestMod -PpulseGeckoDemo` to inspect the spinning/bobbing entity manually.
