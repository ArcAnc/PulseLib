# GUI Rendering

PulseLib can draw a baked model directly in a GUI through [`PLibHelper.renderModelInGui`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/util/helpers/PLibHelper.java).

```java
PLibHelper.renderModelInGui(
        guiGraphics,
        modelData,
        manager.getControllers().values(),
        PRenderTypes.RenderTypeProvider::trianglesGui,
        80,
        60,
        new Vector3f(24.0f, 24.0f, 24.0f),
        0xFFFFFFFF,
        LightTexture.FULL_BRIGHT,
        OverlayTexture.NO_OVERLAY,
        partialTick);
```

Parameters:

* `GuiGraphics guiGraphics` - target GUI renderer.
* `PModelData modelData` - model to draw.
* `Collection<PAnimationController<T>> controllers` - active controllers used to pose bones.
* `Function<ResourceLocation, RenderType> renderType` - usually `trianglesGui`.
* `x`, `y` - GUI position.
* `Vector3f scale` - model scale.
* `packedColor` - ARGB color.
* `packedLight` and `packedOverlay` - standard Minecraft packed values.
* `partialTick` - interpolation time.

For item rendering in GUI, [`PItemRenderer`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/content/renderer/PItemRenderer.java) already switches to immediate GUI drawing internally.

`trianglesGui` uses the `triangles_immediate_lit` program. It receives color, light, overlay, normal matrix, model-view matrix, and projection matrix as uniforms rather than instance attributes. Consequently, GUI rendering does not use the queued instance buffer, multi-draw indirect, weighted OIT, or shader-side deformer streams; deformable GUI meshes use the CPU vertex-buffer path. See [Shaders](shaders.md) for the complete immediate shader contract.

Classes used:

* [`PLibHelper`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/util/helpers/PLibHelper.java)
* [`PBakedModel`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/content/model/baked/PBakedModel.java)
* [`PRenderTypes`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java)
