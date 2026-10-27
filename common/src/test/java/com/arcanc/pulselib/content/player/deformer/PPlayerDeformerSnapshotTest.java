/**
 * @author ArcAnc
 * Created at: 27.10.2026
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
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PPlayerDeformerSnapshotTest
{
	@Test
	void deferredBindingsSurviveModelReuseAndRepeatedReplay()
	{
		var cube = cube();
		var part = List.of(cube);
		cube.setDeformer(position -> position.add(16, 0, 0));
		var first = PPlayerDeformerSnapshot.captureCubes(part);
		cube.setDeformer(position -> position.add(32, 0, 0));
		var second = PPlayerDeformerSnapshot.captureCubes(part);
		cube.setDeformer(position -> position.add(48, 0, 0));
		try (var caller = PPlayerDeformerSnapshot.captureCubes(part))
		{
			first.restore();
			assertEquals(1, minX(cube), 1.0e-6);
			try (var outer = PPlayerDeformerSnapshot.captureCubes(part))
			{
				second.restore();
				assertEquals(2, minX(cube), 1.0e-6);
			}
			assertEquals(1, minX(cube), 1.0e-6);
		}
		assertEquals(3, minX(cube), 1.0e-6);
		first.restore();
		assertEquals(1, minX(cube), 1.0e-6);
	}

	@Test
	void nestedFailureRestoresParentAndOverlayBindings()
	{
		var body = cube();
		var sleeve = cube();
		var root = List.of(body, sleeve);
		PPlayerVertexDeformer caller = position -> position.add(16, 0, 0);
		PPlayerVertexDeformer outer = position -> position.add(32, 0, 0);
		body.setDeformer(caller);
		sleeve.setDeformer(caller);
		try (var scope = PPlayerDeformerSnapshot.captureCubes(root))
		{
			body.setDeformer(outer);
			sleeve.setDeformer(outer);
			assertThrows(IllegalStateException.class, () ->
			{
				try (var inner = PPlayerDeformerSnapshot.captureCubes(root))
				{
					body.setDeformer(PPlayerVertexDeformer.IDENTITY);
					sleeve.setDeformer(PPlayerVertexDeformer.IDENTITY);
					throw new IllegalStateException("nested cube draw");
				}
			});
			assertSame(outer, body.getDeformer());
			assertSame(outer, sleeve.getDeformer());
			assertEquals(2, minX(sleeve), 1.0e-6);
		}
		assertSame(caller, body.getDeformer());
		assertSame(caller, sleeve.getDeformer());
		assertEquals(1, minX(sleeve), 1.0e-6);
	}

	@Test
	void identitySnapshotClearsLaterBindingAndKeepsVanillaVertices()
	{
		var cube = cube();
		var part = List.of(cube);
		List<Vector3f> vanilla = vertices(cube);
		var identity = PPlayerDeformerSnapshot.captureCubes(part);
		cube.setDeformer(position -> position.add(32, 0, 0));
		identity.restore();
		assertSame(PPlayerVertexDeformer.IDENTITY, cube.getDeformer());
		assertEquals(vanilla, vertices(cube));
	}

	@Test
	void restorationDoesNotReplaceCubeMembershipOrVanillaGeometry()
	{
		var captured = cube();
		var added = cube();
		List<ModelPart.Cube> cubes = new ArrayList<>(List.of(captured));
		var part = cubes;
		var snapshot = PPlayerDeformerSnapshot.captureCubes(part);
		captured.setDeformer(position -> position.add(16, 0, 0));
		PPlayerVertexDeformer later = position -> position.add(32, 0, 0);
		added.setDeformer(later);
		cubes.clear();
		cubes.add(added);
		snapshot.restore();
		assertSame(PPlayerVertexDeformer.IDENTITY, captured.getDeformer());
		assertSame(later, added.getDeformer());
		assertEquals(List.of(added), part);
		assertEquals(2, minX(added), 1.0e-6);
	}

	@Test
	void bindingSnapshotDoesNotPretendToFreezeCallbackValues()
	{
		var cube = cube();
		var part = List.of(cube);
		float[] liveValue = {16};
		cube.setDeformer(position -> position.add(liveValue[0], 0, 0));
		var snapshot = PPlayerDeformerSnapshot.captureCubes(part);
		liveValue[0] = 32;
		cube.setDeformer(PPlayerVertexDeformer.IDENTITY);
		snapshot.restore();
		assertEquals(2, minX(cube), 1.0e-6);
	}

	private static PDeformedCuboid cube()
	{
		return new PDeformedCuboid(0, 0, 0, 0, 0, 2, 2, 2, 0, 0, 0, false, 64, 64, EnumSet.allOf(Direction.class));
	}

	private static double minX(PDeformedCuboid cube)
	{
		List<Vector3f> vertices = vertices(cube);
		assertTrue(vertices.size() >= 24);
		return vertices.stream().mapToDouble(vertex -> vertex.x).min().orElseThrow();
	}

	private static List<Vector3f> vertices(PDeformedCuboid cube)
	{
		List<Vector3f> vertices = new ArrayList<>();
		VertexConsumer consumer = (VertexConsumer)Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(), new Class<?>[] {VertexConsumer.class}, (proxy, method, args) ->
		{
			if (method.getName().equals("addVertex"))
				vertices.add(new Vector3f((float)args[0], (float)args[1], (float)args[2]));
			return method.getReturnType() == VertexConsumer.class ? proxy : null;
		});
		cube.compile(new PoseStack().last(), consumer, 15728880, 0, -1);
		return vertices;
	}

}
