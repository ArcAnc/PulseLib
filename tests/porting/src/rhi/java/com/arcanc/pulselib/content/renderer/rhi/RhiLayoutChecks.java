/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.arcanc.pulselib.content.renderer.gl.PGlDynamicGeometry;
import com.arcanc.pulselib.content.renderer.legacy.GlDynamicGeometry;
import com.arcanc.pulselib.content.renderer.plan.PDynamicGeometry;
import com.arcanc.pulselib.content.renderer.plan.PDrawCommand;
import com.arcanc.pulselib.content.renderer.plan.PDrawGroup;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PGeometrySource;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PMeshHandle;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/** Boundary tests target packed-stream offsets and std140 bytes rather than duplicating draw implementation. */
public final class RhiLayoutChecks
{
	public static void main(String[] args)
	{
		for (int capacity : new int[]{3, 128, 512})
			for (int count : new int[]{0, 1, capacity, capacity + 1, 511, 512, 513, 1025})
				check(capacity, count);
		checkGroupOrder();
		checkGeometrySources();
		expectFailure(() -> new PRhiInstanceLayout(256, 512, 16384));
		var header = new PInstanceHeader(new Matrix4f(), -1, 0, 0);
		var group = new PDrawGroup(new PPipelineHandle(1), new PMeshHandle(1), new PDrawCommand(3, 1, 0, 0, 1), true, List.of(header));
		expectFailure(() -> new PRhiInstanceLayout(256, 128, 16384).pack(new PRenderPlan(List.of(group), List.of(header))));
		System.out.println("PASS std140 bytes, zero/511/512/513/1025 instances, multiple-group order, packed firstInstance, alignment 256 and full-block padding");
	}

	private static void checkGeometrySources()
	{
		for (Class<?> type : List.of(PGlDynamicGeometry.class, GlDynamicGeometry.class, PRhiDynamicGeometry.class))
			assertTrue(List.of(type.getInterfaces()).contains(PDynamicGeometry.class), "Original direct marker interface retained");
		assertTrue(!AutoCloseable.class.isAssignableFrom(GlDynamicGeometry.class) &&
				AutoCloseable.class.isAssignableFrom(PGlDynamicGeometry.class) &&
				AutoCloseable.class.isAssignableFrom(PRhiDynamicGeometry.class), "Canonical AutoCloseable assignability retained");
		var initial = new PGeometryData(ByteBuffer.allocate(0), ByteBuffer.allocate(0), 24, 0, PGeometryData.IndexType.UNSIGNED_INT);
		var facade = new GlDynamicGeometry();
		assertTrue(facade.geometry().vertexStride() == 24 && facade.indexCount() == 0, "Canonical empty facade layout");
		facade.vertexBuffer().update(initial);
		for (PGeometrySource source : List.of(new PGlDynamicGeometry(initial), facade, new PRhiDynamicGeometry(initial)))
		{
			assertTrue(source.geometrySource().geometrySource() == source.geometrySource(), "Non-delegating canonical identity");
			assertTrue(source.geometry() == initial, "Original CPU data identity");
			int[] calls = {0};
			Object replaced = new Object();
			Object removed = new Object();
			source.onClose(replaced, () -> calls[0] += 100);
			source.onClose(replaced, () -> calls[0]++);
			source.onClose(removed, () -> calls[0] += 100);
			source.removeCloseListener(removed);
			source.onClose(new Object(), () ->
			{
				calls[0]++;
				throw new IllegalStateException("Expected first listener failure");
			});
			source.onClose(new Object(), () ->
			{
				calls[0]++;
				throw new IllegalStateException("Expected second listener failure");
			});
			try
			{
				source.close();
				throw new AssertionError("Listener failure must propagate");
			}
			catch (IllegalStateException expected)
			{
				assertTrue(expected.getSuppressed().length == 1, "Both listener failures preserved");
			}
			source.close();
			assertTrue(calls[0] == 3, "Every remaining listener runs exactly once despite failures");
			try
			{
				source.geometry();
				throw new AssertionError("Closed geometry must be rejected");
			}
			catch (IllegalStateException expected)
			{
				assertTrue(calls[0] == 3, "Closed access cannot repeat callbacks");
			}
		}
		System.out.println("PASS canonical/RHI CPU geometry sources, facade layout, listener replacement/removal, all-listener failure propagation and idempotent close");
	}

