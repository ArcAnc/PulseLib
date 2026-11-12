/**
 * @author ArcAnc
 * Created at: 30.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.animatable.PAnimationCameraShake;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonCameraSpace;
import com.arcanc.pulselib.platform.PulseClientServices;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies PulseLib integration to {@code Camera}.
 */
@Mixin(Camera.class)
public abstract class CameraMixin
{
	/**
	 * Sets the position.
	 * @param pos the pos to use.
	 */
	@Shadow
	protected abstract void setPosition(Vec3 pos);

	/**
	 * Performs the pulselib$follow animated head operation.
	 * @param partialTick the partial tick to use.
	 * @param ci the ci to use.
	 */
	@Inject(method = "alignWithEntity", at = @At("TAIL"))
	private void pulselib$followAnimatedHead(float partialTick, CallbackInfo ci)
	{
		Camera camera = (Camera)(Object)this;
		Entity entity = camera.entity();
		if (camera.isDetached() || entity != Minecraft.getInstance().player || !(entity instanceof LocalPlayer player))
			return;

		PPlayerAnimations.PPlayerCameraPose pose = PPlayerAnimations.cameraPose(player, partialTick);
		float shake = PAnimationCameraShake.sample(partialTick);
		if (pose == null && shake == 0.0f)
			return;
		if (pose == null)
		{
			Vector3f euler = camera.rotation().getEulerAnglesYXZ(new Vector3f());
			PulseClientServices.CAMERA.setRotation((Camera)(Object)this, 180.0f - (float)Math.toDegrees(euler.y) + shake,
												   -(float)Math.toDegrees(euler.x) + shake * 0.5f,
												   -(float)Math.toDegrees(euler.z));
			return;
		}

		Quaternionf bodyRotation = new Quaternionf().rotationY((float)Math.toRadians(180.0f - Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot)));

		Vector3f positionOffset = PFirstPersonCameraSpace.toMinecraftOffset(
															 pose.positionOffset(player.getEyeHeight()))
									  .rotate(bodyRotation);
		this.setPosition(camera.position().add(positionOffset.x, positionOffset.y, positionOffset.z));

		if (pose.hasRotation())
		{
			Quaternionf modelRotation = PFirstPersonCameraSpace.toMinecraftRotation(pose.rotation());
			Quaternionf worldRotation = new Quaternionf(bodyRotation).mul(modelRotation).mul(new Quaternionf(bodyRotation).invert());
			Quaternionf cameraRotation = worldRotation.mul(new Quaternionf(camera.rotation()));
			Vector3f euler = cameraRotation.getEulerAnglesYXZ(new Vector3f());
			PulseClientServices.CAMERA.setRotation((Camera)(Object)this,
												   180.0f - (float)Math.toDegrees(euler.y) + shake,
												   -(float)Math.toDegrees(euler.x) + shake * 0.5f,
												   -(float)Math.toDegrees(euler.z));
		}
	}
}
