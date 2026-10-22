/**
 * @author ArcAnc
 * Created at: 22.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.plan;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects command addressing, resource identity and portable frame/geometry ownership. */
class PRenderPlanContractTest
{
	@Test
	void packingRebasesOnlyInstanceRangesAndPreservesGeometryCommands()
	{
		var pipeline = new PPipelineHandle(1);
		var mesh = new PMeshHandle(2);
		var first = instance(3);
		var second = instance(7);
		var groups = new ArrayList<>(List.of(
				new PDrawGroup(pipeline, mesh, new PDrawCommand(6, 2, 5, -2, 99), true, List.of(first, second)),
				new PDrawGroup(pipeline, mesh, new PDrawCommand(3, 1, 11, 4, 99), false, List.of(second))));
		var plan = new PRenderPlan(groups);
		groups.clear();
		assertEquals(List.of(first, second, second), plan.instances());
		assertEquals(new PDrawCommand(6, 2, 5, -2, 0), plan.groups().getFirst().command());
		assertEquals(new PDrawCommand(3, 1, 11, 4, 2), plan.groups().getLast().command());
		assertTrue(plan.groups().getFirst().writeDepth());
		assertFalse(plan.groups().getLast().writeDepth());
		assertThrows(UnsupportedOperationException.class, () -> plan.instances().clear());
		assertThrows(UnsupportedOperationException.class, () -> plan.groups().clear());
	}

