/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.legacy;

import com.arcanc.pulselib.content.renderer.gl.PGlMultiDrawExecutor;
import com.arcanc.pulselib.content.renderer.plan.PDrawCommand;
import com.arcanc.pulselib.content.renderer.plan.PDrawGroup;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PRenderCapabilities;
import com.arcanc.pulselib.content.renderer.plan.PRenderCapabilityMatrix;
import com.arcanc.pulselib.content.renderer.plan.PRenderExecutor;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.mojang.blaze3d.pipeline.RenderTarget;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes compiled render plans using OpenGL geometry arenas, instancing and weighted transparency.
 */
public final class GlDrawExecutor implements PRenderExecutor
{
	private final GlResourceRegistry resources;
	private final PGlMultiDrawExecutor executor;

	/** Shares the registry's native handles and geometry arena. */
	public GlDrawExecutor(GlResourceRegistry resources)
	{
		this.resources = resources;
		this.executor = new PGlMultiDrawExecutor(resources.nativeResources(), resources.geometryArena().nativeArena());
	}

	@Override
	public PRenderCapabilities capabilities()
	{
		return this.executor.capabilities();
	}

	/** Reports the actual features of the active OpenGL context. */
	public PRenderCapabilityMatrix capabilityMatrix()
	{
		return GlCapabilityProbe.currentContext();
	}

	@Override
	public void execute(PRenderPlan plan, PRenderFrame frame)
	{
		this.executor.execute(meshLocal(plan), frame);
	}

	/** Executes with an explicit source of opaque depth. */
	public void execute(PRenderPlan plan, PRenderFrame frame, RenderTarget depthSource)
	{
		this.executor.execute(meshLocal(plan), frame, depthSource);
	}

	/** Executes ordinary local transparency into the captured caller target. */
	public void executeLocal(PRenderPlan plan, PRenderFrame frame, GlRenderState target)
	{
		if (plan.isEmpty())
			return;
		target.bindTarget();
		this.executor.executeImmediateWaiting(meshLocal(plan), frame);
	}

	/** Resolves pending OIT into the explicit native destination. */
	public void compositeOit(RenderTarget destination)
	{
		this.executor.compositeOit(destination);
	}

	/** Resolves OIT with the five native Fabulous input targets. */
	public void compositeFabulousOit(RenderTarget destination, List<RenderTarget> targets)
	{
		this.executor.compositeFabulousOit(destination, targets);
	}

	@Override
	public void cleanup()
	{
		this.executor.cleanup(false);
		GlStreamStorage.cleanup();
	}

	private PRenderPlan meshLocal(PRenderPlan plan)
	{
		List<PDrawGroup> groups = new ArrayList<>(plan.groups().size());
		for (PDrawGroup group : plan.groups())
		{
			PDrawCommand command = group.command();
			if (this.resources.isGeometry(group.mesh()))
			{
				GlGeometryArena.Slice slice = this.resources.geometry(group.mesh());
				PGeometryData.IndexType indexType = this.resources.nativeResources().geometryData(group.mesh()).indexType();
				command = new PDrawCommand(command.indexCount(), command.instanceCount(),
						Math.subtractExact(command.firstIndex(), slice.firstIndex(indexType)),
						Math.subtractExact(command.baseVertex(), slice.baseVertex()), command.firstInstance());
			}
			groups.add(group.withCommand(command));
		}
		return new PRenderPlan(groups, plan.instances());
	}
}
