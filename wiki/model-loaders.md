# Model Loaders

[`PModelLoader`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/data/PModelLoader.java) resolves a registered canonical model ID into physical resources, parses the selected source, and returns a [`PLoadedModel`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/data/PLoadedModel.java). [`PModelCache`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/PModelCache.java) bakes and keys models by canonical ID.

## Built-in glTF loader

[`PGltfModelLoader`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/data/gltf/PGltfModelLoader.java) is registered by default. `PModelData.entity(examplemod:robot)` has the canonical ID `examplemod:entity/robot`; the loader resolves it in this order:

```text
assets/examplemod/glmodels/entity/robot.glb
assets/examplemod/glmodels/entity/robot.gltf
```

The loaded representation is private to the loader. Replacing the GLB with a glTF JSON file does not alter `PModelData`, `PModelResource`, texture lookup, or the baked-cache key.

## Gecko loader

[`PGeckoModelLoader`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelLoader.java) is also built in. A canonical ID such as `examplemod:entity/robot` resolves to:

```text
assets/examplemod/geckolib/models/entity/robot.geo.json
```

Use the loader-specific `PModelData` factory overload and register the same instance:

```java
PModelData robot = PModelData.entity(
		Identifier.fromNamespaceAndPath("examplemod", "robot"),
		PGeckoModelLoader.INSTANCE.id());
registrar.model(robot);
```

The loader uses the physical `.geo.json` source to locate animations and animation sidecars.

## Custom loader

Implement the canonical single-model contract: `id()`, `modelResourceCandidates(...)`, and `loadModel(...)`. The default `loadModels(...)` adapter prepares a registered batch and delivers results in registration order after the whole batch succeeds. `physicalResourceCandidates(...)` delegates to `modelResourceCandidates(...)`.

```java
public final class MyModelLoader implements PModelLoader
{
	@Override
	public Identifier id()
	{
		return Identifier.fromNamespaceAndPath("examplemod", "my_format");
	}

	@Override
	public List<Identifier> modelResourceCandidates(Identifier modelId)
	{
		return List.of(modelId.withPrefix("mymodels/").withSuffix(".json"));
	}

	@Override
	public CompletableFuture<PLoadedModel> loadModel(Executor executor,
	                                               ResourceManager resources,
	                                               Identifier modelId)
	{
		return CompletableFuture.supplyAsync(() ->
		{
			Identifier source = modelResourceCandidates(modelId).getFirst();
			try (InputStream input = resources.getResourceOrThrow(source).open())
			{
				PModel parsed = MyFormatParser.parse(input);
				return new PLoadedModel(modelId, source, parsed);
			}
			catch (IOException exception)
			{
				throw new CompletionException(exception);
			}
		}, executor);
	}
}
```

`MyFormatParser` is your format-specific parser, returning a `PModel`. Preserve the canonical `modelId` in `PLoadedModel`; use the selected physical `source` for relative files, sidecars and diagnostics. A loader with several candidates must select the first existing resource and report failures with those paths.

Register your loader once during client initialization, before the first resource reload:

```java
PModelCache.registerModelLoader(new MyModelLoader());
```

Use that loader's ID in both `PModelData` and `registerModels(PModelRegistrar)` declarations. Loader registration is separate from the scoped plugin model registrar.

## Reload lifecycle

Both loaders invoke the native `PModelCache.reload(SharedState, Executor, PreparationBarrier, Executor)` path. Preparation runs in the background; baking and cache publication wait for the barrier and run on the game executor. Failed preparation must not publish a partial model batch. Resolve baked models and sprites again after reload rather than retaining old GPU resources.

The canonical six-argument reload overload remains as an adapter for callers that already supply a resource manager, preparation barrier and profilers. Mods using the normal loader lifecycle do not need to call either overload themselves.
