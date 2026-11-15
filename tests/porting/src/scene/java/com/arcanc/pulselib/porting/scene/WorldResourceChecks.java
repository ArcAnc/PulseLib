/**
 * @author ArcAnc
 * Created at: 15.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene;

import com.arcanc.pulselib.content.renderer.rhi.PRhiFrameResources;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceDescriptor;

/** Native framegraph failure and release-failure semantics, with CPU resources for exact ownership counts. */
final class WorldResourceChecks
{
	private WorldResourceChecks()
	{
	}

	static void run()
	{
		int[] counts = new int[2];
		var descriptor = new ResourceDescriptor<Object>()
		{
			@Override
			public Object allocate()
			{
				counts[0]++;
				return new Object();
			}

			@Override
			public void free(Object value)
			{
				counts[1]++;
			}
		};
		var failure = new IllegalStateException("Expected graph failure");
		try (var resources = new PRhiFrameResources(GraphicsResourceAllocator.UNPOOLED))
		{
			var graph = new FrameGraphBuilder();
			var pass = graph.addPass("expected_abort");
			pass.createsInternal("lease_a", descriptor);
			pass.createsInternal("lease_b", descriptor);
			pass.disableCulling();
			pass.executes(() ->
			{
				throw failure;
			});
			graph.execute(resources);
			throw new AssertionError("Native graph failure must propagate");
		}
		catch (IllegalStateException expected)
		{
			SceneChecks.check(expected == failure, "Original framegraph exception preserved");
		}
		SceneChecks.check(counts[0] == 2 && counts[1] == 2, "Every native framegraph lease returned exactly once after pass failure");
		int[] releases = new int[1];
		var closeFailure = new IllegalStateException("Expected release failure");
		var failing = new ResourceDescriptor<Object>()
		{
			@Override
			public Object allocate()
			{
				return new Object();
			}

			@Override
			public void free(Object resource)
			{
				releases[0]++;
				if (releases[0] == 1)
					throw closeFailure;
			}
		};
		try (var resources = new PRhiFrameResources(GraphicsResourceAllocator.UNPOOLED))
		{
			resources.acquire(failing);
			resources.acquire(failing);
			throw failure;
		}
		catch (IllegalStateException expected)
		{
			SceneChecks.check(expected == failure && expected.getSuppressed().length == 1 && expected.getSuppressed()[0] == closeFailure,
					"Resource errors remain suppressed on the original render failure");
		}
		SceneChecks.check(releases[0] == 2, "A failed release cannot skip other owned resources");
		System.out.println("PASS native framegraph resource failure: exact acquire/release counts, original exception and suppressed cleanup failure");
	}
}
