/**
 * @author ArcAnc
 * Created at: 26.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.content.model.PMaterial;
import com.arcanc.pulselib.content.model.PMeshPrimitive;
import com.arcanc.pulselib.content.model.animation.PAnimationPoseResolver;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAutomaticMeshAttachments;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PPlayerMeshRootsTest
{
	@Test
	void staticMeshBelowBoundSkeletonInheritsItsAnimation() throws ReflectiveOperationException
	{
		Fixture fixture = new Fixture();
		assertEquals(List.of("socket"), fixture.roots(fixture.builder.build()));
	}

	@Test
	void explicitAncestorSuppressesDuplicateDescendantRendering() throws ReflectiveOperationException
	{
		Fixture fixture = new Fixture();
		assertEquals(List.of("body"), fixture.roots(fixture.builder.meshAttachment("body").meshAttachment("mesh").build()));
	}

	private static final class Fixture
	{
		private final PBakedModel model;
		private final PPlayerAnimationDefinition.Builder builder;

		private Fixture()
		{
			var body = new PBakedBone("body", new Vector3f(), new Quaternionf(), new ArrayList<>(), null, List.of());
			var socket = new PBakedBone("socket", new Vector3f(), new Quaternionf(), new ArrayList<>(), body, List.of());
			var mesh = new PBakedBone("mesh", new Vector3f(), new Quaternionf(), new ArrayList<>(), socket, List.of(mesh()));
			body.children().add(socket);
			socket.children().add(mesh);
			var idle = new PBakedBone("unrelated", new Vector3f(), new Quaternionf(), new ArrayList<>(), null, List.of(mesh()));
			this.model = new PBakedModel(List.of(body, idle), Map.of());
			var data = new PModelData(id("mesh_roots"), id("gecko"))
			{
				@Override
				public PBakedModel getModel()
				{
					return model;
				}
			};
			this.builder = PPlayerAnimationDefinition.builder(data).bind(PPlayerPart.BODY, "body");
		}

		@SuppressWarnings("unchecked")
		private List<String> roots(PPlayerAnimationDefinition definition) throws ReflectiveOperationException
		{
			var resolver = new PAnimationPoseResolver<PPlayerAnimationInstance>(this.model, List.of(), PAnimationPoseResolver.defaultContexts(), 0);
			Field active = PAnimationPoseResolver.class.getDeclaredField("activeBones");
			active.setAccessible(true);
			((Set<String>)active.get(resolver)).add("body");
			return PPlayerAutomaticMeshAttachments.roots(new PPlayerAnimationFrame(definition, resolver)).stream().map(PBakedBone::name).toList();
		}

		private static PBakedMesh mesh()
		{
			var geometry = new PGeometryData(ByteBuffer.allocate(3 * 24), ByteBuffer.allocate(6), 24, 3, PGeometryData.IndexType.UNSIGNED_SHORT);
			var source = new PMeshPrimitive(3, FloatBuffer.allocate(9), FloatBuffer.allocate(9), FloatBuffer.allocate(6), 3, ByteBuffer.allocate(6), 5123, new PMaterial("0"));
			return new PBakedMesh(UUID.randomUUID(), geometry, "0", false, PAlphaMode.OPAQUE, source, id("mesh_roots"));
		}

		private static Identifier id(String path)
		{
			return Identifier.fromNamespaceAndPath("pulselibtest", path);
		}
	}
}
