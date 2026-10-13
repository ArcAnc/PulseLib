/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.content.model.animation.PTransform;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonCameraSpace;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonPresentation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PPlayerCameraTest
{
	@Test
	void cameraOnlyRotationDoesNotMoveItsNonzeroBindOrigin() throws ReflectiveOperationException
	{
		PPlayerHierarchyTest.Fixture fixture = new PPlayerHierarchyTest.Fixture();
		Quaternionf turn = new Quaternionf().rotationY((float)Math.PI / 2);
		fixture.pose.set(4, fixture.model.bone(4).basePosition(), turn, new Vector3f(1));
		PTransform camera = fixture.frame().cameraAnchorModelDelta();
		assertNotNull(camera);
		assertVector(new Vector3f(), camera.translation());
		assertRotation(turn, camera.rotation());
	}

	@Test
	void rootRotationMovesTheCameraAlongItsActualArc() throws ReflectiveOperationException
	{
		PPlayerHierarchyTest.Fixture fixture = new PPlayerHierarchyTest.Fixture();
		Quaternionf turn = new Quaternionf().rotationY((float)Math.PI / 2);
		fixture.pose.set(0, new Vector3f(), turn, new Vector3f(1));
		PTransform camera = fixture.frame().cameraAnchorModelDelta();
		Vector3f bindPoint = fixture.model.bone(4).basePosition();
		Vector3f expected = new Matrix4f().rotate(turn).transformPosition(new Vector3f(bindPoint)).sub(bindPoint);
		assertVector(expected, camera.translation());
		assertRotation(turn, camera.rotation());
	}

	@Test
	void translatedRotatedScaledParentsKeepCameraAndItemInOneFrame() throws ReflectiveOperationException
	{
		PPlayerHierarchyTest.Fixture fixture = new PPlayerHierarchyTest.Fixture();
		fixture.pose.set(0, new Vector3f(1, 2, 3), new Quaternionf().rotationY(0.5f), new Vector3f(2, 3, 4));
		fixture.pose.set(4, new Vector3f(0.3f, 1.7f, 0.4f), new Quaternionf().rotationX(0.4f), new Vector3f(1));
		PPlayerAnimationFrame frame = fixture.frame();
		Vector3f bindPoint = fixture.model.bone(4).basePosition();
		assertVector(fixture.cameraMatrix().transformPosition(new Vector3f()).sub(bindPoint), frame.cameraAnchorModelDelta().translation());
		PFirstPersonPresentation presentation = PFirstPersonPresentation.create(frame);
		assertNotNull(presentation);
		Matrix4f itemView = new Matrix4f();
		assertTrue(presentation.anchorMatrix(PPlayerAnimationAnchors.RIGHT_ITEM, itemView));
		PPlayerHierarchyTest.assertMatrix(fixture.product(3), fixture.cameraMatrix().mul(itemView));
	}

	@Test
	void cameraUsesTheSameExplicitRootPivotAsTheVisualPlayer() throws ReflectiveOperationException
	{
		PPlayerHierarchyTest.Fixture fixture = new PPlayerHierarchyTest.Fixture(new Vector3f(0.4f, 0.5f, 0.6f));
		Vector3f pivot = new Vector3f(0.2f, 0.3f, 0.4f);
		Quaternionf turn = new Quaternionf().rotationY((float)Math.PI / 2);
		Vector3f translation = new Vector3f(1, 0, 0);
		fixture.pose.set(0, new Vector3f(fixture.model.bone(0).basePosition()).add(translation), turn, new Vector3f(2, 3, 4));
		PPlayerAnimationDefinition definition = PPlayerAnimationDefinition.builder(fixture.definition.modelData()).
				bind(PPlayerPart.ROOT, "root").
				anchor(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA, "camera").
				rootPivot(pivot).
				build();
		PPlayerAnimationFrame frame = new PPlayerAnimationFrame(definition, fixture.frame().resolver());
		Vector3f bindPoint = fixture.model.bone(4).basePosition();
		Matrix4f visualRoot = new Matrix4f().translate(translation).translate(pivot).rotate(turn).scale(2, 3, 4).
				translate(new Vector3f(pivot).negate());
		assertVector(visualRoot.transformPosition(new Vector3f(bindPoint)).sub(bindPoint), frame.cameraAnchorModelDelta().translation());
		assertRotation(turn, frame.cameraAnchorModelDelta().rotation());
	}

	@Test
	void cameraInheritsEveryIntermediateParent() throws ReflectiveOperationException
	{
		PPlayerHierarchyTest.Fixture fixture = new PPlayerHierarchyTest.Fixture(new Vector3f(), true);
		Quaternionf root = new Quaternionf().rotationY(0.3f);
		Quaternionf body = new Quaternionf().rotationX(0.4f);
		Quaternionf arm = new Quaternionf().rotationZ(0.5f);
		Quaternionf camera = new Quaternionf().rotationY(0.6f);
		fixture.pose.set(0, new Vector3f(0.1f, 0.2f, 0.3f), root, new Vector3f(2, 3, 4));
		fixture.pose.set(1, new Vector3f(0.4f, 0.3f, 0.2f), body, new Vector3f(0.7f, 1.2f, 0.9f));
		fixture.pose.set(2, new Vector3f(0.3f, 0.2f, 0.1f), arm, new Vector3f(1, 0.8f, 1.1f));
		fixture.pose.set(4, fixture.model.bone(4).basePosition(), camera, new Vector3f(1));
		PTransform delta = fixture.frame().cameraAnchorModelDelta();
		assertVector(fixture.cameraMatrix().transformPosition(new Vector3f()).sub(0.7f, 2, 0.3f), delta.translation());
		assertRotation(new Quaternionf(root).mul(body).mul(arm).mul(camera), delta.rotation());
	}

	@Test
	void rotatedBindFramesUseTheVisualRootsReferenceBasis() throws ReflectiveOperationException
	{
		Quaternionf rootBind = new Quaternionf().rotationY(0.4f);
		Quaternionf cameraBind = new Quaternionf().rotationX(0.3f);
		PPlayerHierarchyTest.Fixture fixture = new PPlayerHierarchyTest.Fixture(new Vector3f(0.4f, 0.5f, 0.6f), false, rootBind, cameraBind);
		Quaternionf rootTurn = new Quaternionf().rotationZ(0.2f);
		Quaternionf cameraTurn = new Quaternionf().rotationY(0.5f);
		fixture.pose.set(0, fixture.model.bone(0).basePosition(), new Quaternionf(rootTurn).mul(rootBind), new Vector3f(1));
		fixture.pose.set(4, fixture.model.bone(4).basePosition(), new Quaternionf(cameraTurn).mul(cameraBind), new Vector3f(1));
		PTransform delta = fixture.frame().cameraAnchorModelDelta();
		Vector3f bindPoint = fixture.model.bone(4).basePosition();
		assertVector(new Matrix4f().rotate(rootTurn).transformPosition(new Vector3f(bindPoint)).sub(bindPoint), delta.translation());
		assertRotation(new Quaternionf(rootTurn).mul(cameraTurn), delta.rotation());
	}

	@Test
	void rootlessRigUsesAConstantHeadBindReference() throws ReflectiveOperationException
	{
		PPlayerHierarchyTest.Fixture fixture = new PPlayerHierarchyTest.Fixture(new Vector3f(0.4f, 0.5f, 0.6f), true);
		Matrix4f reference = fixture.product(1).invert();
		Vector3f bindPoint = new Matrix4f(reference).mul(fixture.cameraMatrix()).transformPosition(new Vector3f());
		Quaternionf rootTurn = new Quaternionf().rotationY(0.4f);
		Quaternionf bodyTurn = new Quaternionf().rotationX(0.3f);
		fixture.pose.set(0, new Vector3f(1, 2, 3), rootTurn, new Vector3f(2, 3, 4));
		fixture.pose.set(1, fixture.model.bone(1).basePosition(), bodyTurn, new Vector3f(1));
		PPlayerAnimationDefinition definition = PPlayerAnimationDefinition.builder(fixture.definition.modelData()).
				bind(PPlayerPart.HEAD, "body").
				anchor(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA, "camera").
				build();
		PTransform delta = new PPlayerAnimationFrame(definition, fixture.frame().resolver()).cameraAnchorModelDelta();
		assertVector(reference.mul(fixture.cameraMatrix()).transformPosition(new Vector3f()).sub(bindPoint), delta.translation());
		assertRotation(new Quaternionf(rootTurn).mul(bodyTurn), delta.rotation());
	}

	@Test
	void minecraftYawPitchRollRoundTripPreservesTheAnimatedQuaternion()
	{
		Quaternionf expected = new Quaternionf().rotationYXZ(0.7f, -0.4f, 0.3f);
		Vector3f euler = expected.getEulerAnglesYXZ(new Vector3f());
		Quaternionf actual = PFirstPersonCameraSpace.fromMinecraftAngles(
				180 - (float)Math.toDegrees(euler.y), -(float)Math.toDegrees(euler.x),
				-(float)Math.toDegrees(euler.z), new Quaternionf());
		assertRotation(expected, actual);
	}

	static void assertVector(Vector3f expected, Vector3f actual)
	{
		assertTrue(expected.equals(actual, 0.00002f), () -> "expected " + expected + " actual " + actual);
	}

	static void assertRotation(Quaternionf expected, Quaternionf actual)
	{
		assertTrue(Math.abs(expected.dot(actual)) > 0.99999f, () -> "expected " + expected + " actual " + actual);
	}
}
