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
import com.arcanc.pulselib.content.renderer.gl.PGlDynamicGeometry;
import com.arcanc.pulselib.content.renderer.plan.PDrawCommand;
import com.arcanc.pulselib.content.renderer.plan.PDrawGroup;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PMeshHandle;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.GpuDevice;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Actual queue adapter and GPU execution; explicit native preparation because this fixture has no Minecraft instance. */
final class RhiOpaqueChecks
{
	static void run(GpuDevice device, PRhiMaterial.Attachments target, Function<RenderType, PreparedRenderType> prepare,
			PRenderFrame frame, PBakedMesh baked, boolean cutout, boolean emissive, Consumer<Boolean> clear, Supplier<ByteBuffer> pixels)
	{
		RenderType source = source(cutout, emissive);
		var pipeline = new PPipelineHandle(41);
		var mesh = new PMeshHandle(42);
		var geometry = new HashMap<PMeshHandle, Object>();
		geometry.put(mesh, baked);
		var policy = new AtomicReference<>(PTransparencyMode.ORDINARY);
		var deviceLookups = new AtomicInteger();
		var preparations = new AtomicInteger();
		var activeSource = new AtomicReference<>(source);
		var wrongPreparation = new AtomicBoolean();
		var renderer = new PRhiOpaqueRenderer(ignored -> activeSource.get(), ignored -> policy.get(), geometry::get, () ->
		{
			deviceLookups.incrementAndGet();
			return device;
		}, type ->
		{
			preparations.incrementAndGet();
			PreparedRenderType prepared = prepare.apply(type);
			return wrongPreparation.get() ? new PreparedRenderType(PRhiPipelines.portable().pipeline(false, false),
					prepared.outputTarget(), prepared.dynamicTransforms(), prepared.scissorState(), prepared.textures()) : prepared;
		}, ignored -> target);
		var packed = new ArrayList<PInstanceHeader>();
		for (int i = 0; i < 9; i++)
			packed.add(new PInstanceHeader(new Matrix4f(), 0, 0, 0));
		for (int i = 0; i < 1025; i++)
			packed.add(RhiProfileChecks.instance(i));
		var command = new PDrawCommand(3, 1025, 0, 0, 9);
		var group = new PDrawGroup(pipeline, mesh, command, true, packed.subList(9, packed.size()));
		var plan = new PRenderPlan(List.of(group), packed);
		try
		{
			RhiLayoutChecks.assertTrue(renderer.canExecute(plan), "Canonical opaque queue profile accepted");
			renderer.execute(PRenderPlan.EMPTY, frame);
			RhiLayoutChecks.assertTrue(deviceLookups.get() == 0 && preparations.get() == 0, "Selection and empty plans do not initialize GPU resources");
			var unknown = RenderPipeline.builder().withLocation(Identifier.fromNamespaceAndPath("porting_fixture", "fullscreen")).
					withVertexShader(Identifier.fromNamespaceAndPath("pulselib", "core/oit_composite")).
					withFragmentShader(Identifier.fromNamespaceAndPath("pulselib", "core/oit_composite")).
					withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false).
					withDepthStencilState(DepthStencilState.DEFAULT).build();
			activeSource.set(RenderType.create("unknown_queue_type", RenderSetup.builder(unknown).createRenderSetup()));
			RhiLayoutChecks.assertTrue(!renderer.canExecute(plan), "Unrecognized native pipeline remains with caller");
			activeSource.set(source);
			policy.set(PTransparencyMode.OIT);
			RhiLayoutChecks.assertTrue(!renderer.canExecute(plan), "Explicit OIT policy stays on caller's existing path");
			try
			{
				renderer.execute(plan, frame);
				throw new AssertionError("Opaque adapter accepted OIT");
			}
			catch (UnsupportedOperationException expected)
			{
				RhiLayoutChecks.assertTrue(deviceLookups.get() == 0 && preparations.get() == 0, "Unsupported plan rejected before any GPU side effects");
			}
			policy.set(PTransparencyMode.ORDINARY);
			var deformed = new PInstanceHeader(new Matrix4f(), -1, 0, 0, 0, 0, 1);
			var unsupported = new PDrawGroup(pipeline, mesh, new PDrawCommand(3, 1, 0, 0, 0), true, List.of(deformed));
			RhiLayoutChecks.assertTrue(!renderer.canExecute(new PRenderPlan(List.of(group, unsupported))), "Mixed supported/GPU-deformed plan is not split");
			RhiLayoutChecks.assertTrue(!renderer.canExecute(new PRenderPlan(List.of(
					new PDrawGroup(pipeline, mesh, command, false, group.instances())))), "Transparent draw-depth policy not rewritten");
			clear.accept(false);
			renderer.execute(plan, frame);
			RhiProfileChecks.checkPixels(pixels.get(), cutout);
			RhiLayoutChecks.assertTrue(deviceLookups.get() == 1 && preparations.get() == 1, "One device lookup and one source prepare across nine native draws");
			clear.accept(true);
			renderer.execute(plan, frame);
			RhiProfileChecks.checkPixels(pixels.get(), false);
			RhiLayoutChecks.assertTrue(deviceLookups.get() == 1 && preparations.get() == 2, "Bindings cached; native context prepared again on next execution");
			clear.accept(true);
			wrongPreparation.set(true);
			try
			{
				renderer.execute(plan, frame);
				throw new AssertionError("Mismatched native preparation accepted");
			}
			catch (IllegalArgumentException expected)
			{
				RhiLayoutChecks.assertTrue(expected.getMessage().contains("different source pipeline"), "Native source pipeline identity verified before drawing");
			}
			wrongPreparation.set(false);
			RhiProfileChecks.checkPixels(pixels.get(), true);
			try (var dynamic = new PGlDynamicGeometry(baked.geometry()))
			{
				geometry.put(mesh, dynamic);
				dynamic.update(RhiProfileChecks.geometry(-1, true));
				clear.accept(true);
				renderer.execute(plan, frame);
				RhiProfileChecks.checkPixel(pixels.get(), 0, emissive ? RhiProfileChecks.color(0) : RhiProfileChecks.shade(RhiProfileChecks.color(0)));
			}
			geometry.put(mesh, baked);
			renderer.cleanup();
			renderer.cleanup();
			RhiLayoutChecks.assertTrue(!target.color().isClosed(), "Cleanup does not own host output resources");
			clear.accept(true);
			var compiler = new PFrameCompiler<String>();
			for (int i = 0; i < 1025; i++)
				compiler.submit("opaque", pipeline, mesh, 3, RhiProfileChecks.instance(i), false);
			renderer.execute(compiler.compileOpaque("opaque"), frame);
			RhiProfileChecks.checkPixels(pixels.get(), false);
			RhiLayoutChecks.assertTrue(compiler.isEmpty(), "Canonical compiler consumed its opaque stage");
			RhiLayoutChecks.assertTrue(deviceLookups.get() == 2, "Cleanup followed by lazy resource recreation");
			System.out.println("PASS opaque queue adapter " + (emissive ? "emissive " : "lit ") + (cutout ? "cutout" : "solid") +
					": canonical compiler/pipeline mapping, 1025 pixels, packed firstInstance=9, atomic selection, native prepare/cache and dynamic/cleanup/recreate");
		}
		finally
		{
			renderer.cleanup();
		}
	}

	/** Native metadata matching canonical IDs/shader references; only the adapted RHI profile is submitted to GPU. */
	private static RenderType source(boolean cutout, boolean emissive)
	{
		String path = "pipeline/triangles_" + (emissive ? "emissive_" : "") + (cutout ? "cutout" : "solid") + "_no_cull";
		var pipeline = RenderPipeline.builder().withLocation(Identifier.fromNamespaceAndPath("pulselib", path)).
				withVertexShader(Identifier.fromNamespaceAndPath("pulselib", "core/triangles")).
				withFragmentShader(Identifier.fromNamespaceAndPath("pulselib", "core/triangles")).
				withVertexBinding(0, PRhiPipelines.POSITION_TEX_NORMAL).
				withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false).
				withDepthStencilState(DepthStencilState.DEFAULT).build();
		return RenderType.create("canonical_opaque_queue_source", RenderSetup.builder(pipeline).createRenderSetup());
	}
}
