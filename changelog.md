## 1.2.0 — MultiLoader (beta)

Complete list of changes relative to the `1.21.1` branch, compiled on October 14, 2026 from the final diff of source code, resources, build configuration, and documentation.

| Property | Base branch | Current branch |
| --- | --- | --- |
| Branch | `1.21.1` | `1.21.1-multiloader` |
| Comparison commit | `e364f608e2694106f6c14dbe6df24ecf9008b37c` | `05803231617a2f6ac76f77ef37f256e03dcec835` |
| PulseLib version | `1.1.5.3` | `1.2.0`, beta |
| Minecraft / Java | `1.21.1` / Java 21 | `1.21.1` / Java 21 |
| Loaders | NeoForge | NeoForge and Fabric |

The current branch contains 10 commits on top of the base branch. The working tree was clean at the time of comparison. File moves and formatting are accounted for separately from behavioral changes. This changelog describes the prepared beta; updating it does not publish a release or Maven artifacts.

### MultiLoader and loader integration

- Split the monolithic NeoForge project into `common`, `neoforge`, and `fabric`. Shared animations, models, renderers, deformers, attachments, and resources reside in Common; loader integration resides in the respective modules.
- Added Fabric support with separate main/client initializers, `fabric.mod.json`, an Access Widener, and client lifecycle adapters.
- NeoForge retains its own bootstrap, events, Access Transformer, and `IClientItemExtensions` support.
- Shared code accesses platform operations through `IPulsePlatform`, client camera control through `IPulseCameraService`, and plugin discovery through `IPulsePluginFinder`. Providers are selected through `ServiceLoader`; missing or multiple providers are reported during initialization.
- Adapted shader registration, the texture atlas and runtime sprite source, model loading, render layer installation, item rendering, animation ticks, and entity manager removal.
- Added Fabric queue flush points after block entities, particles, and weather, matching the NeoForge stages. Existing first-person and GUI paths are preserved.
- Separated shared and platform-specific Mixins into `pulselib-common.mixins.json`, `pulselib-neoforge.mixins.json`, and `pulselib-fabric.mixins.json`. Shared calls to NeoForge-only methods are replaced with services and Accessor Mixins.
- Fabric requires Fabric API. No mandatory Architectury API dependency was added.

### Automatic plugins and extension registration

- Added `@PulsePlugin`, `IPulseCommonPlugin`, and `IPulseClientPlugin`. Both loaders use the same API.
- Plugins are discovered automatically: NeoForge uses the loader's annotation scan, while Fabric scans bytecode in installed mod roots. JARs, development directories, and ZIP filesystem roots are supported; Fabric also handles separate compiled class and resource roots.
- Discovery runs once. Plugins are filtered by physical side before class loading: CLIENT plugins are not initialized on a dedicated server. Supported sides are `CLIENT` by default, `SERVER`, and `BOTH`, with validation of the corresponding interface.
- Validation checks for a public concrete class, a public no-argument constructor, the annotation, and a unique plugin ID. Duplicate class discoveries are merged; callback order follows sorted `ResourceLocation` IDs.
- Added separate `registerAnimationChannels`, `registerAnimationEvents`, `registerDeformers`, `registerAttachments`, `registerPlayerAnimations`, `registerAnimatedAttachments`, and `registerModels` callbacks.
- Added `PTypeRegistrar`, `PAttachmentRegistrar`, `PPlayerAnimationRegistrar`, `PAnimatedAttachmentRegistrar`, `PModelRegistrar`, and a shared registrar lifecycle. Using a registrar after its callback has finished is reported as an error.
- Channel, event, and deformer types are registered on the appropriate physical sides through the shared API. Client content is registered in separate phases.
- One-time phases run in a defined order and do not repeat on resource reload. Plugin instances survive reloads; only model declarations are collected again.
- Type, player animation, and model declarations are validated before applying each plugin's batch. Loader ID, texture reference, and registry ID conflicts receive explicit diagnostics.
- Extended `PRegistry` with `entries()`, `validateAll(...)`, and `registerAll(...)`; its content snapshot is returned as an immutable map.
- External plugin failures are logged with the class, mod, and phase. A one-time callback failure disables subsequent callbacks for that plugin; a model declaration failure allows a retry on reload. Unaffected plugins continue processing.
- Internal PulseLib errors abort initialization. Fatal discovery failures retain their cause, do not publish a partial plugin list, and do not trigger another scan. A failed one-time phase blocks further execution and callback retries; previously applied batches are neither rolled back nor repeated. The model phase can recover after a reload failure.
- Re-registering the same living attachment definition or animated attachment renderer no longer adds a duplicate.

