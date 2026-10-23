/**
 * @author ArcAnc
 * Created at: 22.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.TestBlockEntityRenderer;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.gl.PGlDynamicGeometry;
import com.arcanc.pulselib.content.renderer.gl.PGlGeometryArena;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.stream.IntStream;

/** Uses the real executor, actual GPU primitive counts and readback to verify portable geometry uploads. */
public final class Stage41bGeometryProbe
{
	private static final PRenderQueue.RenderStage STAGE = new PRenderQueue.RenderStage("stage41b_geometry");
	private static @Nullable PGeometryData shortData;
	private static @Nullable PGeometryData shortCopy;
	private static @Nullable PGeometryData intData;
	private static @Nullable PGlDynamicGeometry dynamic;
	private static int frames;
	private static int pending;
	private static int passes;

	private Stage41bGeometryProbe()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(Stage41bGeometryProbe::render);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage41bGeometryProbe::shutdown);
	}

	public static void tick(int tick)
	{
		if (tick == 100 || tick == 230)
			pending = tick;
	}

	private static void render(RenderLevelStageEvent.AfterOpaqueBlocks event)
	{
		var model = TestBlockEntityRenderer.MODEL_DATA.getModel();
		if (model == null)
			return;
		if (shortData == null)
		{
			PBakedMesh mesh = IntStream.range(0, model.boneCount()).mapToObj(model::bone).flatMap(bone -> bone.meshes().stream()).findFirst().orElseThrow();
			int stride = PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize();
			ByteBuffer vertices = mesh.geometry().vertices();
			ByteBuffer raw = mesh.geometry().indices().order(ByteOrder.nativeOrder());
			ByteBuffer shorts = ByteBuffer.allocate(mesh.geometry().indexCount() * 2).order(ByteOrder.nativeOrder());
			ByteBuffer ints = ByteBuffer.allocate(mesh.geometry().indexCount() * 4).order(ByteOrder.nativeOrder());
			for (int i = 0; i < mesh.geometry().indexCount(); i++)
			{
				int index = mesh.geometry().indexType() == PGeometryData.IndexType.UNSIGNED_SHORT ? Short.toUnsignedInt(raw.getShort()) : raw.getInt();
				check(index <= 65535, "Chosen original fixture must fit 16-bit indices");
				shorts.putShort((short)index);
				ints.putInt(index);
			}
			shorts.flip();
			ints.flip();
			shortData = new PGeometryData(vertices, shorts, stride, mesh.geometry().indexCount(), PGeometryData.IndexType.UNSIGNED_SHORT);
			shortCopy = new PGeometryData(vertices, shorts, stride, mesh.geometry().indexCount(), PGeometryData.IndexType.UNSIGNED_SHORT);
			intData = new PGeometryData(vertices, ints, stride, mesh.geometry().indexCount(), PGeometryData.IndexType.UNSIGNED_INT);
			dynamic = new PGlDynamicGeometry(shortData);
		}
		frames++;
		ByteBuffer moved = ByteBuffer.allocate(shortData.vertices().remaining()).order(ByteOrder.nativeOrder());
		moved.put(shortData.vertices()).flip();
		for (int i = 4; i < moved.limit(); i += shortData.vertexStride())
			moved.putFloat(i, moved.getFloat(i) + (frames % 11) * 0.01f);
		dynamic.update(new PGeometryData(moved, shortData.indices(), shortData.vertexStride(), shortData.indexCount(), shortData.indexType()));
		var camera = event.getLevelRenderState().cameraRenderState.pos;
		var type = PRenderTypes.RenderTypeProvider.trianglesSolid(PResourceCache.ATLAS_LOCATION);
		for (int i = 0; i < 6; i++)
		{
			var header = new PInstanceHeader(new Matrix4f().translation((float)(-3 + i - camera.x), (float)(82 - camera.y), (float)(12 - camera.z)).scale(0.4f), -1, 0xF000F0, 0);
			if (i < 2)
				PRenderQueue.submit(STAGE, type, shortData, header);
			else if (i == 2)
				PRenderQueue.submit(STAGE, type, shortCopy, header);
			else if (i == 3)
				PRenderQueue.submit(STAGE, type, intData, header);
			else
				PRenderQueue.submit(STAGE, type, dynamic, header);
		}
		int query = pending == 0 ? 0 : GL15.glGenQueries();
		try
		{
			if (query != 0)
				GL15.glBeginQuery(GL30.GL_PRIMITIVES_GENERATED, query);
			try
			{
				PRenderQueue.flush(STAGE);
			}
			finally
			{
				if (query != 0)
					GL15.glEndQuery(GL30.GL_PRIMITIVES_GENERATED);
			}
			if (query != 0)
			{
				int primitives = GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT);
				check(primitives == 6 * (shortData.indexCount() / 3), "Both index widths and all six GPU instances must draw");
				verifyUploads();
				PLibDatabase.LOGGER.info("STAGE 4.1b.1 GEOMETRY PASS: tick={}, frames={}, primitives={}, 16/32-bit indices, static/dynamic bytes match GPU, slots<=9", pending, frames, primitives);
				pending = 0;
				passes++;
			}
		}
		finally
		{
			if (query != 0)
				GL15.glDeleteQueries(query);
		}
	}

	private static void verifyUploads()
	{
		Object executor = field(PRenderQueue.class, null, "EXECUTOR");
		Object arena = field(executor.getClass(), executor, "geometry");
		var immutable = (Map<?, ?>)field(arena.getClass(), arena, "geometrySlices");
		check(immutable.containsKey(shortData) && immutable.containsKey(shortCopy) && immutable.containsKey(intData), "All three portable identities must be uploaded alongside baked model geometry");
		check(immutable.get(shortData) != immutable.get(shortCopy), "Equal bytes with distinct identity must retain distinct GPU slices");
		verifySlice((PGlGeometryArena.Slice)immutable.get(shortData), shortData);
		verifySlice((PGlGeometryArena.Slice)immutable.get(shortCopy), shortCopy);
		verifySlice((PGlGeometryArena.Slice)immutable.get(intData), intData);
		var dynamicPages = (Map<?, ?>)field(arena.getClass(), arena, "dynamicPages");
		var slots = (Map<?, ?>)dynamicPages.get(dynamic);
		check(slots.size() > 0 && slots.size() <= 9, "Dynamic allocation count must stay bounded by the existing frame ring");
		Object current = slots.values().stream().filter(allocation -> field(allocation.getClass(), allocation, "data") == dynamic.geometry()).findFirst().orElseThrow();
		verifySlice((PGlGeometryArena.Slice)field(current.getClass(), current, "slice"), dynamic.geometry());
	}

	static void verifySlice(PGlGeometryArena.Slice slice, PGeometryData data)
	{
		int vertices = (int)field(slice.page().getClass(), slice.page(), "vertexBuffer");
		int indices = (int)field(slice.page().getClass(), slice.page(), "indexBuffer");
		check(data.vertices().equals(read(vertices, (long)slice.baseVertex() * data.vertexStride(), data.vertices().remaining())), "Vertex bytes must match actual arena GPU storage");
		check(data.indices().equals(read(indices, slice.indexOffset(), data.indexCount() * data.indexType().bytes())), "Index bytes must match actual arena GPU storage");
	}

	static ByteBuffer read(int buffer, long offset, int size)
	{
		int previous = GL11.glGetInteger(GL31.GL_COPY_READ_BUFFER);
		ByteBuffer bytes = MemoryUtil.memAlloc(size);
		try
		{
			GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, buffer);
			GL15.glGetBufferSubData(GL31.GL_COPY_READ_BUFFER, offset, bytes);
			ByteBuffer copy = ByteBuffer.allocate(size).order(ByteOrder.nativeOrder());
			copy.put(bytes).flip();
			return copy;
		}
		finally
		{
			GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, previous);
			MemoryUtil.memFree(bytes);
		}
	}

	private static void shutdown(GameShuttingDownEvent event)
	{
		check(passes == 2 && frames > 100, "Both real pre/post reload geometry probes must complete");
		Object executor = field(PRenderQueue.class, null, "EXECUTOR");
		Object arena = field(executor.getClass(), executor, "geometry");
		check(((Map<?, ?>)field(arena.getClass(), arena, "dynamicPages")).isEmpty(), "Dynamic GPU pages must be released by normal shutdown");
		check(((Map<?, ?>)field(arena.getClass(), arena, "geometrySlices")).isEmpty(), "Static geometry handles must be released");
		check(field(executor.getClass(), executor, "stateVertices") == null && field(executor.getClass(), executor, "stateIndices") == null, "Pass-state bindings must be released");
		PLibDatabase.LOGGER.info("STAGE 4.1b.1 GEOMETRY SHUTDOWN PASS: static/dynamic pages=0, pass-state buffers released");
	}

	static @Nullable Object field(Class<?> type, @Nullable Object owner, String name)
	{
		try
		{
			var field = type.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(owner);
		}
		catch (ReflectiveOperationException exception)
		{
			throw new AssertionError(exception);
		}
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}
}
