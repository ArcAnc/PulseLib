> Historical audit of the original 1.x baseline. Stage 2 removes the compatibility adapters described during the initial migration; the current architecture is in [multiloader.md](multiloader.md).

# PulseLib 1.21.1 migration audit

Base: `e364f60`, branch `1.21.1-multiloader`. Baseline `compileJava` passed with ModDevGradle 2.0.140 and Gradle 9.2.1.

## Platform boundary

Common keeps animation controllers/managers, player poses, glTF/Gecko parsers, geometry buffers, GPU deformers, instancing, render queue/executor, OIT, shader definitions, caches and attachment definitions. Loaders own discovery, mod lifecycle, shaders/atlases, item extension wiring and armor injection points. ServiceLoader resolves the platform without Architectury.

`PLibRegistration`'s deferred game registries and the disabled demos are NeoForge-specific. Animation/event/deformer registries remain shared. The NeoForge source variants of `PLibRegistration` and `PItemAnimatable` retain their original public names and signatures.

## Rendering and reload findings

NeoForge flushes solids after block entities; combined entity/translucent queues and GPU frame completion happen after particles; OIT composites into the weather target after weather. Fabric AFTER_ENTITIES is too early. Fabric needs local injections after block entities and particles and before weather target unbinding.

Original registration declarations are rebuilt only when reload listeners register, not on every F3+T. Rebuild at the start of each client resource reload, before atlas preparation. Plugin instances must survive reload. Model baking must follow atlas upload on the game executor.

`HumanoidArmorLayerMixin` targets a NeoForge-patched method and belongs in the NeoForge config. Fabric uses vanilla armor hooks. AT access to QuadrupedModel.body and CubeDeformation.growX/Y/Z requires a Fabric access widener. OIT uses NeoForge RenderSystem.backupGlState/restoreGlState patches; replace them with a common GL state snapshot that restores Minecraft's cached state via GlStateManager.

Existing animation managers have both auto-subscriber annotations and explicit registration; remove annotations and wire runtime handlers once per loader. BlockEntityMixin is client-only and remains in the client mixin list.

## Original direct NeoForge dependencies

- `src/main/java/com/arcanc/pulselib/content/animatable/PItemAnimatable.java`: `net.neoforged.neoforge.client.extensions.common.IClientItemExtensions`
- `src/main/java/com/arcanc/pulselib/content/animatable/PLibAnimationTicker.java`: `net.neoforged.bus.api.IEventBus`, `net.neoforged.neoforge.common.NeoForge`, `net.neoforged.neoforge.event.tick.LevelTickEvent`
- `src/main/java/com/arcanc/pulselib/content/animatable/instance/InstanceAnimationManager.java`: `net.neoforged.bus.api.SubscribeEvent`, `net.neoforged.fml.common.EventBusSubscriber`, `net.neoforged.neoforge.client.event.ClientTickEvent`, `net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent`
- `src/main/java/com/arcanc/pulselib/content/animatable/singleton/SingletonAnimationManager.java`: `net.neoforged.bus.api.SubscribeEvent`, `net.neoforged.fml.common.EventBusSubscriber`, `net.neoforged.neoforge.client.event.ClientTickEvent`
- `src/main/java/com/arcanc/pulselib/content/registration/PLibRegistration.java`: `net.neoforged.bus.api.IEventBus`, `net.neoforged.neoforge.registries.DeferredBlock`, `net.neoforged.neoforge.registries.DeferredHolder`, `net.neoforged.neoforge.registries.DeferredItem`, `net.neoforged.neoforge.registries.DeferredRegister`
- `src/main/java/com/arcanc/pulselib/content/renderer/PRenderStagesHandler.java`: `net.neoforged.bus.api.IEventBus`, `net.neoforged.neoforge.client.event.RenderLevelStageEvent`, `net.neoforged.neoforge.common.NeoForge`
- `src/main/java/com/arcanc/pulselib/content/renderer/legacy/GlWeightedBlendedOit.java`: `net.neoforged.neoforge.client.GlStateBackup`
- `src/main/java/com/arcanc/pulselib/util/PRenderTypes.java`: `net.neoforged.bus.api.IEventBus`, `net.neoforged.neoforge.client.event.RegisterShadersEvent`
- `src/main/java/com/arcanc/pulselib/util/PResourceCache.java`: `net.neoforged.bus.api.IEventBus`, `net.neoforged.fml.ModLoader`, `net.neoforged.neoforge.client.event.RegisterMaterialAtlasesEvent`
- `src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchorResolvers.java`: `net.neoforged.bus.api.IEventBus`
- `src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PLibArmorHandler.java`: `net.neoforged.bus.api.IEventBus`, `net.neoforged.neoforge.client.event.EntityRenderersEvent`
- `src/main/java/com/arcanc/pulselib/PLib.java`: `net.neoforged.bus.api.IEventBus`, `net.neoforged.fml.ModContainer`, `net.neoforged.fml.common.Mod`, `net.neoforged.fml.loading.FMLLoader`
- `src/main/java/com/arcanc/pulselib/content/event/ClientEvents.java`: `net.neoforged.bus.api.EventPriority`, `net.neoforged.bus.api.IEventBus`, `net.neoforged.fml.ModLoader`, `net.neoforged.fml.event.lifecycle.FMLClientSetupEvent`, `net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent`, `net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent`, `net.neoforged.neoforge.client.extensions.common.IClientItemExtensions`, `net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent`, `net.neoforged.neoforge.common.NeoForge`, `net.neoforged.neoforge.event.level.LevelEvent`
- `src/main/java/com/arcanc/pulselib/content/event/CommonEvents.java`: `net.neoforged.bus.api.IEventBus`
- `src/main/java/com/arcanc/pulselib/content/event/PulseLibEvents.java`: `net.neoforged.bus.api.Event`, `net.neoforged.fml.event.IModBusEvent`
- `src/main/java/com/arcanc/pulselib/content/registration/item/TestBlockItem.java`: `net.neoforged.neoforge.client.extensions.common.IClientItemExtensions`
- `src/main/java/com/arcanc/pulselib/content/registration/player/PPlayerBallDemo.java`: `net.neoforged.bus.api.IEventBus`, `net.neoforged.fml.loading.FMLLoader`, `net.neoforged.neoforge.client.event.ClientTickEvent`, `net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent`, `net.neoforged.neoforge.common.NeoForge`
- `src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PArmorClientExtensions.java`: `net.neoforged.neoforge.client.extensions.common.IClientItemExtensions`

## Dependencies exposed by compiling against vanilla NeoForm

`SpriteContents.getOriginalImage()` and `Entity.shouldRiderSit()` are NeoForge additions despite having no NeoForge imports. Use a common image accessor and a platform ride-pose policy. RenderStateShard nested classes/fields and RenderType.create were widened by NeoForge; equivalent Common AT/Fabric AW access is needed. Add-layer access uses a common Mixin invoker.