Details and examples: [plugins and migration](wiki/plugins.md).

### Breaking API changes and migration from 1.1.x

Source and binary compatibility with the entire `1.1.x` API are not guaranteed. Main removals and replacements:

| Previous API | Replacement |
| --- | --- |
| `PulseLibEvents.TypeRegistrationEvent` | The three corresponding `IPulseCommonPlugin` callbacks with `PTypeRegistrar` |
| `PulseLibEvents.RegisterResourceEvent`, including nested `ModelRegistration` | `IPulseClientPlugin.registerModels(PModelRegistrar)` and `PModelRegistrar.ModelRegistration` |
| `PulseLibEvents.AttachmentRegistrationEvent` and its nested registrar | `registerAttachments(PAttachmentRegistrar)` |
| `PulseLibEvents.PlayerAnimationRegistrationEvent` and its nested registrar | `registerPlayerAnimations(PPlayerAnimationRegistrar)` |
| `PulseLibEvents.PlayerAnimatedAttachmentRegistrationEvent` and its nested registrar | `registerAnimatedAttachments(PAnimatedAttachmentRegistrar)` |
| `PItemAnimatable.registerClientExtension()` | `getPulseItemRenderer()`; the loader installs the renderer automatically |
| `PLibRegistration.init(IEventBus)` | `PLibRegistration.init()`; regular game objects are registered through the loader API |
| `PLibRegistration.BlockReg`, `BETypeReg`, `ItemReg`, `EntityTypeReg` | Game registrations moved to the external test mod |
| `PResourceCache.postEvent()` and `register(IEventBus)` | Model declarations through a plugin callback; the atlas is connected by the loader adapter |
| `PAttachmentAnchorResolvers.init(IEventBus)` | `init()` |
| `PLibArmorHandler.register(IEventBus)` | Shared `addLayers(renderer)` operation called by the loader adapter |
| `PRenderTypes.register(IEventBus)` | `ShadersProvider.registerShaders(ShaderRegistrar)` through the loader adapter |
| `PRenderStagesHandler.register(IEventBus)` | `afterBlockEntities()`, `afterParticles()`, `afterWeather(destination)` |
| `PLibAnimationTicker.register(IEventBus)` | `clientTick(ClientLevel)` and `afterClientTick()` through the loader adapter |
| Animation managers' `removeUnused(ClientTickEvent.Post)` and `InstanceAnimationManager.removeEntityManager(EntityLeaveLevelEvent)` | `removeUnused()` and `removeManager(entity)` called by loader callbacks |

`PulseLibEvents` itself has been removed. Extensions are declared through `@PulsePlugin`; a separate PulseLib entrypoint in `fabric.mod.json` and manual plugin manager calls are not required. Regular Minecraft blocks, items, and entities still use their loader's lifecycle.

### Items, armor, and attachments

