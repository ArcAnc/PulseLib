/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.content.model.animation.PAnimationPoseResolver;
import com.arcanc.pulselib.content.model.animation.PModelPose;
import com.arcanc.pulselib.content.model.animation.PPose;
import com.arcanc.pulselib.content.model.animation.PTransform;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonPresentation;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PPlayerHierarchyTest
{
	@Test
	void rootTranslationIsAppliedOnce() throws ReflectiveOperationException
	{
		Fixture fixture = new Fixture();
		fixture.pose.set(0, new Vector3f(1, 0, 0), new Quaternionf(), new Vector3f(1));
		PPlayerAnimationFrame frame = fixture.frame();
		Matrix4f actual = frame.modelTransform("root").matrix().mul(frame.currentRootRelativeTransform("item").matrix());
		assertMatrix(fixture.product(3), actual);
		assertMatrix(fixture.product(3), frame.actionTransform(PPlayerAnimationAnchors.RIGHT_ITEM).matrix());
	}

	@Test
	void rootRotationAndIntermediateParentsAreInheritedOnce() throws ReflectiveOperationException
	{
		Fixture fixture = new Fixture();
		fixture.pose.set(0, new Vector3f(), new Quaternionf().rotationZ((float)Math.PI / 2), new Vector3f(1));
		fixture.pose.set(1, new Vector3f(0.4f, 0.3f, 0.2f), new Quaternionf().rotationY(0.3f), new Vector3f(1));
		fixture.pose.set(2, new Vector3f(0.2f, 0.5f, 0.1f), new Quaternionf().rotationX(0.4f), new Vector3f(1));
		fixture.pose.set(3, new Vector3f(0.1f, 0.2f, 0.3f), new Quaternionf().rotationZ(0.2f), new Vector3f(1));
		PPlayerAnimationFrame frame = fixture.frame();
		assertMatrix(fixture.product(3), frame.modelTransform("root").matrix().mul(frame.currentRootRelativeTransform("item").matrix()));
		assertMatrix(fixture.product(3), frame.actionTransform(PPlayerAnimationAnchors.RIGHT_ITEM).matrix());
	}

	@Test
	void nonuniformScalePreservesExactHierarchyMatrices() throws ReflectiveOperationException
	{
		Fixture fixture = new Fixture();
		fixture.pose.set(0, new Vector3f(0.3f, 0.4f, 0.5f), new Quaternionf().rotationY(0.5f), new Vector3f(2, 3, 4));
		fixture.pose.set(1, new Vector3f(0.4f, 0.3f, 0.2f), new Quaternionf().rotationZ(0.6f), new Vector3f(0.7f, 1.2f, 0.9f));
		fixture.pose.set(2, new Vector3f(0.2f, 0.5f, 0.1f), new Quaternionf().rotationX(0.4f), new Vector3f(1));
		Matrix4f actual = new Matrix4f();
		assertTrue(fixture.frame().modelMatrix("item", actual));
		assertMatrix(fixture.product(3), actual);
	}

	@Test
	void firstPersonItemAndMeshUseOneCameraFrame() throws ReflectiveOperationException
	{
		Fixture fixture = new Fixture();
		fixture.pose.set(0, new Vector3f(1, 2, 3), new Quaternionf().rotationY(0.5f), new Vector3f(2, 3, 4));
		fixture.pose.set(2, new Vector3f(0.2f, 0.5f, 0.1f), new Quaternionf().rotationX(0.4f), new Vector3f(1));
		PFirstPersonPresentation presentation = PFirstPersonPresentation.create(fixture.frame());
		assertNotNull(presentation);
		Matrix4f actual = new Matrix4f();
		assertTrue(presentation.anchorMatrix(PPlayerAnimationAnchors.RIGHT_ITEM, actual));
		assertMatrix(fixture.cameraMatrix().invert().mul(fixture.product(3)), actual);
		assertTrue(presentation.boneMatrix("item", actual));
		assertMatrix(fixture.cameraMatrix().invert().mul(fixture.product(3)), actual);
	}

	@Test
	void fractionalRootsDoNotLeakIntoChildTransforms() throws ReflectiveOperationException
	{
		for (float weight : new float[]{0, 0.1f, 0.25f, 0.5f, 0.9f, 1})
		{
			Fixture first = new Fixture();
			Fixture second = new Fixture();
			first.pose.set(0, new Vector3f(weight, 0, 0), new Quaternionf().rotationY(weight), new Vector3f(1));
			second.pose.set(0, new Vector3f(0, 1 - weight, 0), new Quaternionf().rotationZ(1 - weight), new Vector3f(1));
			Matrix4f outer = first.product(0).mul(second.product(0));
			assertMatrix(new Matrix4f(outer).mul(first.productWithoutRoot(3)),
					new Matrix4f(outer).mul(first.frame().currentRootRelativeTransform("item").matrix()));
			assertMatrix(new Matrix4f(outer).mul(second.productWithoutRoot(3)),
					new Matrix4f(outer).mul(second.frame().currentRootRelativeTransform("item").matrix()));
		}
	}

	@Test
	void exactRootRelativeMatrixSurvivesScaleRotationAndBindOffset() throws ReflectiveOperationException
	{
		Fixture fixture = new Fixture();
		fixture.pose.set(0, new Vector3f(0.3f, 0.4f, 0.5f), new Quaternionf().rotationY(0.5f), new Vector3f(2, 3, 4));
		fixture.pose.set(1, new Vector3f(0.4f, 0.3f, 0.2f), new Quaternionf().rotationZ(0.6f), new Vector3f(0.7f, 1.2f, 0.9f));
		PPlayerAnimationFrame frame = fixture.frame();
		Matrix4f relative = new Matrix4f();
		assertTrue(frame.currentRootRelativeMatrix("item", relative));
		assertMatrix(fixture.productWithoutRoot(3), relative);
		Vector3f pivot = new Vector3f(0.2f, 0.3f, 0.4f);
		Matrix4f visualRoot = new Matrix4f().translate(fixture.pose.translation(0)).translate(pivot).
				rotate(fixture.pose.rotation(0)).scale(fixture.pose.scale(0)).translate(new Vector3f(pivot).negate());
		assertMatrix(new Matrix4f(visualRoot).mul(fixture.productWithoutRoot(3)), new Matrix4f(visualRoot).mul(relative));
		assertTrue(fixture.product(3).getTranslation(new Vector3f()).equals(frame.rootRelativeTransform("item").translation(), 0.00002f));
	}

	@Test
	void commandOverridesPreserveTheExactUnderlyingAffineMatrix()
	{
		Matrix4f exact = new Matrix4f().scale(2, 3, 4).rotateY(0.4f).translate(0.2f, 0.3f, 0.4f);
		PTransform sampled = PTransform.fromMatrix(exact);
		PTransform edited = new PTransform(sampled.translation().add(1, 0, 0), sampled.rotation(), sampled.scale());
		Matrix4f expected = new Matrix4f().translate(1, 0, 0).mul(exact);
		assertMatrix(expected, PPlayerAnimationSpace.applyChannelOverride(new Matrix4f(exact), edited, sampled));
		assertMatrix(exact, PPlayerAnimationSpace.applyChannelOverride(new Matrix4f(exact), sampled, sampled));
	}

	@Test
	void nonzeroRootBindRemainsAvailableForUnrootedConsumers() throws ReflectiveOperationException
	{
		Vector3f bind = new Vector3f(0.3f, 0.4f, 0.5f);
		Fixture fixture = new Fixture(bind);
		fixture.pose.set(0, new Vector3f(bind).add(1, 0, 0), new Quaternionf().rotationY(0.5f), new Vector3f(1));
		PPlayerAnimationFrame frame = fixture.frame();
		assertMatrix(new Matrix4f().translate(new Vector3f(bind).negate()).mul(fixture.product(3)),
				frame.rootRelativeTransform("item").matrix());
		Matrix4f relative = new Matrix4f();
		assertTrue(frame.currentRootRelativeMatrix("item", relative));
		assertMatrix(fixture.productWithoutRoot(3), relative);
	}

	static void assertMatrix(Matrix4f expected, Matrix4f actual)
	{
		assertTrue(expected.equals(actual, 0.00002f), () -> "expected " + expected + "actual " + actual);
	}

	static final class Fixture
	{
		final PBakedModel model;
		final PPose pose;
		final PPlayerAnimationDefinition definition;
		private final PAnimationPoseResolver<PPlayerAnimationInstance> resolver;

		Fixture()
		{
			this(new Vector3f());
		}

		Fixture(Vector3f rootBind)
		{
			PBakedBone root = bone("root", rootBind, null);
			PBakedBone body = bone("body", new Vector3f(0, 0.5f, 0), root);
			PBakedBone arm = bone("arm", new Vector3f(0.5f, 0, 0), body);
			bone("item", new Vector3f(0, 0.5f, 0), arm);
			bone("camera", new Vector3f(0.2f, 1.5f, 0.3f), root);
			this.model = new PBakedModel(List.of(root), Map.of());
			this.pose = this.model.bindPose();
			PModelData data = new PModelData(ResourceLocation.fromNamespaceAndPath("pulselibtest", "hierarchy"),
					ResourceLocation.fromNamespaceAndPath("pulselib", "gecko"))
			{
				@Override
				public PBakedModel getModel()
				{
					return model;
				}
			};
			this.definition = PPlayerAnimationDefinition.builder(data).
					bind(PPlayerPart.ROOT, "root").
					bind(PPlayerPart.BODY, "body").
					bind(PPlayerPart.RIGHT_ARM, "arm").
					anchor(PPlayerAnimationAnchors.RIGHT_ITEM, "item").
					anchor(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA, "camera").
					build();
			this.resolver = new PAnimationPoseResolver<PPlayerAnimationInstance>(this.model, List.of(), PAnimationPoseResolver.defaultContexts(), 0);
		}

		PPlayerAnimationFrame frame() throws ReflectiveOperationException
		{
			Field local = PAnimationPoseResolver.class.getDeclaredField("localPose");
			local.setAccessible(true);
			PPose destination = (PPose)local.get(this.resolver);
			for (int index = 0; index < this.model.boneCount(); index++)
			{
				destination.setAnimated(index, this.pose.translation(index), this.pose.rotation(index), this.pose.scale(index));
			}
			Field resolved = PAnimationPoseResolver.class.getDeclaredField("modelPose");
			resolved.setAccessible(true);
			((PModelPose)resolved.get(this.resolver)).update(this.model, destination);
			return new PPlayerAnimationFrame(this.definition, this.resolver);
		}

		Matrix4f product(int last)
		{
			return localMatrix(0).mul(productWithoutRoot(last));
		}

		Matrix4f productWithoutRoot(int last)
		{
			Matrix4f matrix = new Matrix4f();
			for (int index = 1; index <= last; index++)
			{
				matrix.mul(localMatrix(index));
			}
			return matrix;
		}

		Matrix4f cameraMatrix()
		{
			return localMatrix(0).mul(localMatrix(4));
		}

		private Matrix4f localMatrix(int index)
		{
			return new Matrix4f().translation(this.pose.translation(index)).rotate(this.pose.rotation(index)).scale(this.pose.scale(index));
		}

		private static PBakedBone bone(String name, Vector3f position, @Nullable PBakedBone parent)
		{
			PBakedBone bone = new PBakedBone(name, position, new Quaternionf(), new ArrayList<>(), parent, List.of());
			if (parent != null)
			{
				parent.children().add(bone);
			}
			return bone;
		}
	}
}
