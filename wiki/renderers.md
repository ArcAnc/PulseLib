# Renderers

A PulseLib renderer is the bridge between Minecraft's render call and PulseLib's baked animated model. It gets the current animatable, asks for model data, binds the animation manager to the baked model, and submits every posed bone to the render queue.

All built-in renderers implement [`PRenderer`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/renderer/PRenderer.java) and use the same submit lifecycle:

* `preSubmit(...)` - hook before PulseLib submits model meshes.
* `trueSubmit(...)` - default PulseLib model submission.
* `postSubmit(...)` - hook after model submission.

Most custom renderers need a model-data constructor and a render-state implementation. Override `preSubmit` or `postSubmit` when you need to submit extra PulseLib geometry or collector nodes around the model.

## Molang context

The built-in entity, block entity, and item renderers prepare a Molang context once per controller and render pass, then reuse it for all mixed bones. Override `populateMolangContext(...)` to add renderer-specific `query.*` values. The full API and the current `variable.*` lifetime limitation are documented in [Molang animations](molang-animations.md).

## Block renderer

Use [`PBlockRenderer`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/renderer/PBlockRenderer.java) for block entities. The block itself should hide vanilla rendering; the block entity renderer becomes the visible model.

```java
public class CrusherRenderer extends PBlockRenderer<CrusherBlockEntity, CrusherRenderState> {
    public CrusherRenderer(BlockEntityRendererProvider.Context context) {
        super(new DefaultBlockModelData.DefaultBlockModelDataBuilder(
                        Identifier.fromNamespaceAndPath("examplemod", "crusher"))
                        .build(),
                PRenderTypes.RenderTypeProvider::trianglesCutout);
    }

    @Override
    public CrusherRenderState createRenderState() {
        return new CrusherRenderState();
    }
}

public class CrusherRenderState extends PBlockRenderState.Impl<CrusherBlockEntity> {}
```

Register it through NeoForge:

```java
@SubscribeEvent
public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerBlockEntityRenderer(MyBlockEntities.CRUSHER.get(), CrusherRenderer::new);
}
```

## Item renderer

Use [`PItemRenderer`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/renderer/PItemRenderer.java) when the item model needs real animation instead of a static baked item JSON. The item must implement [`PAnimatable`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/animatable/PAnimatable.java).

```java
public class WandRenderer extends PItemRenderer<WandItem, WandRenderState> {
    public WandRenderer() {
        super(new DefaultItemModelData.DefaultItemModelDataBuilder(
                        Identifier.fromNamespaceAndPath("examplemod", "wand"))
                        .build(),
                PRenderTypes.RenderTypeProvider::trianglesSolid);
    }

    @Override
    protected WandRenderState createRenderState() {
        return new WandRenderState();
    }
}

public class WandRenderState extends PItemRenderState.Impl<WandItem> {}
```

In GUI context, `PItemRenderer` submits to the `GUI` queue and immediately flushes that stage through the collector. In `FIRST_PERSON_LEFT_HAND` and `FIRST_PERSON_RIGHT_HAND` contexts, it submits to the dedicated `FIRST_PERSON` stage, which PulseLib flushes and composites immediately after the current hand pass. That pass is vanilla normally and PulseLib's replacement pass while an enabled player first-person animation is active. It preserves the render type supplied to its constructor; use a render type compatible with the contexts in which the item is displayed.

## Entity renderer

Use [`PEntityRenderer`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderer.java) when the entire entity model is a PulseLib model.

```java
public class RobotRenderer extends PEntityRenderer<RobotEntity, PEntityRenderState.Impl<RobotEntity>> {
    public RobotRenderer(EntityRendererProvider.Context context) {
        super(context,
                new DefaultEntityModelData.DefaultEntityModelDataBuilder(
                        Identifier.fromNamespaceAndPath("examplemod", "robot")).build(),
                PRenderTypes.RenderTypeProvider::trianglesSolid);
    }

    @Override
    public PEntityRenderState.Impl<RobotEntity> createRenderState() {
        return PLibHelper.entityRenderState();
    }
}
```

For `LivingEntity` subclasses, `PEntityRenderer` applies vanilla body rotation, sleeping/death/spin transforms, entity scale, and head yaw/pitch for a bone named `head`.

## Render type requirement

PulseLib models are triangle meshes. Use [`PRenderTypes.RenderTypeProvider`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java):

```java
PRenderTypes.RenderTypeProvider::trianglesSolid
PRenderTypes.RenderTypeProvider::trianglesCutout
PRenderTypes.RenderTypeProvider::trianglesTranslucent
```

These are the queued variants used by all three renderer classes, including `PItemRenderer` in GUI display context. `trianglesGui` is an instant-rendering compatibility alias and is not compatible with the instanced queue. Do not pass vanilla entity/block `RenderType` values unless they use a compatible triangle vertex format and shader setup.

The renderer APIs use 26.1 render states and `SubmitNodeCollector`; the old `MultiBufferSource`/`BlockEntityWithoutLevelRenderer` examples do not apply to this branch.