- `PItemAnimatable.getPulseItemRenderer()` returns a renderer through the shared Minecraft API. The default implementation returns `null`, allowing items without a custom renderer. Custom item rendering uses JSON with `"parent": "builtin/entity"`.
- Added a shared `PArmorModelProvider` with `getPulseArmorModel(...)` and `setupPulseArmorAnimations(...)` for humanoid armor models on both loaders.
- Fabric installs the armor model, applies animations and deformation, and hides vanilla armor through its Mixin. NeoForge integrates the shared provider into `PArmorClientExtensions`, retaining platform-specific custom/generic models.
- Living and humanoid attachment layers, along with the animated player attachment layer, are installed by adapters on both loaders.
- Moved item renderer creation examples into separate client factories so that shared item classes can load on a dedicated server.

### Overlay playback lifecycle

- Active overlays are stored in a `LinkedHashMap<Integer, OverlayPlayback>` keyed by graph state index. Each definition has at most one active instance; its initial activation order is preserved across restarts.
- Retriggering restarts the existing playback, resets clip and fade clocks, and cancels the previous completion. Runtime speed settings and instance identity are preserved, including retriggers during the completion tick.
- Different definitions can run simultaneously, including those sharing a trigger: all matching definitions are processed before the trigger is consumed.
- `OneShotOverlay` remains an immutable definition. Its speed is used on activation; mutable speed, position, fade clocks, and cancellation state belong to the runtime instance.
- Speed is integrated into clip time. A change from `1 → 0 → 0.5 → 1` preserves the current position; the entire elapsed interval is not recalculated at the new speed.
- At `speed == 0`, time and an independent copy of the evaluated overlay pose are preserved. The overlay continues contributing to blending and stays active until resumed, restarted, or explicitly canceled. Pausing at 65% resumes from 65%.
- Fade-in and fade-out clocks are independent of clip time. Pausing playback does not stop either fade. A natural fade that has already started can reach zero weight while paused, retaining the active instance.
- Manual cancellation freezes clip time and reduces the current weight over `fadeOutDuration`; a zero duration removes the instance immediately. Cancellation works while paused; repeated cancellation does not restart the timer.
- Triggering or restarting during fade-in or fade-out reuses the same instance and starts a new playback. `reset()` clears all overlays and their frozen pose caches.
- Normal playback at nonzero speed finishes at the endpoint for its direction and releases runtime state. Each tick reuses the existing playback.
- Missing clips are checked before activation; the fail-fast message includes the state and clip names. No invisible, indefinitely active overlay is created.
- `NaN` and infinite speeds are rejected. Fade durations must be finite and nonnegative.

Added public `PAnimationGraphRuntime` API:

| Method, with `String stateName` and `int stateIndex` overloads | Result |
| --- | --- |
| `overlaySpeed(...)` | Current speed of the active instance |
| `setOverlaySpeed(..., float speed)` | Change speed, pause, resume, or reverse direction |
| `restartOverlay(...)` | Restart the active instance; `false` if the definition is inactive |
| `stopOverlay(...)` | Cancel with fade-out; `false` if the definition is inactive |

The string identifies a graph state name. An unknown, ambiguous, or non-overlay state throws `IllegalArgumentException`; the index overload resolves ambiguity. Getting or setting the speed of an inactive overlay throws `IllegalStateException`. These methods are available through the existing `controller.graphRuntime()` without recreating the controller, graph, or definition.

### Reverse overlay playback

- `OneShotOverlay.speed` and runtime speed now accept finite negative values. The magnitude sets the speed, the sign sets the direction, and zero pauses playback.
- Activation or restart at negative speed begins at the end of the clip. Playback moves toward zero, is clamped to `[0, clip.length]`, and is released on completion.
- Changing the sign through `setOverlaySpeed(...)` reverses the animation from its current position. Passing through zero speed preserves the last nonzero direction; restarting paused reverse playback holds the final frame until resumed.
- An overlay initially declared with zero speed holds its first frame.
- Natural fade-out accounts for the distance to the endpoint and the absolute speed. Reversing direction cancels the previous automatic fade, restores weight from its current value over `fadeOutDuration`, and then uses the fade for the new endpoint. Weight recovery continues while paused.
- Changing direction does not undo manual cancellation; triggering or restarting cancels it as part of a new playback.
- Reverse events are dispatched in descending time order: the departure boundary is excluded, and the arrival boundary is included. Pause/resume does not repeat the event at the pivot point; crossing a marker again after reversing dispatches it again.
- This speed contract change applies to overlays. Speed contracts for other graph state types are preserved.

