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
import com.arcanc.pulselib.content.renderer.gl.PGlResourceRegistry;
import com.arcanc.pulselib.content.renderer.plan.PDrawCommand;
import com.arcanc.pulselib.content.renderer.plan.PDrawGroup;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.util.PRenderTypes;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
		RhiLayoutChecks.assertTrue(source == source(cutout, emissive) && source.outline().isPresent(), "Production factory cache and outline metadata");
		var registry = new PGlResourceRegistry();
		var pipeline = registry.pipeline(source);
		var mesh = registry.mesh(baked);
		var deviceLookups = new AtomicInteger();
		var preparations = new AtomicInteger();
		var wrongPreparation = new AtomicBoolean();
		var renderer = new PRhiOpaqueRenderer(registry::pipeline, registry::transparency, registry::geometryResource, () ->
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
			var unknownHandle = registry.pipeline(RenderType.create("unknown_queue_type", RenderSetup.builder(unknown).createRenderSetup()), PTransparencyMode.ORDINARY);
			var unknownPlan = new PRenderPlan(List.of(new PDrawGroup(unknownHandle, mesh, command, true, group.instances())), packed);
			RhiLayoutChecks.assertTrue(!renderer.canExecute(unknownPlan), "Unrecognized native pipeline remains with caller");
			var translucent = PRenderTypes.RenderTypeProvider.trianglesTranslucent(Identifier.fromNamespaceAndPath("pulselib", "profile_fixture"));
			var oitHandle = registry.pipeline(translucent, PTransparencyMode.OIT);
			var oitPlan = new PRenderPlan(List.of(new PDrawGroup(oitHandle, mesh, command, true, group.instances())), packed);
			RhiLayoutChecks.assertTrue(!renderer.canExecute(oitPlan), "Explicit OIT policy stays on caller's existing path");
			try
			{
				renderer.execute(oitPlan, frame);
				throw new AssertionError("Opaque adapter accepted OIT");
			}
			catch (UnsupportedOperationException expected)
			{
				RhiLayoutChecks.assertTrue(deviceLookups.get() == 0 && preparations.get() == 0, "Unsupported plan rejected before any GPU side effects");
			}
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
				var dynamicHandle = registry.dynamic(dynamic);
				dynamic.update(RhiProfileChecks.geometry(-1, true));
				clear.accept(true);
				renderer.execute(new PRenderPlan(List.of(new PDrawGroup(pipeline, dynamicHandle, command, true, group.instances())), packed), frame);
				RhiProfileChecks.checkPixel(pixels.get(), 0, emissive ? RhiProfileChecks.color(0) : RhiProfileChecks.shade(RhiProfileChecks.color(0)));
			}
			renderer.cleanup();
			renderer.cleanup();
			registry.clear();
			pipeline = registry.pipeline(source);
			mesh = registry.mesh(baked);
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
					": production factories/registry/compiler, 1025 pixels, packed firstInstance=9, native prepare/cache and dynamic/cleanup/recreate");
		}
		finally
		{
			renderer.cleanup();
			registry.clear();
		}
	}

	/** Uses the actual MultiLoader public factories, including their native setup and cache. */
	private static RenderType source(boolean cutout, boolean emissive)
	{
		var texture = Identifier.fromNamespaceAndPath("pulselib", "profile_fixture");
		if (emissive)
			return cutout ? PRenderTypes.RenderTypeProvider.trianglesEmissiveCutout(texture) : PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(texture);
		return cutout ? PRenderTypes.RenderTypeProvider.trianglesCutout(texture) : PRenderTypes.RenderTypeProvider.trianglesSolid(texture);
	}
}