	@Test
	void malformedCommandsCannotDescribeNegativeRangesOrMismatchInstances()
	{
		assertThrows(IllegalArgumentException.class, () -> new PDrawCommand(-1, 1, 0, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> new PDrawCommand(3, -1, 0, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> new PDrawCommand(3, 1, -1, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> new PDrawCommand(3, 1, 0, 0, -1));
		assertThrows(IllegalArgumentException.class, () -> new PDrawGroup(new PPipelineHandle(1), new PMeshHandle(2),
				new PDrawCommand(3, 2, 0, 0, 0), true, List.of(instance(0))));
	}

	@Test
	void equalValuedHandlesFromDifferentOwnersDoNotBatchTogether()
	{
		var compiler = new PFrameCompiler<String>();
		var firstPipeline = new PPipelineHandle(1);
		var otherPipeline = new PPipelineHandle(1);
		var firstMesh = new PMeshHandle(2);
		var otherMesh = new PMeshHandle(2);
		assertEquals(firstPipeline, otherPipeline);
		assertNotSame(firstPipeline, otherPipeline);
		compiler.submit("world", firstPipeline, firstMesh, 3, instance(1), false);
		compiler.submit("world", otherPipeline, firstMesh, 3, instance(2), false);
		compiler.submit("world", firstPipeline, otherMesh, 3, instance(3), false);
		assertEquals(3, compiler.compile("world").groups().size());
	}

	@Test
	void differentSubmeshCommandsRetainTheirOffsetsInsteadOfMerging()
	{
		var compiler = new PFrameCompiler<String>();
		var pipeline = new PPipelineHandle(1);
		var mesh = new PMeshHandle(2);
		var command = new PDrawCommand(3, 1, 6, 2, 0);
		compiler.submit("world", pipeline, mesh, command, instance(1), false);
		compiler.submit("world", pipeline, mesh, command, instance(2), false);
		compiler.submit("world", pipeline, mesh, new PDrawCommand(3, 1, 9, 4, 0), instance(3), false);
		var plan = compiler.compile("world");
		assertEquals(2, plan.groups().size());
		assertEquals(3, plan.instances().size());
		var repeated = plan.groups().stream().filter(group -> group.command().firstIndex() == 6).findFirst().orElseThrow();
		assertEquals(2, repeated.command().instanceCount());
		assertEquals(2, repeated.command().baseVertex());
		assertSame(pipeline, repeated.pipeline());
		assertSame(mesh, repeated.mesh());
	}

	@Test
	void ordinaryTransparentOrderRemainsInterleavedWhileOitBatchesSeparately()
	{
		var compiler = new PFrameCompiler<String>();
		var sorted = new PPipelineHandle(1);
		var oit = new PPipelineHandle(2);
		var mesh = new PMeshHandle(3);
		compiler.submit("entity", sorted, mesh, 3, instance(2), true);
		compiler.submit("block", oit, mesh, 3, instance(3), true);
		compiler.submit("block", sorted, mesh, 3, instance(9), true);
		compiler.submit("entity", oit, mesh, 3, instance(7), true);
		var plan = compiler.compileCombined(List.of("entity", "block"), pipeline -> pipeline == oit);
		assertEquals(2, plan.groups().size());
		assertEquals(2, plan.groups().getFirst().command().instanceCount());
		assertSame(oit, plan.groups().getFirst().pipeline());
		assertEquals(List.of(9f, 2f), plan.groups().getLast().instances().stream().map(value -> value.transform().m32()).toList());
		assertTrue(compiler.isEmpty());
	}

	@Test
	void geometryOwnsItsBytesAndRejectsShortIndices()
	{
		ByteBuffer vertices = ByteBuffer.wrap(new byte[] {1, 2, 3, 4});
		ByteBuffer indices = ByteBuffer.wrap(new byte[] {0, 1});
		var geometry = new PGeometryData(vertices, indices, 4, 1, PGeometryData.IndexType.UNSIGNED_SHORT);
		vertices.put(0, (byte)9);
		indices.put(0, (byte)9);
		assertEquals(1, geometry.vertices().get(0));
		assertEquals(0, geometry.indices().get(0));
		assertTrue(geometry.vertices().isReadOnly());
		assertTrue(geometry.indices().isReadOnly());
		assertEquals(0, vertices.position());
		assertThrows(IllegalArgumentException.class, () -> new PGeometryData(vertices, indices, 4, 2, PGeometryData.IndexType.UNSIGNED_SHORT));
	}

	@Test
	void capturedFrameAndInstanceDetachFromMutableCallerMatrices()
	{
		var view = new Matrix4f().translation(1, 2, 3);
		var projection = new Matrix4f().perspective(1, 1.5f, 0.05f, 1000);
		var frame = new PRenderFrame(view, projection);
		var instance = new PInstanceHeader(view, -1, 0, 0, 5, 6, 7);
		view.identity();
		projection.identity();
		assertEquals(3f, frame.modelView().m32());
		assertEquals(0f, frame.projection().m33());
		assertEquals(3f, instance.transform().m32());
		assertEquals(5, instance.auxiliaryOperationOffset());
		assertEquals(6, instance.auxiliaryValueOffset());
		assertEquals(7, instance.auxiliaryOperationCount());
	}

	@Test
	void capabilitySelectionRequiresEveryFeatureUsedByTheSelectedStrategy()
	{
		var direct = new PRenderCapabilityMatrix(true, false, true, false, false);
		assertFalse(direct.supportsMultiDrawIndirect());
		assertEquals(PRenderCapabilities.StorageMode.TEXEL_BUFFER, direct.bestPoseStorage());
		assertEquals(PRenderCapabilities.SubmissionMode.DIRECT,
				direct.selectedCapabilities(PRenderCapabilities.StorageMode.CPU, false).submissionMode());
		assertThrows(IllegalArgumentException.class, () -> direct.selectedCapabilities(PRenderCapabilities.StorageMode.SSBO, false));
		assertThrows(IllegalArgumentException.class, () -> direct.selectedCapabilities(PRenderCapabilities.StorageMode.CPU, true));
		var accelerated = new PRenderCapabilityMatrix(true, true, true, true, true);
		var selected = accelerated.selectedCapabilities(PRenderCapabilities.StorageMode.SSBO, true);
		assertEquals(PRenderCapabilities.SubmissionMode.MULTI_DRAW_INDIRECT, selected.submissionMode());
		assertEquals(PRenderCapabilities.UploadMode.PERSISTENT_RING, selected.uploadMode());
		assertEquals(PRenderCapabilities.StorageMode.SSBO, selected.storageMode());
		assertThrows(IllegalArgumentException.class,
				() -> PRenderCapabilityMatrix.NONE.selectedCapabilities(PRenderCapabilities.StorageMode.TEXEL_BUFFER, false));
	}

	private static PInstanceHeader instance(float depth)
	{
		return new PInstanceHeader(new Matrix4f().translation(0, 0, depth), -1, 0, 0);
	}
}
