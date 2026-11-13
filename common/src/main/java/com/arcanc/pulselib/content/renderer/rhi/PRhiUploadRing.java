/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

/** Three WRITE-upload slots, protected by RHI fences; no persistent mapping or native API calls. */
final class PRhiUploadRing implements AutoCloseable
{
	private final GpuDevice device;
	private final Slot[] slots = {new Slot(), new Slot(), new Slot()};
	private int current;
	private boolean failed;

	PRhiUploadRing(GpuDevice device)
	{
		this.device = device;
	}

	GpuBuffer upload(CommandEncoder encoder, ByteBuffer data)
	{
		if (this.failed)
			throw new IllegalStateException("RHI upload stream needs cleanup after a submission failure");
		Slot slot = this.slots[this.current];
		if (slot.fence != null)
			encoder.submit();
		slot.await();
		if (slot.buffer == null || slot.buffer.size() < data.remaining())
		{
			GpuBuffer replacement = this.device.createBuffer(() -> "PulseLib RHI frame stream",
					GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, data.remaining());
			if (slot.buffer != null)
				slot.buffer.close();
			slot.buffer = replacement;
		}
		encoder.writeToBuffer(slot.buffer.slice(0, data.remaining()), data);
		return slot.buffer;
	}

	void submitted(CommandEncoder encoder)
	{
		try
		{
			this.slots[this.current].fence = encoder.createFence();
			this.current = (this.current + 1) % this.slots.length;
		}
		catch (RuntimeException | Error failure)
		{
			this.failed = true;
			throw failure;
		}
	}

	@Override
	public void close()
	{
		this.device.createCommandEncoder().submit();
		for (Slot slot : this.slots)
		{
			slot.await();
			if (slot.buffer != null)
			{
				slot.buffer.close();
				slot.buffer = null;
			}
		}
		this.current = 0;
		this.failed = false;
	}

	private static final class Slot
	{
		private @Nullable GpuBuffer buffer;
		private @Nullable GpuFence fence;

		private void await()
		{
			if (this.fence != null)
			{
				if (!this.fence.awaitCompletion(Long.MAX_VALUE))
					throw new IllegalStateException("RHI upload fence did not complete");
				this.fence.close();
				this.fence = null;
			}
		}
	}
}
