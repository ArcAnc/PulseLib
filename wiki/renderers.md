# Renderers

A PulseLib renderer is the bridge between Minecraft's render call and PulseLib's baked animated model. It gets the current animatable, asks for model data, binds the animation manager to the baked model, and submits every posed bone to the render queue. Static baked meshes go through the geometry-arena path; supported deformer stacks add per-instance GPU deformation data, while unsupported stacks use the CPU dynamic-geometry fallback. See [Render backend](render-backend.md) for the execution details.

All built-in renderers implement [`PRenderer`](../common/src/main/java/com/arcanc/pulselib/content/renderer/PRenderer.java) and use the same submit lifecycle:

* `preSubmit(...)` - hook before PulseLib submits model meshes.
* `trueSubmit(...)` - default PulseLib model submission.
* `postSubmit(...)` - hook after model submission.

Most custom renderers only need a constructor. Register each model and its material texture references as a `PModelResource`, since resource registration determines what PulseLib loads. Override `preSubmit` or `postSubmit` when you need to draw something extra around the PulseLib model.

## Molang context

The built-in entity, block entity, and item renderers prepare frame queries once per controller and render pass, then reuse them for all mixed bones. Each controller retains its own persistent variables and random state. Override `populateMolangContext(...)` to add renderer-specific `query.*` values. See [Molang animations](molang-animations.md) for context lifetime and expression behavior.

## Block renderer

Use [`PBlockRenderer`](../common/src/main/java/com/arcanc/pulselib/content/renderer/PBlockRenderer.java) for block entities. The block itself should hide vanilla rendering; the block entity renderer becomes the visible model.

```java
public class CrusherRenderer extends PBlockRenderer<CrusherBlockEntity>
{
	public CrusherRenderer(BlockEntityRendererProvider.Context context)
	{
		super(PModelData.block(
						ResourceLocation.fromNamespaceAndPath("examplemod", "crusher")),
				PRenderTypes.RenderTypeProvider::trianglesCutout);
	}
}
```

Register it on the NeoForge mod event bus, on the client:

```java
@SubscribeEvent
public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
{
	event.registerBlockEntityRenderer(MyBlockEntities.CRUSHER.get(), CrusherRenderer::new);
}
```

On Fabric, register it from your client initializer instead:

```java
BlockEntityRendererRegistry.register(MyBlockEntities.CRUSHER.get(), CrusherRenderer::new);
```

Pass your registered block-entity type; the holder name and `.get()` depend on your mod's registry API. Entity renderers use the same NeoForge event's `registerEntityRenderer(...)`, or Fabric's `EntityRendererRegistry.register(...)`.

## Item renderer

Use [`PItemRenderer`](../common/src/main/java/com/arcanc/pulselib/content/renderer/PItemRenderer.java) when the item model needs real animation instead of a static baked item JSON. The item implements [`PItemAnimatable`](../common/src/main/java/com/arcanc/pulselib/content/animatable/PItemAnimatable.java) and returns its cached renderer from `getPulseItemRenderer()`. Both loader adapters install it automatically; keep renderer construction in a separate client helper as shown in [Items](pulselib-items.md).

```java
public class WandRenderer extends PItemRenderer<WandItem>
{
	public WandRenderer(BlockEntityRenderDispatcher blockEntityRenderDispatcher, EntityModelSet entityModelSet)
	{
		super(PModelData.item(
						ResourceLocation.fromNamespaceAndPath("examplemod", "wand")),
				PRenderTypes.RenderTypeProvider::trianglesSolid,
				blockEntityRenderDispatcher,
				entityModelSet);
	}
}
```

In GUI context, `PItemRenderer` uses `trianglesGui` and immediate drawing. It deliberately bypasses the world/entity queue, so GUI poses and CPU deformation do not consume a queued render stage.

## Entity renderer

Use [`PEntityRenderer`](../common/src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderer.java) when the entire entity model is a PulseLib model.

```java
public class RobotRenderer extends PEntityRenderer<RobotEntity>
{
	public RobotRenderer(EntityRendererProvider.Context context)
	{
		super(context,
				PModelData.entity(
						ResourceLocation.fromNamespaceAndPath("examplemod", "robot")),
				PRenderTypes.RenderTypeProvider::trianglesSolid);
	}
}
```

For `LivingEntity` subclasses, `PEntityRenderer` applies vanilla body rotation, sleeping/death/spin transforms, entity scale, and head yaw/pitch for a bone named `head`.

## Render type requirement

PulseLib models are triangle meshes. Use [`PRenderTypes.RenderTypeProvider`](../common/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java):

```java
PRenderTypes.RenderTypeProvider::trianglesSolid
PRenderTypes.RenderTypeProvider::trianglesCutout
PRenderTypes.RenderTypeProvider::trianglesTranslucent
PRenderTypes.RenderTypeProvider::trianglesGui
```

Do not pass vanilla entity/block `RenderType` values unless they use a compatible triangle vertex format and shader setup.

World renderers use the queued instanced shader path. `trianglesGui` uses the immediate path instead, and `trianglesImmediate` is available for advanced direct rendering. See [Shaders](shaders.md) for the exact program, attribute, and sampler mapping.

`PModelData.getModel()` is nullable until the model is loaded. Built-in renderers skip unavailable models. Custom consumers should do the same and resolve baked models and atlas sprites again after resource reload, rather than retaining stale GPU resources.
