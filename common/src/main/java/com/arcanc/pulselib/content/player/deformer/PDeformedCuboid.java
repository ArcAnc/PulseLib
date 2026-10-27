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
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Provides support for deformed cuboid.
 */
public final class PDeformedCuboid extends ModelPart.Cube
{
	private static final float MODEL_SCALE = 1.0f / 16.0f;
	private final List<Face> faces;
	private volatile Binding binding = Binding.IDENTITY;

	/**
	 * Creates an instance of the enclosing type.
	 * @param texCoordU the tex coord u to use.
	 * @param texCoordV the tex coord v to use.
	 * @param originX the origin x to use.
	 * @param originY the origin y to use.
	 * @param originZ the origin z to use.
	 * @param dimensionX the dimension x to use.
	 * @param dimensionY the dimension y to use.
	 * @param dimensionZ the dimension z to use.
	 * @param growX the grow x to use.
	 * @param growY the grow y to use.
	 * @param growZ the grow z to use.
	 * @param mirror the mirror to use.
	 * @param texWidth the tex width to use.
	 * @param texHeight the tex height to use.
	 * @param visibleFaces the visible faces to use.
	 */
	public PDeformedCuboid(int texCoordU, int texCoordV, float originX, float originY, float originZ,
						float dimensionX, float dimensionY, float dimensionZ, float growX, float growY, float growZ,
						boolean mirror, float texWidth, float texHeight, Set<Direction> visibleFaces)
	{
		super(texCoordU, texCoordV, originX, originY, originZ, dimensionX, dimensionY, dimensionZ,
				growX, growY, growZ, mirror, texWidth, texHeight, visibleFaces);
		this.faces = buildFaces(texCoordU, texCoordV, originX, originY, originZ, dimensionX, dimensionY, dimensionZ,
				growX, growY, growZ, mirror, texWidth, texHeight, visibleFaces);
	}

	/**
	 * Sets the deformer.
	 * @param deformer the deformer to use.
	 */
	void setDeformer(PPlayerVertexDeformer deformer)
	{
		this.binding = deformer == PPlayerVertexDeformer.IDENTITY ? Binding.IDENTITY : new Binding(deformer, null);
	}

	PPlayerVertexDeformer getDeformer()
	{
		return this.binding.deformer;
	}

	Binding getBinding()
	{
		return this.binding;
	}

	void setBinding(Binding binding)
	{
		this.binding = binding;
	}

	/** Samples every emitted local quad, including vertex-dependent custom operations, without retaining their callbacks. */
	Binding freezeBinding()
	{
		Binding active = this.binding;
		if (active.quads != null || active.deformer == PPlayerVertexDeformer.IDENTITY)
			return active;
		List<Quad> quads = new ArrayList<>();
		for (Face face : this.faces)
			face.tessellate(active.deformer, quad -> quads.add(quad.freeze()));
		return new Binding(PPlayerVertexDeformer.IDENTITY, List.copyOf(quads));
	}

	/**
	 * Performs the compile operation.
	 * @param pose the pose to use.
	 * @param consumer the consumer to use.
	 * @param packedLight the packed light to use.
	 * @param packedOverlay the packed overlay to use.
	 * @param color the color to use.
	 */
	@Override
	public void compile(PoseStack.Pose pose, VertexConsumer consumer, int packedLight, int packedOverlay, int color)
	{
		Binding active = this.binding;
		if (active.quads != null)
		{
			for (Quad quad : active.quads)
				quad.render(pose, consumer, packedLight, packedOverlay, color);
			return;
		}
		if (active.deformer == PPlayerVertexDeformer.IDENTITY)
		{
			super.compile(pose, consumer, packedLight, packedOverlay, color);
			return;
		}
		for (Face face : this.faces)
			face.render(pose, consumer, packedLight, packedOverlay, color, active.deformer);
	}

	/** Live invocation bindings and immutable submitted geometry are restored by the same scope. */
	record Binding(PPlayerVertexDeformer deformer, @Nullable List<Quad> quads)
	{
		private static final Binding IDENTITY = new Binding(PPlayerVertexDeformer.IDENTITY, null);
	}

