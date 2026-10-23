/**
 * @author ArcAnc
 * Created at: 23.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.mixin.GlBufferAccessor;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.model.baked.PMeshRenderMaterial;
import com.arcanc.pulselib.content.model.baked.PMeshTextureVariants;
import com.arcanc.pulselib.content.model.baked.PGpuDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerPrepareContext;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PDeformerValueSource;
import com.arcanc.pulselib.content.model.deformer.PHingeDeformer;
import com.arcanc.pulselib.content.model.deformer.PHingeDefinition;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformer;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.TestBlockEntityRenderer;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.gl.PGlGeometryArena;
import com.arcanc.pulselib.content.renderer.gl.PGlRenderHost;
import com.arcanc.pulselib.content.renderer.gl.PGlResourceRegistry;
import com.arcanc.pulselib.content.renderer.legacy.GlDynamicGeometry;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.nio.ByteOrder;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static com.arcanc.pulselib.test.stage1.Stage41bGeometryProbe.field;
import static com.arcanc.pulselib.test.stage1.Stage41bGeometryProbe.read;
import static com.arcanc.pulselib.test.stage1.Stage41bGeometryProbe.verifySlice;

/** Real GPU checks for canonical baked models, portable material variants and deformation lifetime. */
public final class Stage41b2BakedProbe
{
	private static final Set<Object> OWNERS = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final PRenderQueue.RenderStage SENTINEL = new PRenderQueue.RenderStage("stage41b2_sentinel");
	private static final PMeshDeformer<Float> SHIFT = new PMeshDeformer<>()
	{
		@Override
		public Identifier id()
		{
			return PLibDatabase.rl("stage41b2_shift");
		}

		@Override
		public MapCodec<Float> codec()
		{
			return Codec.FLOAT.fieldOf("shift");
		}

		@Override
		public void prepare(PDeformerPrepareContext context, Float distance)
		{
			context.add((position, values) -> position.add(distance, 0, 0));
		}
	};
	private static int pending;
	private static int passes;
	private static int lookups;
	private static int modelLookups;
	private static @Nullable GlDynamicGeometry previous;