### Restart transitions from the evaluated pose

- `PPoseMixer.Transition` and `PAnimationGraphRuntime` implement the same RESTART semantics: capture the actual blend of base sources and use a frozen copy as the source of the new transition.
- Fixed transitions without a previous active transition: the source is taken from the current evaluated pose instead of clearing source layers and snapping to the reference/bind pose.
- Interrupting `WALK → RUN` at 40% with a RESTART into ATTACK starts from the evaluated 60% WALK / 40% RUN blend. The previous clips no longer animate the snapshot.
- The mixer captures `mixer.mix(reference, layers())` directly in `transitionTo(...)`. The graph captures `sampleBasePose(...)` in `beginTransition(...)` after advancing the tick, at `partialTick = 1`, before replacing the transition. The previous render frame's cache is not used.
- Added internal `PPoseSnapshot` with deep copies of translation, quaternion rotation, scale, valid/dirty flags, channel presence, scalar values/presence, and visibility. Sampling returns copies; storage is not shared with mutable source poses.
- Added shared `PPoseMixer.blend(reference, source, target, alpha)`: translation and scale use linear interpolation, while rotation uses shortest-path quaternion slerp. The endpoints `alpha = 0` and `alpha = 1` return the source and target respectively, including their channels.
- Missing channels retain their semantics and use the reference during interpolation. An explicitly animated identity channel differs from an absent channel; a dirty flag alone does not turn missing TRS channels into identity channels.
- Extended `PPose` with `copy()`, `scalarChannelCount()`, `hasScalarChannel()`, `hasTranslation()`, `hasRotation()`, `hasScale()`, `hasVisibility()`, `isVisible()`, and the `visibility(...)` setter. Pose copying preserves scalar storage and metadata.
- Graph base sampling is separate from independent overlays. The snapshot excludes overlays; their time, speed, pause/cancel state, and runtime identity are preserved, and their contribution is applied once on top of the new base transition.
- `PAnimationRuntime` and `PAnimationPoseResolver` use the evaluated pose for blending, visibility, and active bones, including bones represented only by the frozen source. Multiple-controller ordering and Molang this-values from preceding controllers are preserved.
- The snapshot is local to its runtime/transition, contains no clip or model references, does not advance its own time, and generates no animation events.
- Repeated RESTART evaluates the current blend, replaces the previous snapshot, and retains one frozen source. Transition completion or reset releases it; old playback/source lists do not grow.
- `FROM_CURRENT` retains live weighted sources, including the live target of a previous snapshot transition; `COMPLETE_CURRENT` retains its interruption lock. Snapshots apply only to RESTART. A zero blend duration switches immediately to the target.

API additions: `sampleBasePose(...)`, `samplePose(...)`, `layers(model, partialTick)`, `tickPrecise(...)` with a snapshot sampler, `PAnimationRuntime.captureGraphBase(...)`, `PPoseMixer.blend(...)`, and `PPoseMixer.Transition(mixer, reference, initial)`.

`PAnimationController.setGraphSnapshotContexts(provider)` allows current game queries to be supplied during capture. Custom renderer queries must come from current game state through this provider or a snapshot sampler; without a provider, previously supplied custom queries are retained while playback time/anim_time is updated.

Existing `OneShotOverlay`, `PAnimationGraph`, and `PPoseMixer.Transition(initial)` constructors, previous tick/layers/controller signatures, and previous `Layer`/`EventTrack` forms are preserved. An explicit mixer transition constructor supports an external reference pose, hierarchy, and mesh-space/additive capture. Custom consumers that evaluate poses solely from the old `layers()` must switch to `samplePose()` for RESTART: a frozen pose cannot be represented as an animation clip.

