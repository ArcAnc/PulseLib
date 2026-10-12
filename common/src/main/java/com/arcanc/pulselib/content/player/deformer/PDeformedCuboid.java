/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.deformer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PDeformedCuboid extends ModelPart.Cube
{
	private static final float MODEL_SCALE = 1.0f / 16.0f;
	private final List<Face> faces;
	private volatile PPlayerVertexDeformer deformer = PPlayerVertexDeformer.IDENTITY;

	public PDeformedCuboid(int texCoordU, int texCoordV, float originX, float originY, float originZ,
						float dimensionX, float dimensionY, float dimensionZ, float growX, float growY, float growZ,
						boolean mirror, float texWidth, float texHeight, Set<Direction> visibleFaces)
	{
		super(texCoordU, texCoordV, originX, originY, originZ, dimensionX, dimensionY, dimensionZ,
				growX, growY, growZ, mirror, texWidth, texHeight, visibleFaces);
		this.faces = buildFaces(texCoordU, texCoordV, originX, originY, originZ, dimensionX, dimensionY, dimensionZ,
				growX, growY, growZ, mirror, texWidth, texHeight, visibleFaces);
	}

	void setDeformer(PPlayerVertexDeformer deformer)
	{
		this.deformer = deformer;
	}

	@Override
	public void compile(PoseStack.Pose pose, VertexConsumer consumer, int packedLight, int packedOverlay, int color)
	{
		PPlayerVertexDeformer active = this.deformer;
		if (active == PPlayerVertexDeformer.IDENTITY)
		{
			super.compile(pose, consumer, packedLight, packedOverlay, color);
			return;
		}
		for (Face face : this.faces)
			face.render(pose, consumer, packedLight, packedOverlay, color, active);
	}

	private static List<Face> buildFaces(int u, int v, float x, float y, float z, float dx, float dy, float dz,
										  float growX, float growY, float growZ, boolean mirror, float textureWidth, float textureHeight,
										  Set<Direction> visible)
	{
		float x0 = x - growX, y0 = y - growY, z0 = z - growZ;
		float x1 = x + dx + growX, y1 = y + dy + growY, z1 = z + dz + growZ;
		if (mirror)
		{
			float swap = x0;
			x0 = x1;
			x1 = swap;
		}
		Vector3f a = new Vector3f(x0, y0, z0), b = new Vector3f(x1, y0, z0), c = new Vector3f(x1, y1, z0), d = new Vector3f(x0, y1, z0);
		Vector3f e = new Vector3f(x0, y0, z1), f = new Vector3f(x1, y0, z1), g = new Vector3f(x1, y1, z1), h = new Vector3f(x0, y1, z1);
		float u0 = u, u1 = u + dz, u2 = u + dz + dx, u3 = u + dz + dx + dx, u4 = u + dz + dx + dz, u5 = u + dz + dx + dz + dx;
		float v0 = v, v1 = v + dz, v2 = v + dz + dy;
		List<Face> faces = new ArrayList<>(visible.size());
		if (visible.contains(Direction.DOWN)) faces.add(new Face(f, e, a, b, u1, v0, u2, v1, textureWidth, textureHeight));
		if (visible.contains(Direction.UP)) faces.add(new Face(c, d, h, g, u2, v1, u3, v0, textureWidth, textureHeight));
		if (visible.contains(Direction.WEST)) faces.add(new Face(a, e, h, d, u0, v1, u1, v2, textureWidth, textureHeight));
		if (visible.contains(Direction.NORTH)) faces.add(new Face(b, a, d, c, u1, v1, u2, v2, textureWidth, textureHeight));
		if (visible.contains(Direction.EAST)) faces.add(new Face(f, b, c, g, u2, v1, u4, v2, textureWidth, textureHeight));
		if (visible.contains(Direction.SOUTH)) faces.add(new Face(e, f, g, h, u4, v1, u5, v2, textureWidth, textureHeight));
		return faces;
	}

	private record Face(Vector3f p00, Vector3f p10, Vector3f p11, Vector3f p01,
						float u0, float v0, float u1, float v1, float textureWidth, float textureHeight)
	{
		void render(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, int color, PPlayerVertexDeformer deformer)
		{
			int uSegments = segments(this.p00.distance(this.p10));
			int vSegments = segments(this.p00.distance(this.p01));
			for (int y = 0; y < vSegments; y++)
			{
				float y0 = (float)y / vSegments, y1 = (float)(y + 1) / vSegments;
				for (int x = 0; x < uSegments; x++)
				{
					float x0 = (float)x / uSegments, x1 = (float)(x + 1) / uSegments;
					Vector3f q00 = interpolate(x0, y0), q10 = interpolate(x1, y0), q11 = interpolate(x1, y1), q01 = interpolate(x0, y1);
					deformer.deform(q00); deformer.deform(q10); deformer.deform(q11); deformer.deform(q01);
					Vector3f normal = new Vector3f(q10).sub(q00).cross(new Vector3f(q01).sub(q00)).normalize().mul(pose.normal());
					emit(pose, consumer, q00, color, lerp(this.u0, this.u1, x0) / this.textureWidth, lerp(this.v0, this.v1, y0) / this.textureHeight, overlay, light, normal);
					emit(pose, consumer, q10, color, lerp(this.u0, this.u1, x1) / this.textureWidth, lerp(this.v0, this.v1, y0) / this.textureHeight, overlay, light, normal);
					emit(pose, consumer, q11, color, lerp(this.u0, this.u1, x1) / this.textureWidth, lerp(this.v0, this.v1, y1) / this.textureHeight, overlay, light, normal);
					emit(pose, consumer, q01, color, lerp(this.u0, this.u1, x0) / this.textureWidth, lerp(this.v0, this.v1, y1) / this.textureHeight, overlay, light, normal);
				}
			}
		}

		private Vector3f interpolate(float u, float v)
		{
			Vector3f bottom = new Vector3f(this.p00).lerp(this.p10, u);
			return bottom.lerp(new Vector3f(this.p01).lerp(this.p11, u), v);
		}

		private static int segments(float length)
		{
			return Math.clamp((int)Math.ceil(length / 2.0f), 1, 8);
		}

		private static float lerp(float a, float b, float amount)
		{
			return a + (b - a) * amount;
		}

		private static void emit(PoseStack.Pose pose, VertexConsumer consumer, Vector3f position, int color, float u, float v,
								 int overlay, int light, Vector3f normal)
		{
			Vector4f transformed = new Vector4f(position.x * MODEL_SCALE, position.y * MODEL_SCALE, position.z * MODEL_SCALE, 1.0f).mul(pose.pose());
			consumer.addVertex(transformed.x, transformed.y, transformed.z, color, u, v, overlay, light, normal.x, normal.y, normal.z);
		}
	}
}
