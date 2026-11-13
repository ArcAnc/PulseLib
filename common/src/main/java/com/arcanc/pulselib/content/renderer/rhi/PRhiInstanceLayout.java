/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.arcanc.pulselib.content.renderer.plan.PDrawGroup;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import org.jetbrains.annotations.ApiStatus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/** std140 layout adapted from master RhiDrawExecutor, preserving canonical packed instance ranges. */
@ApiStatus.Internal
public record PRhiInstanceLayout(int alignment, int capacity, int uniformByteLimit)
{
	public static final int STRIDE = 112;
	/** Fits the 16 KiB minimum uniform-block limit; shaders must declare this exact array capacity. */
	public static final int PORTABLE_CAPACITY = 128;

	public PRhiInstanceLayout
	{
		if (alignment < 1 || capacity < 1 || capacity > 512 || (long)capacity * STRIDE > uniformByteLimit)
			throw new IllegalArgumentException("Invalid RHI uniform layout or block limit");
	}

	public int align(int offset)
	{
		if (offset < 0)
			throw new IllegalArgumentException("Negative RHI uniform offset");
		return Math.toIntExact(((long)offset + this.alignment - 1) / this.alignment * this.alignment);
	}

	/** Each chunk gets its own aligned, full shader block; no offset*stride alignment assumption. */
	public Packet pack(PRenderPlan plan)
	{
		int projectionOffset = align(176);
		int size = Math.addExact(projectionOffset, 64);
		List<Batch> batches = new ArrayList<>();
		for (PDrawGroup group : plan.groups())
		{
			var command = group.command();
			if ((long)command.firstInstance() + command.instanceCount() > plan.instances().size())
				throw new IllegalArgumentException("RHI command exceeds packed instance stream");
			if (command.indexCount() == 0)
				continue;
			for (int offset = 0; offset < command.instanceCount();)
			{
				int count = Math.min(this.capacity, command.instanceCount() - offset);
				size = align(size);
				batches.add(new Batch(group, Math.addExact(command.firstInstance(), offset), count, size));
				size = Math.addExact(size, Math.multiplyExact(this.capacity, STRIDE));
				offset += count;
			}
		}
		ByteBuffer bytes = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
		for (Batch batch : batches)
		{
			bytes.position(batch.uniformOffset());
			for (int i = 0; i < batch.instanceCount(); i++)
				write(bytes, plan.instances().get(batch.firstInstance() + i));
		}
		bytes.clear();
		return new Packet(bytes, List.copyOf(batches), projectionOffset);
	}

	private static void write(ByteBuffer bytes, PInstanceHeader instance)
	{
		instance.transform().get(bytes);
		bytes.position(bytes.position() + 64);
		int color = instance.packedColor();
		bytes.putFloat((color >> 16 & 255) / 255f).putFloat((color >> 8 & 255) / 255f).
				putFloat((color & 255) / 255f).putFloat((color >>> 24) / 255f);
		bytes.putFloat(instance.packedLight() >> 4 & 15).putFloat(instance.packedLight() >> 20 & 15);
		bytes.putFloat(instance.packedOverlay() & 65535).putFloat(instance.packedOverlay() >>> 16);
		bytes.putInt(instance.auxiliaryOperationOffset()).putInt(instance.auxiliaryValueOffset()).
				putInt(instance.auxiliaryOperationCount()).putInt(0);
	}

	public record Batch(PDrawGroup group, int firstInstance, int instanceCount, int uniformOffset)
	{
	}

	public record Packet(ByteBuffer bytes, List<Batch> batches, int projectionOffset)
	{
	}
}
