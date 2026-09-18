# Resources

> [!IMPORTANT]
> PulseLib does not draw model textures directly from arbitrary files. It first collects them into a runtime atlas, then the baked model stores UVs for that atlas. This is why model-resource registration is a required step instead of an optional convenience.

Model resources are registered through [`PulseLibEvents.RegisterResourceEvent`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/event/PulseLibEvents.java).

## Register model resources

Subscribe on the mod event bus and register every model together with its material texture references:

```java
@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ExampleClientEvents {
    @SubscribeEvent
    public static void registerPulseResources(PulseLibEvents.RegisterResourceEvent event) {
        event.model(Identifier.fromNamespaceAndPath(ExampleMod.MOD_ID, "entity/robot"))
                .texture("textures/body", Identifier.fromNamespaceAndPath(
                        ExampleMod.MOD_ID, "entity/robot/body"))
                .texture("textures/eyes", Identifier.fromNamespaceAndPath(
                        ExampleMod.MOD_ID, "entity/robot/eyes"));
    }
}
```

`event.model(...)` uses the glTF loader by default. The model id is relative to its loader root: the glTF loader adds `glmodels/`, and the Gecko loader adds `geckolib/models/`; call `event.model(model, PGeckoModelLoader.INSTANCE.id())` when registering a Gecko model. Repeated calls for one model extend the same registration. A resource registration is also what makes PulseLib load and bake that model, so every `PModelData` path needs one matching registration.

The glTF loader accepts both `.glb` and `.gltf`. When the registered id has no extension, PulseLib tries `<model>.glb` first and then `<model>.gltf`; the same fallback applies when the registered extension is missing. If both files exist, the registered extension wins, and an extension-less registration therefore selects `.glb`. Use the actual extension in `PModelData`, because baked models are stored under the path of the file that was loaded:

```java
event.model(Identifier.fromNamespaceAndPath("examplemod", "entity/robot"));

PModelData data = new PModelData.Builder(
        Identifier.fromNamespaceAndPath("examplemod", "glmodels/entity/robot.gltf"),
        "").build();
```
If neither candidate exists, resource reload fails with `Registered model was not loaded; tried: ...`, followed by both paths.

Each `texture` key is the reference stored in the model material. It preserves its complete directory path, while a final `.png` is ignored: `body/claws.png` becomes `body/claws`, and remains distinct from `armor/claws`. The value is a Minecraft resource location relative to `textures` without `.png`:

```text
assets/examplemod/textures/entity/robot/body.png
```

becomes:

```java
Identifier.fromNamespaceAndPath("examplemod", "entity/robot/body")
```

## Gecko model fallback texture

When a cube in a Gecko model has no `texture` field, PulseLib assigns the material reference `"0"`. Register that reference as the model's fallback texture:

```java
event.model(Identifier.fromNamespaceAndPath("examplemod", "entity/robot"),
                PGeckoModelLoader.INSTANCE.id())
        .texture("0", Identifier.fromNamespaceAndPath("examplemod", "entity/robot/fallback"));
```

## Runtime atlas

The atlas is registered by PulseLib itself. Your mod contributes model-resource texture mappings.

Runtime atlas classes:

* [`PResourceCache`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/PResourceCache.java)
* [`RuntimeLoader`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/model/textures/atlas/RuntimeLoader.java)

PulseLib registers the atlas at:

```java
PResourceCache.ATLAS_LOCATION // pulselib:textures/atlas.png
```

Renderers normally pass `PResourceCache.ATLAS_LOCATION` to `PRenderTypes`, so you rarely need to access the atlas manually.

## Alpha modes

PulseLib classifies each atlas sprite into one of four alpha modes:

* `opaque` always selects the solid pipeline.
* `cutout` selects the cutout pipeline for fully transparent holes and hard edges.
* `translucent` selects the blended pipeline and weighted OIT when supported.
* `auto` inspects the sprite while the model is baked. All-alpha-255 textures become opaque, textures containing only alpha 0 and 255 become cutout, and any intermediate alpha makes the texture translucent.

