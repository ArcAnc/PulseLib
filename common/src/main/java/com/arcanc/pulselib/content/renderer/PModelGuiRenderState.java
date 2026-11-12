/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;

import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.mixin.GuiGraphicsExtractorAccessor;
import com.arcanc.pulselib.content.model.animation.PPose;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/** One GUI extraction snapshot; no live controllers or caller-owned matrices survive extraction. */
@ApiStatus.Internal
public record PModelGuiRenderState(PBakedModel model, PPose animationPose, Matrix4f transform,
                                  Function<Identifier, RenderType> renderType, int color, int packedLight, int packedOverlay,
                                  int x0, int y0, int x1, int y1,
                                  @Nullable ScreenRectangle scissorArea, ScreenRectangle bounds) implements PictureInPictureRenderState
{
	/** Extracts an animation pose without issuing GPU work or changing the caller's GUI pose. */
	public static <T extends PAnimatable<T>> void extract(GuiGraphicsExtractor graphics, PModelData modelData,
	                                                     Collection<PAnimationController<T>> controllers,
	                                                     Function<Identifier, RenderType> renderType, int x, int y, Vector3f scale,
	                                                     int color, int light, int overlay, float partialTick)
	{
		PBakedModel model = modelData.getModel();
		if (model == null)
			return;
		var access = (GuiGraphicsExtractorAccessor)graphics;
		var parameters = access.pulselib$parameters(1);
		var guiPose = parameters.pose();
		Matrix4f transform = new Matrix4f().m00(guiPose.m00()).m01(guiPose.m01()).
				m10(guiPose.m10()).m11(guiPose.m11()).m30(guiPose.m20()).m31(guiPose.m21()).
				translate(x, y, 200).scale(scale);
		if (!transform.isFinite())
			throw new IllegalArgumentException("GUI model transform must be finite");
		PPose pose = model.evaluate(controllers, Map.of(), partialTick);
		ScreenRectangle bounds = bounds(model, pose, transform, graphics.guiWidth(), graphics.guiHeight(), parameters.scissor());
		if (bounds != null)
			access.pulselib$getRenderState().addPicturesInPictureState(new PModelGuiRenderState(
					model, pose, transform, renderType, color, light, overlay,
					bounds.left(), bounds.top(), bounds.right(), bounds.bottom(), parameters.scissor(), bounds));
	}

	/** Bounds use the same indexed local pose and exact parent matrices as baked traversal, including shear. */
	private static @Nullable ScreenRectangle bounds(PBakedModel model, PPose pose, Matrix4f root,
	                                               int width, int height, @Nullable ScreenRectangle scissor)
	{
		@Nullable Matrix4f[] transforms = new Matrix4f[model.boneCount()];
		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;
		Vector3f vertex = new Vector3f();
		for (int index = 0; index < model.boneCount(); index++)
		{
			int parent = model.parentIndex(index);
			if (!pose.isVisible(index) || parent >= 0 && transforms[parent] == null)
				continue;
			Matrix4f matrix = new Matrix4f(parent < 0 ? root : transforms[parent]).
					translate(pose.translation(index)).rotate(pose.rotation(index)).scale(pose.scale(index));
			transforms[index] = matrix;
			for (var mesh : model.bone(index).meshes())
			{
				if (mesh.textureReference().isEmpty())
					continue;
				var geometry = mesh.geometry();
				var vertices = geometry.vertices().order(ByteOrder.nativeOrder());
				for (int offset = 0; offset + 12 <= vertices.limit(); offset += geometry.vertexStride())
				{
					matrix.transformPosition(vertex.set(vertices.getFloat(offset), vertices.getFloat(offset + 4), vertices.getFloat(offset + 8)));
					if (!vertex.isFinite())
						throw new IllegalArgumentException("GUI model bounds must be finite");
					minX = Math.min(minX, vertex.x);
					minY = Math.min(minY, vertex.y);
					maxX = Math.max(maxX, vertex.x);
					maxY = Math.max(maxY, vertex.y);
				}
			}
		}
		if (minX >= maxX || minY >= maxY)
			return null;
		int x0 = (int)Math.max(0, Math.floor(minX) - 1);
		int y0 = (int)Math.max(0, Math.floor(minY) - 1);
		int x1 = (int)Math.min(width, Math.ceil(maxX) + 1);
		int y1 = (int)Math.min(height, Math.ceil(maxY) + 1);
		if (x0 >= x1 || y0 >= y1)
			return null;
		ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0);
		return scissor == null ? bounds : bounds.intersection(scissor);
	}

	@Override
	public float scale()
	{
		return 1;
	}
}
