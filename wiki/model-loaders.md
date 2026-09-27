# Model Loaders

PulseLib model loading is extensible through [`PModelLoader`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/data/PModelLoader.java). A loader resolves a registered canonical ID to physical resource candidates, selects a source, parses it, and returns a `PLoadedModel`. [`PModelCache`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/util/PModelCache.java) bakes it under the canonical ID.

## Built-in glTF loader

[`PGltfModelLoader`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/data/gltf/PGltfModelLoader.java) is registered by default. For canonical ID `examplemod:entity/robot`, it tries:

```text
assets/examplemod/glmodels/entity/robot.glb
assets/examplemod/glmodels/entity/robot.gltf
```

`.glb` has precedence; `.gltf` is the fallback. The physical extension never affects the cache key. Animation-event sidecars use the selected physical source, so their existing relative lookup behavior is preserved.

The glTF parser gets a material texture reference from the base-colour image URI, then the image name, then the texture name. That string is the key passed to `PModelResource.texture(...)` and is resolved into the runtime atlas during baking.

## Gecko loader

[`PGeckoModelLoader`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelLoader.java) resolves canonical ID `examplemod:entity/robot` to:

```text
assets/examplemod/geckolib/models/entity/robot.geo.json
```

It uses the selected physical model source to find Gecko animation files and animation-event sidecars. Register it during client initialization, before PulseLib collects model-resource registrations:

```java
PModelCache.registerModelLoader(PGeckoModelLoader.INSTANCE);
```

Use it with model data and registration:

```java
PModelData robot = PModelData.entity(
        ResourceLocation.fromNamespaceAndPath("examplemod", "robot"),
        PGeckoModelLoader.INSTANCE.id());
event.model(robot).texture("body", bodyTexture);
```

## Custom loader

Custom loaders receive a canonical ID, so they own all resource-root, extension, fallback, and sidecar conventions:

```java
public final class MyModelLoader implements PModelLoader {
    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ResourceLocation> modelResourceCandidates(ResourceLocation modelId) {
        return List.of(modelId.withPrefix("mymodels/").withSuffix(".json"));
    }

    @Override
    public CompletableFuture<PLoadedModel> loadModel(Executor executor,
                                                      ResourceManager resources,
                                                      ResourceLocation modelId) {
        return CompletableFuture.supplyAsync(() -> {
            ResourceLocation source = modelResourceCandidates(modelId).getFirst();
            PModel model = parse(resources.getResourceOrThrow(source));
            return new PLoadedModel(modelId, source, model);
        }, executor);
    }
}
```

Register the loader during client initialization, before PulseLib collects `RegisterResourceEvent` registrations. The registration controls which models are loaded; loaders must not scan and parse every physical model file. Physical resource reloads reuse that logical registration set.
