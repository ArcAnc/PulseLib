/**
 * @author ArcAnc
 * Created at: 14.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.plan;

import com.arcanc.pulselib.util.PRenderTypes;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Provides support for frame compiler.
 */
public final class PFrameCompiler<S, P, M, I>
{
	private final Map<S, Map<DrawKey<P, M>, List<I>>> opaque = new Object2ObjectOpenHashMap<>();
	private final Map<S, List<TransparentSubmission<P, M, I>>> translucent = new Object2ObjectOpenHashMap<>();
	private final Comparator<? super P> pipelineOrder;
	private final ToDoubleFunction<I> distanceSquared;

	/**
	 * Creates an instance of the enclosing type.
	 * @param pipelineOrder the pipeline order to use.
	 * @param distanceSquared the distance squared to use.
	 */
	public PFrameCompiler(Comparator<? super P> pipelineOrder, ToDoubleFunction<I> distanceSquared)
	{
		this.pipelineOrder = pipelineOrder;
		this.distanceSquared = distanceSquared;
	}

	/**
	 * Performs the submit operation.
	 * @param stage the stage to use.
	 * @param pipeline the pipeline to use.
	 * @param mesh the mesh to use.
	 * @param instance the instance to use.
	 * @param transparent the transparent to use.
	 */
	public void submit(S stage, P pipeline, M mesh, I instance, boolean transparent)
	{
		DrawKey<P, M> key = new DrawKey<>(pipeline, mesh);
		if (transparent)
		{
			this.translucent.computeIfAbsent(stage, ignored -> new ObjectArrayList<>()).add(
					new TransparentSubmission<>(key, instance, this.distanceSquared.applyAsDouble(instance)));
			return;
		}
		this.opaque.computeIfAbsent(stage, ignored -> new Object2ObjectOpenHashMap<>()).
				computeIfAbsent(key, ignored -> new ObjectArrayList<>()).add(instance);
	}

	/**
	 * Performs the compile operation.
	 * @param stage the stage to use.
	 * @return the value produced by this operation.
	 */
	public PRenderPlan<P, M, I> compile(S stage)
	{
		Map<DrawKey<P, M>, List<I>> opaqueGroups = this.opaque.remove(stage);
		List<TransparentSubmission<P, M, I>> translucentGroups = this.translucent.remove(stage);
		if (opaqueGroups == null && translucentGroups == null)
			return PRenderPlan.empty();

		List<PDrawGroup<P, M, I>> groups = new ArrayList<>();
		if (opaqueGroups != null)
			for (Map.Entry<DrawKey<P, M>, List<I>> entry : opaqueGroups.entrySet())
				groups.add(group(entry.getKey(), entry.getValue(), true));
		groups.sort(Comparator.comparing(PDrawGroup<P, M, I>::pipeline, this.pipelineOrder));

		if (translucentGroups != null && !translucentGroups.isEmpty())
		{
			Map<DrawKey<P, M>, List<I>> oitGroups = new Object2ObjectOpenHashMap<>();
			List<TransparentSubmission<P, M, I>> sortedGroups = new ObjectArrayList<>();
			for (TransparentSubmission<P, M, I> submission : translucentGroups)
			{
				if (canBatchWithOit(submission.key()))
					oitGroups.computeIfAbsent(submission.key(), ignored -> new ObjectArrayList<>()).add(submission.instance());
				else
					sortedGroups.add(submission);
			}

			for (Map.Entry<DrawKey<P, M>, List<I>> entry : oitGroups.entrySet())
				groups.add(group(entry.getKey(), entry.getValue(), false));

			sortedGroups.sort(Comparator.comparingDouble(TransparentSubmission<P, M, I>::distanceSquared).reversed());
			DrawKey<P, M> activeKey = null;
			List<I> activeInstances = new ObjectArrayList<>();
			for (TransparentSubmission<P, M, I> submission : sortedGroups)
			{
				if (activeKey != null && !activeKey.equals(submission.key()))
				{
					groups.add(group(activeKey, activeInstances, false));
					activeInstances = new ObjectArrayList<>();
				}
				activeKey = submission.key();
				activeInstances.add(submission.instance());
			}
			if (activeKey != null)
				groups.add(group(activeKey, activeInstances, false));
		}

		return groups.isEmpty() ? PRenderPlan.empty() : new PRenderPlan<>(groups);
	}

	/**
	 * Performs the clear operation.
	 */
	public void clear()
	{
		this.opaque.clear();
		this.translucent.clear();
	}

	/**
	 * Performs the group operation.
	 * @param key the key to use.
	 * @param instances the instances to use.
	 * @param writeDepth the write depth to use.
	 * @return the value produced by this operation.
	 */
	private static <P, M, I> PDrawGroup<P, M, I> group(DrawKey<P, M> key, List<I> instances, boolean writeDepth)
	{
		return new PDrawGroup<>(key.pipeline(), key.mesh(), writeDepth, instances);
	}

	/**
	 * Determines whether the object can batch with oit.
	 * @param key the key to use.
	 * @return the value produced by this operation.
	 */
	private static <P, M> boolean canBatchWithOit(DrawKey<P, M> key)
	{
		return key.pipeline() instanceof RenderType renderType && PRenderTypes.usesOit(renderType) &&
				renderType.outputTarget().getRenderTarget().useDepth;
	}

	/**
	 * Identifies one GPU draw resource pair.
	 *
	 * Meshes contain vertex and index buffers.  Their value hash codes may walk
	 * every buffer element, so batching must use the resource identities rather
	 * than structural equality.
	 */
	private record DrawKey<P, M>(P pipeline, M mesh)
	{
		@Override
		public boolean equals(Object object)
		{
			return this == object || object instanceof DrawKey<?, ?> that &&
					this.pipeline == that.pipeline && this.mesh == that.mesh;
		}

		@Override
		public int hashCode()
		{
			return 31 * System.identityHashCode(this.pipeline) + System.identityHashCode(this.mesh);
		}
	}

/**
 * Immutable value object representing transparent submission.
 */
	private record TransparentSubmission<P, M, I>(DrawKey<P, M> key, I instance, double distanceSquared)
	{
	}
}
