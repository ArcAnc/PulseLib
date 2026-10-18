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
 *
 * @param name the name
 * @param basePosition the unanimated local bone position
 * @param baseRotation the unanimated local bone rotation
 * @param children the child bones in hierarchy order
 * @param parent the parent; may be {@code null}
 * @param meshes the meshes
 */
public record PBakedBone(String name,
                         Vector3f basePosition,
                         Quaternionf baseRotation,
                         List<PBakedBone> children,
                         @Nullable PBakedBone parent,
                         List<PBakedMesh> meshes)
{
	/**
	 * Submits this bone's meshes and child bones using the supplied animation pose and material resolution.
	 *
	 * @param poseStack the current model-to-render pose stack
	 * @param model the baked model supplying indexed bones and animations
	 * @param pose the indexed bone pose
	 * @param renderType the render type
	 * @param color the packed ARGB vertex color
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 */
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

	/**
	 * Submits this bone's meshes and child bones using the supplied animation pose and material resolution.
	 *
	 * @param poseStack the current model-to-render pose stack
	 * @param model the baked model supplying indexed bones and animations
	 * @param pose the indexed bone pose
	 * @param resolver the resolver
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 */
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

	/**
	 * Submits this bone's meshes and child bones using the supplied animation pose and material resolution.
	 *
	 * @param <T> the animatable owner type
	 * @param poseStack the current model-to-render pose stack
	 * @param modelData the canonical model and loader selection
	 * @param controllers the animation controllers in pose composition order
	 * @param renderType the render type
	 * @param color the packed ARGB vertex color
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 */
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

	/**
	 * Submits this bone's meshes and child bones using the supplied animation pose and material resolution.
	 *
	 * @param <T> the animatable owner type
	 * @param poseStack the current model-to-render pose stack
	 * @param modelData the canonical model and loader selection
	 * @param controllers the animation controllers in pose composition order
	 * @param molangContexts the frame-specific Molang context of each controller
	 * @param renderType the render type
	 * @param color the packed ARGB vertex color
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 */
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
	
	/**
	 * Submits this bone's meshes and child bones using the supplied animation pose and material resolution.
	 *
	 * @param <T> the animatable owner type
	 * @param poseStack the current model-to-render pose stack
	 * @param modelData the canonical model and loader selection
	 * @param controllers the animation controllers in pose composition order
	 * @param resolver the resolver
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 */
	public <T extends PAnimatable<T>> void instantDraw(PoseStack poseStack,
	                                                   PModelData modelData,
	                                                   Collection<PAnimationController<T>> controllers,
	                                                   PMeshRenderResolver resolver,
	                                                   PMeshRenderContext inherited,
	                                                   float partialTick)
	{
		instantDraw(poseStack, modelData, controllers, Map.of(), resolver, inherited, partialTick);
	}

	/**
	 * Submits this bone's meshes and child bones using the supplied animation pose and material resolution.
	 *
	 * @param <T> the animatable owner type
	 * @param poseStack the current model-to-render pose stack
	 * @param modelData the canonical model and loader selection
	 * @param controllers the animation controllers in pose composition order
	 * @param molangContexts the frame-specific Molang context of each controller
	 * @param resolver the resolver
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 */
	public <T extends PAnimatable<T>> void instantDraw(PoseStack poseStack,
	                                                   PModelData modelData,
	                                                   Collection<PAnimationController<T>> controllers,
	                                                   Map<PAnimationController<T>, MolangParser.Context> molangContexts,
	                                                   PMeshRenderResolver resolver,
	                                                   PMeshRenderContext inherited,
	                                                   float partialTick)
	{
		PAnimationPoseResolver<T> poseResolver = new PAnimationPoseResolver<>(
				modelData.getModel(),
				controllers,
				(controller, tick) ->
				{
					MolangParser.Context context = molangContexts.get(controller);
					return context == null ? PAnimationPoseResolver.<T>defaultContexts().context(controller, tick) : context;
				},
				partialTick);
		instantDraw(poseStack, poseResolver, resolver, inherited);
	}

	/**
	 * Renders this subtree from one resolved animation pose.
	 *
	 * @param poseStack the current model-to-render pose stack
	 * @param poseResolver the pose resolver
	 * @param resolver the resolver
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 */
	public void instantDraw(PoseStack poseStack,
	                        PAnimationPoseResolver<?> poseResolver,
	                        PMeshRenderResolver resolver,
	                        PMeshRenderContext inherited)
	{
		if (!poseResolver.isVisible(this))
			return;
		PAnimationPoseResolver.BonePose resolved = poseResolver.resolve(this);
		if (resolved == null)
			return;
		BoneFrame frame = resolved.localTransform();
		poseStack.pushPose();
		poseStack.translate(frame.translation().x(), frame.translation().y(), frame.translation().z());
		poseStack.mulPose(frame.rotation());
		poseStack.scale(frame.scale().x(), frame.scale().y(), frame.scale().z());
		Matrix4f matrix4fstack = new Matrix4f(RenderSystem.getModelViewMatrix());
		matrix4fstack.mul(poseStack.last().pose());
		this.meshes().forEach(mesh -> drawMesh(mesh, this, resolver, inherited, poseStack, matrix4fstack));
		this.children().forEach(child -> child.instantDraw(poseStack, poseResolver, resolver, inherited));
		poseStack.popPose();
	}
	
	/**
	 * Samples the ordered controller contributions for this bone.
	 *
	 * @param <T> the animatable owner type
	 * @param model the baked model supplying indexed bones and animations
	 * @param controllers the animation controllers in pose composition order
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the accumulated controller transform for this bone, or {@code null} when no animation channels contribute to the bone
	 */
	public <T extends PAnimatable<T>>@Nullable BoneFrame mixBone(
			PBakedModel model,
			Collection<PAnimationController<T>> controllers, float partialTick)
	{
		return mixBone(model, controllers, Map.of(), partialTick);
	}

	/**
	 * Samples the ordered controller contributions for this bone.
	 *
	 * @param <T> the animatable owner type
	 * @param model the baked model supplying indexed bones and animations
	 * @param controllers the animation controllers in pose composition order
	 * @param molangContexts the frame-specific Molang context of each controller
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the accumulated controller transform for this bone, or {@code null} when no animation channels contribute to the bone
	 */
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
	
	/**
	 * Mutable hierarchy node used while converting source bones into baked render bones.
	 */
	public static final class PBakedBoneBuilder
	{
		/**
		 * Source-model identity retained while linking the hierarchy.
		 */
		public final UUID uuid;
		/**
		 * Bone name used by animation tracks and attachment lookup.
		 */
		public final String name;
		/**
		 * Unanimated local bone position.
		 */
		public final Vector3f basePosition;
		/**
		 * Unanimated local bone rotation.
		 */
		public final Quaternionf baseRotation;
		
		/**
		 * Parent builder node, or {@code null} for a root bone.
		 */
		public PBakedBoneBuilder parent;
		/**
		 * Child builder nodes linked below this bone.
		 */
		public final List<PBakedBoneBuilder> children = new ArrayList<>();
		/**
		 * Render-ready meshes attached to this bone.
		 */
		public final List<PBakedMesh> meshes = new ArrayList<>();
		
		/**
		 * Creates a mutable baked-bone hierarchy node with the supplied bind transform.
		 *
		 * @param uuid the stable source-model bone identity
		 * @param name the name
		 * @param basePosition the unanimated local bone position
		 * @param baseRotation the unanimated local bone rotation
		 */
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
