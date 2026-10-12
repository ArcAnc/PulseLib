/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.content.animatable.PAnimationEventCallbacks;
import com.arcanc.pulselib.content.animatable.PAnimationCameraShake;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.helpers.PLibCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.joml.Vector3f;

public final class PAnimationEventTypes
{
	private PAnimationEventTypes() { }

	public static final PAnimationEventType<SoundData> SOUND = type("sound", PEventSide.PRESENTATION_ONLY,
			RecordCodecBuilder.mapCodec(instance -> instance.group(
					ResourceLocation.CODEC.fieldOf("sound").forGetter(SoundData::sound),
					Codec.STRING.optionalFieldOf("locator", "").forGetter(SoundData::locator),
					Codec.FLOAT.optionalFieldOf("volume", 1.0f).forGetter(SoundData::volume),
					Codec.FLOAT.optionalFieldOf("pitch", 1.0f).forGetter(SoundData::pitch)).
			apply(instance, SoundData::new)),
			(context, data) -> {
				PAnimationEventContext.PAnimationEventDispatcherBridge.Position position = context.position(data.locator());
				if (position == null)
					return;
				SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(data.sound());
				if (sound == null)
				{
					PLibDatabase.LOGGER.warn("Missing animation sound event: {}", data.sound());
					return;
				}
				position.level().playLocalSound(position.x(), position.y(), position.z(), sound, SoundSource.NEUTRAL,
						data.volume(), data.pitch(), false);
			});

	public static final PAnimationEventType<ParticleData> PARTICLE = type("particle", PEventSide.PRESENTATION_ONLY,
			RecordCodecBuilder.mapCodec(instance -> instance.group(
					ResourceLocation.CODEC.fieldOf("particle").forGetter(ParticleData::particle),
					Codec.STRING.optionalFieldOf("locator", "").forGetter(ParticleData::locator),
					PLibCodecs.VECTOR3F_CODEC.optionalFieldOf("offset", new Vector3f()).forGetter(ParticleData::offset),
					PLibCodecs.VECTOR3F_CODEC.optionalFieldOf("motion", new Vector3f()).forGetter(ParticleData::motion)).
			apply(instance, ParticleData::new)),
			(context, data) -> {
				PAnimationEventContext.PAnimationEventDispatcherBridge.Position position = context.position(data.locator());
				if (position == null)
					return;
				ParticleType<?> particleType = BuiltInRegistries.PARTICLE_TYPE.get(data.particle());
				if (!(particleType instanceof ParticleOptions options))
				{
					PLibDatabase.LOGGER.warn("Missing or unsupported simple animation particle: {}", data.particle());
					return;
				}
				Vector3f offset = data.offset();
				Vector3f motion = data.motion();
				position.level().addParticle(options, position.x() + offset.x(), position.y() + offset.y(), position.z() + offset.z(),
						motion.x(), motion.y(), motion.z());
			});

	public static final PAnimationEventType<CameraShakeData> CAMERA_SHAKE = type("camera_shake", PEventSide.PRESENTATION_ONLY,
			RecordCodecBuilder.mapCodec(instance -> instance.group(
					Codec.FLOAT.fieldOf("strength").forGetter(CameraShakeData::strength),
					Codec.FLOAT.optionalFieldOf("duration", 4.0f).forGetter(CameraShakeData::duration),
					Codec.FLOAT.optionalFieldOf("frequency", 12.0f).forGetter(CameraShakeData::frequency)).
			apply(instance, CameraShakeData::new)),
			(context, data) -> PAnimationCameraShake.add(data.strength(), data.duration(), data.frequency()));

	public static final PAnimationEventType<LocatorCallbackData> LOCATOR_CALLBACK = type("locator_callback", PEventSide.BOTH,
			RecordCodecBuilder.mapCodec(instance -> instance.group(
					ResourceLocation.CODEC.fieldOf("callback").forGetter(LocatorCallbackData::callback),
					Codec.STRING.optionalFieldOf("locator", "").forGetter(LocatorCallbackData::locator)).
			apply(instance, LocatorCallbackData::new)),
			(context, data) -> PAnimationEventCallbacks.dispatch(data.callback(), context, data.locator()));

	public static final PAnimationEventType<AnimationParameterData> ANIMATION_PARAMETER = type("animation_parameter", PEventSide.BOTH,
			RecordCodecBuilder.mapCodec(instance -> instance.group(
					Codec.STRING.optionalFieldOf("controller", "").forGetter(AnimationParameterData::controller),
					Codec.STRING.fieldOf("parameter").forGetter(AnimationParameterData::parameter),
					Codec.FLOAT.optionalFieldOf("value", 1.0f).forGetter(AnimationParameterData::value),
					Codec.BOOL.optionalFieldOf("trigger", false).forGetter(AnimationParameterData::trigger)).
			apply(instance, AnimationParameterData::new)),
			(context, data) -> {
				PAnimationController<?> target = data.controller().isBlank() ? context.controller() : context.poseControllers().stream()
						.filter(controller -> controller.name().equals(data.controller())).findFirst().orElse(null);
				if (target == null || target.graphRuntime() == null)
				{
					PLibDatabase.LOGGER.debug("Animation parameter event targets no graph controller: {}", data.controller());
					return;
				}
				if (data.trigger())
					target.trigger(data.parameter());
				else
					target.setParameter(data.parameter(), data.value());
			});

	private static <T> PAnimationEventType<T> type(String path, PEventSide side, MapCodec<T> codec,
	                                                EventExecutor<T> executor)
	{
		return new PAnimationEventType<>()
		{
			private final ResourceLocation id = PLibDatabase.rl(path);
			@Override public ResourceLocation id() { return this.id; }
			@Override public MapCodec<T> codec() { return codec; }
			@Override public PEventSide side() { return side; }
			@Override public void execute(PAnimationEventContext context, T data) { executor.execute(context, data); }
		};
	}

	@FunctionalInterface
	private interface EventExecutor<T> { void execute(PAnimationEventContext context, T data); }

	public record SoundData(ResourceLocation sound, String locator, float volume, float pitch) { }
	public record ParticleData(ResourceLocation particle, String locator, Vector3f offset, Vector3f motion) { }
	public record CameraShakeData(float strength, float duration, float frequency) { }
	public record LocatorCallbackData(ResourceLocation callback, String locator) { }
	public record AnimationParameterData(String controller, String parameter, float value, boolean trigger) { }
}
