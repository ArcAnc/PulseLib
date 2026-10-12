# PulseLib 1.21.1 MultiLoader

Implementation on branch `1.21.1-multiloader`, based on `1.21.1` commit `e364f60`. See [the initial dependency audit](multiloader-audit.md), [plugin API and migration](../wiki/plugins.md), and [installation](../wiki/installation.md).

## Modules and platform boundary

- `common`: NeoForm/ModDevGradle, Mojang/Parchment mappings, animation engine, player poses, model loaders, resource/model caches, attachment definitions/layers, geometry arenas, instancing, GPU deformers, OIT and shader definitions. Its 297 Java source files have no NeoForge imports. The Common JAR is not a game mod and is not published to Maven.
- `neoforge`: entrypoint, EventBus adapters, ASM scan-data finder, shaders/atlases, legacy registration events and item extensions. Source variants keep the public `PLibRegistration` and `PItemAnimatable` names/signatures used by old NeoForge integrations.
- `fabric`: Loom, own mod initializers, ASM mod-root finder, Fabric callbacks and localized vanilla rendering/armor/atlas Mixins. Other mods' PulseLib plugins need no loader entrypoint.
- `buildSrc`: shared Java/dependency/resource conventions, loader assembly, publishing and opt-in external integration fixture.

The layout follows [Jared's 1.21.1 MultiLoader template](https://github.com/jaredlll08/MultiLoader-Template/tree/1.21.1), with no Forge module. `ServiceLoader` supplies `IPulsePlatform`, `IPulsePluginFinder` and the lazy client camera service. Exactly one platform provider is required; missing/duplicate providers fail initialization. Architectury is not required.

Java 21, Minecraft 1.21.1 and Parchment `2024.11.17` are retained. Gradle stays at 9.2.1 and ModDevGradle at 2.0.140. Fabric uses Loom 1.12.7, Loader 0.19.5 and Fabric API 0.116.17+1.21.1. Publication plugins use Minotaur 2.10.0 and CurseForgeGradle 1.1.28. The libraries retain their existing jgltf/Jackson versions.

## Automatic discovery and registration

The plugin design follows JEI's annotated-plugin approach, with [JEI's NeoForge finder](https://github.com/mezz/JustEnoughItems/blob/1.21.1/NeoForge/src/main/java/mezz/jei/neoforge/startup/ForgePluginFinder.java) as the platform reference. NeoForge uses `ModList.getAllScanData()` without opening mod JARs again. Fabric visits loader-owned `ModContainer.getRootPaths()`, including extracted nested mods and ZIP filesystems, using ASM without resolving superclass/interface bytecode. Development fallback is restricted to Fabric's declared classpath groups and compiled siblings of known Gradle resource roots. Arbitrary directories and the complete application classpath are not scanned.

Finder results are cached in memory, class names deduplicated, physical side checked before loading, and instances sorted by ID. Public/concrete/interface/annotation/constructor/ID checks diagnose external failures with mod/class/phase. Internal failures propagate. Scanning is measured and logged; a representative production Fabric scan visited 60 roots and found two candidates in approximately 195 ms, while NeoForge startup discovery used its precomputed metadata in single-digit milliseconds. No disk cache was added.

`PulsePluginLifecycle` enforces ordered, non-reentrant phases. Types/attachments/player animations/animated attachments register once. Model declarations repeat against a freshly cleared map. Registrars expire after callbacks. Type, model and player animation batches validate conflicts before application; attachment lists avoid registering identical existing definitions/renderers twice. Plugin instances remain alive across reloads. Deprecated NeoForge events delegate to these same registrars and dispatch once for their corresponding one-time phases.

## Runtime and rendering

Runtime ticks, unload/disconnect, reload and render stages call common operations explicitly. There is no runtime reflection/annotation scanning in the game handlers, and animation managers no longer auto-subscribe to NeoForge events. Fabric disconnect dispatches cleanup onto the Minecraft/render thread; common cleanup asserts that thread before touching GL resources.

Render stage order is preserved: solid queue after block entities, combined entity/translucent queue and GPU frame completion after particles, OIT composite after weather and before the weather target is unbound. Fabric hooks vanilla 1.21.1's exact invocation points, including both graphics branches, rather than assuming similarly named callbacks are equivalent. Its stage order was compared with NeoForge's patched `LevelRenderer`.

Shaders use NeoForge's registration event or Fabric's `CoreShaderRegistrationCallback`, sharing all program definitions. Resource reload starts with plugin model declarations, followed by runtime texture resolution, atlas stitching/upload, then model baking and GPU cache replacement. Integrated-server data reload is excluded from the client declaration hook. Both glTF and Gecko loaders register by default. Concurrent model preparation uses a concurrent map; temporary native baking buffers are closed.

Instancing selects the GL 3.3 function or `ARB_instanced_arrays` entrypoint according to the context. OIT restores indexed blend state for all supported draw buffers plus Minecraft's cached blend/depth/scissor state. Transparency classification uses a Mixin interface rather than names that change in a remapped runtime.

`PItemAnimatable.getPulseItemRenderer()` and `PArmorModelProvider` provide automatic item/humanoid armor adaptation on both loaders. Existing NeoForge `registerClientExtension()` and generic/custom `IClientItemExtensions` behavior remain supported. Attachment definitions keep their packages and implement vanilla armor hiding on Fabric through the vanilla armor injection point.

## Mixins and access

`pulselib-common.mixins.json` contains shared player/camera/model/first-person/hand/geometry/reload hooks and accessors. Existing `BlockEntityMixin`, `CameraMixin`, `CubeDefinitionMixin`, `EntityModelSetMixin`, `GameRendererMixin`, `ItemInHandRendererMixin`, `LivingEntityRendererMixin`, `ModelPartCubesMixin`, `VertexBufferAccessor`, and both `RenderTypeAccessor` targets remain common client Mixins.

`pulselib-neoforge.mixins.json` retains the armor hook at NeoForge's `IClientItemExtensions.setupModelAnimations` and its three-angle camera accessor. Fabric's config contains vanilla armor, atlas construction, sprite-source access, exact level stages and world teardown. Common camera code delegates roll handling to the lazy platform camera service. `SpriteContents.getOriginalImage()` is replaced with a common accessor, while NeoForge's `shouldRiderSit()` has a platform policy.

Common ATs allow compilation against vanilla NeoForm; Fabric's AW supplies equivalent game access to render state shards/types, original CubeDeformation/Quadruped fields and RenderType creation. A Mixin invoker adds entity layers. Fabric Loom's current remapping path rewrites Mixins without a legacy refmap AP. Mixin Extras works in both loader runs and in the remapped production Fabric JAR.

## Artifacts, CI and publication

Game JARs:

- `neoforge/build/libs/pulselib-1.21.1-neoforge-1.1.5.3.jar`
- `fabric/build/libs/pulselib-1.21.1-fabric-1.1.5.3.jar`

Both contain six nested library JARs: jgltf model/impl-v1/impl-v2 and Jackson annotations/core/databind. NeoForge uses Jar-in-Jar; Fabric uses Loom inclusion. Common is compiled into each loader artifact.

NeoForge Maven coordinates remain `com.arcanc.pulselib:pulselib-neoforge:1.21.1-1.1.5.3`; Fabric adds `com.arcanc.pulselib:pulselib-fabric:1.21.1-1.1.5.3`. Main/source/Javadoc/POM/module/checksum output was verified in `/tmp/pulselib-maven`. GitHub Actions builds both loaders, runs both dedicated servers with an external plugin JAR and verifies PASS markers, then uploads release artifacts, test reports and smoke logs. The existing Maven Pages workflow publishes both coordinates. Existing Modrinth/CurseForge project IDs are retained with loader-specific versions/files and a Fabric API requirement. No release was uploaded.

## Verification

Environment: Linux x86_64, Temurin 21.0.11, NVIDIA GTX 1660 Ti. Fabric exercised an OpenGL 3.2 context with required extensions; NeoForge exercised OpenGL 4.6.

- `./gradlew build` and local Maven output: passed; Common, NeoForge and Fabric compile and assemble.
- Nine automated tests: passed. Coverage includes directory/JAR/loader-owned ZIP discovery, annotation parsing without superclass resolution, invalid metadata, restricted dev roots, class/ID deduplication, external failure isolation, internal failure propagation, server-side class-load prevention, lifecycle ordering and instance reuse across two model reloads.
- Both development clients and dedicated servers: reached startup. Both detected annotation-only plugin classes from separate class/resource directories. Both skipped CLIENT classes on dedicated servers. The fixture's client plugin fails if it is initialized on the server.
- External test JAR discovery: passed on Fabric server and both clients, with no PulseLib-specific entrypoints. The fixture has ordinary loader initialization solely for registering Minecraft items.
- Both in-world client fixtures: exercised glTF/Gecko model baking, animation controller/player animation, automatic custom item renderer, common armor model, vanilla armor hiding, living and animated attachment layers, instanced geometry, GPU bend deformation and OIT.
- Two `Minecraft.reloadResourcePacks()` cycles (the operation used by F3+T): plugin constructors and one-time registrations stayed at one, model callbacks ran three times, model/sprite identities changed, and rendering remained valid.
- GL readback: solid `[60, 210, 250, 255]`, OIT `[57, 197, 235, 128]`; no GL error. OIT assertions require partial alpha, preventing an opaque fallback from passing.
- Normal disconnect: tested actual deletion of allocated geometry buffer IDs and empty instance/singleton/player animation managers.
- Isolated NeoForge packaged client/dedicated server: passed using ModDevGradle's prepared Minecraft/loader artifacts, with PulseLib/test mods loaded exclusively from JARs in a separate `mods` folder and jgltf/Jackson excluded from the launch/legacy classpaths.
- Isolated production Fabric client and dedicated server: passed with official Minecraft, the remapped PulseLib/test JARs and Fabric API. jgltf/Jackson were excluded from the launch classpath and loaded from the packaged mod. This caught and fixed remapped class-name classification and network-thread GPU cleanup issues missed by development runs.

The reproducible fixture and commands are in [tests/testmod/README.md](../tests/testmod/README.md). Runtime diagnostic logs were written under `/tmp/pulselib-*.log`; they are not included in release JARs.

## Limits of the verification

The GPU checks cover this hardware and the fixture geometry, not all drivers or extended leak/performance stress. Sodium/Iris/shader-pack interoperability, every existing third-party NeoForge integration, and arbitrary generic armor models on Fabric have not been exhaustively tested. The shared armor provider supports humanoid models; NeoForge's generic model extension remains available. Some historical event-handler method signatures changed as documented in the migration guide; full binary compatibility is not claimed. Modrinth/CurseForge upload tasks were configured but not sent to those services.
