# PulseLib 1.21.1 MultiLoader — 1.2.0

Branch: 1.21.1-multiloader. Original baseline: 1.21.1 / e364f60. The [initial audit](multiloader-audit.md) is historical; stage 2 deliberately removes the compatibility decisions from stage 1. See [plugins and removed APIs](../wiki/plugins.md), [installation](../wiki/installation.md), and the [demo/smoke commands](../tests/testmod/README.md).

## Architecture

- common: NeoForm/ModDevGradle, animation engine, poses, model parsers/caches, attachments, geometry arenas, instancing, GPU deformers, OIT and shaders. No NeoForge/Fabric imports and no required Architectury API.
- neoforge: mod entrypoint, native EventBus runtime adapters, scan-data plugin finder, shader/atlas/armor/item integration.
- fabric: Loom, native initializers/callbacks, ASM mod-root scanner, localized renderer/armor/atlas Mixins and Access Widener.
- buildSrc: shared Java/resource/dependency/assembly/publication conventions and opt-in external demo source set.
- tests/testmod: the original library demos implemented once, with small ordinary game-registry/renderer/key adapters for each loader; excluded from library artifacts.

ServiceLoader supplies the physical-side/platform policy, plugin finder and lazy client camera service. Exactly one provider is required. Plugin registration is common code, not a platform service or a loader event bridge.

PLibRegistration and PItemAnimatable each exist only in common. The former owns built-in channel/event/deformer registries and init(); the latter exposes getPulseItemRenderer(). PulseLibEvents, five registration events, registerClientExtension(), the mixed LEGACY type registrar, compatibility source substitutions and PResourceCache.postEvent() are removed. Native IClientItemExtensions remains an internal NeoForge adapter for the current common item/armor API.

Version 1.2.0 intentionally contains incompatible API changes. Existing NeoForge mods may require source adaptation: replace old extension registration events with `@PulsePlugin` callbacks and `PItemAnimatable.registerClientExtension()` with `getPulseItemRenderer()`. No deprecated compatibility adapters are provided.

Java 21, Minecraft 1.21.1, Mojang/Parchment 2024.11.17, Gradle 9.2.1, ModDevGradle 2.0.140, Loom 1.12.7, Fabric Loader 0.19.5 and Fabric API 0.116.17+1.21.1 are retained. Existing jgltf/Jackson versions are preserved. There is no Forge module or common installable mod.

## Discovery and lifecycle failures

NeoForge reads ModList.getAllScanData() only. Fabric scans loader-owned mod root paths using ASM without loading ordinary classes, including nested mod ZIP filesystems and development directories. Its fallback is limited to declared Fabric classpath groups and known compiled siblings of Gradle resource roots. Results are deduplicated/cached once; side filtering precedes class loading, and plugins execute by sorted ResourceLocation ID.

Discovery uses try/finally. Only the completed list is published. A fatal finder/internal construction error is cached as terminal; subsequent calls report its cause instead of rescanning or reconstructing partial instances. Recursive discovery is rejected. Invalid constructors/annotations/interfaces/IDs and duplicate IDs from external mods are diagnosed with mod/class/phase and isolated.

Lifecycle states are PENDING, ACTIVE, COMPLETE and FAILED. Fatal one-time failure clears the active phase, blocks later phases and cannot replay registrations already committed. Models can retry after a failed reload. External callback failure discards its staged batch; one-time failure disables that plugin, while model callbacks can retry. Model declarations are staged across the phase, then replace the resource cache only on success. No partial model declaration map is published after a fatal failure.

## Rendering and resources

The rendering engine remains common: GlDrawExecutor, geometry buffers, instancing, CPU/GPU deformation, OIT, poses and glTF/Gecko parsers. Its existing renderer/legacy package contains the active GL backend, not a retained deprecated registration API.

Fabric after-block-entities now anchors at Long2ObjectMap.long2ObjectEntrySet(), where vanilla begins destruction rendering after block-entity batches and outline processing. This preserves the original boundary without a destroyProgress profiler-string dependency. Particle hooks execute immediately after ParticleEngine.render(), before target unbinding; weather hooks execute after renderSnowAndRain(), before weather target unbinding. Probe Mixins validate actual block → particles → weather order on both loaders.

Model callbacks collect declarations before textures/atlas work. Atlas references invalidate, sprites stitch/upload, models bake and GPU caches replace. Server data-pack reloads are excluded from the client hook. Both built-in parsers are registered once. Plugin instances and one-time content survive repeated resource reloads.

OIT preserves indexed blend factors/equations/enables for every draw buffer, plus depth/scissor and Minecraft cached state. Instancing chooses core or ARB entrypoints according to context capabilities. Native temporary baking buffers are closed. Normal disconnect schedules Fabric GPU cleanup on the render thread and empties animation managers.

Logging covers measured scans, plugin identities/origins, phase failure causes, resource declaration/model counts and elapsed reload time, model sources/shader initialization at DEBUG, and explicit smoke results. It does not log ticks or render operations in the library.

## Artifacts and release configuration