	private Stage41b2BakedProbe()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(Stage41b2BakedProbe::render);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage41b2BakedProbe::shutdown);
	}

	public static void ownerLookup(Object owner)
	{
		check(owner != null, "Owner must be assigned before model lookup during extraction");
		OWNERS.add(owner);
		lookups++;
	}

	public static void ownerModelLookup(Object owner)
	{
		check(owner != null, "Model override must receive the extracted owner");
		modelLookups++;
	}

	public static void tick(int tick)
	{
		if (tick == 100 || tick == 230)
			pending = tick;
	}

	private static void render(RenderLevelStageEvent.AfterOpaqueBlocks event)
	{
		if (pending == 0)
			return;
		var data = TestBlockEntityRenderer.MODEL_DATA;
		var model = data.getModel();
		check(model != null, "Real canonical baked model must be available");
		if (previous != null)
			expectClosed(previous);
		var bones = IntStream.range(0, model.boneCount()).mapToObj(model::bone).toList();
		PBakedBone bone = bones.stream().filter(value -> !value.meshes().isEmpty()).findFirst().orElseThrow();
		PBakedMesh mesh = bone.meshes().getFirst();
		int triangles = bones.stream().flatMap(value -> value.meshes().stream()).filter(value -> !value.textureReference().isEmpty()).mapToInt(value -> value.geometry().indexCount() / 3).sum();
		check(triangles > 0 && mesh.geometry().indexCount() == mesh.source().indicesCount(), "Source geometry and baked index counts must agree");
		var type = PRenderTypes.RenderTypeProvider.trianglesSolid(PResourceCache.ATLAS_LOCATION);
		var context = new PMeshRenderContext(ignored -> type, -1, 0xF000F0, 0);
		PoseStack pose = new PoseStack();
		var camera = event.getLevelRenderState().cameraRenderState.pos;
		pose.translate(-camera.x, 82 - camera.y, 15 - camera.z);
		Matrix4f original = new Matrix4f(pose.last().pose());
		var frame = new PGlRenderHost().captureFrame();
		var header = new PInstanceHeader(original, -1, 0xF000F0, 0);
		PRenderQueue.submit(SENTINEL, type, mesh.geometry(), header);
		query(triangles, () -> model.instantDraw(pose, data, List.of(), (ignoredBone, ignoredMesh, inherited) -> inherited, context, 0));
		query(mesh.geometry().indexCount() / 3, () -> PRenderQueue.flush(SENTINEL));
		check(original.equals(pose.last().pose()), "Direct model draw must restore its caller's pose");
		var indexedPose = model.bindPose();
		indexedPose.visibility(model.boneIndex(bone), false);
		query(0, () -> bone.instantDraw(pose, model, indexedPose, (ignoredBone, ignoredMesh, inherited) -> inherited, context));
		try
		{
			bone.instantDraw(pose, model, model.bindPose(), (ignoredBone, ignoredMesh, inherited) ->
			{
				throw new IllegalArgumentException("probe material failure");
			}, context);
			throw new AssertionError("Material resolver exception must propagate");
		}
		catch (IllegalArgumentException expected)
		{
			check(expected.getMessage().equals("probe material failure"), "Unexpected drawing failure");
		}
		check(original.equals(pose.last().pose()), "Resolver failure must also restore the pose");

		var variant = PMeshTextureVariants.resolve(mesh, TestBlockEntityRenderer.TORUS);
		check(variant.textureLocation().equals(PResourceCache.spriteId(TestBlockEntityRenderer.TORUS)), "Texture override must select the requested atlas sprite");
		check(variant == PMeshTextureVariants.resolve(mesh, TestBlockEntityRenderer.TORUS), "Texture variants must be cached");
		var material = PMeshRenderMaterial.resolve(mesh, context.withTexture(TestBlockEntityRenderer.TORUS).withEmissive(true));
		check(material.mesh() == variant && material.emissive() && material.packedLight() == 0xF000F0, "Material overrides must retain geometry, emissive and lighting semantics");
		var subdivided = PGpuDeformedMeshBuffers.resolve(mesh, 1);
		check(subdivided.indexCount() == mesh.geometry().indexCount() * 4, "GPU subdivision must retain all four child triangles");

		var deformation = new PMeshDeformation(PDeformerStack.compile(List.of(new PDeformerInstance<>(SHIFT, 0.25f))), PDeformerValueSource.DEFAULTS, new Object(), 0);
		var dynamic = PDeformedMeshBuffers.resolve(mesh, deformation);
		check(dynamic == PDeformedMeshBuffers.resolve(mesh, deformation), "The same mesh/owner key must reuse its CPU geometry source");
		var other = PDeformedMeshBuffers.resolve(mesh, new PMeshDeformation(deformation.stack(), deformation.values(), new Object(), 0));
		check(other != dynamic, "Independent owners must not share mutable deformation geometry");
		var vertices = dynamic.vertexBuffer().geometry().vertices().order(ByteOrder.nativeOrder());
		check(Math.abs(vertices.getFloat(0) - mesh.source().positions().get(0) - 0.25f) < 0.00001f, "Custom CPU operation must be applied to real baked vertices");
		query(mesh.geometry().indexCount() / 3, () -> PRenderQueue.drawImmediate(type, mesh, deformation, header, frame));
		verifyDynamic(dynamic);
		var registry = (PGlResourceRegistry)field(PRenderQueue.class, null, "RESOURCES");
		var handle = registry.dynamic(dynamic);
		PDeformedMeshBuffers.close(mesh);
		expectClosed(dynamic);
		try
		{
			registry.indexCount(handle);
			throw new AssertionError("Closing a CPU mesh cache must invalidate its GPU handle");
		}
		catch (IllegalStateException expected)
		{
			check(expected.getMessage().contains("Unknown PulseLib mesh"), "Unexpected handle error");
		}
		Object arena = field(field(PRenderQueue.class, null, "EXECUTOR").getClass(), field(PRenderQueue.class, null, "EXECUTOR"), "geometry");
		check(!((Map<?, ?>)field(arena.getClass(), arena, "dynamicPages")).containsKey(dynamic.vertexBuffer()), "Closing one CPU mesh must remove its GPU pages immediately");
		previous = PDeformedMeshBuffers.resolve(mesh, deformation);
		query(mesh.geometry().indexCount() / 3, () -> PRenderQueue.drawImmediate(type, mesh, deformation, header, frame));

		var hinge = new PMeshDeformation(PDeformerStack.compile(List.of(new PDeformerInstance<>(PHingeDeformer.INSTANCE,
				new PHingeDefinition(new Vector3f(), new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), new PChannelReference<>("", 0.4f))))), PDeformerValueSource.DEFAULTS, new Object(), 1);
		check(PGpuDeformerBuffers.submit(hinge).operationCount() == 1, "Built-in deformation must stay on the GPU path");
		query(subdivided.indexCount() / 3, () -> PRenderQueue.drawImmediate(type, mesh, hinge, header, frame));
		var bindings = PGpuDeformerBuffers.upload();
		var operations = read(((GlBufferAccessor)bindings.operations()).pulselib$getHandle(), 0, PGpuDeformerBuffers.operations().size() * Float.BYTES).order(ByteOrder.nativeOrder());
		for (float value : PGpuDeformerBuffers.operations())
			check(operations.getFloat() == value, "Actual GPU deformation operations must match the canonical definition stream");
		check(OWNERS.size() >= 5 && lookups > 100 && modelLookups > 100, "Real Entity/BlockEntity/Item model selection must call the canonical owner hook");
		PLibDatabase.LOGGER.info("STAGE 4.1b.2 BAKED PASS: tick={}, modelTriangles={}, direct draw preserves queue/pose, visibility, materials, CPU fallback readback, GPU subdivision/deformer UBO, owners={}, dataLookups={}, modelLookups={}", pending, triangles, OWNERS.size(), lookups, modelLookups);
		passes++;
		pending = 0;
	}

	private static void verifyDynamic(GlDynamicGeometry geometry)
	{
		Object executor = field(PRenderQueue.class, null, "EXECUTOR");
		Object arena = field(executor.getClass(), executor, "geometry");
		var slots = (Map<?, ?>)((Map<?, ?>)field(arena.getClass(), arena, "dynamicPages")).get(geometry.vertexBuffer());
		check(slots != null && !slots.isEmpty(), "Custom CPU fallback must reach the real GPU arena");
		Object allocation = slots.values().stream().filter(value -> field(value.getClass(), value, "data") == geometry.vertexBuffer().geometry()).findFirst().orElseThrow();
		verifySlice((PGlGeometryArena.Slice)field(allocation.getClass(), allocation, "slice"), geometry.vertexBuffer().geometry());
	}

	private static void expectClosed(GlDynamicGeometry geometry)
	{
		try
		{
			geometry.vertexBuffer().geometry();
			throw new AssertionError("Reload/close must release old dynamic geometry sources");
		}
		catch (IllegalStateException expected)
		{
			check(expected.getMessage().contains("closed"), "Unexpected resource error");
		}
	}

	private static void query(int expected, Runnable draw)
	{
		int query = GL15.glGenQueries();
		try
		{
			GL15.glBeginQuery(GL30.GL_PRIMITIVES_GENERATED, query);
			try
			{
				draw.run();
			}
			finally
			{
				GL15.glEndQuery(GL30.GL_PRIMITIVES_GENERATED);
			}
			int actual = GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT);
			check(actual == expected, "Expected " + expected + " actual GPU triangles, got " + actual);
		}
		finally
		{
			GL15.glDeleteQueries(query);
		}
	}

	private static void shutdown(GameShuttingDownEvent event)
	{
		check(passes == 2 && previous != null, "Both pre/post reload baked checks must complete");
		expectClosed(previous);
		PLibDatabase.LOGGER.info("STAGE 4.1b.2 BAKED SHUTDOWN PASS: dynamic sources closed after normal cleanup");
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}
}
