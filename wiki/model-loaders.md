# Model Loaders

[`PModelLoader`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/data/PModelLoader.java) resolves a registered canonical model ID into physical resources, parses the selected source, and returns a [`PLoadedModel`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/data/PLoadedModel.java). [`PModelCache`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/PModelCache.java) bakes and keys models by canonical ID.

## Built-in glTF loader

[`PGltfModelLoader`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/data/gltf/PGltfModelLoader.java) is registered by default. `PModelData.entity(examplemod:robot)` has the canonical ID `examplemod:entity/robot`; the loader resolves it in this order:

```text
assets/examplemod/glmodels/entity/robot.glb
assets/examplemod/glmodels/entity/robot.gltf
```

The loaded representation is private to the loader. Replacing the GLB with a glTF JSON file does not alter `PModelData`, `PModelResource`, texture lookup, or the baked-cache key.

## Gecko loader

[`PGeckoModelLoader`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelLoader.java) is also built in. A canonical ID such as `examplemod:entity/robot` resolves to:

```text
assets/examplemod/geckolib/models/entity/robot.geo.json
```

Use the loader-specific `PModelData` factory overload and register the same instance:

```java
PModelData robot = PModelData.entity(
        Identifier.fromNamespaceAndPath("examplemod", "robot"),
        PGeckoModelLoader.INSTANCE.id());
event.model(robot);
```

The loader uses the physical `.geo.json` source to locate animations and animation sidecars.

## Custom loader

```java
public final class MyModelLoader implements PModelLoader {
    @Override
    public Identifier id() {
        return Identifier.fromNamespaceAndPath("examplemod", "my_format");
    }

    @Override
    public List<Identifier> physicalResourceCandidates(Identifier modelId) {
        return List.of(modelId.withPrefix("mymodels/").withSuffix(".json"));
    }

    @Override
    public CompletableFuture<?> loadModels(Executor executor,
                                           ResourceManager resources,
                                           Collection<PModelResource> registered,
                                           Consumer<PLoadedModel> consumer) {
        return CompletableFuture.runAsync(() -> {
            for (PModelResource resource : registered) {
                Identifier source = physicalResourceCandidates(resource.modelId()).getFirst();
                PModel parsed = parse(resources.getResourceOrThrow(source));
                consumer.accept(new PLoadedModel(resource.modelId(), source, parsed));
            }
        }, executor);
    }
}
```

The loader receives only registrations using its ID. It must preserve `resource.modelId()` when constructing `PLoadedModel`; do not derive a cache ID from a physical filename. Use `source` for relative files, sidecars, and diagnostics.

Register a custom loader before resource reload, then use `PModelData` and `event.model(...)` with its loader ID.
