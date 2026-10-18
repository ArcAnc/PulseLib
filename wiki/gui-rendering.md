# GUI Rendering

PulseLib can draw a baked model directly in a GUI through [`PLibHelper.renderModelInGui`](../common/src/main/java/com/arcanc/pulselib/util/helpers/PLibHelper.java).

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

For item rendering in GUI, [`PItemRenderer`](../common/src/main/java/com/arcanc/pulselib/content/renderer/PItemRenderer.java) already switches to immediate GUI drawing internally.

The helper draws the current pose; it does not initialize or tick controllers or bind their manager to the model. Resolve an initialized manager, wait for `modelData.getModel()` to become available, bind that model to the manager, and advance animation through your tick lifecycle before drawing. Resolve models again after resource reload.

`trianglesGui` uses the `triangles_immediate_lit` program. It receives color, light, overlay, normal matrix, model-view matrix, and projection matrix as uniforms rather than instance attributes. Consequently, GUI rendering does not use the queued instance buffer, multi-draw indirect, weighted OIT, or shader-side deformer streams; deformable GUI meshes use the CPU vertex-buffer path. See [Shaders](shaders.md) for the complete immediate shader contract.

## Entity previews and offscreen targets

Rendering a PulseLib entity through Minecraft's entity dispatcher uses a different path. The inventory/dispatcher hooks enter a `PRenderContexts` scope, capture its meshes in a local queue, and execute that queue when the scope closes. This retains instancing and GPU deformers, uses sorted transparency without world OIT, and restores the captured framebuffer and OpenGL state. Preview submissions are not retained for the next world render stage.

For a custom render target, bind the target before entering the scope. On the render thread:

```java
target.bindWrite(true);
try (PRenderContexts.Scope scope = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
{
	try
	{
		renderPreviewEntity();
	}
	catch (RuntimeException | Error failure)
	{
		scope.discard();
		throw failure;
	}
}
```

Here `target` is your render target and `renderPreviewEntity()` invokes your entity preview renderer. The caller restores its previous destination after completing the offscreen pass. Nested scopes must close on the same render thread in reverse order. Discard incomplete submissions before rethrowing a rendering failure; closing still restores the scope's state. Standard previews already have these hooks and need no manual scope or world-queue flush.

Classes used:

* [`PLibHelper`](../common/src/main/java/com/arcanc/pulselib/util/helpers/PLibHelper.java)
* [`PBakedModel`](../common/src/main/java/com/arcanc/pulselib/content/model/baked/PBakedModel.java)
* [`PRenderTypes`](../common/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java)
