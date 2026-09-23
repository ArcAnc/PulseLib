# Model Data

[`PModelData`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/renderer/modelData/PModelData.java) tells a renderer which model file to use. Texture references belong to a model-local [`PModelResource`](../src/main/java/com/arcanc/pulselib/content/model/resource/PModelResource.java), registered during resource reload.

It is deliberately separate from the renderer. That lets the same renderer logic stay simple while different blocks, items, entities, or layers point at different files. If a PulseLib model is invisible or has missing textures, `PModelData` is one of the first things to inspect.

## Direct builder

Use the direct builder when you already know the exact model path, or when you are using a folder layout that does not match PulseLib's default conventions.

```java
PModelData data = new PModelData.Builder(
        ResourceLocation.fromNamespaceAndPath("examplemod", "glmodels/block/crusher.glb"),
        "").build();
```

Passing an empty `modelType` means "do not rewrite this path." Passing a non-empty type lets PulseLib ask the active model loader to build the conventional path.

## Default builders

For normal mods, default builders expand a short logical name into the resource-pack model path.

```java
PModelData blockData = new DefaultBlockModelData.DefaultBlockModelDataBuilder(
        ResourceLocation.fromNamespaceAndPath("examplemod", "crusher")).build();
```

For the default glTF loader, the example resolves to:

```text
Model: assets/examplemod/glmodels/block/crusher.glb
```

Available default builders:

* [`DefaultBlockModelData`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultBlockModelData.java)
* [`DefaultItemModelData`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultItemModelData.java)
* [`DefaultEntityModelData`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultEntityModelData.java)
* [`DefaultEntityLayerModelData`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultEntityLayerModelData.java)

## Model resources

Register every model used by `PModelData` and every material reference through `PulseLibEvents.RegisterResourceEvent`; only registered models are loaded and baked. Texture references are resolved from that model's `PModelResource`, so two models can use the same relative reference without sharing a texture. glTF registration falls back between `.glb` and `.gltf`; see [Resources](resources.md).

## Gecko model data

PulseLib can load Gecko-style JSON models through [`PGeckoModelLoader`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelLoader.java), but the default registered loader in `PModelCache` is glTF. Register the Gecko loader before client resources reload if your mod needs Gecko paths:

```java
PModelCache.registerModelLoader(PGeckoModelLoader.INSTANCE);

PModelData data = new DefaultEntityModelData.DefaultEntityModelDataBuilder(
        ResourceLocation.fromNamespaceAndPath("examplemod", "robot"),
        PGeckoModelLoader.INSTANCE.id())
        .build();
```

Classes used:

* [`PModelData`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/renderer/modelData/PModelData.java)
* [`PModelCache`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/util/PModelCache.java)
* [`PGltfModelLoader`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/data/gltf/PGltfModelLoader.java)
* [`PGeckoModelLoader`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelLoader.java)
