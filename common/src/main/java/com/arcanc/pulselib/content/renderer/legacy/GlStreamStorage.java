/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.arcanc.pulselib.content.model.deformer.gpu.PFrameStreams;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.mojang.blaze3d.systems.RenderPass;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Uploads pose and deformer streams and binds their storage to the active shader.
 */
public final class GlStreamStorage
{
	private static final Map<PFrameStreams, PGpuDeformerBuffers.UploadBuffers> UPLOADS = new IdentityHashMap<>();
	private static final Map<PFrameStreams, PGpuDeformerBuffers.Bindings> BINDINGS = new IdentityHashMap<>();

	private GlStreamStorage()
	{
	}

	/** Uploads native storage before the caller opens a RenderPass. */
	public static void prepare(PFrameStreams streams)
	{
		PGpuDeformerBuffers.Bindings bindings = streams == PGpuDeformerBuffers.streams() ? PGpuDeformerBuffers.upload() :
				UPLOADS.computeIfAbsent(streams, PGpuDeformerBuffers.UploadBuffers::new).upload();
		BINDINGS.put(streams, bindings);
	}

	/** Binds prepared native texel uniforms to the caller's active RenderPass. */
	public static void bind(RenderPass pass, PFrameStreams streams)
	{
		PGpuDeformerBuffers.Bindings bindings = BINDINGS.get(streams);
		if (bindings == null || bindings.operations().isClosed() || bindings.values().isClosed())
			throw new IllegalStateException("Prepare deformer streams before opening the RenderPass");
		if (streams.deformers().operationsDirty() || streams.deformers().valuesDirty())
			throw new IllegalStateException("Deformer streams changed after prepare");
		pass.setUniform("DeformerOperations", bindings.operations());
		pass.setUniform("DeformerValues", bindings.values());
	}

	/** Releases facade upload owners and shared uploads while retaining CPU definitions. */
	public static void cleanup()
	{
		BINDINGS.clear();
		UPLOADS.values().forEach(PGpuDeformerBuffers.UploadBuffers::close);
		UPLOADS.clear();
		PGpuDeformerBuffers.releaseUploads();
	}
}
