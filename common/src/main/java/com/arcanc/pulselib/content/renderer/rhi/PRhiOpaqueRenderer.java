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
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
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
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Adapts one complete opaque queue plan to the shared native backend. Selection is conservative:
 * unsupported types and world transparency remain with the caller's existing executor. Explicit native
 * deformer streams enable canonical GPU deformation; local draws retain ordinary blending. Never splits or reorders a plan, and never draws any group twice. Native resources are lazy
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
	private final Map<String, Supplier<GpuBufferSlice>> deformerUniforms;
	private final PRhiPipelines profiles = PRhiPipelines.portable();
	private final Map<RenderType, PPipelineHandle> bindings = new IdentityHashMap<>();
	private @Nullable PRhiResourceRegistry resources;
	private @Nullable PRhiDrawExecutor executor;

	public PRhiOpaqueRenderer(Function<PPipelineHandle, RenderType> types,
			Function<PPipelineHandle, PTransparencyMode> policies, Function<PMeshHandle, Object> geometry)
	{
		this(types, policies, geometry, RenderSystem::getDevice, RenderType::prepare, PRhiNativeMaterial::attachments);
	}

	/** Borrows canonical deformer streams without introducing global upload ownership into the adapter. */
	public PRhiOpaqueRenderer(Function<PPipelineHandle, RenderType> types,
			Function<PPipelineHandle, PTransparencyMode> policies, Function<PMeshHandle, Object> geometry,
			Map<String, Supplier<GpuBufferSlice>> deformerUniforms)
	{
		this(types, policies, geometry, RenderSystem::getDevice, RenderType::prepare, PRhiNativeMaterial::attachments, deformerUniforms);
	}

	/** Allows an explicit native preparation/output context, as used by local and offscreen rendering. */
	public PRhiOpaqueRenderer(Function<PPipelineHandle, RenderType> types,
			Function<PPipelineHandle, PTransparencyMode> policies, Function<PMeshHandle, Object> geometry,
			Supplier<GpuDevice> device, Function<RenderType, PreparedRenderType> prepare,
			Function<PreparedRenderType, PRhiMaterial.Attachments> target)
	{
		this(types, policies, geometry, device, prepare, target, Map.of());
	}

	private PRhiOpaqueRenderer(Function<PPipelineHandle, RenderType> types,
			Function<PPipelineHandle, PTransparencyMode> policies, Function<PMeshHandle, Object> geometry,
			Supplier<GpuDevice> device, Function<RenderType, PreparedRenderType> prepare,
			Function<PreparedRenderType, PRhiMaterial.Attachments> target, Map<String, Supplier<GpuBufferSlice>> deformerUniforms)
	{
		this.deformerUniforms = Map.copyOf(deformerUniforms);
		if (!this.deformerUniforms.isEmpty() && !this.deformerUniforms.keySet().equals(Set.of("DeformerOperations", "DeformerValues")))
			throw new IllegalArgumentException("Both native deformer streams must be supplied together");
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
		return canExecute(plan, false);
	}

	/** Local/immediate plans use ordinary alpha, including types whose world policy is OIT. */
	public boolean canExecuteImmediate(PRenderPlan plan)
	{
		return canExecute(plan, true);
	}

	private boolean canExecute(PRenderPlan plan, boolean immediate)
	{
		for (PDrawGroup group : plan.groups())
		{
			RenderPipeline selected = profile(this.types.apply(group.pipeline()), immediate);
			if (selected == null || (!immediate && (!group.writeDepth() || this.policies.apply(group.pipeline()) != PTransparencyMode.ORDINARY)) ||
					(selected.getDepthStencilState() != null && selected.getDepthStencilState().writeDepth()) != group.writeDepth())
				return false;
			if ((long)group.command().firstInstance() + group.command().instanceCount() > plan.instances().size())
				throw new IllegalArgumentException("Opaque command exceeds its packed instance range");
			for (int i = 0; i < group.command().instanceCount(); i++)
			{
				var instance = plan.instances().get(group.command().firstInstance() + i);
				if (instance.auxiliaryOperationCount() < 0 || instance.auxiliaryOperationCount() > 8 ||
						(instance.auxiliaryOperationCount() > 0 && (instance.auxiliaryOperationOffset() < 0 || instance.auxiliaryValueOffset() < 0)))
					throw new IllegalArgumentException("Invalid canonical deformer range");
				if (instance.auxiliaryOperationCount() > 0 && !usesDeformers(selected))
					return false;
			}
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
		execute(plan, frame, false);
	}

	/** Executes one complete local plan without consuming world OIT submissions. */
	public void executeImmediate(PRenderPlan plan, PRenderFrame frame)
	{
		execute(plan, frame, true);
	}

	private void execute(PRenderPlan plan, PRenderFrame frame, boolean immediate)
	{
		if (plan.isEmpty())
			return;
		if (!canExecute(plan, immediate))
			throw new UnsupportedOperationException("Plan requires an opaque feature not yet adapted to RHI");
		PRhiResourceRegistry resources = resources();
		var groups = new ArrayList<PDrawGroup>(plan.groups().size());
		for (PDrawGroup group : plan.groups())
		{
			RenderType type = this.types.apply(group.pipeline());
			PPipelineHandle pipeline = this.bindings.computeIfAbsent(type, source -> material(source, immediate).register(resources));
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

	private PRhiNativeMaterial material(RenderType source, boolean immediate)
	{
		RenderPipeline profile = Objects.requireNonNull(profile(source, immediate));
		RenderType nativeType = RenderType.create("pulselib_rhi_opaque_" + source.pipeline().getLocation(),
				RenderSetup.builder(profile).createRenderSetup());
		return new PRhiNativeMaterial(nativeType, this.profiles.instanceCapacity(), PTransparencyMode.ORDINARY, usesDeformers(profile) ? this.deformerUniforms : Map.of(), () ->
		{
			PreparedRenderType prepared = Objects.requireNonNull(this.prepare.apply(source));
			if (prepared.pipeline() != source.pipeline())
				throw new IllegalArgumentException("Opaque native preparation returned a different source pipeline");
			return new PreparedRenderType(profile, prepared.outputTarget(), prepared.dynamicTransforms(),
					prepared.scissorState(), prepared.textures());
		}, this.target);
	}

	private @Nullable RenderPipeline profile(RenderType type, boolean immediate)
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
		if (!this.deformerUniforms.isEmpty())
		{
			String path = pipeline.getLocation().getPath();
			boolean translucent = Set.of("pipeline/triangles_translucent_no_cull", "pipeline/triangles_emissive_translucent_no_cull").contains(path);
			if ((selected != null && usesDeformers(pipeline)) || (immediate && translucent))
				selected = pipeline;
		}

		if (selected == null || (!immediate && type.hasBlending()) || pipeline.isCull() || pipeline.getDepthStencilState() == null ||
				(!immediate && !pipeline.getDepthStencilState().writeDepth()) || type.format().getVertexSize() != 24)
			return null;
		return selected;
	}

	private static boolean usesDeformers(RenderPipeline pipeline)
	{
		var uniforms = BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts());
		return uniforms.stream().anyMatch(uniform -> uniform.name().equals("DeformerOperations")) &&
				uniforms.stream().anyMatch(uniform -> uniform.name().equals("DeformerValues"));
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