### Cyclic animation events

- Shared `PCyclicAnimationEvents` replaces float cursor boundary traversal with integer cycle indices and double-precision time. FMA preserves the local residual at large absolute times.
- Raw controller and graph dispatch use shared traversal supporting forward/reverse playback, multiple cycles per tick, fractional clip lengths, and negative times.
- Markers at `0` and `length` remain distinct events and are dispatched once in playback order when crossing a loop boundary. Entering a raw reverse stage separately excludes its initial end marker.
- Stationary windows, zero-length clips, and tracks without events do not start traversal. Nonfinite values, unrepresentable cycle ranges, and dispatches exceeding `1 << 20` events are diagnosed before callbacks, without silently truncating the event stream.
- Added `PAnimation.forEachCyclicEvent(double, double, Consumer)` and an overload with `includeReverseStart`, plus graph `tickPrecise(...)` / `PreciseEventTrack` with double-precision event boundaries. Previous float `tick(...)` / `EventTrack` APIs are preserved.

### Fabulous transparency and OpenGL state

- Fixed disappearing or incorrectly overlapping OIT meshes during Fabulous compositing: PulseLib layers are retained until the vanilla transparency chain finishes and are then composited into the final target.
- The new compositor accounts for terrain translucency color/depth, item entities, particles, weather, and clouds; contributions from vanilla surfaces in front of the OIT depth are preserved.
- Added `oit_fabulous_composite` and `oit_fabulous_metadata` shaders and a metadata texture. Collapsed OIT depth is not written into the vanilla translucent target; the metadata texture is released during cleanup or recreation.
- Added `PRenderQueue.compositeFabulousTranslucency(...)` and shared `LevelRendererTransparencyMixin`. The existing four OIT layers and regular compositor are preserved.
- Replaced NeoForge-only `GlStateBackup` with shared `GlStateSnapshot`. It saves and restores framebuffer/viewport, indexed blend factors/equations, depth/scissor state, shader program, VAO, buffers, and texture bindings, accounting for Minecraft caches and exceptions during compositing.
- Added an `ARB_instanced_arrays` fallback for attribute divisors when OpenGL 3.3 core support is unavailable; initialization reports an error if neither capability is available.
- Temporary `ByteBufferBuilder` instances are closed when baking models, CPU/GPU-deformed geometry, and texture variants, including exception paths.

### Resource reload and runtime cleanup

- Model declarations are collected before reload through shared `ResourceReloadMixin` / `PulseRuntime.beforeResourceReload()`, before texture resolution and subsequent model baking.
- `PResourceCache` publishes staged declarations after a successful phase. Texture and loader conflicts are checked before applying the corresponding batch.
- `PModelCache` collects models from parallel load tasks in a `ConcurrentHashMap`; added loader/source diagnostics and reload/baking timings.
- The Gecko loader, already present in the base branch, is included in the built-in model loader registry alongside glTF. Gecko/glTF parsers are preserved.
- Failure to register PulseLib shaders on NeoForge now aborts initialization with diagnostics instead of logging a warning and continuing with a missing shader.
- `PulseRuntime.disconnected()` centralizes cleanup of the render queue, GPU/deformer resources, deformation caches, and instance/singleton/player animation managers on the render thread.
- NeoForge invokes shared cleanup on client level unload; Fabric invokes it on disconnect and `ClientLevel` changes. Existing NeoForge cleanup is moved into shared code.
- Block entity removal calls the client animation manager only when a client level is present; server-side removal no longer accesses this client path.
- Entity manager removal and cleanup of unused managers run through loader callbacks, without NeoForge annotations in Common.

### Visual root motion and attachment hierarchy

