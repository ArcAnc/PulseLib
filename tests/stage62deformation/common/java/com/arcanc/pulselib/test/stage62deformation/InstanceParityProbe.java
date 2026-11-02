/**
 * @author ArcAnc
 * Created at: 02.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62deformation;

import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.gl.PGlDynamicGeometry;
import com.arcanc.pulselib.content.renderer.gl.PGlGeometryArena;
import com.arcanc.pulselib.content.renderer.gl.PGlRenderHost;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.opengl.GlTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Sends verified frozen native arm vertices through the existing portable geometry/instance pipeline. */
public final class InstanceParityProbe
{
	private static final PRenderQueue.RenderStage STAGE = new PRenderQueue.RenderStage("stage62d2b_instances");
	private static final int[] COLORS = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFFFF00FF, 0xFF00FFFF};
	public static final List<Map<String, Object>> RECORDS = new ArrayList<>();
	private static @Nullable PGeometryData shorts;
	private static @Nullable PGeometryData copy;
	private static @Nullable PGeometryData ints;
	private static @Nullable PGlDynamicGeometry dynamic;
	private static boolean armed;
	private static @Nullable PRenderPlan submittedPlan;

	private InstanceParityProbe()
	{
	}

	public static void observe(PRenderPlan plan)
	{
		if (armed)
			submittedPlan = plan;
	}

	public static void reset()
	{
		if (dynamic != null)
			dynamic.close();
		dynamic = null;
		shorts = copy = ints = null;
	}

	private static PGeometryData geometry(boolean wide, float shift)
	{
		List<Vector3f> input = NativeParityProbe.portable;
		check(input.size() > 0 && input.size() % 4 == 0 && input.size() < 65536, "Native quad vertices fit both portable index widths");
		var sprite = PResourceCache.getTextureAtlas().getSprite(PResourceCache.spriteId(PLibDatabase.rl("stage62d2b_white")));
		check(sprite.contents().name().equals(PResourceCache.spriteId(PLibDatabase.rl("stage62d2b_white"))), "Actual white atlas sprite ready");
		float u = (sprite.getU0() + sprite.getU1()) / 2, v = (sprite.getV0() + sprite.getV1()) / 2;
		int stride = PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize();
		ByteBuffer vertices = ByteBuffer.allocate(input.size() * stride).order(ByteOrder.nativeOrder());
		for (Vector3f point : input)
		{
			int start = vertices.position();
			vertices.putFloat(point.x + shift).putFloat(point.y).putFloat(point.z).putFloat(u).putFloat(v).
					put((byte)0).put((byte)0).put((byte)127);
			vertices.position(start + stride);
		}
		vertices.flip();
		int count = input.size() / 4 * 6;
		ByteBuffer indices = ByteBuffer.allocate(count * (wide ? 4 : 2)).order(ByteOrder.nativeOrder());
		for (int quad = 0; quad < input.size(); quad += 4)
			for (int corner : new int[] {0, 1, 2, 0, 2, 3})
				if (wide)
					indices.putInt(quad + corner);
				else
					indices.putShort((short)(quad + corner));
		indices.flip();
		return new PGeometryData(vertices, indices, stride, count, wide ? PGeometryData.IndexType.UNSIGNED_INT : PGeometryData.IndexType.UNSIGNED_SHORT);
	}

	private static Matrix4f transform(PRenderFrame frame, int index)
	{
		Vector3f min = new Vector3f(Float.POSITIVE_INFINITY), max = new Vector3f(Float.NEGATIVE_INFINITY);
		for (var p : NativeParityProbe.portable)
		{
			min.min(p);
			max.max(p);
		}
		Matrix4f inverse = new Matrix4f(frame.projection()).invert();
		float x = -0.8f + index * 0.32f;
		var center = inverse.transformProject(new Vector3f(x, -0.65f, 0));
		var right = inverse.transformProject(new Vector3f(x + 0.12f, -0.65f, 0)).sub(center);
		var up = inverse.transformProject(new Vector3f(x, -0.48f, 0)).sub(center);
		var local = new Matrix4f().m00(right.x).m01(right.y).m02(right.z).
				m10(up.x).m11(up.y).m12(up.z).m30(center.x).m31(center.y).m32(center.z);
		return new Matrix4f(frame.modelView()).invert().mul(local).
				scale(2 / (max.x - min.x), 2 / (max.y - min.y), 0.0001f).
				translate(-(min.x + max.x) / 2, -(min.y + max.y) / 2, -(min.z + max.z) / 2);
	}

	@SuppressWarnings("unchecked")
	public static void draw(int epoch, int frameNumber)
	{
		if (shorts == null)
		{
			shorts = geometry(false, 0);
			copy = geometry(false, 0);
			ints = geometry(true, 0);
			dynamic = new PGlDynamicGeometry(shorts);
		}
		dynamic.update(geometry(false, frameNumber % 2 * 0.01f));
		var frame = new PGlRenderHost().captureFrame();
		submittedPlan = null;
		armed = true;
		int query = GL15.glGenQueries();
		try
		{
			GL15.glBeginQuery(GL30.GL_PRIMITIVES_GENERATED, query);
			try (var scope = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
			{
				var type = PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(PResourceCache.ATLAS_LOCATION);
				for (int i = 0; i < 6; i++)
				{
					var header = new PInstanceHeader(transform(frame, i), COLORS[i], 0xF000F0, OverlayTexture.NO_OVERLAY);
					if (i < 2)
						PRenderQueue.submit(STAGE, type, shorts, header);
					else if (i == 2)
						PRenderQueue.submit(STAGE, type, copy, header);
					else if (i == 3)
						PRenderQueue.submit(STAGE, type, ints, header);
					else
						PRenderQueue.submit(STAGE, type, dynamic, header);
				}
			}
			finally
			{
				GL15.glEndQuery(GL30.GL_PRIMITIVES_GENERATED);
				armed = false;
			}
			PRenderPlan plan = java.util.Objects.requireNonNull(submittedPlan, "Actual native GPU plan");
			check(plan.instances().size() == 6 && plan.groups().size() == 4, "Six instances batched into four compatible geometry groups");
			int actual = GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT);
			check(actual == shorts.indexCount() / 3 * 6, "All six deformed native arm GPU instances generate primitives: " + actual);
			verifyUploads(plan);
			List<List<Integer>> pixels = pixels();
			for (int i = 0; i < 6; i++)
				for (int channel = 0; channel < 3; channel++)
					check(Math.abs(pixels.get(i).get(channel) - (COLORS[i] >> (16 - channel * 8) & 255)) <= 4, "Independent GPU instance pixel/color " + i + ": " + pixels.get(i));
			check(GL11.glGetError() == GL11.GL_NO_ERROR, "No GL errors after portable instancing");
			RECORDS.add(Map.of("epoch", epoch, "frame", frameNumber, "primitives", actual, "instances", plan.instances().size(), "groups", plan.groups().size(), "rgb", pixels));
		}
		finally
		{
			GL15.glDeleteQueries(query);
		}
	}

	private static List<List<Integer>> pixels()
	{
		var target = Minecraft.getInstance().getMainRenderTarget();
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING), row = GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH);
		int fbo = GL30.glGenFramebuffers();
		try
		{
			GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, ((GlTexture)target.getColorTexture()).glId(), 0);
			List<List<Integer>> result = new ArrayList<>();
			for (int i = 0; i < 6; i++)
			{
				var rgb = BufferUtils.createByteBuffer(4);
				GL11.glReadPixels((int)((0.1f + i * 0.16f) * target.width), (int)(0.175f * target.height), 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, rgb);
				result.add(List.of(Byte.toUnsignedInt(rgb.get(0)), Byte.toUnsignedInt(rgb.get(1)), Byte.toUnsignedInt(rgb.get(2))));
			}
			return result;
		}
		finally
		{
			GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, row);
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
			GL30.glDeleteFramebuffers(fbo);
		}
	}

	private static void verifyUploads(PRenderPlan plan)
	{
		Object executor = staticField(PRenderQueue.class, "EXECUTOR"), arena = field(executor, "geometry");
		Map<?, ?> slices = (Map<?, ?>)field(arena, "geometrySlices");
		check(slices.containsKey(shorts) && slices.containsKey(copy) && slices.containsKey(ints) && slices.get(shorts) != slices.get(copy), "Immutable identities have independent GPU slices");
		verifySlice((PGlGeometryArena.Slice)slices.get(shorts), shorts);
		verifySlice((PGlGeometryArena.Slice)slices.get(copy), copy);
		verifySlice((PGlGeometryArena.Slice)slices.get(ints), ints);
		var slots = (Map<?, ?>)((Map<?, ?>)field(arena, "dynamicPages")).get(dynamic);
		check(slots != null && slots.size() > 0 && slots.size() <= 9, "Dynamic frame slots stay bounded");
		Object allocation = slots.values().stream().filter(value -> field(value, "data") == dynamic.geometry()).findFirst().orElseThrow();
		verifySlice((PGlGeometryArena.Slice)field(allocation, "slice"), dynamic.geometry());
		Object stream = field(executor, "instances");
		boolean persistent = (boolean)field(stream, "persistent");
		long slot = persistent ? ((long)field(field(executor, "frameArena"), "submissionIndex") - 1) % 9 : 0;
		ByteBuffer data = read((int)field(stream, "buffer"), slot * (int)field(stream, "capacity"), plan.instances().size() * 96);
		for (int i = 0; i < plan.instances().size(); i++)
		{
			var expected = plan.instances().get(i);
			int offset = i * 96;
			var matrix = expected.transform();
			float[] values = {matrix.m00(), matrix.m10(), matrix.m20(), matrix.m30(), matrix.m01(), matrix.m11(), matrix.m21(), matrix.m31(), matrix.m02(), matrix.m12(), matrix.m22(), matrix.m32()};
			for (int component = 0; component < values.length; component++)
				check(data.getFloat(offset + component * 4) == values[component], "Actual instance matrix bytes match frozen header");
			check(data.getInt(offset + 88) == 0, "Portable CPU-deformed geometry needs no deferred GPU value callback");
		}
	}

	private static void verifySlice(PGlGeometryArena.Slice slice, PGeometryData data)
	{
		check(data.vertices().equals(read((int)field(slice.page(), "vertexBuffer"), (long)slice.baseVertex() * data.vertexStride(), data.vertices().remaining())), "Native deformed vertex bytes match GPU storage");
		check(data.indices().equals(read((int)field(slice.page(), "indexBuffer"), slice.indexOffset(), data.indexCount() * data.indexType().bytes())), "Both index widths match GPU storage");
	}

	private static ByteBuffer read(int buffer, long offset, int size)
	{
		int previous = GL11.glGetInteger(GL31.GL_COPY_READ_BUFFER);
		ByteBuffer bytes = MemoryUtil.memAlloc(size);
		try
		{
			GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, buffer);
			GL15.glGetBufferSubData(GL31.GL_COPY_READ_BUFFER, offset, bytes);
			ByteBuffer result = ByteBuffer.allocate(size).order(ByteOrder.nativeOrder());
			return result.put(bytes).flip();
		}
		finally
		{
			GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, previous);
			MemoryUtil.memFree(bytes);
		}
	}

	private static Object staticField(Class<?> type, String name)
	{
		try
		{
			var field = type.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(null);
		}
		catch (ReflectiveOperationException exception)
		{
			throw new AssertionError(exception);
		}
	}

	private static Object field(Object owner, String name)
	{
		return NativeParityProbe.field(owner, name);
	}

	private static void check(boolean value, String message)
	{
		if (!value)
			throw new AssertionError("STAGE62D2B instancing: " + message);
	}
}
