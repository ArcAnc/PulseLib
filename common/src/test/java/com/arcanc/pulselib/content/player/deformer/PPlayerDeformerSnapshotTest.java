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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

	@Test
	void submittedGeometryStopsCallingLiveValuesAndOperations()
	{
		var cube = cube();
		float[] value = {16};
		int[] calls = {0};
		cube.setDeformer(position ->
		{
			calls[0]++;
			position.add(value[0], position.x < 1 ? 2 : -3, 0);
		});
		List<Vertex> expected = completeVertices(cube, new PoseStack(), -1, 0, 15728880);
		var submitted = PPlayerDeformerSnapshot.captureGeometryCubes(List.of(cube));
		int capturedCalls = calls[0];
		value[0] = 1024;
		cube.setDeformer(position ->
		{
			throw new AssertionError("Later operation must not execute");
		});
		submitted.restore();
		assertEquals(expected, completeVertices(cube, new PoseStack(), -1, 0, 15728880));
		assertEquals(expected, completeVertices(cube, new PoseStack(), -1, 0, 15728880));
		assertEquals(capturedCalls, calls[0]);
		assertSame(PPlayerVertexDeformer.IDENTITY, cube.getDeformer());
	}

	@Test
	void frozenVerticesDoNotAliasVectorsRetainedByCustomOperations()
	{
		var cube = cube();
		List<Vector3f> retained = new ArrayList<>();
		cube.setDeformer(position ->
		{
			position.add(16, 0, 0);
			retained.add(position);
		});
		var snapshot = PPlayerDeformerSnapshot.captureGeometryCubes(List.of(cube));
		retained.forEach(position -> position.set(500));
		snapshot.restore();
		assertEquals(1, minX(cube), 1.0e-6);
	}

	@Test
	void frozenGeometryUsesReplayPoseNormalsUvAndDrawAttributes()
	{
		var cube = cube();
		cube.setDeformer(position -> position.add(position.y * position.y, 2 * position.z, position.x * 0.25f));
		var poses = new PoseStack();
		poses.translate(0.2f, 0.3f, -0.4f);
		poses.scale(-2, 0.5f, 3);
		var expected = completeVertices(cube, poses, 0x12345678, 19, 73);
		var snapshot = PPlayerDeformerSnapshot.captureGeometryCubes(List.of(cube));
		cube.setDeformer(PPlayerVertexDeformer.IDENTITY);
		snapshot.restore();
		assertEquals(expected, completeVertices(cube, poses, 0x12345678, 19, 73));
		var differentLight = completeVertices(cube, poses, -1, 23, 101);
		assertNotEquals(expected, differentLight);
		assertEquals(expected.getFirst().position, differentLight.getFirst().position);
		assertEquals(expected.getFirst().normal, differentLight.getFirst().normal);
		assertEquals(101, differentLight.getFirst().light);
		assertEquals(23, differentLight.getFirst().overlay);
	}

	@Test
	void frozenSnapshotsKeepTwoIndependentOwnersAndNestedCallerBindings()
	{
		var cube = cube();
		var cubes = List.of(cube);
		float[] firstValue = {16};
		float[] secondValue = {32};
		cube.setDeformer(position -> position.add(firstValue[0], 0, 0));
		var first = PPlayerDeformerSnapshot.captureGeometryCubes(cubes);
		cube.setDeformer(position -> position.add(secondValue[0], 0, 0));
		var second = PPlayerDeformerSnapshot.captureGeometryCubes(cubes);
		firstValue[0] = secondValue[0] = 1000;
		PPlayerVertexDeformer liveCaller = position -> position.add(48, 0, 0);
		cube.setDeformer(liveCaller);
		try (var caller = PPlayerDeformerSnapshot.captureCubes(cubes))
		{
			first.restore();
			assertThrows(IllegalStateException.class, () ->
			{
				try (var outer = PPlayerDeformerSnapshot.captureCubes(cubes))
				{
					second.restore();
					assertEquals(2, minX(cube), 1.0e-6);
					throw new IllegalStateException("nested frozen geometry");
				}
			});
			assertEquals(1, minX(cube), 1.0e-6);
		}
		assertSame(liveCaller, cube.getDeformer());
		assertEquals(3, minX(cube), 1.0e-6);
	}

	@Test
	void failedSamplingDoesNotMutateAnyCallerBindingOrOlderSnapshot()
	{
		var first = cube();
		var second = cube();
		PPlayerVertexDeformer firstBinding = position -> position.add(16, 0, 0);
		PPlayerVertexDeformer failedBinding = position ->
		{
			throw new IllegalStateException("custom sampling failure");
		};
		first.setDeformer(firstBinding);
		var older = PPlayerDeformerSnapshot.captureGeometryCubes(List.of(first));
		second.setDeformer(failedBinding);
		assertThrows(IllegalStateException.class, () -> PPlayerDeformerSnapshot.captureGeometryCubes(List.of(first, second)));
		assertSame(firstBinding, first.getDeformer());
		assertSame(failedBinding, second.getDeformer());
		older.restore();
		assertEquals(1, minX(first), 1.0e-6);
	}

	@Test
	void identityAndAlreadyFrozenGeometryAreNotResampled()
	{
		var cube = cube();
		var vanilla = completeVertices(cube, new PoseStack(), -1, 0, 15728880);
		var identity = PPlayerDeformerSnapshot.captureGeometryCubes(List.of(cube));
		cube.setDeformer(position -> position.add(16, 0, 0));
		identity.restore();
		assertEquals(vanilla, completeVertices(cube, new PoseStack(), -1, 0, 15728880));
		cube.setDeformer(position -> position.add(16, 0, 0));
		var first = PPlayerDeformerSnapshot.captureGeometryCubes(List.of(cube));
		first.restore();
		var second = PPlayerDeformerSnapshot.captureGeometryCubes(List.of(cube));
		cube.setDeformer(PPlayerVertexDeformer.IDENTITY);
		second.restore();
		assertEquals(1, minX(cube), 1.0e-6);
	}

	@Test
	void mirrorGrowthVisibleFacesAndSegmentLimitsKeepExactGeometry()
	{
		for (boolean mirror : List.of(false, true))
		{
			var cube = new PDeformedCuboid(3, 7, -2, 1, -1, 19, 3, 5, 0.2f, 0.4f, 0.6f, mirror, 128, 64,
					EnumSet.of(Direction.NORTH, Direction.UP, Direction.EAST));
			cube.setDeformer(position -> position.mul(1.2f, 1.3f, 0.8f));
			var expected = completeVertices(cube, new PoseStack(), -1, 0, 15728880);
			var snapshot = PPlayerDeformerSnapshot.captureGeometryCubes(List.of(cube));
			cube.setDeformer(PPlayerVertexDeformer.IDENTITY);
			snapshot.restore();
			assertEquals(expected, completeVertices(cube, new PoseStack(), -1, 0, 15728880));
		}
	}

	private static List<Vertex> completeVertices(PDeformedCuboid cube, PoseStack poses, int color, int overlay, int light)
	{
		List<Vertex> vertices = new ArrayList<>();
		VertexConsumer consumer = (VertexConsumer)Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(), new Class<?>[] {VertexConsumer.class}, (proxy, method, args) ->
		{
			if (method.getName().equals("addVertex"))
				vertices.add(new Vertex(new Vector3f((float)args[0], (float)args[1], (float)args[2]),
						(int)args[3], (float)args[4], (float)args[5], (int)args[6], (int)args[7],
						new Vector3f((float)args[8], (float)args[9], (float)args[10])));
			return method.getReturnType() == VertexConsumer.class ? proxy : null;
		});
		cube.compile(poses.last(), consumer, light, overlay, color);
		return vertices;
	}

	private record Vertex(Vector3f position, int color, float u, float v, int overlay, int light, Vector3f normal)
	{
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
