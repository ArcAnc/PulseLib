/**
 * @author ArcAnc
 * Created at: 30.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.model.animation.PPoseEasing;
import com.arcanc.pulselib.content.model.animation.PTransitionInterruptionPolicy;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonSettings;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.MolangParser;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Predicate;

public final class PPlayerAnimationDefinition
{
	private final PModelData modelData;
	private final Predicate<Player> predicate;
	private final Map<PPlayerPart, String> bindings;
	private final Set<PPlayerPart> mask;
	private final PPlayerAnimationMask partMask;
	private final PPlayerAnimationBlendMode blendMode;
	private final PPlayerAnimationWeight weight;
	private final Map<PPlayerPart, PPlayerAnimationWeight> partWeights;
	private final Map<String, PPlayerAnimationWeight> boneWeights;
	private final List<PPlayerAnimationDeformer> deformers;
	private final Vector3f rootPivot;
	private final int priority;
	private final float crossfadeDuration;
	private final PPoseEasing crossfadeEasing;
	private final PTransitionInterruptionPolicy transitionInterruptionPolicy;
	private final String syncGroup;
	private final ControllerRegistrar controllerRegistrar;
	private final MolangContextProvider molangContextProvider;

	private final Map<PPlayerAnimationAnchor, String> anchors;
	private final PPlayerFirstPersonSettings firstPersonSettings;
	private final FPItemHider itemHider;

	private PPlayerAnimationDefinition(Builder builder)
	{
		this.modelData = builder.modelData;
		this.predicate = builder.predicate;
		this.bindings = Map.copyOf(builder.bindings);
		this.mask = Set.copyOf(builder.mask);
		this.partMask = builder.partMask == null ? PPlayerAnimationMask.of(this.mask) : builder.partMask;
		this.blendMode = builder.blendMode;
		this.weight = builder.weight;
		this.partWeights = Map.copyOf(builder.partWeights);
		this.boneWeights = Map.copyOf(builder.boneWeights);
		this.deformers = List.copyOf(builder.deformers);
		this.rootPivot = new Vector3f(builder.rootPivot);
		this.priority = builder.priority;
		this.crossfadeDuration = builder.crossfadeDuration;
		this.crossfadeEasing = builder.crossfadeEasing;
		this.transitionInterruptionPolicy = builder.transitionInterruptionPolicy;
		this.syncGroup = builder.syncGroup;
		this.controllerRegistrar = builder.controllerRegistrar;
		this.molangContextProvider = builder.molangContextProvider;
		this.anchors = Map.copyOf(builder.anchors);
		this.firstPersonSettings = builder.firstPersonSettings.copy();
		this.itemHider = builder.itemHider;
	}

	public static Builder builder(PModelData modelData)
	{
		return new Builder(modelData);
	}

	public PModelData modelData()
	{
		return this.modelData;
	}

	public boolean shouldApply(Player player)
	{
		return this.predicate.test(player);
	}

	public Map<PPlayerPart, String> bindings()
	{
		return this.bindings;
	}

	public Set<PPlayerPart> mask()
	{
		return this.mask;
	}

	public boolean appliesTo(Player player, PPlayerPart part, float partialTick)
	{
		return this.partMask.contains(player, part, partialTick);
	}

	public PPlayerAnimationBlendMode blendMode()
	{
		return this.blendMode;
	}

	public float weight(Player player, float partialTick)
	{
		return Math.clamp(this.weight.weight(player, partialTick), 0.0f, 1.0f);
	}

	public float weight(Player player, PPlayerPart part, float partialTick)
	{
		return Math.clamp(weight(player, partialTick) * partWeight(player, part, partialTick), 0.0f, 1.0f);
	}

	public float partWeight(Player player, PPlayerPart part, float partialTick)
	{
		PPlayerAnimationWeight partWeight = this.partWeights.getOrDefault(part, PPlayerAnimationWeight.FULL);
		return Math.clamp(partWeight.weight(player, partialTick), 0.0f, 1.0f);
	}

	public float boneWeight(Player player, String boneName, float partialTick)
	{
		PPlayerAnimationWeight boneWeight = this.boneWeights.getOrDefault(boneName, PPlayerAnimationWeight.FULL);
		return Math.clamp(boneWeight.weight(player, partialTick), 0.0f, 1.0f);
	}

	public List<PPlayerAnimationDeformer> deformers()
	{
		return this.deformers;
	}

	public Vector3f rootPivot()
	{
		return new Vector3f(this.rootPivot);
	}

	public int priority()
	{
		return this.priority;
	}

	public float crossfadeDuration()
	{
		return this.crossfadeDuration;
	}

	public PPoseEasing crossfadeEasing()
	{
		return this.crossfadeEasing;
	}

	public PTransitionInterruptionPolicy transitionInterruptionPolicy()
	{
		return this.transitionInterruptionPolicy;
	}

	public String syncGroup()
	{
		return this.syncGroup;
	}
	
	public Map<PPlayerAnimationAnchor, String> anchors()
	{
		return this.anchors;
	}

	public PPlayerFirstPersonSettings firstPersonSettings()
	{
		return this.firstPersonSettings.copy();
	}

	public FPItemHider itemHider()
	{
		return this.itemHider;
	}
	
	void registerControllers(PAnimationManager.PAnimationRegistrar<PPlayerAnimationInstance> registrar)
	{
		this.controllerRegistrar.register(registrar);
	}

	void populateMolangContext(Player player,
	                           PPlayerAnimationInstance instance,
	                           PAnimationController<PPlayerAnimationInstance> controller,
	                           MolangParser.Context context,
	                           float partialTick)
	{
		this.molangContextProvider.populate(player, instance, controller, context, partialTick);
	}

	@FunctionalInterface
	public interface ControllerRegistrar
	{
		ControllerRegistrar EMPTY = registrar -> {};

		void register(PAnimationManager.PAnimationRegistrar<PPlayerAnimationInstance> registrar);
	}

	@FunctionalInterface
	public interface MolangContextProvider
	{
		MolangContextProvider EMPTY = (player, instance, controller, context, partialTick) -> {};

		void populate(Player player,
		              PPlayerAnimationInstance instance,
		              PAnimationController<PPlayerAnimationInstance> controller,
		              MolangParser.Context context,
		              float partialTick);
	}

	@FunctionalInterface
	public interface FPItemHider
	{
		FPItemHider NEVER = (player, hand, stack) -> false;
		FPItemHider ALWAYS = (player, hand, stack) -> true;
		boolean hide(LocalPlayer player,
			          InteractionHand hand,
			          ItemStack stack);
	}

	public static final class Builder
	{
		private final PModelData modelData;
		private Predicate<Player> predicate = player -> true;
		private final Map<PPlayerPart, String> bindings = new LinkedHashMap<>();
		private final EnumSet<PPlayerPart> mask = EnumSet.noneOf(PPlayerPart.class);
		private PPlayerAnimationMask partMask;
		private PPlayerAnimationBlendMode blendMode = PPlayerAnimationBlendMode.ADDITIVE_LOCAL;
		private PPlayerAnimationWeight weight = PPlayerAnimationWeight.FULL;
		private final Map<PPlayerPart, PPlayerAnimationWeight> partWeights = new LinkedHashMap<>();
		private final Map<String, PPlayerAnimationWeight> boneWeights = new LinkedHashMap<>();
		private final List<PPlayerAnimationDeformer> deformers = new ArrayList<>();
		private Vector3f rootPivot = new Vector3f();
		private int priority;
		private float crossfadeDuration;
		private PPoseEasing crossfadeEasing = PPoseEasing.LINEAR;
		private PTransitionInterruptionPolicy transitionInterruptionPolicy = PTransitionInterruptionPolicy.FROM_CURRENT;
		private String syncGroup = "";
		private ControllerRegistrar controllerRegistrar = ControllerRegistrar.EMPTY;
		private MolangContextProvider molangContextProvider = MolangContextProvider.EMPTY;

		private final Map<PPlayerAnimationAnchor, String> anchors = new HashMap<>();
		private PPlayerFirstPersonSettings firstPersonSettings = PPlayerFirstPersonSettings.DISABLED;
		private FPItemHider itemHider = FPItemHider.NEVER;

		private Builder(PModelData modelData)
		{
			this.modelData = Objects.requireNonNull(modelData);
		}

		public Builder when(Predicate<Player> predicate)
		{
			this.predicate = Objects.requireNonNull(predicate);
			return this;
		}

		public Builder bind(PPlayerPart part, String boneName)
		{
			if (boneName == null || boneName.isBlank())
				throw new IllegalArgumentException("Player animation bone name cannot be blank");
			this.bindings.put(Objects.requireNonNull(part), boneName);
			return this;
		}
		
		public Builder mask(PPlayerPart... parts)
		{
			this.mask.clear();
			for (PPlayerPart part : parts)
				this.mask.add(Objects.requireNonNull(part));
			this.partMask = null;
			return this;
		}
		
		public Builder mask(PPlayerAnimationMask mask)
		{
			this.mask.clear();
			this.partMask = Objects.requireNonNull(mask);
			return this;
		}

		public Builder blendMode(PPlayerAnimationBlendMode blendMode)
		{
			this.blendMode = Objects.requireNonNull(blendMode);
			return this;
		}

		public Builder weight(float weight)
		{
			return weight((player, partialTick) -> weight);
		}

		public Builder weight(PPlayerAnimationWeight weight)
		{
			this.weight = Objects.requireNonNull(weight);
			return this;
		}

		public Builder partWeight(PPlayerPart part, float weight)
		{
			return partWeight(part, (player, partialTick) -> weight);
		}

		public Builder partWeight(PPlayerPart part, PPlayerAnimationWeight weight)
		{
			this.partWeights.put(Objects.requireNonNull(part), Objects.requireNonNull(weight));
			return this;
		}

		public Builder boneWeight(String boneName, float weight)
		{
			return boneWeight(boneName, (player, partialTick) -> weight);
		}

		public Builder boneWeight(String boneName, PPlayerAnimationWeight weight)
		{
			if (boneName == null || boneName.isBlank())
				throw new IllegalArgumentException("Player animation bone name cannot be blank");
			this.boneWeights.put(boneName, Objects.requireNonNull(weight));
			return this;
		}

		public Builder deform(PPlayerPart part,
		                      PDeformerStack stack,
		                      PPlayerAnimationDeformerValueSource values)
		{
			this.deformers.add(new PPlayerAnimationDeformer(part, stack, values));
			return this;
		}
		
		public Builder rootPivot(Vector3f rootPivot)
		{
			this.rootPivot = new Vector3f(Objects.requireNonNull(rootPivot));
			return this;
		}

		public Builder rootPivot(float x, float y, float z)
		{
			return rootPivot(new Vector3f(x, y, z));
		}
		
		public Builder priority(int priority)
		{
			this.priority = priority;
			return this;
		}

		public Builder crossfade(float duration, PPoseEasing easing, PTransitionInterruptionPolicy interruptionPolicy)
		{
			if (duration < 0.0f)
				throw new IllegalArgumentException("Crossfade duration must be non-negative");
			this.crossfadeDuration = duration;
			this.crossfadeEasing = Objects.requireNonNull(easing);
			this.transitionInterruptionPolicy = Objects.requireNonNull(interruptionPolicy);
			return this;
		}

		public Builder syncGroup(String syncGroup)
		{
			this.syncGroup = syncGroup == null ? "" : syncGroup;
			return this;
		}

		public Builder controllers(ControllerRegistrar controllerRegistrar)
		{
			this.controllerRegistrar = Objects.requireNonNull(controllerRegistrar);
			return this;
		}

		public Builder populateMolangContext(MolangContextProvider provider)
		{
			this.molangContextProvider = Objects.requireNonNull(provider);
			return this;
		}
		
		public Builder anchor(PPlayerAnimationAnchor playerAnimationAnchor, String boneName)
		{
			Objects.requireNonNull(playerAnimationAnchor);
			Objects.requireNonNull(boneName);
			this.anchors.put(playerAnimationAnchor, boneName);
			return this;
		}
		
		public Builder firstPerson(PPlayerFirstPersonSettings settings)
		{
			this.firstPersonSettings = Objects.requireNonNull(settings);
			return this;
		}

		public Builder hideItemInHands(FPItemHider itemHider)
		{
			this.itemHider = Objects.requireNonNull(itemHider);
			return this;
		}

		public PPlayerAnimationDefinition build()
		{
			if (this.bindings.isEmpty())
				throw new IllegalStateException("A player animation definition needs at least one bone binding");
			if (this.mask.isEmpty() && this.partMask == null)
				this.mask.addAll(this.bindings.keySet());
			return new PPlayerAnimationDefinition(this);
		}
	}
}
