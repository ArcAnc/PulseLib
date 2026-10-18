/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration;

import com.arcanc.pulselib.content.model.animation.PAnimationChannel;
import com.arcanc.pulselib.content.model.animation.PAnimationChannelType;
import com.arcanc.pulselib.content.model.animation.PAnimationEventType;
import com.arcanc.pulselib.content.model.animation.PAnimationEventTypes;
import com.arcanc.pulselib.content.model.deformer.*;
import com.arcanc.pulselib.util.PLibDatabase;
import org.joml.Quaternionf;
import org.joml.Vector3f;


/**
 * Built-in animation channel, event and mesh deformer registries.
 */
public class PLibRegistration
{
	/**
	 * Registered animation channel types and the built-in transform channels.
	 */
	public static class AnimationChannelReg
	{
		/**
		 * Animation channel type registry indexed by namespaced identifier.
		 */
		public static final PRegistry<PAnimationChannelType<?>> CHANNEL_TYPES = new PRegistry<>();

		/**
		 * Built-in vector translation animation channel.
		 */
		public static final PAnimationChannelType<Vector3f> POSITION = CHANNEL_TYPES.register(
				PLibDatabase.rl("position"), new PAnimationChannel.Vector3fChannelType(PLibDatabase.rl("position"), new Vector3f(), false));
		/**
		 * Built-in quaternion rotation animation channel.
		 */
		public static final PAnimationChannelType<Quaternionf> ROTATION = CHANNEL_TYPES.register(
				PLibDatabase.rl("rotation"), new PAnimationChannel.QuaternionChannelType(PLibDatabase.rl("rotation")));
		/**
		 * Built-in vector scale animation channel.
		 */
		public static final PAnimationChannelType<Vector3f> SCALE = CHANNEL_TYPES.register(
				PLibDatabase.rl("scale"), new PAnimationChannel.Vector3fChannelType(PLibDatabase.rl("scale"), new Vector3f(1f), true));

		private static void init()
		{
		}
	}

	/**
	 * Registered animation event types and built-in event handlers.
	 */
	public static class AnimationEventReg
	{
		/**
		 * Animation event type registry indexed by namespaced identifier.
		 */
		public static final PRegistry<PAnimationEventType<?>> EVENT_TYPES = new PRegistry<>();

		/**
		 * Plays a sound at an animation event locator.
		 */
		public static final PAnimationEventType<PAnimationEventTypes.SoundData> SOUND =
				EVENT_TYPES.register(PAnimationEventTypes.SOUND.id(), PAnimationEventTypes.SOUND);
		/**
		 * Spawns a particle effect at an animation event locator.
		 */
		public static final PAnimationEventType<PAnimationEventTypes.ParticleData> PARTICLE =
				EVENT_TYPES.register(PAnimationEventTypes.PARTICLE.id(), PAnimationEventTypes.PARTICLE);
		/**
		 * Adds a temporary client camera shake.
		 */
		public static final PAnimationEventType<PAnimationEventTypes.CameraShakeData> CAMERA_SHAKE =
				EVENT_TYPES.register(PAnimationEventTypes.CAMERA_SHAKE.id(), PAnimationEventTypes.CAMERA_SHAKE);
		/**
		 * Invokes a registered callback with a resolved animation locator.
		 */
		public static final PAnimationEventType<PAnimationEventTypes.LocatorCallbackData> LOCATOR_CALLBACK =
				EVENT_TYPES.register(PAnimationEventTypes.LOCATOR_CALLBACK.id(), PAnimationEventTypes.LOCATOR_CALLBACK);
		/**
		 * Updates or triggers an animation graph parameter.
		 */
		public static final PAnimationEventType<PAnimationEventTypes.AnimationParameterData> ANIMATION_PARAMETER =
				EVENT_TYPES.register(PAnimationEventTypes.ANIMATION_PARAMETER.id(), PAnimationEventTypes.ANIMATION_PARAMETER);

		private static void init()
		{
		}
	}
	
	/**
	 * Registered mesh deformer types and their built-in implementations.
	 */
	public static class MeshDeformerReg
	{
		/**
		 * Mesh deformer type registry indexed by namespaced identifier.
		 */
		public static final PRegistry<PMeshDeformer<?>> DEFORMERS = new PRegistry<>();

		/**
		 * Built-in bend mesh deformer type.
		 */
		public static final PMeshDeformer<PBendDefinition> BEND = DEFORMERS.register(PBendDeformer.INSTANCE.id(), PBendDeformer.INSTANCE);
		/**
		 * Built-in hinge mesh deformer type.
		 */
		public static final PMeshDeformer<PHingeDefinition> HINGE = DEFORMERS.register(PHingeDeformer.INSTANCE.id(), PHingeDeformer.INSTANCE);
		/**
		 * Built-in twist mesh deformer type.
		 */
		public static final PMeshDeformer<PTwistDefinition> TWIST = DEFORMERS.register(PTwistDeformer.INSTANCE.id(), PTwistDeformer.INSTANCE);
		/**
		 * Built-in stretch mesh deformer type.
		 */
		public static final PMeshDeformer<PStretchDefinition> STRETCH = DEFORMERS.register(PStretchDeformer.INSTANCE.id(), PStretchDeformer.INSTANCE);
		/**
		 * Built-in squash mesh deformer type.
		 */
		public static final PMeshDeformer<PSquashDefinition> SQUASH = DEFORMERS.register(PSquashDeformer.INSTANCE.id(), PSquashDeformer.INSTANCE);
		/**
		 * Built-in taper mesh deformer type.
		 */
		public static final PMeshDeformer<PTaperDefinition> TAPER = DEFORMERS.register(PTaperDeformer.INSTANCE.id(), PTaperDeformer.INSTANCE);
		/**
		 * Built-in wave mesh deformer type.
		 */
		public static final PMeshDeformer<PWaveDefinition> WAVE = DEFORMERS.register(PWaveDeformer.INSTANCE.id(), PWaveDeformer.INSTANCE);

		private static void init()
		{
		}
	}

	/**
	 * Initializes the built-in registries before plugin type registration.
	 */
	public static void init()
	{
		AnimationChannelReg.init();
		AnimationEventReg.init();
		MeshDeformerReg.init();
	}
}
