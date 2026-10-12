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

The example loads `assets/example/glmodels/example.gltf` and declares `assets/example/textures/example.png` as texture `base`. For Gecko models, use `PModelData.direct(id("example"), PGeckoModelLoader.INSTANCE.id())`; place geometry under `geckolib/models` and animations under `geckolib/animations`, as described in [model loaders](model-loaders.md).

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

## Migrating NeoForge integrations

1. Move each `PulseLibEvents` registration listener's body into the matching plugin method from the table.
2. Add `@PulsePlugin`, implement `IPulseClientPlugin`, supply an ID and a public no-argument constructor.
3. Remove the old listener for registrations you moved. Do not register the same content through both APIs.
4. Replace `registerClientExtension()` custom renderer overrides with `getPulseItemRenderer()` and move renderer construction into a client helper.
5. Compile shared sources against Common/Mojang mappings; put remaining loader event listeners in the loader module.

`PulseLibEvents` remains on NeoForge with its original packages and nested registration classes, deprecated as a compatibility adapter. Resource events still run on reload; types/attachments/player animations/animated attachments run once. Event registrar changes are committed after dispatch, so a listener must not read a just-declared type from the backing registry during that same dispatch.

`PLib`, NeoForge `PLibRegistration.init(IEventBus)`, `PRegistry`, model/animation/rendering packages, and `PItemAnimatable.registerClientExtension()` remain available on NeoForge. The latter is deprecated and still honors existing overrides. Classes containing loader lifecycle handlers (`ClientEvents`, `CommonEvents`, `PRenderStagesHandler`, animation ticker/manager event methods, `PLibArmorHandler`) now delegate to common runtime methods; directly calling their old event-handler signatures requires source migration. Their new signatures describe the operation rather than a loader event.
