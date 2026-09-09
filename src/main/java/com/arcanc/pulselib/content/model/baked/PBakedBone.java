/**
 * @author ArcAnc
 * Created at: 28.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.baked;


import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.model.animation.BoneFrame;
import com.arcanc.pulselib.content.model.animation.PAnimationPoseResolver;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PTextureCache;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.jetbrains.annotations.ApiStatus;
import org.joml.*;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * WARNING! Name should be unique per bone!
 */
public class PBakedBone
{
	private final String name;
	private final Vector3f basePosition;
	private final Quaternionf baseRotation;
	private final List<PBakedBone> children;
	private final @Nullable PBakedBone parent;
	private final List<PBakedMesh> meshes;
	private @Nullable MappableRingBuffer colorLightOverlay;
	private static final int FULL_BRIGHT = 0x00F000F0;
	
	public PBakedBone(String name,
	                  Vector3f basePosition,
	                  Quaternionf baseRotation,
	                  List<PBakedBone> children,
	                  @Nullable PBakedBone parent,
	                  List<PBakedMesh> meshes)
	{
		this.name = name;
		this.basePosition = basePosition;
		this.baseRotation = baseRotation;
		this.children = children;
		this.parent = parent;
		this.meshes = meshes;
	}
	
	@ApiStatus.Internal
	public void ensureBufferInitialized()
	{
		if (this.colorLightOverlay != null)
			return;
		this.colorLightOverlay = new MappableRingBuffer(
				() -> PLibDatabase.rl("color_overlay").toLanguageKey(),
				GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE,
				new Std140SizeCalculator().
						putVec4().
						putVec2().
						putIVec2().
						putIVec4().
						get());
	}
	
	public <T extends PAnimatable<T>>void instantDraw(PoseStack poseStack,
	                                                  PModelData modelData,
	                                                  Collection<PAnimationController<T>> controllers,
	                                                  Function<Identifier, RenderType> renderType,
	                                                  int color,
	                                                  int packedOverlay,
	                                                  float partialTick)
	{
		instantDraw(poseStack, modelData, controllers, renderType, color, FULL_BRIGHT, packedOverlay, partialTick);
	}

	public <T extends PAnimatable<T>>void instantDraw(PoseStack poseStack,
	                                                  PModelData modelData,
	                                                  Collection<PAnimationController<T>> controllers,
	                                                  Function<Identifier, RenderType> renderType,
	                                                  int color,
	                                                  int packedLight,
	                                                  int packedOverlay,
	                                                  float partialTick)
	{
		PMeshRenderContext context = new PMeshRenderContext(
				renderType,
				color,
				packedLight,
				packedOverlay);
		instantDraw(poseStack, modelData, controllers, (bone, mesh, inherited) -> inherited, context, partialTick);
	}
	
	public <T extends PAnimatable<T>>void instantDraw(PoseStack poseStack,
	                                                  PModelData modelData,
	                                                  Collection<PAnimationController<T>> controllers,
	                                                  PMeshRenderResolver resolver,
	                                                  PMeshRenderContext inherited,
	                                                  float partialTick)
	{
		PAnimationPoseResolver<T> poseResolver = new PAnimationPoseResolver<>(
				modelData.getModel(),
				controllers,
				PAnimationPoseResolver.defaultContexts(),
				partialTick);
		instantDraw(poseStack, poseResolver, resolver, inherited);
	}
	
	public void instantDraw(PoseStack poseStack,
	                        PAnimationPoseResolver<?> poseResolver,
	                        PMeshRenderResolver resolver,
	                        PMeshRenderContext inherited)
	{
		BoneFrame frame = poseResolver.resolve(this).localTransform();
		poseStack.pushPose();
		poseStack.translate(frame.translation().x(), frame.translation().y(), frame.translation().z());
		poseStack.mulPose(frame.rotation());
		poseStack.scale(frame.scale().x(), frame.scale().y(), frame.scale().z());
		
		Minecraft mc = PLibRenderHelper.mc();
		
		TextureAtlas atlas = PTextureCache.getTextureAtlas();
		Matrix4f matrix4fStack = new Matrix4f(RenderSystem.getModelViewMatrix());
		matrix4fStack.mul(poseStack.last().pose());
		GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().
				writeTransform(matrix4fStack, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), new Vector3f(), new Matrix4f());
				
