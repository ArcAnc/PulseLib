# Resources

> [!IMPORTANT]
> PulseLib does not draw model textures directly from arbitrary files. It first collects them into a runtime atlas, then the baked model stores UVs for that atlas. This is why model-resource registration is a required step instead of an optional convenience.

Model resources are registered through [`PulseLibEvents.RegisterResourceEvent`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/event/PulseLibEvents.java).

## Register model resources

Subscribe on the mod event bus and register every model together with its material texture references:

```java
@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ExampleClientEvents {
    @SubscribeEvent
    public static void registerPulseResources(PulseLibEvents.RegisterResourceEvent event) {
        event.model(ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "entity/robot"))
                .texture("textures/body", ResourceLocation.fromNamespaceAndPath(
                        ExampleMod.MOD_ID, "entity/robot/body"))
                .texture("textures/eyes", ResourceLocation.fromNamespaceAndPath(
                        ExampleMod.MOD_ID, "entity/robot/eyes"));
    }
}
```

`event.model(...)` uses the glTF loader by default. The model id is relative to its loader root: the glTF loader adds `glmodels/`, and the Gecko loader adds `geckolib/models/`; call `event.model(model, PGeckoModelLoader.INSTANCE.id())` when registering a Gecko model. Repeated calls for one model extend the same registration. A resource registration is also what makes PulseLib load and bake that model, so every `PModelData` path needs one matching registration.

The glTF loader accepts both `.glb` and `.gltf`. When the registered id has no extension, PulseLib tries `<model>.glb` first and then `<model>.gltf`; the same fallback applies when the registered extension is missing. If both files exist, the registered extension wins, and an extension-less registration therefore selects `.glb`. Use the actual extension in `PModelData`, because baked models are stored under the path of the file that was loaded:

```java
event.model(ResourceLocation.fromNamespaceAndPath("examplemod", "entity/robot"));

PModelData data = new PModelData.Builder(
        ResourceLocation.fromNamespaceAndPath("examplemod", "glmodels/entity/robot.gltf"),
        "").build();
```
If neither candidate exists, resource reload fails with `Registered model was not loaded: ...; checked resources: ...`, followed by both paths.

Each `texture` key is the reference stored in the model material. It preserves its complete directory path, while a final `.png` is ignored: `body/claws.png` becomes `body/claws`, and remains distinct from `armor/claws`. The value is a Minecraft resource location relative to `textures` without `.png`:

```text
assets/examplemod/textures/entity/robot/body.png
```

becomes:

```java
ResourceLocation.fromNamespaceAndPath("examplemod", "entity/robot/body")
```

## Animation-event sidecars

An animation-event sidecar is optional and needs no `RegisterResourceEvent` entry: the model loader finds it while loading its model or animation file. The first existing candidate is used, so keep only one sidecar for a model unless you deliberately want the earlier path to take precedence. Its contents use the `animations` JSON object described in [Animation events](animation-events.md).

For a glTF model at `assets/examplemod/glmodels/entity/robot.glb` or `.gltf`, PulseLib checks these paths in order:

```text
assets/examplemod/glmodels/entity/robot.events.json
assets/examplemod/glmodels/entity/robot.animation_events.json
assets/examplemod/glmodels/events/entity/robot.events.json
assets/examplemod/glmodels/events/robot.events.json
```

Gecko sidecars are associated with the animation JSON that the loader selected. For an animation at `assets/examplemod/geckolib/animations/robot.animation.json`, the candidates are:

```text
assets/examplemod/geckolib/animations/robot.events.json
assets/examplemod/geckolib/animations/robot.animation_events.json
assets/examplemod/geckolib/animations/events/robot.events.json
```

When the selected Gecko animation is in a subdirectory, the first two sidecar paths stay beside that selected animation file and the third path uses an `events/` directory beside it.

### glTF sidecar example

`time` is specified in seconds. The following file at `assets/examplemod/glmodels/entity/robot.events.json` adds effects, a callback, a graph-controller trigger, and visibility tracks to the `attack` animation:

