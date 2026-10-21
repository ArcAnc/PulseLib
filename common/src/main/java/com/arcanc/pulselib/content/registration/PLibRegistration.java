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
import com.arcanc.pulselib.content.model.deformer.PBendDefinition;
import com.arcanc.pulselib.content.model.deformer.PBendDeformer;
import com.arcanc.pulselib.content.model.deformer.PHingeDefinition;
import com.arcanc.pulselib.content.model.deformer.PHingeDeformer;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformer;
import com.arcanc.pulselib.content.model.deformer.PSquashDefinition;
import com.arcanc.pulselib.content.model.deformer.PSquashDeformer;
import com.arcanc.pulselib.content.model.deformer.PStretchDefinition;
import com.arcanc.pulselib.content.model.deformer.PStretchDeformer;
import com.arcanc.pulselib.content.model.deformer.PTaperDefinition;
import com.arcanc.pulselib.content.model.deformer.PTaperDeformer;
import com.arcanc.pulselib.content.model.deformer.PTwistDefinition;
import com.arcanc.pulselib.content.model.deformer.PTwistDeformer;
import com.arcanc.pulselib.content.model.deformer.PWaveDefinition;
import com.arcanc.pulselib.content.model.deformer.PWaveDeformer;
import com.arcanc.pulselib.util.PLibDatabase;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Provides support for lib registration.
 */
public class PLibRegistration
{
/**
 * Provides support for animation channel reg.
 */
	public static class AnimationChannelReg
	{
		public static final PRegistry<PAnimationChannelType<?>> CHANNEL_TYPES = new PRegistry<>();

		public static final PAnimationChannelType<Vector3f> POSITION = CHANNEL_TYPES.register(
				PLibDatabase.rl("position"), new PAnimationChannel.Vector3fChannelType(PLibDatabase.rl("position"), new Vector3f(), false));
		public static final PAnimationChannelType<Quaternionf> ROTATION = CHANNEL_TYPES.register(
				PLibDatabase.rl("rotation"), new PAnimationChannel.QuaternionChannelType(PLibDatabase.rl("rotation")));
		public static final PAnimationChannelType<Vector3f> SCALE = CHANNEL_TYPES.register(
				PLibDatabase.rl("scale"), new PAnimationChannel.Vector3fChannelType(PLibDatabase.rl("scale"), new Vector3f(1f), true));

		/**
		 * Performs the init operation.
		 */
		private static void init()
		{
		}
	}

/**
 * Provides support for animation event reg.
 */
	public static class AnimationEventReg
	{
		public static final PRegistry<PAnimationEventType<?>> EVENT_TYPES = new PRegistry<>();

		public static final PAnimationEventType<PAnimationEventTypes.SoundData> SOUND =
				EVENT_TYPES.register(PAnimationEventTypes.SOUND.id(), PAnimationEventTypes.SOUND);
		public static final PAnimationEventType<PAnimationEventTypes.ParticleData> PARTICLE =
				EVENT_TYPES.register(PAnimationEventTypes.PARTICLE.id(), PAnimationEventTypes.PARTICLE);
		public static final PAnimationEventType<PAnimationEventTypes.CameraShakeData> CAMERA_SHAKE =
				EVENT_TYPES.register(PAnimationEventTypes.CAMERA_SHAKE.id(), PAnimationEventTypes.CAMERA_SHAKE);
		public static final PAnimationEventType<PAnimationEventTypes.LocatorCallbackData> LOCATOR_CALLBACK =
				EVENT_TYPES.register(PAnimationEventTypes.LOCATOR_CALLBACK.id(), PAnimationEventTypes.LOCATOR_CALLBACK);
		public static final PAnimationEventType<PAnimationEventTypes.AnimationParameterData> ANIMATION_PARAMETER =
				EVENT_TYPES.register(PAnimationEventTypes.ANIMATION_PARAMETER.id(), PAnimationEventTypes.ANIMATION_PARAMETER);

		/**
		 * Performs the init operation.
		 */
		private static void init()
		{
		}
	}

/**
 * Provides support for mesh deformer reg.
 */
	public static class MeshDeformerReg
	{
		public static final PRegistry<PMeshDeformer<?>> DEFORMERS = new PRegistry<>();

		public static final PMeshDeformer<PBendDefinition> BEND = DEFORMERS.register(PBendDeformer.INSTANCE.id(), PBendDeformer.INSTANCE);
		public static final PMeshDeformer<PHingeDefinition> HINGE = DEFORMERS.register(PHingeDeformer.INSTANCE.id(), PHingeDeformer.INSTANCE);
		public static final PMeshDeformer<PTwistDefinition> TWIST = DEFORMERS.register(PTwistDeformer.INSTANCE.id(), PTwistDeformer.INSTANCE);
		public static final PMeshDeformer<PStretchDefinition> STRETCH = DEFORMERS.register(PStretchDeformer.INSTANCE.id(), PStretchDeformer.INSTANCE);
		public static final PMeshDeformer<PSquashDefinition> SQUASH = DEFORMERS.register(PSquashDeformer.INSTANCE.id(), PSquashDeformer.INSTANCE);
		public static final PMeshDeformer<PTaperDefinition> TAPER = DEFORMERS.register(PTaperDeformer.INSTANCE.id(), PTaperDeformer.INSTANCE);
		public static final PMeshDeformer<PWaveDefinition> WAVE = DEFORMERS.register(PWaveDeformer.INSTANCE.id(), PWaveDeformer.INSTANCE);

		/**
		 * Performs the init operation.
		 */
		private static void init()
		{
		}
	}


	/**
	 * Performs the init operation.
	 */
	public static void init()
	{
		AnimationChannelReg.init();
		AnimationEventReg.init();
		MeshDeformerReg.init();

	}
}
