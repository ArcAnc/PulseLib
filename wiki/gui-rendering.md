# GUI Rendering

Minecraft 26.1.2 separates GUI extraction from drawing. PulseLib supports both special item models and arbitrary animated models inside a screen; both use isolated render scopes around actual drawing.

## Animated items

Implement [`PItemRenderer`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/content/renderer/PItemRenderer.java), register its special-model codec for your loader, and reference it from `assets/<modid>/items/<item>.json`. Minecraft invokes the special renderer for inventory and screen items. See [PulseLib Items](pulselib-items.md) for the complete setup.

Use queued `trianglesSolid`, `trianglesCutout`, or `trianglesTranslucent` in the renderer constructor, including for `ItemDisplayContext.GUI`. `trianglesGui` uses the instant layout and belongs to the helper below. GUI item execution uses a local compiler and flushes it when the scope closes; it does not drain pending world geometry or OIT.

## A model inside a screen

Use [`PLibHelper.renderModelInGui(...)`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/helpers/PLibHelper.java) during GUI extraction:

```java
PLibHelper.renderModelInGui(
		graphics, // GuiGraphicsExtractor supplied by the screen
		MODEL_DATA,
		manager.getControllers().values(),
		PRenderTypes.RenderTypeProvider::trianglesGui,
		x, y,
		new Vector3f(32.0f, 32.0f, 32.0f),
		0xFFFFFFFF, packedLight, packedOverlay, partialTick);
```

`MODEL_DATA` must have a matching [model-resource registration](resources.md#register-model-resources). Supply controllers in composition order, tick them through their normal manager lifecycle, and select an instant render type (`trianglesGui` / `trianglesInstantTranslucent`, or an appropriate instant solid/cutout variant).

The helper evaluates and captures the model pose, GUI pose, position, scale, tint, light, overlay and scissor for that invocation. It adds a native picture-in-picture render state rather than issuing GPU work during extraction. Later controller, pose-stack or scale-vector changes do not alter the captured pose and transform. A model that has not loaded, or a model whose projected bounds are fully clipped, adds no GUI state. Keep a supplied render-type function stable until execution.

PulseLib registers its picture-in-picture renderer on both loaders. The helper uses native GUI bounds and clipping, including nested screen calls. Do not manually flush the world queue from a screen callback.

## Custom offscreen rendering

Advanced callers can use [`PRenderContexts`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/content/renderer/PRenderContexts.java) for local GUI/offscreen queue ownership. Bind the intended output attachments before entering the scope and keep them alive until it closes. Enter around actual render execution: a `SubmitNodeCollector` callback may execute after its extraction method has returned.

Use normal item rendering or the helper for standard screens; they already establish the required lifetime and state restoration.