- `PPlayerAnimationFrame.modelMatrix(...)` now computes the exact product of parent matrices. Added `bindMatrix(...)`, `currentRootRelativeMatrix(...)`, and `currentRootRelativeTransform(...)`.
- Nested anchors and meshes inherit all intermediate translation, rotation, and scale. Nonuniform scale and rotation are preserved in the matrix pipeline instead of losing affine components during intermediate TRS composition.
- The third-person attachment stack already contains visual ROOT motion; child contributions are relative to the current ROOT. Double application of root motion is eliminated.
- `rootRelativeTransform(...)` preserves the representation relative to the ROOT bind frame for consumers needing full visual motion. Rootless rigs use the HEAD bind reference.
- First-person items, arms, custom anchors, and automatic meshes use a shared camera-relative VIEW presentation. ROOT and camera transforms are not applied twice.
- Automatic mesh discovery accounts for inherited animation and static mesh descendants of the active rig, including camera-only animation.
- Added matrix helpers in `PPlayerAnimationSpace`, shared `visualRootMatrix(...)` with a pivot, and preservation of caller channel overrides on top of the exact original affine matrix.
- The existing root-motion extraction API is preserved. Documentation clarifies the visual contract: extracting a delta and applying ROOT to a pose do not move the entity, hitbox, or server position; the calling mod handles physical movement.

### Animated camera

- Camera displacement is evaluated at the actual bind camera point, accounting for ROOT, pivot, and the full parent hierarchy. Fixed the use of the affine delta's translation at the origin.
- A rotation-only camera channel with a nonzero bind origin no longer creates spurious displacement. Root rotation moves the camera origin along the corresponding arc.
- Rotated/scaled intermediate parents, rotated bind references, and rootless rigs are supported; the camera and first-person attachments use consistent transform spaces.
- Fixed conversion of Gecko/glTF source axes before applying the camera delta. First-person activation weighting during transitions is preserved.
- Added `PFirstPersonCameraSpace.fromMinecraftAngles(...)`; Fabric and NeoForge use a consistent yaw/pitch/roll convention through the camera service, including signed roll.
- VANILLA camera mode preserves native position and rotation. The animated camera remains visual and does not change player physics.

### External demo/test mod

- Moved the original TestBlock, TestBlockEntity/Renderer, TestEntity/Render, TestBlockItem/Renderer, TestArmor, TestTailItem, PTestArmor, TestDayTimeColor, ball toss, and acrobatic player demos into `tests/testmod`, with shared implementations and small loader adapters.
- Demo classes and assets are excluded from release library JARs. Original demo resource ZIPs are removed; models, animations, and textures are unpacked into the test mod, with vanilla item/blockstate descriptors added.
- Demo content is registered under the `pulselibtest` namespace. V starts ball toss, B starts acrobatics; block/entity/item/armor/tail examples and a Gecko entity alternative to glTF are available.
- `CommonTestPlugin` and `ClientTestPlugin` exercise registration through `@PulsePlugin`; added source-set runs, test JAR installation, and Fabric `remapTestmodJar` for intermediary mappings.
- Smoke checks cover original models, actual mesh rendering, GPU deformation, solid/OIT pixels, GL state, and buffer deletion. Added explicit cleanup and natural disconnect probes; negative controls distinguish a working lifecycle hook from manual cleanup.
- Automated game modes use a new marked run directory and a disposable world. Existing or unmarked saves are not accepted as test scenes; regular manual demo runs retain their behavior.
- Added a transparency harness with 38 pixel/state regression cases, the real vanilla transparency chain, resource reload, resizing, and world/weather probes.
- Added opt-in GPU timestamp profiling for Fancy/Fabulous, offscreen 1080p/1440p/4K, and separate world measurements; reports include mean/p95, pass timings, fullscreen calls, and allocations.
- Added a player motion/camera harness for Gecko and glTF with blend modes/weights, ROOT masks, multiple definitions, reload, observer actors, and integrated-server respawn.
- Added overlay lifecycle and transition restart smoke checks on the original entity rig with 11 bones and `idle`/`walk` clips: pose continuity at 40% interruption, forward/reverse/paused overlays, 1,000 retriggers, instance identity/count, cancellation/fade-out, and reset.

