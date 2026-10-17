/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.plan;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public final class PFrameCompiler<S>
{
	private final Map<S, Map<DrawKey, List<PInstanceHeader>>> opaque = new Object2ObjectOpenHashMap<>();
	private final Map<S, List<TransparentSubmission>> translucent = new Object2ObjectOpenHashMap<>();

	private final ToDoubleFunction<PInstanceHeader> sortDepth;

	public PFrameCompiler()
	{
		this(PFrameCompiler :: distanceSquared);
	}

	public PFrameCompiler(ToDoubleFunction<PInstanceHeader> sortDepth)
	{
		this.sortDepth = sortDepth;
	}

	public void submit(S stage,
	                   PPipelineHandle pipeline,
	                   PMeshHandle mesh,
	                   int indexCount,
	                   PInstanceHeader instance,
	                   boolean transparent)
	{
		this.submit(stage, pipeline, mesh, new PDrawCommand(indexCount, 1, 0, 0, 0), instance, transparent);
	}

	public void submit(S stage,
	                   PPipelineHandle pipeline,
	                   PMeshHandle mesh,
	                   PDrawCommand command,
	                   PInstanceHeader instance,
	                   boolean transparent)
	{
		DrawKey key = new DrawKey(pipeline, mesh, command);
		if (transparent)
		{
			this.translucent.computeIfAbsent(stage, ignored -> new ObjectArrayList<>()).add(
					new TransparentSubmission(key, instance, this.sortDepth.applyAsDouble(instance)));
			return;
		}
		this.opaque.computeIfAbsent(stage, ignored -> new Object2ObjectOpenHashMap<>()).
				computeIfAbsent(key, ignored -> new ObjectArrayList<>()).add(instance);
	}

	public PRenderPlan compile(S stage)
	{
		return compile(stage, ignored -> false);
	}

	public PRenderPlan compile(S stage, Predicate<PPipelineHandle> canBatchWithOit)
	{
		return compileCombined(List.of(stage), canBatchWithOit);
	}

	/** Consumes only opaque submissions, leaving transparency for its later pass. */
	public PRenderPlan compileOpaque(S stage)
	{
		return compile(this.opaque.remove(stage), null, ignored -> false);
	}

	/** Sorts ordinary transparency across all requested sources before forming batches. */
	public PRenderPlan compileCombined(List<S> stages, Predicate<PPipelineHandle> canBatchWithOit)
	{
		Map<DrawKey, List<PInstanceHeader>> opaqueGroups = new Object2ObjectOpenHashMap<>();
		List<TransparentSubmission> translucentGroups = new ObjectArrayList<>();
		for (S stage : stages)
		{
			Map<DrawKey, List<PInstanceHeader>> opaqueStage = this.opaque.remove(stage);
			if (opaqueStage != null)
				opaqueStage.forEach((key, instances) -> opaqueGroups.computeIfAbsent(key,
						ignored -> new ObjectArrayList<>()).addAll(instances));
			List<TransparentSubmission> translucentStage = this.translucent.remove(stage);
			if (translucentStage != null)
				translucentGroups.addAll(translucentStage);
		}
		return compile(opaqueGroups, translucentGroups, canBatchWithOit);
	}

	private PRenderPlan compile(@Nullable Map<DrawKey, List<PInstanceHeader>> opaqueGroups,
	                           @Nullable List<TransparentSubmission> translucentGroups,
	                           Predicate<PPipelineHandle> canBatchWithOit)
	{
		if (opaqueGroups == null && translucentGroups == null)
			return PRenderPlan.EMPTY;

		List<PDrawGroup> groups = new ArrayList<>();
		if (opaqueGroups != null)
			for (Map.Entry<DrawKey, List<PInstanceHeader>> entry : opaqueGroups.entrySet())
				groups.add(group(entry.getKey(), entry.getValue(), true));
		groups.sort(Comparator.comparingLong(group -> group.pipeline().value()));

		if (translucentGroups != null && !translucentGroups.isEmpty())
		{
			Map<DrawKey, List<PInstanceHeader>> oitGroups = new Object2ObjectOpenHashMap<>();
			List<TransparentSubmission> sortedGroups = new ObjectArrayList<>();
			for (TransparentSubmission submission : translucentGroups)
			{
				if (canBatchWithOit.test(submission.key().pipeline()))
					oitGroups.computeIfAbsent(submission.key(), ignored -> new ObjectArrayList<>()).add(submission.instance());
				else
					sortedGroups.add(submission);
			}

			for (Map.Entry<DrawKey, List<PInstanceHeader>> entry : oitGroups.entrySet())
				groups.add(group(entry.getKey(), entry.getValue(), false));

			sortedGroups.sort(Comparator.comparingDouble(TransparentSubmission :: sortDepth).reversed());
			DrawKey activeKey = null;
			List<PInstanceHeader> activeInstances = new ObjectArrayList<>();
			for (TransparentSubmission submission : sortedGroups)
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
		return groups.isEmpty() ? PRenderPlan.EMPTY : new PRenderPlan(groups);
	}

	/** Consumes every stage of a compiler owned by one local context. */
	public PRenderPlan compileAll(Predicate<PPipelineHandle> canBatchWithOit)
	{
		var stages = new LinkedHashSet<>(this.opaque.keySet());
		stages.addAll(this.translucent.keySet());
		return compileCombined(new ArrayList<>(stages), canBatchWithOit);
	}

	/** Discards selected stages without compiling a draw plan. */
	public void discard(List<S> stages)
	{
		for (S stage : stages)
		{
			this.opaque.remove(stage);
			this.translucent.remove(stage);
		}
	}

	public boolean isEmpty()
	{
		return this.opaque.isEmpty() && this.translucent.isEmpty();
	}

	public void clear()
	{
		this.opaque.clear();
		this.translucent.clear();
	}

	private static PDrawGroup group(DrawKey key, List<PInstanceHeader> instances, boolean writeDepth)
	{
		return new PDrawGroup(key.pipeline(), key.mesh(),
				key.command().withInstances(instances.size(), 0), writeDepth, instances);
	}

	private static float distanceSquared(PInstanceHeader instance)
	{
		return instance.transform().m30() * instance.transform().m30() +
				instance.transform().m31() * instance.transform().m31() +
				instance.transform().m32() * instance.transform().m32();
	}

	private record DrawKey(PPipelineHandle pipeline, PMeshHandle mesh, PDrawCommand command)
	{
		@Override
		public boolean equals(@Nullable Object object)
		{
			return this == object || object instanceof DrawKey that &&
					this.pipeline == that.pipeline && this.mesh == that.mesh && this.command.equals(that.command);
		}

		@Override
		public int hashCode()
		{
			return 31 * (31 * System.identityHashCode(this.pipeline) + System.identityHashCode(this.mesh)) + this.command.hashCode();
		}
	}

	private record TransparentSubmission(DrawKey key, PInstanceHeader instance, double sortDepth)
	{
	}
}
