# Model Loaders

PulseLib model loading is extensible through [`PModelLoader`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/data/PModelLoader.java). Loaded raw models are baked into [`PBakedModel`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/model/baked/PBakedModel.java) by [`PModelCache`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/PModelCache.java).

## Built-in glTF loader

[`PGltfModelLoader`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/data/gltf/PGltfModelLoader.java) is registered by default.

Supported roots and extensions:

```text
assets/<modid>/glmodels/**/*.glb
assets/<modid>/glmodels/**/*.gltf
```

Default path:

```java
new DefaultEntityModelData.DefaultEntityModelDataBuilder(id)
```

resolves to:

```text
assets/<namespace>/glmodels/entity/<path>.glb
```

Model-resource registration can use either extension, or omit it. For an extension-less id, PulseLib checks `.glb` and then `.gltf`; when both files are present, `.glb` is selected. An explicit extension is preferred, with the other format used only as a fallback. `PModelData` must use the extension of the file that was loaded, so use a direct `PModelData` path for `.gltf` models rather than a default builder that generates `.glb`.

The parser is [`PGltfModelParser`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/data/gltf/PGltfModelParser.java). glTF channels are decoded through the registered position, rotation, and scale channel types, so the loaded animation data now uses the same generic track API as other formats.

### Material textures

The glTF loader gets a material texture reference from the base-colour image URI, then the image name, then the texture name. That string is the key passed to `PModelResource.texture(...)` and is resolved into the runtime atlas during baking.

An embedded image represented only by `image.bufferView` has pixel data but no resource identifier. PulseLib cannot infer a Minecraft texture path from those bytes, so a primitive using such an image fails resource reload with `Primitive has no texture reference`. Export external PNG files with image URIs, or assign stable image or texture names and register those names as texture keys.

## Gecko loader

[`PGeckoModelLoader`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelLoader.java) supports:

```text
assets/<modid>/geckolib/models/**/*.geo.json
assets/<modid>/geckolib/models/**/*.json
assets/<modid>/geckolib/animations/**/*.animation.json
assets/<modid>/geckolib/animations/**/*.json
```

Register it:

```java
PModelCache.registerModelLoader(PGeckoModelLoader.INSTANCE);
```

Use it in model data:

```java
PModelData data = new DefaultEntityModelData.DefaultEntityModelDataBuilder(
        Identifier.fromNamespaceAndPath("examplemod", "robot"),
        PGeckoModelLoader.INSTANCE.id())
        .build();
```

The parser is [`PGeckoModelParser`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelParser.java).

Gecko animation vector components may be Molang expressions. See [Molang animations](molang-animations.md) for the supported language, context values, renderer hooks, and persistence rules.

## Custom loader

```java
public final class MyModelLoader implements PModelLoader {
    public static final MyModelLoader INSTANCE = new MyModelLoader();
    private static final Identifier ID =
            Identifier.fromNamespaceAndPath("examplemod", "my_format");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public boolean supports(Identifier modelPath) {
        return modelPath.getPath().startsWith("mymodels/")
                && modelPath.getPath().endsWith(".json");
    }

    @Override
    public Identifier defaultModelLocation(Identifier modelLocation, String modelType) {
        return modelLocation.withPrefix("mymodels/" + modelType + "/").withSuffix(".json");
    }

    @Override
    public CompletableFuture<?> loadModels(Executor backgroundExecutor,
                                           ResourceManager resourceManager,
                                           BiConsumer<Identifier, PModel> elementConsumer) {
        return CompletableFuture.runAsync(() -> {
            // Parse resources and call elementConsumer.accept(modelLocation, model).
        }, backgroundExecutor);
    }
}
```

Register before client resource reload:

```java
PModelCache.registerModelLoader(MyModelLoader.INSTANCE);
```

Register a model that uses this loader through `event.model(model, MyModelLoader.INSTANCE.id())`. The registration's model id is normalized with `normalizeModelResourceLocation(...)` and must be the same id that `loadModels(...)` passes to its consumer; only registered models are baked. Override `modelResourceLocation(...)` or `normalizeModelResourceLocation(...)` when the loader accepts a short model id but loads it from a resource-pack root or supplies a default extension. A loader that accepts several equivalent resource names can override `modelResourceCandidates(...)`; its first candidate is preferred, and the other candidates are fallback locations.

`PModel` contains raw bones, meshes, bone-to-mesh mapping, and animations. `PModelCache` owns baking, vertex buffer creation, atlas UV conversion, emissive metadata, and cache cleanup.
