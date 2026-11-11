/**
 * @author ArcAnc
 * Created at: 11.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data;

import com.arcanc.pulselib.content.model.PModel;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PModelLoaderTest
{
	private static final Identifier FIRST = Identifier.fromNamespaceAndPath("test", "entity/first");
	private static final Identifier SECOND = Identifier.fromNamespaceAndPath("test", "entity/second");

	@Test
	void canonicalOnlyImplementationFeedsNativeBatchInRegistrationOrder()
	{
		var first = new CompletableFuture<PLoadedModel>();
		var second = new CompletableFuture<PLoadedModel>();
		PModelLoader loader = loader(Map.of(FIRST, first, SECOND, second));
		var scheduled = new ArrayDeque<Runnable>();
		var delivered = new ArrayList<PLoadedModel>();
		var completion = loader.loadModels(scheduled::add, ResourceManager.Empty.INSTANCE,
				List.of(resource(FIRST, loader), resource(SECOND, loader)), delivered::add);
		PLoadedModel loadedFirst = loaded(FIRST, loader);
		PLoadedModel loadedSecond = loaded(SECOND, loader);
		second.complete(loadedSecond);
		assertFalse(completion.isDone());
		assertTrue(delivered.isEmpty());
		first.complete(loadedFirst);
		assertFalse(completion.isDone());
		scheduled.removeFirst().run();
		completion.join();
		assertEquals(List.of(loadedFirst, loadedSecond), delivered);
		assertEquals(loader.modelResourceCandidates(FIRST), loader.physicalResourceCandidates(FIRST));
	}

	@Test
	void failedBatchKeepsConsumerEmpty()
	{
		var first = new CompletableFuture<PLoadedModel>();
		var second = new CompletableFuture<PLoadedModel>();
		PModelLoader loader = loader(Map.of(FIRST, first, SECOND, second));
		var delivered = new ArrayList<PLoadedModel>();
		var completion = loader.loadModels(Runnable::run, ResourceManager.Empty.INSTANCE,
				List.of(resource(FIRST, loader), resource(SECOND, loader)), delivered::add);
		var failure = new IllegalStateException("Unreadable second resource");
		first.complete(loaded(FIRST, loader));
		second.completeExceptionally(failure);
		assertSame(failure, assertThrows(CompletionException.class, completion::join).getCause());
		assertTrue(delivered.isEmpty());
	}

	@Test
	void builtInCandidateContractsPreservePhysicalPrecedence()
	{
		assertEquals(List.of(FIRST.withPrefix("glmodels/").withSuffix(".glb"), FIRST.withPrefix("glmodels/").withSuffix(".gltf")),
				PGltfModelLoader.INSTANCE.modelResourceCandidates(FIRST));
		assertEquals(List.of(FIRST.withPrefix("geckolib/models/").withSuffix(".geo.json")),
				PGeckoModelLoader.INSTANCE.modelResourceCandidates(FIRST));
		for (PModelLoader loader : List.of(PGltfModelLoader.INSTANCE, PGeckoModelLoader.INSTANCE))
			assertEquals(loader.modelResourceCandidates(FIRST), loader.physicalResourceCandidates(FIRST));
	}

	@Test
	void missingBuiltInResourcesFailOnTheRequestedExecutorThroughBothEntrypoints()
	{
		for (PModelLoader loader : List.of(PGltfModelLoader.INSTANCE, PGeckoModelLoader.INSTANCE))
		{
			var scheduled = new ArrayDeque<Runnable>();
			var single = loader.loadModel(scheduled::add, ResourceManager.Empty.INSTANCE, FIRST);
			assertFalse(single.isDone());
			scheduled.removeFirst().run();
			var failure = assertThrows(CompletionException.class, single::join);
			assertTrue(failure.getCause().getMessage().contains(FIRST.toString()));
			var delivered = new ArrayList<PLoadedModel>();
			var batch = loader.loadModels(scheduled::add, ResourceManager.Empty.INSTANCE,
					List.of(resource(FIRST, loader)), delivered::add);
			assertFalse(batch.isDone());
			scheduled.removeFirst().run();
			assertThrows(CompletionException.class, batch::join);
			assertTrue(delivered.isEmpty());
		}
	}

	@Test
	void canonicalDefaultLoaderConstantAndNativeAliasShareTheSameId()
	{
		assertSame(PModelData.DEFAULT_MODEL_LOADER_ID, PModelData.DEFAULT_MODEL_LOADER);
		assertEquals(PModelData.DEFAULT_MODEL_LOADER_ID, PModelData.entity(FIRST).getModelLoaderId());
	}

	private static PModelResource resource(Identifier modelId, PModelLoader loader)
	{
		return new PModelResource(modelId, loader.id(), Map.of());
	}

	private static PLoadedModel loaded(Identifier modelId, PModelLoader loader)
	{
		return new PLoadedModel(modelId, loader.modelResourceCandidates(modelId).getFirst(), new PModel());
	}

	private static PModelLoader loader(Map<Identifier, CompletableFuture<PLoadedModel>> models)
	{
		return new PModelLoader()
		{
			@Override
			public Identifier id()
			{
				return Identifier.fromNamespaceAndPath("test", "canonical_loader");
			}

			@Override
			public List<Identifier> modelResourceCandidates(Identifier modelId)
			{
				return List.of(modelId.withPrefix("models/").withSuffix(".json"));
			}

			@Override
			public CompletableFuture<PLoadedModel> loadModel(Executor backgroundExecutor, ResourceManager resourceManager, Identifier modelId)
			{
				return Objects.requireNonNull(models.get(modelId));
			}
		};
	}
}