- neoforge/build/libs/pulselib-1.21.1-neoforge-1.2.0.jar
- fabric/build/libs/pulselib-1.21.1-fabric-1.2.0.jar

Coordinates: com.arcanc.pulselib:pulselib-neoforge:1.21.1-1.2.0 and com.arcanc.pulselib:pulselib-fabric:1.21.1-1.2.0. Release metadata remains beta. Maven group and artifact IDs are unchanged; Maven, Modrinth and CurseForge versions derive from mod_version.

PulseLib uses the author's version scheme rather than strict Semantic Versioning: 1.1.x is for small updates/fixes, 1.2.x is for major updates including MultiLoader and architecture changes, and 2.0.x is a future stage defined by the author. Removing an old API does not automatically increase the major version. Releases require separate authorization.

Both game JARs include six nested libraries (jgltf model/impl-v1/impl-v2 and Jackson annotations/core/databind), platform services and their resources/Mixins. Sources and Javadoc are assembled; Fabric sources are remapped for publication. Common code/resources are compiled into each loader JAR; the Common JAR is not published as a standalone mod. All demo code/assets are excluded from library artifacts. The new Maven version is 1.21.1-1.2.0; previous versions must not be overwritten.

Standard GitHub Actions uses one job with Java 21, Gradle and `./gradlew build --no-daemon`. It compiles Common, NeoForge and Fabric, runs JUnit and assembles both loader JARs plus sources/Javadoc. Loader JARs are uploaded after a successful build; XML/HTML test reports are retained even on failure. There are no Minecraft launches, EULA acceptance, temporary worlds, demo runs or game PASS-marker checks in CI. Compilation output remains available in the Gradle step log. The existing Maven Pages release workflow remains separate. Modrinth/CurseForge retain project IDs, distinct loader versions/files and the Fabric API requirement. No release upload has been executed.

Runtime logs/crash reports/JVM diagnostic logs are ignored. The accidentally tracked common/logs files are removed. Useful original demo resources remain in the external test mod.

## Verification

The game checks below were performed manually during stage 2. They are not standard CI steps and were not repeated for this version/CI-only update. Manual commands remain available in the demo module.

Environment: Linux x86_64, Temurin Java 21, NVIDIA GTX 1660 Ti. Both development libraries and packaged JARs were checked; production Fabric uses official Minecraft/intermediary mappings, while isolated NeoForge uses ModDevGradle-prepared Minecraft/loader artifacts with mods loaded from JARs. jgltf/Jackson were excluded from isolated launch classpaths.

- Full Gradle build and local Maven assembly: passed.
- 18 JUnit tests: passed (17 common, one Fabric). Added finder/internal/constructor/ID/reentrancy failures, partial discovery prevention, failed one-time phase protection, discarded batches and recoverable model failures; existing ASM directory/JAR/ZIP/side/dev-root tests retained.
- NeoForge and Fabric dedicated server discovery: passed; CLIENT initialization is forbidden by the fixture and its class is skipped. Channels/events/deformers register once.
- Original cross-platform demo: nine models declare/bake; glTF block/entity/item/armor/tail/player resources and the animation-only Gecko player rig load. An additional mode uses the original Gecko entity/armor geometry and idle/walk clips.
- Both clients validate actual block/entity/item renderer callbacks, common armor adaptation and vanilla hiding, living/animated attachments, ball-toss player animation, render-stage order, eight instanced GPU-bent item meshes and solid/OIT pixels.
- Solid readback: 504 colored pixels. OIT: 504 colored pixels with partial alpha; no GL error. Indexed blend/depth/scissor snapshots match before/after compositing. Selecting the original pyramid deterministically avoids the item's flat circle mesh being edge-on in this test projection.
- Three consecutive reloads in each of two disposable worlds: constructors/one-time phases stay at one; models/sprites change identity and rendering stays valid. Disconnect deletes captured geometry buffer IDs and empties instance/singleton/player manager collections before entering the next world.
- Sodium 0.6.13 with Iris 1.8.8: both loader runs passed the same two-world checks with original Gecko entity geometry and Fabulous selected. Shader packs were disabled. Exact version sources: [Fabric Sodium](https://modrinth.com/mod/sodium/version/mc1.21.1-0.6.13-fabric), [NeoForge Sodium](https://modrinth.com/mod/sodium/version/mc1.21.1-0.6.13-neoforge), [Fabric Iris](https://modrinth.com/mod/iris/version/1.8.8+1.21.1-fabric), [NeoForge Iris](https://modrinth.com/mod/iris/version/1.8.8+1.21.1-neoforge).

Full command recipes and PASS markers are in tests/testmod/README.md. Local runtime logs are under /tmp/pulse-stage2-*.log; they are not release resources.

## Verification limits

The checks establish behavior on this GPU and these precise configurations, not universal driver/mod compatibility. Enabled Iris shader packs, arbitrary shader-pack pipelines, extended leak/performance stress and other GPU vendors remain unverified. NeoForge graphics checks use the prepared ModDevGradle runtime rather than a separate launcher installation. Shared armor models are humanoid; no legacy generic PulseLib armor API is retained. The original Gecko entity has idle/walk clips only. External release services were not used.
