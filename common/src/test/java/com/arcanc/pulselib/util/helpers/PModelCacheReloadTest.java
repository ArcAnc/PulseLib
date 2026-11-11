/**
 * @author ArcAnc
 * Created at: 11.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.helpers;

import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.data.PLoadedModel;
import com.arcanc.pulselib.data.PModelLoader;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.PResourceCache;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.InactiveProfiler;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PModelCacheReloadTest
{
	@Test
	void canonicalReloadWaitsForBarrierAndPublishesOnGameExecutor() throws Exception
	{
		var resources = new LinkedHashMap<>(PResourceCache.getResourceCache());
		var field = PModelCache.class.getDeclaredField("MODELS");
		field.setAccessible(true);
		Object original = field.get(null);
		try
		{
			PResourceCache.getResourceCache().clear();
			field.set(null, null);
			var gate = new CompletableFuture<Void>();
			var game = new ArrayDeque<Runnable>();
			var barrier = new PreparableReloadListener.PreparationBarrier()
			{
				@Override
				public <T> CompletableFuture<T> wait(T prepared)
				{
					return gate.thenApply(ignored -> prepared);
				}
			};
			var result = PModelCache.reload(barrier, ResourceManager.Empty.INSTANCE, InactiveProfiler.INSTANCE,
					InactiveProfiler.INSTANCE, Runnable::run, game::add);
			assertFalse(result.isDone());
			assertTrue(game.isEmpty());
			assertNull(PModelCache.getModels());
			gate.complete(null);
			assertFalse(result.isDone());
			assertEquals(1, game.size());
			assertNull(PModelCache.getModels());
			game.removeFirst().run();
			result.join();
			assertTrue(PModelCache.getModels().isEmpty());
		}
		finally
		{
			field.set(null, original);
			PResourceCache.getResourceCache().clear();
			PResourceCache.getResourceCache().putAll(resources);
		}
	}

	@Test
	void canonicalReloadPropagatesLoadFailureWithoutPublishingOrCrossingBarrier() throws Exception
	{
		var resources = new LinkedHashMap<>(PResourceCache.getResourceCache());
		var original = PModelCache.getModels();
		var loaderId = Identifier.fromNamespaceAndPath("test", "reload_failure");
		var modelId = Identifier.fromNamespaceAndPath("test", "failed_model");
		var failure = new IllegalStateException("failed model");
		Executor background = task -> { throw new AssertionError("Failed preparation must not schedule result delivery"); };
		var loader = new PModelLoader()
		{
			@Override
			public Identifier id()
			{
				return loaderId;
			}

			@Override
			public List<Identifier> modelResourceCandidates(Identifier id)
			{
				return List.of(id);
			}

			@Override
			public CompletableFuture<PLoadedModel> loadModel(Executor executor, ResourceManager manager, Identifier id)
			{
				assertSame(background, executor);
				assertSame(ResourceManager.Empty.INSTANCE, manager);
				assertEquals(modelId, id);
				return CompletableFuture.failedFuture(failure);
			}
		};
		try
		{
			PResourceCache.getResourceCache().clear();
			PResourceCache.getResourceCache().put(modelId, new PModelResource(modelId, loaderId, Map.of()));
			PModelCache.registerModelLoader(loader);
			var barrier = new PreparableReloadListener.PreparationBarrier()
			{
				@Override
				public <T> CompletableFuture<T> wait(T prepared)
				{
					throw new AssertionError("Failed preparation must not reach the barrier");
				}
			};
			var result = PModelCache.reload(barrier, ResourceManager.Empty.INSTANCE, InactiveProfiler.INSTANCE,
					InactiveProfiler.INSTANCE, background, task -> { throw new AssertionError("No failed model publication"); });
			assertSame(failure, assertThrows(CompletionException.class, result::join).getCause());
			assertSame(original, PModelCache.getModels());
		}
		finally
		{
			PModelCache.unregisterModelLoader(loaderId);
			PResourceCache.getResourceCache().clear();
			PResourceCache.getResourceCache().putAll(resources);
		}
	}
}
