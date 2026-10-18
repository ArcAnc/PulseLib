# Model Loaders

PulseLib model loading is extensible through [`PModelLoader`](../common/src/main/java/com/arcanc/pulselib/data/PModelLoader.java). A loader resolves a registered canonical ID to physical resource candidates, selects a source, parses it, and returns a `PLoadedModel`. [`PModelCache`](../common/src/main/java/com/arcanc/pulselib/util/PModelCache.java) bakes it under the canonical ID.

## Built-in glTF loader

[`PGltfModelLoader`](../common/src/main/java/com/arcanc/pulselib/data/gltf/PGltfModelLoader.java) is registered by default. For canonical ID `examplemod:entity/robot`, it tries:

```text
assets/examplemod/glmodels/entity/robot.glb
assets/examplemod/glmodels/entity/robot.gltf
```

`.glb` has precedence; `.gltf` is the fallback. The physical extension never affects the cache key. Animation-event sidecars use the selected physical source, so their existing relative lookup behavior is preserved.

The glTF parser gets a material texture reference from the base-colour image URI, then the image name, then the texture name. Register that string with `registrar.model(modelData).texture(reference, textureId)` so baking can resolve it into the runtime atlas.

The parser uses jgltf's `readWithoutReferences`: use GLB binary geometry or embed a `.gltf` geometry buffer as a data URI. Separate `.bin` buffer files are not resolved by the current loader. External PNG names are supported as texture references because PulseLib loads the registered Minecraft textures independently.

Mesh primitives need indices, positions, normals, and UV0. Node/bone transforms and translation/rotation/scale animation tracks are supported; arbitrary glTF skinning and morph-target animation are not implemented. glTF animation tracks currently use PulseLib's linear interpolation (quaternion slerp for rotation); sampler `STEP`/`CUBICSPLINE` interpolation is not preserved.

## Gecko loader

[`PGeckoModelLoader`](../common/src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelLoader.java) resolves canonical ID `examplemod:entity/robot` to:

```text
assets/examplemod/geckolib/models/entity/robot.geo.json
```

It is registered by default, alongside glTF, and finds animation files and sidecars from the selected source. Do not register the built-in loader again.

For this model, animation candidates are checked in this order:

```text
assets/examplemod/geckolib/animations/robot.animation.json
assets/examplemod/geckolib/animations/robot.json
assets/examplemod/geckolib/animations/entity/robot.animation.json
assets/examplemod/geckolib/animations/entity/robot.json
```

The first existing file wins. Use unique filenames or omit the flat candidate when organizing same-named models in subdirectories. Without an animation JSON, the geometry still loads, but there is no animation source to which event sidecars can be attached.

Use it with model data and registration:

```java
PModelData robot = PModelData.entity(
		ResourceLocation.fromNamespaceAndPath("examplemod", "robot"),
		PGeckoModelLoader.INSTANCE.id());
registrar.model(robot).texture("body", bodyTexture);
```

## Custom loader

Custom loaders receive a canonical ID, so they own all resource-root, extension, fallback, and sidecar conventions:

```java
public abstract class MyModelLoader implements PModelLoader
{
	private static final ResourceLocation ID =
			ResourceLocation.fromNamespaceAndPath("examplemod", "custom");

	@Override
	public ResourceLocation id()
	{
		return ID;
	}

	@Override
	public List<ResourceLocation> modelResourceCandidates(ResourceLocation modelId)
	{
		return List.of(modelId.withPrefix("mymodels/").withSuffix(".json"));
	}

	@Override
	public CompletableFuture<PLoadedModel> loadModel(Executor executor,
			ResourceManager resources,
			ResourceLocation modelId)
	{
		return CompletableFuture.supplyAsync(() ->
		{
			ResourceLocation source = modelResourceCandidates(modelId).getFirst();
			try (InputStream stream = resources.getResourceOrThrow(source).open())
			{
				PModel model = parse(stream);
				return new PLoadedModel(modelId, source, model);
			}
			catch (IOException exception)
			{
				throw new CompletionException("Cannot load " + source, exception);
			}
		}, executor);
	}

	protected abstract PModel parse(InputStream stream) throws IOException;
}
```

Implement `parse` for your format, then register the concrete loader with `PModelCache.registerModelLoader(loader)` during client initialization, before PulseLib collects `registerModels(PModelRegistrar)` registrations. Use its `id()` when constructing the model data. The registration controls which models are loaded; loaders must not scan and parse every physical model file. The plugin rebuilds declarations on every resource reload, while loader instances remain registered once.
