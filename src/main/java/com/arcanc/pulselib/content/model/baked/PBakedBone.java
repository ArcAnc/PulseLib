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
import com.arcanc.pulselib.content.model.animation.PPose;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * WARNING! Name should be unique per bone!
 */
public record PBakedBone(String name,
                         Vector3f basePosition,
                         Quaternionf baseRotation,
                         List<PBakedBone> children,
                         @Nullable PBakedBone parent,
                         List<PBakedMesh> meshes)
{
	public void instantDraw(PoseStack poseStack,
	                        PBakedModel model,
	                        PPose pose,
	                        Function<ResourceLocation, RenderType> renderType,
	                        int color,
	                        int packedLight,
	                        int packedOverlay)
	{
		PMeshRenderContext context = new PMeshRenderContext(renderType, color, packedLight, packedOverlay);
		instantDraw(poseStack, model, pose, (bone, mesh, inherited) -> inherited, context);
	}

	public void instantDraw(PoseStack poseStack,
	                        PBakedModel model,
	                        PPose pose,
	                        PMeshRenderResolver resolver,
	                        PMeshRenderContext inherited)
	{
		int boneIndex = model.boneIndex(this);
		poseStack.pushPose();
		poseStack.translate(pose.translation(boneIndex).x(), pose.translation(boneIndex).y(), pose.translation(boneIndex).z());
		poseStack.mulPose(pose.rotation(boneIndex));
		poseStack.scale(pose.scale(boneIndex).x(), pose.scale(boneIndex).y(), pose.scale(boneIndex).z());
		Matrix4f matrix4fstack = new Matrix4f(RenderSystem.getModelViewMatrix());
		matrix4fstack.mul(poseStack.last().pose());

		PMeshRenderContext boneContext = inherited;
		this.meshes().forEach(mesh -> drawMesh(mesh, this, resolver, boneContext, poseStack, matrix4fstack));
		this.children().forEach(child -> child.instantDraw(poseStack, model, pose, resolver, boneContext));
		poseStack.popPose();
	}

	public <T extends PAnimatable<T>> void instantDraw(PoseStack poseStack,
	                                                   PModelData modelData,
	                                                   Collection<PAnimationController<T>> controllers,
	                                                   Function<ResourceLocation, RenderType> renderType,
	                                                   int color,
	                                                   int packedLight,
	                                                   int packedOverlay,
	                                                   float partialTick)
	{
		instantDraw(poseStack, modelData, controllers, Map.of(), renderType, color, packedLight, packedOverlay, partialTick);
	}

	public <T extends PAnimatable<T>> void instantDraw(PoseStack poseStack,
	                                                   PModelData modelData,
	                                                   Collection<PAnimationController<T>> controllers,
	                                                   Map<PAnimationController<T>, MolangParser.Context> molangContexts,
	                                                   Function<ResourceLocation, RenderType> renderType,
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
		instantDraw(poseStack, modelData, controllers, molangContexts, (bone, mesh, inherited) -> inherited, context, partialTick);
	}
	
	public <T extends PAnimatable<T>> void instantDraw(PoseStack poseStack,
	                                                   PModelData modelData,
	                                                   Collection<PAnimationController<T>> controllers,
	                                                   PMeshRenderResolver resolver,
	                                                   PMeshRenderContext inherited,
	                                                   float partialTick)
	{
		instantDraw(poseStack, modelData, controllers, Map.of(), resolver, inherited, partialTick);
	}

	public <T extends PAnimatable<T>> void instantDraw(PoseStack poseStack,
	                                                   PModelData modelData,
	                                                   Collection<PAnimationController<T>> controllers,
	                                                   Map<PAnimationController<T>, MolangParser.Context> molangContexts,
	                                                   PMeshRenderResolver resolver,
	                                                   PMeshRenderContext inherited,
	                                                   float partialTick)
	{
		BoneFrame frame = mixBone(modelData.getModel(), controllers, molangContexts, partialTick);
		poseStack.pushPose();
		if (frame != null)
		{
			poseStack.translate(frame.translation().x(), frame.translation().y(), frame.translation().z());
			poseStack.mulPose(frame.rotation());
			poseStack.scale(frame.scale().x(), frame.scale().y(), frame.scale().z());
		}
		else
		{
			poseStack.translate(this.basePosition().x(), this.basePosition().y(), this.basePosition().z());
			poseStack.mulPose(this.baseRotation());
		}
		Matrix4f matrix4fstack = new Matrix4f(RenderSystem.getModelViewMatrix());
		matrix4fstack.mul(poseStack.last().pose());
		
		PMeshRenderContext boneContext = inherited;
		this.meshes().forEach(mesh ->
		{
			if (mesh.textureReference().isEmpty())
				return;
			
			PMeshRenderContext meshContext = resolver.resolve(this, mesh, boneContext);
			PMeshRenderMaterial material = PMeshRenderMaterial.resolve(mesh, meshContext);
			
			RenderType type = material.resolveRenderType(meshContext, PResourceCache.ATLAS_LOCATION);
			
			int u = meshContext.packedOverlay() & 0xFFFF;
			int v = (meshContext.packedOverlay() >> 16) & 0xFFFF;
			int red = FastColor.ARGB32.red(meshContext.color());
			int green = FastColor.ARGB32.green(meshContext.color());
			int blue = FastColor.ARGB32.blue(meshContext.color());
			int alpha = FastColor.ARGB32.alpha(meshContext.color());
			Vector4f colorVector = new Vector4f(red / 255f, green / 255f, blue / 255f, alpha / 255f);
			
			int meshPackedLight = material.packedLight();
			int blockLight = LightTexture.block(meshPackedLight);
			int skyLight = LightTexture.sky(meshPackedLight);
			type.setupRenderState();
			
			ShaderInstance shaderInstance = RenderSystem.getShader();
			if (shaderInstance == null)
				return;
			VertexBuffer vertexBuffer = PDeformedMeshBuffers.resolve(material.mesh(), meshContext.deformation()).vertexBuffer();
			vertexBuffer.bind();
			shaderInstance.safeGetUniform("Color").set(colorVector);
			shaderInstance.safeGetUniform("Light").set(blockLight, skyLight);
			shaderInstance.safeGetUniform("Overlay").set(u, v);
			shaderInstance.safeGetUniform("NormalMat").set(poseStack.last().normal());
			shaderInstance.apply();
			vertexBuffer.drawWithShader(matrix4fstack, RenderSystem.getProjectionMatrix(), shaderInstance);
			VertexBuffer.unbind();
			type.clearRenderState();
		});
		
		this.children().forEach(children ->
				children.instantDraw(poseStack, modelData, controllers, molangContexts, resolver, boneContext, partialTick));
		
		poseStack.popPose();
	}
	
	public <T extends PAnimatable<T>>@Nullable BoneFrame mixBone(
			PBakedModel model,
			Collection<PAnimationController<T>> controllers, float partialTick)
	{
		return mixBone(model, controllers, Map.of(), partialTick);
	}

	public <T extends PAnimatable<T>>@Nullable BoneFrame mixBone(
			PBakedModel model,
			Collection<PAnimationController<T>> controllers,
			Map<PAnimationController<T>, MolangParser.Context> molangContexts,
			float partialTick)
	{
		PAnimationPoseResolver.LocalPose pose = PAnimationPoseResolver.resolveLocal(
				this,
				model,
				controllers,
				(controller, tick) ->
				{
					MolangParser.Context context = molangContexts.get(controller);
					return context == null ? PAnimationPoseResolver.<T>defaultContexts().context(controller, tick) : context;
				},
				partialTick);
		return pose.hasTranslation() || pose.hasRotation() || pose.hasScale() ? pose.localTransform() : null;
	}
	
	public static final class PBakedBoneBuilder
	{
		public final UUID uuid;
		public final String name;
		public final Vector3f basePosition;
		public final Quaternionf baseRotation;
		
		public PBakedBoneBuilder parent;
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

	private static void drawMesh(PBakedMesh mesh,
	                             PBakedBone bone,
	                             PMeshRenderResolver resolver,
	                             PMeshRenderContext inherited,
	                             PoseStack poseStack,
	                             Matrix4f matrix4fstack)
	{
		if (mesh.textureReference().isEmpty())
			return;
		PMeshRenderContext meshContext = resolver.resolve(bone, mesh, inherited);
		PMeshRenderMaterial material = PMeshRenderMaterial.resolve(mesh, meshContext);
		RenderType type = material.resolveRenderType(meshContext, PResourceCache.ATLAS_LOCATION);
		int u = meshContext.packedOverlay() & 0xFFFF;
		int v = (meshContext.packedOverlay() >> 16) & 0xFFFF;
		int color = meshContext.color();
		Vector4f colorVector = new Vector4f(FastColor.ARGB32.red(color) / 255f, FastColor.ARGB32.green(color) / 255f,
				FastColor.ARGB32.blue(color) / 255f, FastColor.ARGB32.alpha(color) / 255f);
		type.setupRenderState();
		ShaderInstance shader = RenderSystem.getShader();
		if (shader != null)
		{
			VertexBuffer vertexBuffer = PDeformedMeshBuffers.resolve(material.mesh(), meshContext.deformation()).vertexBuffer();
			vertexBuffer.bind();
			shader.safeGetUniform("Color").set(colorVector);
			shader.safeGetUniform("Light").set(LightTexture.block(material.packedLight()), LightTexture.sky(material.packedLight()));
			shader.safeGetUniform("Overlay").set(u, v);
			shader.safeGetUniform("NormalMat").set(poseStack.last().normal());
			shader.apply();
			vertexBuffer.drawWithShader(matrix4fstack, RenderSystem.getProjectionMatrix(), shader);
			VertexBuffer.unbind();
		}
		type.clearRenderState();
	}
}
