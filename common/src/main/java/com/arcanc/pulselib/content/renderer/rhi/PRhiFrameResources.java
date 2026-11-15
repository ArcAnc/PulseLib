/**
 * @author ArcAnc
 * Created at: 15.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceDescriptor;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Returns unfinished framegraph leases to their original allocator when a pass throws. */
@ApiStatus.Internal
public final class PRhiFrameResources implements GraphicsResourceAllocator, AutoCloseable
{
	private final GraphicsResourceAllocator allocator;
	private final List<Lease<?>> leases = new ArrayList<>();
	private boolean closed;

	public PRhiFrameResources(GraphicsResourceAllocator allocator)
	{
		this.allocator = Objects.requireNonNull(allocator);
	}

	@Override
	public <T> T acquire(ResourceDescriptor<T> descriptor)
	{
		if (this.closed)
			throw new IllegalStateException("World frame allocator is closed");
		T resource = this.allocator.acquire(descriptor);
		this.leases.add(new Lease<>(descriptor, resource));
		return resource;
	}

	@Override
	public <T> void release(ResourceDescriptor<T> descriptor, T resource)
	{
		for (int index = this.leases.size() - 1; index >= 0; index--)
			if (this.leases.get(index).resource() == resource)
			{
				this.leases.remove(index);
				this.allocator.release(descriptor, resource);
				return;
			}
		throw new IllegalArgumentException("World frame does not own this resource lease");
	}

	@Override
	public void close()
	{
		this.closed = true;
		@Nullable Throwable failure = null;
		while (!this.leases.isEmpty())
		{
			var lease = this.leases.removeLast();
			try
			{
				lease.release(this.allocator);
			}
			catch (RuntimeException | Error error)
			{
				if (failure == null)
					failure = error;
				else
					failure.addSuppressed(error);
			}
		}
		if (failure instanceof RuntimeException exception)
			throw exception;
		if (failure instanceof Error error)
			throw error;
	}

	private record Lease<T>(ResourceDescriptor<T> descriptor, T resource)
	{
		void release(GraphicsResourceAllocator allocator)
		{
			allocator.release(this.descriptor, this.resource);
		}
	}
}
