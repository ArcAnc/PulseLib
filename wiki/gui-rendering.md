# GUI Rendering

PulseLib 26.2 renders animated item models in a GUI through [`PItemRenderer`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/content/renderer/PItemRenderer.java). The renderer detects `ItemDisplayContext.GUI` and registers custom geometry that performs an immediate draw in the current GUI render pass. It preserves the item-specific mesh resolver and Molang context, and the instant shader supports GPU deformers.

There is no public `PLibHelper.renderModelInGui(...)` helper in this branch. For an item, implement a `PItemRenderer` and let Minecraft invoke its special-model renderer:

```java
public final class WandRenderer extends PItemRenderer<WandItem, WandRenderState> {
    public WandRenderer(PModelData data) {
        super(data, PRenderTypes.RenderTypeProvider::trianglesSolid);
    }

    @Override
    protected WandRenderState createRenderState() {
        return new WandRenderState();
    }
}

public final class WandRenderState extends PItemRenderState.Impl<WandItem> {
}
```

For custom GUI-only geometry, use [`PBakedModel.instantDraw(...)`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/content/model/baked/PBakedModel.java) or [`PBakedBone.instantDraw(...)`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/content/model/baked/PBakedBone.java) from a `SubmitNodeCollector` custom-geometry callback. Keep that code inside a renderer or special-model renderer: it needs the callback's saved pose plus the packed light/overlay values supplied by Minecraft.

The custom-geometry callback itself uses `trianglesInstantTranslucent`, but that is only the callback's scheduling type. Each mesh still starts with the renderer's supplied base render type and is converted to the matching instant solid, cutout, or translucent variant; emissive meshes receive the matching emissive instant variant. Use `withAlphaMode(...)` in `resolveMeshRender(...)` to select the instant variant from a mesh's alpha classification. `trianglesGui` remains a compatibility alias for instant translucent rendering, but there is no separate GUI shader.

Classes used:

* [`PItemRenderer`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/content/renderer/PItemRenderer.java)
* [`PItemRenderState`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/content/renderer/base/PItemRenderState.java)
* [`PRenderQueue`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/content/renderer/PRenderQueue.java)
* [`PRenderTypes`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java)