Commands and verification scope: [test mod README](tests/testmod/README.md). Minecraft smoke checks, GPU profiling, and world tests remain opt-in and are not part of mandatory CI.

### Build, dependencies, and publishing

- Migrated Gradle to a multi-project layout with `multiloader-common`, `multiloader-loader`, `multiloader-publishing`, and `multiloader-testmod` convention plugins in `buildSrc`.
- Loader builds compile shared sources and include shared resources in their own JARs. Common serves as a development module and is not published as a separate installable mod or Maven artifact.
- Retained Minecraft 1.21.1, Java 21, Parchment `2024.11.17`, NeoForge `21.1.216`, and ModDevGradle `2.0.140`. Added NeoForm `1.21.1-20240808.144430`, Fabric Loader `0.19.5`, Fabric API `0.116.17+1.21.1`, and Loom `1.12.7`.
- Runtime library versions for jgltf and Jackson are unchanged and explicitly declared; they are bundled through NeoForge JarJar and Fabric include. Common uses compile-only JSR-305, Sponge Mixin, and MixinExtras; added JUnit Jupiter `5.11.4` and ASM for tests. Minotaur is pinned to `2.10.0` instead of the `2.+` range.
- Increased the Gradle heap from 1 GiB to 3 GiB; disabled the configuration cache. The Gradle wrapper is unchanged.
- The shared `./gradlew build --no-daemon` command builds all three modules and runs JUnit. Loader client/server runs use `:<loader>:runClient` / `:<loader>:runServer`.
- Removed the old root NeoForge `data` / `gameTestServer` run configurations and root `src/generated/resources` integration during the build migration. The new opt-in testmod workflow is documented separately.
- Game JARs receive a loader suffix: `pulselib-1.21.1-neoforge-1.2.0.jar` and `pulselib-1.21.1-fabric-1.2.0.jar`; sources/Javadoc artifacts are also configured. The Fabric game JAR is remapped.
- Retained the NeoForge Maven artifactId `pulselib-neoforge`; added `pulselib-fabric`. Beta coordinates: `com.arcanc.pulselib:pulselib-neoforge:1.21.1-1.2.0` and `com.arcanc.pulselib:pulselib-fabric:1.21.1-1.2.0`.
- Maven publication and the publishing workflow publish both loader modules. Modrinth/CurseForge receive separate loader variants, the beta release type, and a Fabric API dependency for Fabric.
- `publishMods` combines upload tasks for both loaders; `publishAll` also includes Maven. The previous standalone root `publishDiscord` task is removed. A regular build does not publish anything.
- Consolidated CI into one build/test job on Java 21; added manual dispatch, uploads of both loader JARs, and JUnit reports. Updated Checkout to v6. Game runs were not added to CI.

### Documentation and source formatting

- Added the [plugin API and migration guide](wiki/plugins.md) and an animation lifecycle validation report; updated [installation](wiki/installation.md), [API reference](wiki/api-reference.md), and [controllers](wiki/animatables-and-controllers.md).
- Migrated wiki examples for models, resources, items, armor, attachments, player animations, and extension types from `PulseLibEvents` to plugin callbacks. Source links now point to Common paths in the current branch.
- Clarified LOCAL/MODEL/ROOT-relative/VIEW spaces, the anchor stack contract, visual root motion, and camera displacement; updated the README and testmod instructions.
- Formatted migrated Java sources to match the original style: tab indentation, Allman braces, expanded blocks/methods, and call chains with the dot at the end of the previous line.
- Preserved authorship and existing creation dates; added missing license headers, package nonnull defaults, and nullable metadata clarifications.
- Added `AGENTS.md`, a Java `.editorconfig`, and ignore rules for runtime logs/crash diagnostics. Removed intermediate review documents; current instructions reside in the wiki and testmod README.