	private static void checkGroupOrder()
	{
		List<PInstanceHeader> packed = new ArrayList<>();
		for (int i = 0; i < 20; i++)
			packed.add(new PInstanceHeader(new Matrix4f().translation(i, 0, 0), -1, 0, 0));
		var first = new PDrawGroup(new PPipelineHandle(1), new PMeshHandle(1), new PDrawCommand(3, 4, 2, -1, 7),
				true, packed.subList(7, 11));
		var second = new PDrawGroup(new PPipelineHandle(2), new PMeshHandle(2), new PDrawCommand(6, 2, 0, 0, 4),
				true, packed.subList(4, 6));
		var packet = new PRhiInstanceLayout(64, 3, 16384).pack(new PRenderPlan(List.of(first, second), packed));
		int[] expectedFirst = {7, 10, 4};
		int[] expectedCount = {3, 1, 2};
		assertTrue(packet.batches().size() == 3, "Multiple-group chunk count");
		for (int i = 0; i < 3; i++)
		{
			var batch = packet.batches().get(i);
			assertTrue(batch.group() == (i < 2 ? first : second), "Draw groups retain plan order");
			assertTrue(batch.firstInstance() == expectedFirst[i] && batch.instanceCount() == expectedCount[i], "Independent group ranges");
			assertTrue(batch.uniformOffset() % 64 == 0, "Alignment 64");
			assertTrue(packet.bytes().getFloat(batch.uniformOffset() + 48) == expectedFirst[i], "Packed stream has priority over group-local indexing");
		}
	}

	private static void check(int capacity, int count)
	{
		List<PInstanceHeader> packed = new ArrayList<>();
		for (int i = 0; i < count + 7; i++)
			packed.add(new PInstanceHeader(new Matrix4f().translation(i, 2, 3), 0x80402010, 0xF000F0, 0x12345678, i, i + 1, 2));
		var command = new PDrawCommand(6, count, 3, -1, 7);
		var group = new PDrawGroup(new PPipelineHandle(1), new PMeshHandle(1), command, true, packed.subList(7, count + 7));
		var packet = new PRhiInstanceLayout(256, capacity, 65536).pack(new PRenderPlan(List.of(group), packed));
		int seen = 0;
		for (var batch : packet.batches())
		{
			assertTrue(batch.uniformOffset() % 256 == 0 && batch.firstInstance() == seen + 7, "Chunk offsets/order");
			for (int i = 0; i < batch.instanceCount(); i++)
			{
				int offset = batch.uniformOffset() + i * PRhiInstanceLayout.STRIDE;
				assertTrue(packet.bytes().getFloat(offset + 48) == seen + 7, "Column-major translation");
				assertTrue(packet.bytes().getFloat(offset + 64) == 64 / 255f, "ARGB red");
				assertTrue(packet.bytes().getFloat(offset + 76) == 128 / 255f, "ARGB alpha");
				assertTrue(packet.bytes().getFloat(offset + 80) == 15 && packet.bytes().getFloat(offset + 84) == 15, "Light");
				assertTrue(packet.bytes().getFloat(offset + 88) == 0x5678 && packet.bytes().getFloat(offset + 92) == 0x1234, "Overlay");
				assertTrue(packet.bytes().getInt(offset + 96) == seen + 7 && packet.bytes().getInt(offset + 104) == 2, "Deformer fields");
				seen++;
			}
			for (int i = batch.instanceCount() * PRhiInstanceLayout.STRIDE; i < capacity * PRhiInstanceLayout.STRIDE; i++)
				assertTrue(packet.bytes().get(batch.uniformOffset() + i) == 0, "Full block tail initialized");
		}
		assertTrue(seen == count, "No dropped/duplicated instances");
	}

	private static void expectFailure(Runnable action)
	{
		try
		{
			action.run();
		}
		catch (IllegalArgumentException expected)
		{
			return;
		}
		throw new AssertionError("Expected rejected layout/instance range");
	}

	static void assertTrue(boolean value, String message)
	{
		if (!value)
			throw new AssertionError(message);
	}
}
