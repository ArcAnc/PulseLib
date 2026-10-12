/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer.gpu;

import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class PDeformerStream
{
	private final Map<PDeformerStack, @Nullable PGpuDeformerStack> stacks = new IdentityHashMap<>();
	private final Map<PGpuDeformerStack, Integer> operationOffsets = new IdentityHashMap<>();
	private final List<Float> operations = new ArrayList<>();
	private final List<Float> values = new ArrayList<>();
	private boolean operationsDirty;
	private boolean valuesDirty;

	public PGpuDeformerBuffers.Submission submit(@Nullable PMeshDeformation deformation)
	{
		if (deformation == null || deformation.stack().isEmpty())
			return PGpuDeformerBuffers.NONE;
		PGpuDeformerStack stack = this.resolve(deformation.stack());
		if (stack == null)
			return PGpuDeformerBuffers.NONE;
		int operationOffset = this.operationOffsets.computeIfAbsent(stack, this :: appendOperations);
		int valueOffset = this.values.size() / 4;
		for (PChannelReference<Float> reference : stack.channels())
		{
			this.values.add(deformation.values().resolve(reference));
			this.values.add(0.0f);
			this.values.add(0.0f);
			this.values.add(0.0f);
		}
		this.valuesDirty = true;
		return new PGpuDeformerBuffers.Submission(operationOffset, valueOffset, stack.operationCount());
	}

	public List<Float> operations()
	{
		return this.operations;
	}

	public List<Float> values()
	{
		return this.values;
	}

	public boolean operationsDirty()
	{
		return this.operationsDirty;
	}

	public boolean valuesDirty()
	{
		return this.valuesDirty;
	}

	public void markOperationsUploaded()
	{
		this.operationsDirty = false;
	}

	public void markValuesUploaded()
	{
		this.valuesDirty = false;
	}

	public void finishFrame()
	{
		this.values.clear();
		this.valuesDirty = true;
	}

	public void clearDefinitions()
	{
		this.stacks.clear();
		this.operationOffsets.clear();
		this.operations.clear();
		this.values.clear();
		this.operationsDirty = true;
		this.valuesDirty = true;
	}

	private @Nullable PGpuDeformerStack resolve(PDeformerStack stack)
	{
		if (this.stacks.containsKey(stack))
			return this.stacks.get(stack);
		PGpuDeformerStack result = PGpuDeformerStack.compile(stack).orElse(null);
		this.stacks.put(stack, result);
		return result;
	}

	private int appendOperations(PGpuDeformerStack stack)
	{
		int result = this.operations.size() / 4;
		for (float value : stack.operationData())
			this.operations.add(value);
		this.operationsDirty = true;
		return result;
	}
}
