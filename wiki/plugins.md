# Automatic PulseLib plugins

The Minecraft 26.1.2 MultiLoader branch uses the public plugin API from `1.21.1-multiloader`. Both Fabric and NeoForge discover plugins and route their lifecycle through shared Common services. Client and dedicated-server bootstrap, reload and renderer integrations have verification evidence on both loaders; the [migration journal](../docs/porting/26.1.2-multiloader.md) records the exact coverage and remaining work.

A public annotated class with a public no-argument constructor is discovered automatically among installed mods. Register ordinary Minecraft blocks, items and entities with the loader; declare PulseLib extensions through plugin callbacks.

```java
import com.arcanc.pulselib.api.plugin.IPulseClientPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import net.minecraft.resources.Identifier;

@PulsePlugin
public final class ExamplePulsePlugin implements IPulseClientPlugin
{
	public static final PModelData MODEL = PModelData.direct(id("example"));

	private static Identifier id(String path)
	{
		return Identifier.fromNamespaceAndPath("examplemod", path);
	}

	@Override
	public Identifier getPluginId()
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

This declares `assets/examplemod/glmodels/example.glb` (with `.gltf` fallback) and `assets/examplemod/textures/example.png` as material reference `base`. See [resources](resources.md). No PulseLib-specific mod entrypoint or event subscription is needed. The manager owns callback dispatch, batch application and registrar closure.

## Physical sides

| Annotation | Interface | Physical process |
| --- | --- | --- |
| `@PulsePlugin` / `Side.CLIENT` | `IPulseClientPlugin` | Client, including integrated servers |
| `@PulsePlugin(PulsePlugin.Side.SERVER)` | `IPulseCommonPlugin` | Dedicated server |
| `@PulsePlugin(PulsePlugin.Side.BOTH)` | `IPulseCommonPlugin` | Both |

`IPulseClientPlugin` extends `IPulseCommonPlugin`. SERVER/BOTH plugins must not implement the client interface or initialize client classes. Use separate plugins for shared types and client content. Every plugin ID must be unique. Constructors initialize the plugin itself; registrations belong in callbacks. Discovery filters physical sides before requesting the plugin class from the class loader.

## Registration phases

| Callback | Registrar | Frequency |
| --- | --- | --- |
| `registerAnimationChannels` | `PTypeRegistrar` | Once |
| `registerAnimationEvents` | `PTypeRegistrar` | Once |
| `registerDeformers` | `PTypeRegistrar` | Once |
| `registerAttachments` | `PAttachmentRegistrar` | Once, client |
| `registerPlayerAnimations` | `PPlayerAnimationRegistrar` | Once, client |
| `registerAnimatedAttachments` | `PAnimatedAttachmentRegistrar` | Once, client |
| `registerModels` | `PModelRegistrar` | Initial resource load and every resource reload |

Phases run in this order; plugins run by sorted `Identifier` ID. A registrar and its model builders expire after the callback. Do not retain them or register asynchronously. Type, player animation and model declarations are staged and checked before each plugin's batch is applied. Plugin instances survive resource reloads; one-time callbacks do not repeat. Resolve baked models and atlas sprites again after reload.

External plugin failures are logged with class, owning mod and phase. A failed one-time callback disables that plugin's remaining callbacks; a failed model declaration can retry on a later reload. Internal PulseLib failures abort initialization. Fatal discovery failures are terminal without publishing partial instances or rescanning. A failed internal one-time phase requires restart; model phases can recover.

## Migrating the NeoForge event API

| Previous API | Callback replacement |
| --- | --- |
| `PulseLibEvents.TypeRegistrationEvent` | Separate channel, event and deformer callbacks |
| `PulseLibEvents.RegisterResourceEvent` | `registerModels(PModelRegistrar)` |
| `AttachmentRegistrationEvent` | `registerAttachments(PAttachmentRegistrar)` |
| `PlayerAnimationRegistrationEvent` | `registerPlayerAnimations(PPlayerAnimationRegistrar)` |
| `PlayerAnimatedAttachmentRegistrationEvent` | `registerAnimatedAttachments(PAnimatedAttachmentRegistrar)` |

Remove the old mod-bus listeners when moving registration bodies into plugins. `PulseLibEvents` and `PResourceCache.postEvent()` are removed. Internal `PLibRegistration` registries remain the runtime backing stores. The migration preserves library version `1.1.5.3`; it does not represent a finished MultiLoader release yet.

[Migration journal](../docs/porting/26.1.2-multiloader.md) · [External plugin/rendering fixture](../tests/stage1/neoforge/java/com/arcanc/pulselib/test/stage1/Stage1Client.java)