```json
{
  "animations": {
    "attack": {
      "events": [
        {
          "type": "sound",
          "time": 0.15,
          "sound": "minecraft:entity.player.attack.strong",
          "locator": "right_hand",
          "volume": 0.9,
          "pitch": 1.1
        },
        {
          "type": "particle",
          "time": 0.18,
          "particle": "minecraft:crit",
          "locator": "right_hand",
          "offset": [0.0, 0.0, 0.0],
          "motion": [0.0, 0.05, 0.0]
        },
        {
          "type": "locator_callback",
          "time": 0.20,
          "callback": "examplemod:attack_hit",
          "locator": "right_hand"
        },
        {
          "type": "animation_parameter",
          "time": 0.35,
          "controller": "combat",
          "parameter": "attack_complete",
          "trigger": true
        }
      ],
      "visibility": {
        "weapon": {
          "0.0": false,
          "0.12": true,
          "0.45": false
        },
        "muzzle_flash": [
          { "time": 0.18, "visible": true },
          { "time": 0.23, "visible": false }
        ]
      }
    }
  }
}
```

`locator_callback` invokes a callback registered through `PAnimationEventCallbacks`. `animation_parameter` addresses a graph controller; an empty `controller` uses the current graph controller, and `trigger: true` invokes the named trigger. Event types may use built-in short names, as above, or namespaced identifiers.

### Gecko sidecar example

The format is identical for Gecko. A sidecar may also omit the outer `animations` object. For example, `assets/examplemod/geckolib/animations/robot.animation_events.json` can contain:

```json
{
  "animation.robot.idle": {
    "events": [
      {
        "type": "camera_shake",
        "time": 0.0,
        "strength": 0.15,
        "duration": 2,
        "frequency": 8
      }
    ],
    "visibility": {
      "glow": [
        { "time": 0.0, "visible": false },
        { "time": 0.5, "visible": true }
      ]
    }
  }
}
```

The animation key must exactly match the animation name in the loaded model. Both visibility forms shown above are supported.

## Gecko model fallback texture

When a cube in a Gecko model has no `texture` field, PulseLib assigns the material reference `"0"`. Register that reference as the model's fallback texture:

```java
event.model(ResourceLocation.fromNamespaceAndPath("examplemod", "entity/robot"),
                PGeckoModelLoader.INSTANCE.id())
        .texture("0", ResourceLocation.fromNamespaceAndPath("examplemod", "entity/robot/fallback"));
```

## Runtime atlas

The atlas is registered by PulseLib itself. Your mod contributes model-resource texture mappings.

Runtime atlas classes:

* [`PResourceCache`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/util/PResourceCache.java)
* [`RuntimeLoader`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/model/textures/atlas/RuntimeLoader.java)

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
            .withTexture(ResourceLocation.fromNamespaceAndPath("examplemod", "entity/robot/eyes_active"))
            .withEmissive(true)
            .withAlphaMode(PAlphaMode.AUTO);
}
```

Pass `null` to `withTexture`, `withEmissive`, or `withAlphaMode` to return to the mesh's baked texture, metadata-derived emissive value, or renderer-selected pipeline respectively. An explicit `false` disables emissive even when the selected sprite metadata marks it emissive. Replacement texture variants cache their own alpha classification.

Prefer `withColor(...)`, `withPackedLight(...)`, `withPackedOverlay(...)`, and the other `with...` methods when changing an inherited context. They preserve every override already attached by another resolver.

## Emissive textures

Emissive textures are useful for eyes, screens, lamps, energy parts, and other pieces that should ignore normal light. PulseLib reads this flag from texture metadata through [`PLibMetadata`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/model/textures/atlas/PLibMetadata.java).

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

You can also choose an emissive render type directly in custom rendering code. Choose the solid, cutout, or translucent variant according to the desired alpha mode:

```java
PRenderTypes.RenderTypeProvider::trianglesSolidEmissive
PRenderTypes.RenderTypeProvider::trianglesCutoutEmissive
PRenderTypes.RenderTypeProvider::trianglesTranslucentEmissive
```

Direct `PBakedBone.instantDraw(...)` accepts the same render-type functions. Use `trianglesImmediateEmissive` for an opaque immediate draw or `trianglesGuiEmissive` for a translucent immediate draw; `trianglesGui` is the non-emissive translucent immediate type.

Classes used:

* [`PResourceCache`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/util/PResourceCache.java)
* [`RuntimeLoader`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/model/textures/atlas/RuntimeLoader.java)
* [`PLibMetadata`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/model/textures/atlas/PLibMetadata.java)
* [`PAlphaMode`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/model/textures/PAlphaMode.java)
* [`PTextureAlphaClassifier`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/model/textures/PTextureAlphaClassifier.java)
* [`PRenderTypes`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java)