### Regression tests and available validation results

Added the following JUnit suites. Counts below are confirmed by the latest available XML reports, generated on October 14, 2026 while validating the implementation before this changelog was compiled; this documentation update did not include another Minecraft or Gradle run.

| Suite | Tests | Main coverage |
| --- | ---: | --- |
| `PulsePluginClassScannerTest` | 3 | Bytecode/side filtering, JAR/directory/ZIP roots, invalid side |
| `PulsePluginLifecycleTest` | 3 | Phase order, one-time execution, failed state, and model retry |
| `PulsePluginManagerTest` | 11 | Dedicated server, deduplication/IDs, failures, atomic batches, reload |
| `FabricDevelopmentRootsTest` | 1 | Separate development roots and exclusion of unrelated classpath groups |
| `PCyclicAnimationEventsTest` | 25 | Forward/reverse loop boundaries, multiple cycles, large times, guards |
| `POverlayLifecycleTest` | 17 | Lifecycle/API, pause/resume, fades, shared triggers, reset, validation |
| `POverlayReverseTest` | 14 | Final-frame pose, reverse cursor/events, direction changes, fades/cancel |
| `PPoseMixerTransitionTest` | 13 | Numerical TRS/masks/scalars, snapshot continuity, and interruption policies |
| `PGraphRestartTest` | 18 | Graph blending, overlays, contexts/controllers, cycles/events/root motion |
| `PPlayerHierarchyTest` | 8 | Parent matrices, ROOT/pivot, nonuniform scale, first-person spaces |
| `PPlayerCameraTest` | 8 | Camera bind point/hierarchy, source axes, and yaw/pitch/roll |
| **Total** | **121** | **Common: 120; Fabric: 1; failures/errors/skips: 0** |

Overlay lifecycle and reverse playback stress regressions include 100,000 paused ticks and 10,000 retriggers with a single instance. Transition restart regressions include 3,000 consecutive RESTART operations each for the mixer and graph, comparisons of evaluated translation/rotation/scale, missing/identity channels, and source state release. Clip names and times are not used as the sole evidence of correct blending.

The latest full `./gradlew build --no-daemon` on Java 21 completed with `BUILD SUCCESSFUL`; a separate run of overlay lifecycle, transition restart, reverse playback, and cyclic event suites passed 87/87 tests. NeoForge uses the shared regression suites; the current set has no separate NeoForge JUnit suite.

The latest opt-in client smoke checks for overlay lifecycle, transition restart, and reverse playback covered Fabric/glTF, Fabric/Gecko, NeoForge/glTF, and NeoForge/Gecko. All passed the animation and client checks and exited with code 0. They validate evaluated poses on loaded models, graphics, and explicit cleanup. Commands and verification scope are documented in the [test mod README](tests/testmod/README.md).

These four runs do not check in-world interruption pixels or networked multiplayer. Transparency pixel/profiling checks, natural disconnect checks, and player motion world checks have separate opt-in harnesses; their results cannot be inferred from successful animation lifecycle smoke checks. Compatibility with arbitrary shader mods and custom renderer queries requires the corresponding configuration and validation.

### Features already present in the base branch

Animation Graph, blend spaces, Pose Mixer, Molang, Gecko/glTF parsers, root-motion extraction, player/first-person animations, anchors/mesh attachments, GPU deformers, the instanced queue, and four-layer OIT already existed in `1.21.1`. This release ports them to both loaders and fixes the areas listed above.

The third-person hand rotation fix from the previous changelog is already included in the base branch before the comparison point and is not a new change in `1.2.0`.

PulseLib's versioning scheme is unchanged: `1.1.x` covers small updates and fixes, `1.2.x` covers major changes including MultiLoader and architecture, and `2.0.x` is reserved for the next stage defined by the author. API incompatibility alone does not determine a move to `2.0`.
