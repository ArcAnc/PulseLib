/**
 * @author ArcAnc
 * Created at: 30.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.model.animation.PAnimationPoseResolver;
import com.arcanc.pulselib.content.model.animation.PTransitionInterruptionPolicy;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.data.gecko.MolangParser;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PPlayerAnimationInstance implements PAnimatable<PPlayerAnimationInstance>
{
	private Player player;
	private final Identifier id;
	private final PPlayerAnimationDefinition definition;
	private final PAnimationManager<PPlayerAnimationInstance> animationManager;
	private boolean targetActive;
	private float activation;
	private float previousActivation;
	private float transitionStart;
	private float transitionTarget;
	private float transitionElapsed;
	private boolean firstPersonTargetActive;
	private float firstPersonActivation;
	private float previousFirstPersonActivation;
	private float firstPersonTransitionStart;
	private float firstPersonTransitionTarget;
	private float firstPersonTransitionElapsed;
	private float firstPersonTransitionDuration;

	PPlayerAnimationInstance(Player player, Identifier id, PPlayerAnimationDefinition definition)
	{
		this.player = Objects.requireNonNull(player);
		this.id = Objects.requireNonNull(id);
		this.definition = Objects.requireNonNull(definition);
		long key = Objects.hash(player.getUUID(), id);
		this.animationManager = new InstanceAnimationManager<>(this, new AnimManagerKey(key));
	}

	public Player player()
	{
		return this.player;
	}

	void updatePlayer(Player player)
	{
		this.player = Objects.requireNonNull(player);
	}

	public Identifier id()
	{
		return this.id;
	}

	public PPlayerAnimationDefinition definition()
	{
		return this.definition;
	}

	@Override
	public PAnimationManager<PPlayerAnimationInstance> getAnimationManager(AnimManagerKey key)
	{
		return this.animationManager;
	}

	@Override
	public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<PPlayerAnimationInstance> registrar)
	{
		this.definition.registerControllers(registrar);
	}

	@Nullable PAnimationController<PPlayerAnimationInstance> controller(String controllerName)
	{
		return this.animationManager.getControllers().get(controllerName);
	}

	void stopAllControllers()
	{
		this.animationManager.getControllers().values().forEach(PAnimationController :: stop);
	}

	static void synchronize(List<PPlayerAnimationInstance> instances)
	{
		PPlayerAnimationInstance leader = instances.getFirst();
		PBakedModel leaderModel = leader.definition.modelData().getModel();
		if (leaderModel == null)
			return;
		for (Map.Entry<String, PAnimationController<PPlayerAnimationInstance>> entry : leader.animationManager.getControllers().entrySet())
		{
			float phase = entry.getValue().cyclePhase(leaderModel);
			if (Float.isNaN(phase))
				continue;
			for (int index = 1; index < instances.size(); index++)
			{
				PPlayerAnimationInstance follower = instances.get(index);
				PBakedModel followerModel = follower.definition.modelData().getModel();
				PAnimationController<PPlayerAnimationInstance> controller = follower.animationManager.getControllers().get(entry.getKey());
				if (followerModel != null && controller != null)
					controller.syncCycle(followerModel, phase);
			}
		}
	}

	void tick(boolean shouldApply)
	{
		PBakedModel model = this.definition.modelData().getModel();
		if (model == null)
			return;

		this.animationManager.bindModel(model);
		updateActivation(shouldApply);
		updateFirstPersonActivation(shouldApply && hasActiveController());
		if (shouldApply || this.activation > 0.0f)
			this.animationManager.tick();
	}

	boolean isContributing()
	{
		return this.activation > 0.0f || this.previousActivation > 0.0f || this.targetActive;
	}

	boolean hasActiveController()
	{
		return this.animationManager.getControllers().values().stream().anyMatch(controller ->
				controller.isPlaying() || controller.isPaused());
	}

	float activationWeight(float partialTick)
	{
		return Mth.lerp(partialTick, this.previousActivation, this.activation);
	}

	boolean isFirstPersonContributing()
	{
		return this.firstPersonActivation > 0.0f || this.previousFirstPersonActivation > 0.0f || this.firstPersonTargetActive;
	}

	float firstPersonActivationWeight(float partialTick)
	{
		return Mth.lerp(partialTick, this.previousFirstPersonActivation, this.firstPersonActivation);
	}

	private void updateActivation(boolean shouldApply)
	{
		this.previousActivation = this.activation;
		if (shouldApply != this.targetActive)
		{
			if (this.transitionElapsed < this.definition.crossfadeDuration() &&
					this.definition.transitionInterruptionPolicy() == PTransitionInterruptionPolicy.COMPLETE_CURRENT)
			{
				// The current fade is allowed to finish; the new request is sampled next tick.
			}
			else
			{
			this.transitionStart = this.definition.transitionInterruptionPolicy() == PTransitionInterruptionPolicy.RESTART ?
						(this.targetActive ? 1.0f : 0.0f) : this.activation;
				this.transitionTarget = shouldApply ? 1.0f : 0.0f;
				this.transitionElapsed = 0.0f;
				this.targetActive = shouldApply;
			}
		}
		float duration = this.definition.crossfadeDuration();
		if (duration <= 0.0f)
		{
			this.activation = this.targetActive ? 1.0f : 0.0f;
			return;
		}
		this.transitionElapsed = Math.min(this.transitionElapsed + 1.0f, duration);
		float alpha = this.definition.crossfadeEasing().transform(this.transitionElapsed / duration);
		this.activation = Mth.lerp(alpha, this.transitionStart, this.transitionTarget);
	}

	private void updateFirstPersonActivation(boolean shouldApply)
	{
		var settings = this.definition.firstPersonSettings();
		this.previousFirstPersonActivation = this.firstPersonActivation;
		if (!settings.enabled())
		{
			this.firstPersonTargetActive = false;
			this.firstPersonActivation = 0.0f;
			return;
		}

		if (shouldApply != this.firstPersonTargetActive)
		{
			if (this.firstPersonTransitionElapsed < this.firstPersonTransitionDuration &&
					this.definition.transitionInterruptionPolicy() == PTransitionInterruptionPolicy.COMPLETE_CURRENT)
				return;

			this.firstPersonTransitionStart = this.definition.transitionInterruptionPolicy() == PTransitionInterruptionPolicy.RESTART ?
					(this.firstPersonTargetActive ? 1.0f : 0.0f) : this.firstPersonActivation;
			this.firstPersonTransitionTarget = shouldApply ? 1.0f : 0.0f;
			this.firstPersonTransitionDuration = shouldApply ? settings.transitionIn() : settings.transitionOut();
			this.firstPersonTransitionElapsed = 0.0f;
			this.firstPersonTargetActive = shouldApply;
		}

		if (this.firstPersonTransitionDuration <= 0.0f)
		{
			this.firstPersonActivation = this.firstPersonTargetActive ? 1.0f : 0.0f;
			return;
		}

		this.firstPersonTransitionElapsed = Math.min(this.firstPersonTransitionElapsed + 1.0f, this.firstPersonTransitionDuration);
		float alpha = this.definition.crossfadeEasing().transform(this.firstPersonTransitionElapsed / this.firstPersonTransitionDuration);
		this.firstPersonActivation = Mth.lerp(alpha, this.firstPersonTransitionStart, this.firstPersonTransitionTarget);
	}

	public @Nullable PPlayerAnimationFrame sampleFrame(float partialTick)
	{
		PBakedModel model =
				this.definition.modelData().getModel();
		
		if (model == null)
			return null;
		
		PAnimationPoseResolver<PPlayerAnimationInstance> resolver =
				new PAnimationPoseResolver<>(
						model,
						this.animationManager.
										getControllers().
										values(),
						(controller, tick) ->
						{
							MolangParser.Context context =
									new MolangParser.Context().
											query(
												"anim_time",
												controller.getInterpolatedTime(tick)).
											randomSeed(
												this.animationManager.key().key());
							
							this.definition.populateMolangContext(
									this.player,
									this,
									controller,
									context,
									tick
							);
							
							return context;
						},
						partialTick
				);
		
		return new PPlayerAnimationFrame(
				this.definition,
				resolver
		);
		
	}
}
