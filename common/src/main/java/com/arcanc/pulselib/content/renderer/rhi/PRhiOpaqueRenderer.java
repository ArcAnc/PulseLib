/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.renderer.plan.PDrawGroup;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PGeometrySource;
import com.arcanc.pulselib.content.renderer.plan.PMeshHandle;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.arcanc.pulselib.content.renderer.plan.PRenderCapabilities;
import com.arcanc.pulselib.content.renderer.plan.PRenderExecutor;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Adapts one complete opaque queue plan to the shared native backend. Selection is conservative:
 * unsupported types, transparent policies and GPU deformer streams remain with the caller's existing
 * executor. Never splits or reorders a plan, and never draws any group twice. Native resources are lazy
 * and owned here; portable CPU geometry and host textures/targets stay with their original owners.
 */
@ApiStatus.Internal
public final class PRhiOpaqueRenderer implements PRenderExecutor
{
	private final Function<PPipelineHandle, RenderType> types;
	private final Function<PPipelineHandle, PTransparencyMode> policies;
	private final Function<PMeshHandle, Object> geometry;
	private final Supplier<GpuDevice> device;
	private final Function<RenderType, PreparedRenderType> prepare;
	private final Function<PreparedRenderType, PRhiMaterial.Attachments> target;
	private final PRhiPipelines profiles = PRhiPipelines.portable();
	private final Map<RenderType, PPipelineHandle> bindings = new IdentityHashMap<>();
	private @Nullable PRhiResourceRegistry resources;
	private @Nullable PRhiDrawExecutor executor;

	public PRhiOpaqueRenderer(Function<PPipelineHandle, RenderType> types,
			Function<PPipelineHandle, PTransparencyMode> policies, Function<PMeshHandle, Object> geometry)
	{
		this(types, policies, geometry, RenderSystem::getDevice, RenderType::prepare, PRhiNativeMaterial::attachments);
	}

	/** Allows an explicit native preparation/output context, as used by local and offscreen rendering. */
	public PRhiOpaqueRenderer(Function<PPipelineHandle, RenderType> types,
			Function<PPipelineHandle, PTransparencyMode> policies, Function<PMeshHandle, Object> geometry,
			Supplier<GpuDevice> device, Function<RenderType, PreparedRenderType> prepare,
			Function<PreparedRenderType, PRhiMaterial.Attachments> target)
	{
		this.types = Objects.requireNonNull(types);
		this.policies = Objects.requireNonNull(policies);
		this.geometry = Objects.requireNonNull(geometry);
		this.device = Objects.requireNonNull(device);
		this.prepare = Objects.requireNonNull(prepare);
		this.target = Objects.requireNonNull(target);
	}

	/** Inspects CPU metadata only; no preparation, upload, device lookup or OpenGL call occurs here. */
	public boolean canExecute(PRenderPlan plan)
	{
		for (PDrawGroup group : plan.groups())
		{
			if (!group.writeDepth() || this.policies.apply(group.pipeline()) != PTransparencyMode.ORDINARY ||
					profile(this.types.apply(group.pipeline())) == null)
				return false;
			if ((long)group.command().firstInstance() + group.command().instanceCount() > plan.instances().size())
				throw new IllegalArgumentException("Opaque command exceeds its packed instance range");
			for (int i = 0; i < group.command().instanceCount(); i++)
				if (plan.instances().get(group.command().firstInstance() + i).auxiliaryOperationCount() != 0)
					return false;
		}
		return true;
	}

	@Override
	public PRenderCapabilities capabilities()
	{
		return new PRenderCapabilities(PRenderCapabilities.StorageMode.CPU,
				PRenderCapabilities.SubmissionMode.DIRECT, PRenderCapabilities.UploadMode.WRITE);
	}

	@Override
	public void execute(PRenderPlan plan, PRenderFrame frame)
	{
		if (plan.isEmpty())
			return;
		if (!canExecute(plan))
			throw new UnsupportedOperationException("Plan requires an opaque feature not yet adapted to RHI");
		PRhiResourceRegistry resources = resources();
		var groups = new ArrayList<PDrawGroup>(plan.groups().size());
		for (PDrawGroup group : plan.groups())
		{
			RenderType type = this.types.apply(group.pipeline());
			PPipelineHandle pipeline = this.bindings.computeIfAbsent(type, source -> material(source).register(resources));
			PMeshHandle mesh = switch (this.geometry.apply(group.mesh()))
			{
				case PBakedMesh baked -> resources.mesh(baked);
				case PGeometryData data -> resources.geometry(data);
				case PGeometrySource source -> resources.dynamic(source);
				default -> throw new IllegalArgumentException("Opaque queue geometry has no portable CPU source");
			};
			groups.add(new PDrawGroup(pipeline, mesh, group.command(), group.writeDepth(), group.instances()));
		}
		Objects.requireNonNull(this.executor).execute(new PRenderPlan(groups, plan.instances()), frame);
	}

	private PRhiResourceRegistry resources()
	{
		if (this.resources == null)
		{
			GpuDevice device = Objects.requireNonNull(this.device.get());
			this.resources = new PRhiResourceRegistry(device);
			this.executor = new PRhiDrawExecutor(device, this.resources);
		}
		return this.resources;
	}

	private PRhiNativeMaterial material(RenderType source)
	{
		RenderPipeline profile = Objects.requireNonNull(profile(source));
		RenderType nativeType = RenderType.create("pulselib_rhi_opaque_" + source.pipeline().getLocation(),
				RenderSetup.builder(profile).createRenderSetup());
		return new PRhiNativeMaterial(nativeType, this.profiles.instanceCapacity(), PTransparencyMode.ORDINARY, Map.of(), () ->
		{
			PreparedRenderType prepared = Objects.requireNonNull(this.prepare.apply(source));
			if (prepared.pipeline() != source.pipeline())
				throw new IllegalArgumentException("Opaque native preparation returned a different source pipeline");
			return new PreparedRenderType(profile, prepared.outputTarget(), prepared.dynamicTransforms(),
					prepared.scissorState(), prepared.textures());
		}, this.target);
	}

	private @Nullable RenderPipeline profile(RenderType type)
	{
		RenderPipeline pipeline = type.pipeline();
		@Nullable RenderPipeline selected = null;
		for (int variant = 0; variant < 4; variant++)
			if (pipeline == this.profiles.pipeline((variant & 1) != 0, (variant & 2) != 0))
				selected = pipeline;
		if (selected == null)
		{
			var id = pipeline.getLocation();
			if (!id.getNamespace().equals("pulselib"))
				return null;
			selected = switch (id.getPath())
			{
				case "pipeline/triangles_solid_no_cull" -> this.profiles.pipeline(false, false);
				case "pipeline/triangles_cutout_no_cull" -> this.profiles.pipeline(true, false);
				case "pipeline/triangles_emissive_solid_no_cull" -> this.profiles.pipeline(false, true);
				case "pipeline/triangles_emissive_cutout_no_cull" -> this.profiles.pipeline(true, true);
				default -> null;
			};
		}
		if (selected == null || type.hasBlending() || pipeline.isCull() || pipeline.getDepthStencilState() == null ||
				!pipeline.getDepthStencilState().writeDepth() || type.format().getVertexSize() != 24)
			return null;
		return selected;
	}

	/** Reload/disconnect invalidates native handles; the next supported plan recreates them lazily. */
	@Override
	public void cleanup()
	{
		try
		{
			if (this.executor != null)
				this.executor.cleanup();
		}
		finally
		{
			this.executor = null;
			this.resources = null;
			this.bindings.clear();
		}
	}
}
