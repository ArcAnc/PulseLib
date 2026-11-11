/**
 * @author ArcAnc
 * Created at: 11.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer.gpu;

import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PDeformerValueSource;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.content.model.deformer.PStretchDeformer;
import com.arcanc.pulselib.content.model.deformer.PStretchDefinition;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PFrameStreamsTest
{
	@Test
	void poseOffsetsAndDirtyStateResetAtFrameBoundary()
	{
		var poses = new PPoseStream();
		assertFalse(poses.dirty());
		assertEquals(0, poses.append(1, 2, 3, 4));
		assertEquals(1, poses.append(5, 6, 7, 8));
		assertEquals(List.of(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f), poses.values());
		assertTrue(poses.dirty());
		poses.markUploaded();
		assertFalse(poses.dirty());
		poses.finishFrame();
		assertTrue(poses.dirty());
		assertTrue(poses.values().isEmpty());
		assertEquals(0, poses.append(9, 10, 11, 12));
	}

	@Test
	void frameResetRetainsDefinitionsAndDefinitionResetInvalidatesBothStreams()
	{
		var streams = new PFrameStreams();
		var deformation = deformation();
		var first = streams.deformers().submit(deformation);
		assertTrue(first.enabled());
		assertEquals(PGpuDeformerStack.STRETCH, streams.deformers().operations().getFirst().intValue());
		streams.poses().append(1, 2, 3, 4);
		streams.deformers().markOperationsUploaded();
		streams.finishFrame();
		assertTrue(streams.poses().values().isEmpty());
		assertTrue(streams.deformers().values().isEmpty());
		assertFalse(streams.deformers().operationsDirty());
		var second = streams.deformers().submit(deformation);
		assertEquals(first.operationOffset(), second.operationOffset());
		assertEquals(0, second.valueOffset());
		assertEquals(1.5f, streams.deformers().values().getFirst());
		streams.clearDefinitions();
		assertTrue(streams.deformers().operationsDirty());
		assertTrue(streams.deformers().operations().isEmpty());
		assertTrue(streams.deformers().values().isEmpty());
		assertFalse(streams.deformers().submit(null).enabled());
	}

	@Test
	void canonicalGlobalStreamsAliasTheNativeUploadData()
	{
		PGpuDeformerBuffers.clearDefinitions();
		try
		{
			var streams = PGpuDeformerBuffers.streams();
			assertSame(streams.deformers().operations(), PGpuDeformerBuffers.operations());
			assertSame(streams.deformers().values(), PGpuDeformerBuffers.values());
			assertSame(PGpuDeformerBuffers.NONE, PGpuDeformerBuffers.submit(null));
			assertTrue(PGpuDeformerBuffers.submit(deformation()).enabled());
			streams.poses().append(1, 2, 3, 4);
			PGpuDeformerBuffers.finishFrame();
			assertTrue(streams.poses().values().isEmpty());
			assertTrue(PGpuDeformerBuffers.values().isEmpty());
			assertFalse(PGpuDeformerBuffers.operations().isEmpty());
		}
		finally
		{
			PGpuDeformerBuffers.clearDefinitions();
		}
		assertTrue(PGpuDeformerBuffers.operations().isEmpty());
	}

	private static PMeshDeformation deformation()
	{
		var definition = new PStretchDefinition(new Vector3f(), new Vector3f(1, 0, 0), PChannelReference.constant(1.5f));
		var stack = PDeformerStack.compile(List.of(new PDeformerInstance<>(PStretchDeformer.INSTANCE, definition)));
		return new PMeshDeformation(stack, PDeformerValueSource.DEFAULTS, stack);
	}
}
