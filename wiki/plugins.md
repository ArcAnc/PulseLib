# Automatic PulseLib plugins

Minecraft 1.21.1 uses the same plugin API on NeoForge and Fabric. A public annotated class with a public no-argument constructor is sufficient. Do not add a PulseLib entrypoint to `fabric.mod.json`, call the manager, or subscribe to an event bus.

```java
import com.arcanc.pulselib.api.plugin.IPulseClientPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import net.minecraft.resources.ResourceLocation;

@PulsePlugin
public class ExamplePulsePlugin implements IPulseClientPlugin
{
	public static final PModelData MODEL = PModelData.direct(id("example"));

	private static ResourceLocation id(String path)
	{
		return ResourceLocation.fromNamespaceAndPath("example", path);
	}

	@Override
	public ResourceLocation getPluginId()
	{
		return id("main");
	}

	@Override
	public void registerModels(PModelRegistrar registrar)
	{
		registrar.model(MODEL).texture("base", id("example"));
	}
}
```

The example loads `assets/example/glmodels/example.glb`, falling back to `example.gltf`, and declares `assets/example/textures/example.png` as texture `base`. For Gecko models, use `PModelData.direct(id("example"), PGeckoModelLoader.INSTANCE.id())`; place geometry under `geckolib/models` and animations under `geckolib/animations`, as described in [model loaders](model-loaders.md).

## Physical sides

| Annotation | Interface | Where the class is loaded |
| --- | --- | --- |
| `@PulsePlugin` or `@PulsePlugin(PulsePlugin.Side.CLIENT)` | `IPulseClientPlugin` | Physical client, including integrated servers |
| `@PulsePlugin(PulsePlugin.Side.SERVER)` | `IPulseCommonPlugin` | Dedicated server |
| `@PulsePlugin(PulsePlugin.Side.BOTH)` | `IPulseCommonPlugin` | Both physical sides |

`IPulseClientPlugin` extends `IPulseCommonPlugin` and can register channel/event/deformer types as well as client content. SERVER/BOTH classes must not implement the client interface or reference client-only classes in initialization. Split a mod's shared types and client content into two plugins when both sides need the shared types. Every plugin ID must be unique across installed mods. Constructors should only initialize the plugin; use registration methods for content.

## Registration phases

| Method | Registrar | Frequency |
| --- | --- | --- |
| `registerAnimationChannels` | `PTypeRegistrar` | Once |
| `registerAnimationEvents` | `PTypeRegistrar` | Once |
| `registerDeformers` | `PTypeRegistrar` | Once |
| `registerAttachments` | `PAttachmentRegistrar` | Once, client |
| `registerPlayerAnimations` | `PPlayerAnimationRegistrar` | Once, client |
| `registerAnimatedAttachments` | `PAnimatedAttachmentRegistrar` | Once, client |
| `registerModels` | `PModelRegistrar` | Initial resource load and every resource reload |

Phases run in the table's order; plugins run by sorted `ResourceLocation` ID. Type, player animation and model declarations are staged and validated before applying each plugin's batch. Registrars expire when their callback ends: never retain them or use them from asynchronous work. Existing `PRegistry` instances remain the backing registries for types.

Plugin instances survive F3+T. Model declaration maps are cleared, declarations collected, textures resolved and stitched, then models and GPU caches rebuilt. Never cache a `PBakedModel` or atlas sprite indefinitely; resolve through your `PModelData` when rendering.

A malformed external plugin is logged with class, mod and phase. A failure in a one-time phase disables its remaining callbacks; a model declaration failure can retry on the next reload. Internal PulseLib failures abort initialization. Discovery reads bytecode once, filters physical sides before class loading, and never scans in ticks, rendering, or reload handlers.

## Items and armor

Implement `PItemAnimatable.getPulseItemRenderer()` to return a cached `PItemRenderer` / `BlockEntityWithoutLevelRenderer`. Both loaders install the renderer automatically. The item's JSON model must use `"parent": "builtin/entity"`.

Keep renderer construction in a separate client class. For example, the item method can delegate to `ExampleItemClient.renderer(this)`, whose implementation holds the cached renderer. Avoid constructing an anonymous renderer directly inside the shared item class: JVM verification can resolve its client superclass while loading the item on a dedicated server. The integration fixture demonstrates a server-tested arrangement.

Armor items can implement `PArmorModelProvider` to return a `HumanoidModel` and set up its animations on either loader. Living attachments, vanilla armor hiding and additional layers use the existing attachment definitions. NeoForge's existing `IClientItemExtensions` custom/generic models remain supported; the common provider exposes humanoid models, while arbitrary generic NeoForge models retain their platform-specific API.

## Migrating from PulseLib 1.1.x

PulseLib 1.2.0 adds Fabric support and deliberately removes the old NeoForge-specific extension registration mechanism. Existing mods may require source adaptation; source and binary compatibility with 1.1.x is not guaranteed. `@PulsePlugin` is the only extension registration API on both loaders.

| Removed API | Replacement |
| --- | --- |
| `PulseLibEvents` and its five registration event classes | `@PulsePlugin` with the corresponding callback from the phase table |
| `TypeRegistrationEvent` / mixed `PTypeRegistrar.Kind.LEGACY` | Separate `registerAnimationChannels`, `registerAnimationEvents`, `registerDeformers` |
| `RegisterResourceEvent` / `PResourceCache.postEvent()` | `registerModels(PModelRegistrar)` |
| Attachment/player/animated attachment events | The matching `IPulseClientPlugin` callbacks |
| `PLibRegistration.init(IEventBus)` and demo game-object registries | Common `PLibRegistration.init()`; ordinary loader registration for Minecraft content |
| NeoForge-specific `PItemAnimatable.registerClientExtension()` | Common `getPulseItemRenderer()` with a separate client factory |
| Historical event-handler signatures on library managers | Loader callbacks call the common runtime operations; extensions use plugin callbacks |

Move registration bodies into a public annotated plugin, implement the appropriate interface, and supply a unique ID plus a public no-argument constructor. Remove old listeners; do not manually call plugin registration. Register ordinary game blocks/items/entities with the loader, then contribute their PulseLib models/attachments through the plugin. Both loaders use the same plugin class.

A fatal discovery error is terminal for that manager: its flag is reset in `finally`, no partial list is published, and later calls report the original failure without rescanning or reconstructing plugins. A fatal one-time phase enters `FAILED`, clears the active phase and requires a restart. Successfully applied earlier batches are never replayed. Model phases can retry after a failed reload; declarations publish only after the full phase succeeds. External plugin batches that throw are discarded, while healthy extensions continue.

The [external demo mod](../tests/testmod/README.md) contains the library's original block, entity, armor, tail and player demos, with one shared implementation and ordinary loader adapters. Its `ClientTestPlugin` shows all client callbacks without any PulseLib-specific loader entrypoint.
