/**
 * @author ArcAnc
 * Created at: 07.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.baked.PGpuDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.deformer.PBendDeformer;
import com.arcanc.pulselib.content.model.deformer.PBendDefinition;
import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PDeformerValueSource;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.legacy.GlRenderState;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.util.List;
import java.util.stream.IntStream;

import static com.arcanc.pulselib.test.RegressionChecks.check;

/** Populates the actual queue/backend resources; does not replace any renderer or cleanup hook. */
final class SmokeLifecycleWorkload
{
	private static final PRenderQueue.RenderStage STAGE = new PRenderQueue.RenderStage("smoke_lifecycle");

	private SmokeLifecycleWorkload()
	{
	}

	static void draw()
	{
		var model = ClientTestPlugin.GLTF.getModel();
		PBakedMesh mesh = IntStream.range(0, model.boneCount()).mapToObj(model::bone).
				flatMap(bone -> bone.meshes().stream()).findFirst().orElseThrow();
		var projection = RenderSystem.getProjectionMatrixBuffer();
		var projectionType = RenderSystem.getProjectionType();
		var color = RenderSystem.outputColorTextureOverride;
		var depth = RenderSystem.outputDepthTextureOverride;
		var target = new TextureTarget("PulseLib lifecycle resources", 64, 64, true);
		int query = GL15.glGenQueries();
		RenderSystem.getModelViewStack().pushMatrix().identity();
		try (var state = new GlRenderState(); var identity = new ProjectionMatrixBuffer("PulseLib lifecycle projection"))
		{
			RenderSystem.setProjectionMatrix(identity.getBuffer(new Matrix4f()), ProjectionType.ORTHOGRAPHIC);
			RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), 0, target.getDepthTexture(), 1);
			RenderSystem.outputColorTextureOverride = target.getColorTextureView();
			RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();
			PDeformerStack stack = PDeformerStack.compile(List.of(new PDeformerInstance<>(PBendDeformer.INSTANCE,
					new PBendDefinition(new Vector3f(), new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), 1f, 1f,
							new PChannelReference<>("test_angle", (float)Math.toRadians(15))))));
			var deformation = new PMeshDeformation(stack, PDeformerValueSource.DEFAULTS, mesh, 1);
			var submission = PGpuDeformerBuffers.submit(deformation);
			check(submission.operationCount() > 0, "GPU deformer stream must receive operations");
			var geometry = PGpuDeformedMeshBuffers.resolve(mesh, 1);
			var type = PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(PResourceCache.ATLAS_LOCATION);
			GL15.glBeginQuery(GL30.GL_PRIMITIVES_GENERATED, query);
			try
			{
				var header = new PInstanceHeader(new Matrix4f().scale(0.01f), -1, 0xF000F0, OverlayTexture.NO_OVERLAY);
				PRenderQueue.submit(STAGE, type, PDeformedMeshBuffers.resolve(mesh, deformation).vertexBuffer(), header, PTransparencyMode.ORDINARY);
				PRenderQueue.flush(STAGE, target);
				for (int i = 0; i < 8; i++)
					PRenderQueue.submit(STAGE, type, geometry,
							new PInstanceHeader(new Matrix4f().scale(0.01f).translate(0, 0, i * 0.001f), 0x80FFFFFF, 0xF000F0, OverlayTexture.NO_OVERLAY,
									submission.operationOffset(), submission.valueOffset(), submission.operationCount()), PTransparencyMode.OIT);
				PRenderQueue.flush(STAGE, target);
				PRenderQueue.compositeTranslucency(target);
			}
			finally
			{
				GL15.glEndQuery(GL30.GL_PRIMITIVES_GENERATED);
			}
			int primitives = GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT);
			check(primitives > 0, "Lifecycle fixture must execute real GPU draws before capture");
			PLibDatabase.LOGGER.info("PULSE TEST LIFECYCLE WORKLOAD PASS: primitives={}", primitives);
			PGpuDeformerBuffers.finishFrame();
		}
		finally
		{
			GL15.glDeleteQueries(query);
			RenderSystem.outputColorTextureOverride = color;
			RenderSystem.outputDepthTextureOverride = depth;
			RenderSystem.setProjectionMatrix(projection, projectionType);
			RenderSystem.getModelViewStack().popMatrix();
			target.destroyBuffers();
		}
	}
}