	/** All vectors are created by tessellation, remain private and are never modified during replay. */
	private record Quad(Vector3f p00, Vector3f p10, Vector3f p11, Vector3f p01, Vector3f normal,
			float u0, float v0, float u1, float v1)
	{
		private Quad freeze()
		{
			// A custom operation may keep its input vectors; submitted vertices must not alias them.
			return new Quad(new Vector3f(this.p00), new Vector3f(this.p10), new Vector3f(this.p11), new Vector3f(this.p01),
					new Vector3f(this.normal), this.u0, this.v0, this.u1, this.v1);
		}

		private void render(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, int color)
		{
			Vector3f transformedNormal = new Vector3f(this.normal).mul(pose.normal());
			Face.emit(pose, consumer, this.p00, color, this.u0, this.v0, overlay, light, transformedNormal);
			Face.emit(pose, consumer, this.p10, color, this.u1, this.v0, overlay, light, transformedNormal);
			Face.emit(pose, consumer, this.p11, color, this.u1, this.v1, overlay, light, transformedNormal);
			Face.emit(pose, consumer, this.p01, color, this.u0, this.v1, overlay, light, transformedNormal);
		}
	}

	/**
	 * Builds the faces.
	 * @param u the u to use.
	 * @param v the v to use.
	 * @param x the x to use.
	 * @param y the y to use.
	 * @param z the z to use.
	 * @param dx the dx to use.
	 * @param dy the dy to use.
	 * @param dz the dz to use.
	 * @param growX the grow x to use.
	 * @param growY the grow y to use.
	 * @param growZ the grow z to use.
	 * @param mirror the mirror to use.
	 * @param textureWidth the texture width to use.
	 * @param textureHeight the texture height to use.
	 * @param visible the visible to use.
	 * @return the value produced by this operation.
	 */
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

/**
 * Immutable value object representing face.
 */
	private record Face(Vector3f p00, Vector3f p10, Vector3f p11, Vector3f p01,
						float u0, float v0, float u1, float v1, float textureWidth, float textureHeight)
	{
		/**
		 * Performs the render operation.
		 * @param pose the pose to use.
		 * @param consumer the consumer to use.
		 * @param light the light to use.
		 * @param overlay the overlay to use.
		 * @param color the color to use.
		 * @param deformer the deformer to use.
		 */
		void render(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, int color, PPlayerVertexDeformer deformer)
		{
			this.tessellate(deformer, quad -> quad.render(pose, consumer, light, overlay, color));
		}

		private void tessellate(PPlayerVertexDeformer deformer, Consumer<Quad> output)
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
					Vector3f normal = new Vector3f(q10).sub(q00).cross(new Vector3f(q01).sub(q00)).normalize();
					output.accept(new Quad(q00, q10, q11, q01, normal,
							lerp(this.u0, this.u1, x0) / this.textureWidth, lerp(this.v0, this.v1, y0) / this.textureHeight,
							lerp(this.u0, this.u1, x1) / this.textureWidth, lerp(this.v0, this.v1, y1) / this.textureHeight));
				}
			}
		}

		/**
		 * Performs the interpolate operation.
		 * @param u the u to use.
		 * @param v the v to use.
		 * @return the value produced by this operation.
		 */
		private Vector3f interpolate(float u, float v)
		{
			Vector3f bottom = new Vector3f(this.p00).lerp(this.p10, u);
			return bottom.lerp(new Vector3f(this.p01).lerp(this.p11, u), v);
		}

		/**
		 * Performs the segments operation.
		 * @param length the length to use.
		 * @return the value produced by this operation.
		 */
		private static int segments(float length)
		{
			return Math.clamp((int)Math.ceil(length / 2.0f), 1, 8);
		}

		/**
		 * Performs the lerp operation.
		 * @param a the a to use.
		 * @param b the b to use.
		 * @param amount the amount to use.
		 * @return the value produced by this operation.
		 */
		private static float lerp(float a, float b, float amount)
		{
			return a + (b - a) * amount;
		}

		/**
		 * Performs the emit operation.
		 * @param pose the pose to use.
		 * @param consumer the consumer to use.
		 * @param position the position to use.
		 * @param color the color to use.
		 * @param u the u to use.
		 * @param v the v to use.
		 * @param overlay the overlay to use.
		 * @param light the light to use.
		 * @param normal the normal to use.
		 */
		private static void emit(PoseStack.Pose pose, VertexConsumer consumer, Vector3f position, int color, float u, float v,
								 int overlay, int light, Vector3f normal)
		{
			Vector4f transformed = new Vector4f(position.x * MODEL_SCALE, position.y * MODEL_SCALE, position.z * MODEL_SCALE, 1.0f).mul(pose.pose());
			consumer.addVertex(transformed.x, transformed.y, transformed.z, color, u, v, overlay, light, normal.x, normal.y, normal.z);
		}
	}
}
