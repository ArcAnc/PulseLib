/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;

import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PModelPartSnapshotTest
{
	@Test
	void nestedFailureRestoresOuterThenCallerIncludingChildren()
	{
		var child = new ModelPart(List.of(), Map.of());
		var root = new ModelPart(List.of(), Map.of("overlay", child));
		root.x = 4;
		child.zRot = 0.3f;
		child.xScale = 2;
		try (var outer = PModelPartSnapshot.capture(root))
		{
			root.x = 11;
			child.zRot = 0.7f;
			child.xScale = 3;
			child.visible = false;
			assertThrows(IllegalStateException.class, () ->
			{
				try (var inner = PModelPartSnapshot.capture(root))
				{
					root.x = -1;
					child.resetPose();
					child.visible = true;
					child.skipDraw = true;
					throw new IllegalStateException("nested renderer");
				}
			});
			assertEquals(11, root.x);
			assertEquals(0.7f, child.zRot);
			assertEquals(3, child.xScale);
			assertFalse(child.visible);
			assertFalse(child.skipDraw);
		}
		assertEquals(4, root.x);
		assertEquals(0.3f, child.zRot);
		assertEquals(2, child.xScale);
		assertTrue(child.visible);
		assertFalse(child.skipDraw);
	}
}
