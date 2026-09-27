# Model Data

[`PModelData`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/renderer/modelData/PModelData.java) tells a renderer which logical model to use. Its model ID is canonical and is separate from the resource-pack file selected by its loader. Material texture references belong to a model-local [`PModelResource`](../src/main/java/com/arcanc/pulselib/content/model/resource/PModelResource.java), collected before model and atlas reload work begins.

Use the factories for the conventional model groups:

```java
PModelData block = PModelData.block(
        ResourceLocation.fromNamespaceAndPath("examplemod", "crusher"));
PModelData item = PModelData.item(
        ResourceLocation.fromNamespaceAndPath("examplemod", "wand"));
PModelData entity = PModelData.entity(
        ResourceLocation.fromNamespaceAndPath("examplemod", "robot"));
PModelData direct = PModelData.direct(
        ResourceLocation.fromNamespaceAndPath("examplemod", "gui/gene_screen"));
```

The first three prepend `block/`, `item/`, or `entity/` to form the canonical ID. `direct(...)` takes an already canonical ID and does not add a prefix. `entityLayer(entityType, modelId)` creates an entity model beneath the entity type's path for render layers. None of these IDs include `glmodels/`, `geckolib/models/`, `.glb`, `.gltf`, or `.geo.json`.

For the default glTF loader, `PModelData.entity(examplemod:robot)` has canonical ID `examplemod:entity/robot`. The loader searches these physical candidates in order:

```text
assets/examplemod/glmodels/entity/robot.glb
assets/examplemod/glmodels/entity/robot.gltf
```

The cache remains keyed by `examplemod:entity/robot` whichever source exists.

## Model resources

Register the same model data object with `PulseLibEvents.RegisterResourceEvent`. This removes the need to reproduce the canonical ID at a second call site:

```java
public static final PModelData ROBOT_MODEL = PModelData.entity(
        ResourceLocation.fromNamespaceAndPath("examplemod", "robot"));

// renderer
super(context, ROBOT_MODEL, PRenderTypes.RenderTypeProvider::trianglesSolid);

// resource registration
event.model(ROBOT_MODEL)
        .texture("body", ResourceLocation.fromNamespaceAndPath("examplemod", "entity/robot/body"));
```

Only registered models are loaded and baked. Texture references are resolved from that model's canonical `PModelResource`, so two models can use the same reference without sharing a texture.

## Custom loaders and custom model data

Pass a custom loader ID to any factory:

```java
PModelData geckoRobot = PModelData.entity(
        ResourceLocation.fromNamespaceAndPath("examplemod", "robot"),
        PGeckoModelLoader.INSTANCE.id());
```

`PModelData` is intentionally inheritable. A custom subclass can override `getModel()` to select a model dynamically or supply a multi-model arrangement while retaining private immutable base state.