The default metadata mode is `auto`. You can override it next to the emissive flag in the texture's `.png.mcmeta` file:

```json
{
  "pulselib": {
    "alpha_mode": "translucent",
    "emissive": false
  }
}
```

The render type supplied to the renderer remains authoritative by default. Opt into the sprite's baked classification with `withAlphaMode(PAlphaMode.AUTO)`, or force `OPAQUE`, `CUTOUT`, or `TRANSLUCENT`. Passing `null` restores the renderer's original render-type function.

## Per-mesh texture, emissive, and alpha overrides

`PMeshRenderContext` can override a mesh's colour, packed light, packed overlay, deformation, texture, emissive state, or alpha mode at render time. The replacement texture must still be registered in the runtime atlas. The first use of a `(base mesh, replacement texture)` pair lazily bakes and caches a matching `PBakedMesh`; later renders reuse it.

```java
@Override
public PMeshRenderContext resolve(PBakedBone bone, PBakedMesh mesh,
                                  PMeshRenderContext inherited) {
    if (!mesh.textureReference().equals("textures/eyes"))
        return inherited;
    return inherited
            .withTexture(Identifier.fromNamespaceAndPath("examplemod", "entity/robot/eyes_active"))
            .withEmissive(true)
            .withAlphaMode(PAlphaMode.AUTO);
}
```

Pass `null` to `withTexture`, `withEmissive`, or `withAlphaMode` to return to the mesh's baked texture, metadata-derived emissive value, or renderer-selected pipeline respectively. An explicit `false` disables emissive even when the selected sprite metadata marks it emissive. Replacement texture variants cache their own alpha classification.

Prefer `withColor(...)`, `withPackedLight(...)`, `withPackedOverlay(...)`, and the other `with...` methods when changing an inherited context. They preserve every override already attached by another resolver.

## Emissive textures

Emissive textures are useful for eyes, screens, lamps, energy parts, and other pieces that should ignore normal light. PulseLib reads this flag from texture metadata through [`PLibSpriteMetadata`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/model/textures/atlas/PLibSpriteMetadata.java).

To mark a texture as emissive, add a `.png.mcmeta` file next to it:

```text
assets/examplemod/textures/entity/robot/eyes.png
assets/examplemod/textures/entity/robot/eyes.png.mcmeta
```

```json
{
  "pulselib": {
    "emissive": true
  }
}
```

When `PModelCache` bakes the model, each mesh stores whether its sprite is emissive. The default renderers also honour a `PMeshRenderContext.withEmissive(...)` override and automatically switch to an emissive variant through:

```java
PRenderTypes.RenderTypeProvider.emissiveVariant(baseType, PResourceCache.ATLAS_LOCATION);
```

You can also choose an emissive render type directly in custom rendering code:

```java
PRenderTypes.RenderTypeProvider::trianglesEmissiveCutout
PRenderTypes.RenderTypeProvider::trianglesEmissiveTranslucent
PRenderTypes.RenderTypeProvider::trianglesEmissiveSolid
```

Choose the solid, cutout, or translucent variant according to the desired alpha mode. Direct `PBakedBone.instantDraw(...)` rendering has matching `trianglesInstantEmissiveSolid`, `trianglesInstantEmissiveCutout`, and `trianglesInstantEmissiveTranslucent` variants; `trianglesGui` remains a compatibility alias for the non-emissive instant translucent pipeline.

Classes used:

* [`PResourceCache`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/PResourceCache.java)
* [`RuntimeLoader`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/model/textures/atlas/RuntimeLoader.java)
* [`PLibSpriteMetadata`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/model/textures/atlas/PLibSpriteMetadata.java)
* [`PAlphaMode`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/model/textures/PAlphaMode.java)
* [`PTextureAlphaClassifier`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/model/textures/PTextureAlphaClassifier.java)
* [`PRenderTypes`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java)