		PMeshRenderContext boneContext = inherited;
		this.meshes().forEach(mesh ->
		{
			if (mesh.textureName().isEmpty())
				return;
			
			PMeshRenderContext meshContext = resolver.resolve(this, mesh, boneContext);
			PMeshRenderMaterial material = PMeshRenderMaterial.resolve(mesh, meshContext);
			RenderType type = material.resolveInstantRenderType(meshContext, PTextureCache.ATLAS_LOCATION);

			RenderTarget renderTarget = type.outputTarget().getRenderTarget();

			GpuTextureView colorAttachment = RenderSystem.outputColorTextureOverride != null
					? RenderSystem.outputColorTextureOverride
					: renderTarget.getColorTextureView();

			GpuTextureView depthTexture = renderTarget.useDepth
					? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : renderTarget.getDepthTextureView())
					: null;
			
			GpuTextureView lightTexture = mc.gameRenderer.levelLightmap();
			OverlayTexture overlayTexture = mc.gameRenderer.overlayTexture();
			
			this.ensureBufferInitialized();
			try (GpuBuffer.MappedView colorLightOverlayMappedView = RenderSystem.getDevice().
					createCommandEncoder().
					mapBuffer(this.colorLightOverlay.currentBuffer(), false, true))
			{
				int u = meshContext.packedOverlay() & 0xFFFF;
				int v = (meshContext.packedOverlay() >> 16) & 0xFFFF;
				
				PGpuDeformerBuffers.Submission deformer = PGpuDeformerBuffers.submit(meshContext.deformation());
				Std140Builder.intoBuffer(colorLightOverlayMappedView.data()).
						putVec4(ARGB.vector4fFromARGB32(meshContext.color())).
						putVec2(LightCoordsUtil.block(material.packedLight()), LightCoordsUtil.sky(material.packedLight())).
						putIVec2(new Vector2i(u, v)).
						putIVec4(deformer.operationOffset(), deformer.valueOffset(), deformer.operationCount(), 0);
				
			}
			
			PBakedMesh deformedMesh = PSubdividedMeshCache.resolve(material.mesh(), meshContext.deformation() == null ? 0 : meshContext.deformation().subdivisionLevel());
			PGpuDeformerBuffers.Bindings deformerBuffers = PGpuDeformerBuffers.upload();
			try(RenderPass pass = RenderSystem.getDevice().createCommandEncoder().
					createRenderPass(mesh.uuid() :: toString, colorAttachment, OptionalInt.empty(), depthTexture, OptionalDouble.empty()))
			{
				pass.setPipeline(type.pipeline());
				RenderSystem.bindDefaultUniforms(pass);
				pass.setUniform("ColorOverlay", colorLightOverlay.currentBuffer());
				pass.setUniform("DynamicTransforms", transforms);
				pass.setUniform("DeformerOperations", deformerBuffers.operations());
				pass.setUniform("DeformerValues", deformerBuffers.values());
				pass.bindTexture("Sampler0", atlas.getTextureView(), atlas.getSampler());
				pass.bindTexture("Sampler1", overlayTexture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
				pass.bindTexture("Sampler2", lightTexture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
				pass.setVertexBuffer(0, deformedMesh.vbo());
				pass.setIndexBuffer(deformedMesh.indices(), deformedMesh.indexType());
				
				pass.drawIndexed(0, 0, deformedMesh.indicesCount(), 1);
			}
		});
		
		this.children().forEach(children ->
				children.instantDraw(poseStack, poseResolver, resolver, boneContext));
		
		poseStack.popPose();
	}
	
	public <T extends PAnimatable<T>>@Nullable BoneFrame mixBone(
			PBakedModel model,
			Collection<PAnimationController<T>> controllers,
			float partialTick)
	{
		return mixBone(model, controllers, Map.of(), partialTick);
	}

	public <T extends PAnimatable<T>>@Nullable BoneFrame mixBone(
			PBakedModel model,
			Collection<PAnimationController<T>> controllers,
			Map<PAnimationController<T>, MolangParser.Context> molangContexts,
			float partialTick)
	{
		PAnimationPoseResolver<T> resolver = new PAnimationPoseResolver<>(
				model,
				controllers,
				(controller, tick) -> molangContexts.getOrDefault(controller,
						PAnimationPoseResolver.<T>defaultContexts().context(controller, tick)),
				partialTick);
		PAnimationPoseResolver.BonePose pose = resolver.resolve(this);
		return pose.isAnimated() ? pose.localTransform() : null;
	}
	
	public String name()
	{
		return this.name;
	}
	
	public Vector3f basePosition()
	{
		return this.basePosition;
	}
	
	public Quaternionf baseRotation()
	{
		return this.baseRotation;
	}
	
	public List<PBakedBone> children()
	{
		return this.children;
	}
	
	@Nullable
	public PBakedBone parent()
	{
		return this.parent;
	}
	
	public List<PBakedMesh> meshes()
	{
		return this.meshes;
	}
	
	public static final class PBakedBoneBuilder
	{
		public final UUID uuid;
		public final String name;
		public final Vector3f basePosition;
		public final Quaternionf baseRotation;
		
		public @Nullable PBakedBoneBuilder parent;
		public final List<PBakedBoneBuilder> children = new ArrayList<>();
		public final List<PBakedMesh> meshes = new ArrayList<>();
		
		public PBakedBoneBuilder(
				UUID uuid,
				String name,
				Vector3f basePosition,
				Quaternionf baseRotation)
		{
			this.uuid = uuid;
			this.name = name;
			this.basePosition = basePosition;
			this.baseRotation = baseRotation;
		}
	}
}
