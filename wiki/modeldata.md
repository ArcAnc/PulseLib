# Model Data

[`PModelData`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/renderer/modelData/PModelData.java) tells a renderer which canonical model to obtain. It never contains a loader root or filename extension. [`PModelResource`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/model/resource/PModelResource.java) holds the matching registration and texture mappings.

Use the factories for the conventional canonical IDs:

```java
PModelData block = PModelData.block(Identifier.fromNamespaceAndPath("examplemod", "crusher"));
PModelData item = PModelData.item(Identifier.fromNamespaceAndPath("examplemod", "wand"));
PModelData entity = PModelData.entity(Identifier.fromNamespaceAndPath("examplemod", "robot"));
PModelData armor = PModelData.entityLayer(
        Identifier.fromNamespaceAndPath("examplemod", "robot"),
        Identifier.fromNamespaceAndPath("examplemod", "armor"));
PModelData gui = PModelData.direct(Identifier.fromNamespaceAndPath("examplemod", "gui/gene_screen"));
```

The first three create `block/crusher`, `item/wand`, and `entity/robot`; `entityLayer(...)` creates `entity/robot/armor`. `direct(...)` uses the supplied canonical path unchanged. Each factory has an overload taking a loader ID:

```java
PModelData geckoRobot = PModelData.entity(
        Identifier.fromNamespaceAndPath("examplemod", "robot"),
        PGeckoModelLoader.INSTANCE.id());
```

For the default glTF loader, `examplemod:entity/robot` is resolved internally to `assets/examplemod/glmodels/entity/robot.glb`, then to `.gltf` when the GLB is absent. The cache and renderer keep using `examplemod:entity/robot` in both cases.

For renderers constructed in code, register the same object used by the renderer so the ID cannot drift:

```java
public static final PModelData ROBOT_MODEL =
        PModelData.entity(Identifier.fromNamespaceAndPath("examplemod", "robot"));

event.model(ROBOT_MODEL)
        .texture("textures/body", Identifier.fromNamespaceAndPath("examplemod", "entity/robot/body"));
```

`PModelData` instances are not compared with each other. The resource and baked-model caches are keyed by canonical `Identifier`, so separate instances select the same baked model when their `model_id` values are equal.

Item special renderers are data-driven: their renderer `PModelData` is decoded from item JSON after resource registrations are collected. Create a separate static `PModelData` for registration:

```java
public static final PModelData WAND_RESOURCE =
        PModelData.item(Identifier.fromNamespaceAndPath("examplemod", "wand"));

event.model(WAND_RESOURCE)
        .texture("body", Identifier.fromNamespaceAndPath("examplemod", "item/wand/body"));
```

```json
{
  "model_id": "examplemod:item/wand"
}
```

The decoded object is a different `PModelData` instance, but it looks up `examplemod:item/wand` in `PModelCache`, which is the entry loaded from `WAND_RESOURCE`. If JSON supplies `model_loader`, it must match the loader used by the registration.

`PModelData` is not final. Custom implementations can call its protected constructor and override lookup methods when a renderer needs dynamic or multiple models. Register every canonical model that such an implementation can select.

The codec uses `model_id` and optional `model_loader`:

```json
{
  "model_id": "examplemod:item/wand",
  "model_loader": "pulselib:gltf"
}
```

Physical paths such as `glmodels/item/wand.glb` and `geckolib/models/robot.geo.json` are loader implementation details and must not appear in model data.
