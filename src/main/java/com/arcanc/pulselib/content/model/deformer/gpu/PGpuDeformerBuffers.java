/**
 * @author ArcAnc
 * Created at: 10.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer.gpu;

import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Provides support for gpu deformer buffers.
 */
public final class PGpuDeformerBuffers
{
	public static final Submission NONE = new Submission(-1, -1, 0);
	private static final PDeformerStream STREAM = new PDeformerStream();

	/**
	 * Creates an instance of the enclosing type.
	 */
	private PGpuDeformerBuffers()
	{
	}

	/**
	 * Performs the submit operation.
	 * @param deformation the deformation to use.
	 * @return the value produced by this operation.
	 */
	public static Submission submit(@Nullable PMeshDeformation deformation)
	{
		return STREAM.submit(deformation);
	}

	/**
	 * Performs the operations operation.
	 * @return the value produced by this operation.
	 */
	public static List<Float> operations()
	{
		return STREAM.operations();
	}

	/**
	 * Performs the values operation.
	 * @return the value produced by this operation.
	 */
	public static List<Float> values()
	{
		return STREAM.values();
	}

	/**
	 * Performs the operations dirty operation.
	 * @return the value produced by this operation.
	 */
	public static boolean operationsDirty()
	{
		return STREAM.operationsDirty();
	}

	/**
	 * Performs the values dirty operation.
	 * @return the value produced by this operation.
	 */
	public static boolean valuesDirty()
	{
		return STREAM.valuesDirty();
	}

	/**
	 * Performs the mark operations uploaded operation.
	 */
	public static void markOperationsUploaded()
	{
		STREAM.markOperationsUploaded();
	}

	/**
	 * Performs the mark values uploaded operation.
	 */
	public static void markValuesUploaded()
	{
		STREAM.markValuesUploaded();
	}

	/**
	 * Performs the finish frame operation.
	 */
	public static void finishFrame()
	{
		STREAM.finishFrame();
	}

	/**
	 * Performs the upload operation.
	 * @return the value produced by this operation.
	 */
	public static Bindings upload()
	{
		return UPLOAD_BUFFERS.upload();
	}

	/**
	 * Performs the cleanup operation.
	 */
	public static void cleanup()
	{
		UPLOAD_BUFFERS.close();
		STREAM.clearDefinitions();
	}

/**
 * Immutable value object representing submission.
 */
	public record Submission(int operationOffset, int valueOffset, int operationCount)
	{
	}

/**
 * Immutable value object representing bindings.
 */
	public record Bindings(GpuBuffer operations, GpuBuffer values)
	{
	}

	private static final UploadBuffers UPLOAD_BUFFERS = new UploadBuffers();

/**
 * Provides support for upload buffers.
 */
	private static final class UploadBuffers
	{
		private static final int MINIMUM_SIZE = Float.BYTES * 4;

		private @Nullable MappableRingBuffer operations;
		private @Nullable MappableRingBuffer values;

		/**
		 * Performs the upload operation.
		 * @return the value produced by this operation.
		 */
		private Bindings upload()
		{
			this.operations = upload(this.operations, PGpuDeformerBuffers.operations(), PGpuDeformerBuffers.operationsDirty(), "deformer_operations");
			this.values = upload(this.values, PGpuDeformerBuffers.values(), PGpuDeformerBuffers.valuesDirty(), "deformer_values");
			PGpuDeformerBuffers.markOperationsUploaded();
			PGpuDeformerBuffers.markValuesUploaded();
			return new Bindings(this.operations.currentBuffer(), this.values.currentBuffer());
		}

		/**
		 * Performs the close operation.
		 */
		private void close()
		{
			if (this.operations != null)
			{
				this.operations.close();
				this.operations = null;
			}
			if (this.values != null)
			{
				this.values.close();
				this.values = null;
			}
		}

		/**
		 * Performs the upload operation.
		 * @param buffer the buffer to use.
		 * @param data the data to use.
		 * @param dirty the dirty to use.
		 * @param label the label to use.
		 * @return the value produced by this operation.
		 */
		private static MappableRingBuffer upload(@Nullable MappableRingBuffer buffer,
		                                         List<Float> data,
		                                         boolean dirty,
		                                         String label)
		{
			int size = Math.max(MINIMUM_SIZE, data.size() * Float.BYTES);
			if (buffer == null || buffer.size() < size)
			{
				if (buffer != null)
					buffer.close();
				buffer = new MappableRingBuffer(
						() -> PLibDatabase.rl(label).toLanguageKey(),
						GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER | GpuBuffer.USAGE_MAP_WRITE,
						size);
				dirty = true;
			}
			else if (dirty)
				buffer.rotate();
			if (dirty)
				try (GpuBuffer.MappedView mapped = RenderSystem.getDevice().createCommandEncoder().mapBuffer(buffer.currentBuffer(), false, true))
				{
					var bytes = mapped.data();
					for (float value : data)
					{
						int bits = Float.floatToRawIntBits(value);
						bytes.put((byte)bits);
						bytes.put((byte)(bits >> 8));
						bytes.put((byte)(bits >> 16));
						bytes.put((byte)(bits >> 24));
					}
				}
			return buffer;
		}
	}
}
