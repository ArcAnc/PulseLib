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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies animated player camera motion and event-driven camera shake after vanilla camera setup.
 */
@Mixin(Camera.class)
public abstract class CameraMixin
{
	/**
	 * Updates the vanilla camera position in world coordinates.
	 *
	 * @param pos the pos
	 */
	@Shadow
	protected abstract void setPosition(Vec3 pos);

	@Unique
	private void pulselib$setRotation(float yaw, float pitch, float roll)
	{
		PulseClientServices.CAMERA.setRotation((Camera)(Object)this, yaw, pitch, roll);
	}

	/**
	 * Adds local-player animation camera offsets, rotation and camera shake after vanilla setup.
	 *
	 * @param level the level
	 * @param entity the entity being animated or rendered
	 * @param detached the detached
	 * @param thirdPersonReverse the third person reverse
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param ci the Mixin callback for the intercepted method
	 */
	@Inject(method = "setup", at = @At("TAIL"))
	private void pulselib$followAnimatedHead(BlockGetter level,
	                                          Entity entity,
	                                          boolean detached,
	                                          boolean thirdPersonReverse,
	                                          float partialTick,
	                                          CallbackInfo ci)
	{
		if (detached || entity != Minecraft.getInstance().player || !(entity instanceof LocalPlayer player))
			return;

		PPlayerAnimations.PPlayerCameraPose pose = PPlayerAnimations.cameraPose(player, partialTick);
		float shake = PAnimationCameraShake.sample(partialTick);
		if (pose == null && shake == 0.0f)
			return;
		if (pose == null)
		{
			Camera camera = (Camera)(Object)this;
			this.pulselib$setRotation(camera.getYRot() + shake, camera.getXRot() + shake * 0.5f, 0.0f);
			return;
		}

		Camera camera = (Camera)(Object)this;
		Quaternionf bodyRotation = new Quaternionf().rotationY((float)Math.toRadians(180.0f - Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot)));

		Vector3f positionOffset = PFirstPersonCameraSpace.toMinecraftOffset(pose.positionOffset(player.getEyeHeight())).rotate(bodyRotation);
		this.setPosition(camera.getPosition().add(positionOffset.x, positionOffset.y, positionOffset.z));

		if (!pose.hasRotation())
			return;
		Quaternionf modelRotation = PFirstPersonCameraSpace.toMinecraftRotation(pose.rotation());
		Quaternionf worldRotation = new Quaternionf(bodyRotation).
				mul(modelRotation).
				mul(new Quaternionf(bodyRotation).invert());
		Quaternionf cameraRotation = worldRotation.mul(new Quaternionf(camera.rotation()));
		Vector3f euler = cameraRotation.getEulerAnglesYXZ(new Vector3f());
		this.pulselib$setRotation(
				180.0f - (float)Math.toDegrees(euler.y) + shake,
				-(float)Math.toDegrees(euler.x) + shake * 0.5f,
				-(float)Math.toDegrees(euler.z));
	}
}
