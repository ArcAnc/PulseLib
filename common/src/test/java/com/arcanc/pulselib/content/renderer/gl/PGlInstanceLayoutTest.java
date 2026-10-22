/**
 * @author ArcAnc
 * Created at: 22.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.gl;

import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PDrawCommand;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Checks the actual bytes consumed by the unchanged instancing shaders. */
class PGlInstanceLayoutTest
{
	@Test
	void commandResolvesArenaOffsetsAndPreservesPackedInstanceRange()
	{
		var command = new PDrawCommand(6, 5, 9, -2, 11);
		assertEquals(new PGlIndirectStream.Command(6, 5, 109, 38, 11),
				PGlMultiDrawExecutor.resolveCommand(command, 100, 40));
		assertThrows(ArithmeticException.class,
				() -> PGlMultiDrawExecutor.resolveCommand(command, Integer.MAX_VALUE, 0));
	}

	@Test
	void canonicalHeaderKeepsAffineRowsColorLightOverlayAndDeformerOffsets()
	{
		var transform = new Matrix4f().translation(2, 3, 4).scale(5, 6, 7);
		var header = new PInstanceHeader(transform, 0x80402010, (11 << 20) | (7 << 4), (9 << 16) | 3, 13, 17, 19);
		var bytes = ByteBuffer.allocate(PGlInstanceStream.STRIDE).order(ByteOrder.nativeOrder());
		PGlInstanceStream.write(bytes, header);
		assertEquals(96, bytes.position());
		assertEquals(5f, bytes.getFloat(0));
		assertEquals(2f, bytes.getFloat(12));
		assertEquals(6f, bytes.getFloat(20));
		assertEquals(3f, bytes.getFloat(28));
		assertEquals(7f, bytes.getFloat(40));
		assertEquals(4f, bytes.getFloat(44));
		assertEquals(64f / 255f, bytes.getFloat(48));
		assertEquals(32f / 255f, bytes.getFloat(52));
		assertEquals(16f / 255f, bytes.getFloat(56));
		assertEquals(128f / 255f, bytes.getFloat(60));
		assertEquals(7f, bytes.getFloat(64));
		assertEquals(11f, bytes.getFloat(68));
		assertEquals(3f, bytes.getFloat(72));
		assertEquals(9f, bytes.getFloat(76));
		assertEquals(13, bytes.getInt(80));
		assertEquals(17, bytes.getInt(84));
		assertEquals(19, bytes.getInt(88));
		assertEquals(0, bytes.getInt(92));
	}
}
