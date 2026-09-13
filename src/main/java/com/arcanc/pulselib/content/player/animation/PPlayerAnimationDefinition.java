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
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public final class PPlayerAnimationDefinition
{
	private final PModelData modelData;
	private final Predicate<Player> predicate;
	private final Map<PPlayerPart, String> bindings;
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
	private final ItemRenderPolicy itemRenderPolicy;
	@Nullable
	private final String itemVisibilityController;
	@Nullable
	private final ItemVisibilityPolicy itemVisibilityPolicy;

	/**
	 * Creates an instance of the enclosing type.
	 * @param builder the builder to use.
	 */
	private PPlayerAnimationDefinition(Builder builder)
	{
		this.modelData = builder.modelData;
		this.predicate = builder.predicate;
		this.bindings = Map.copyOf(builder.bindings);
		this.partMask = builder.partMask == null ? PPlayerAnimationMask.of(builder.mask) : builder.partMask;
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
		this.firstPersonSettings = builder.firstPersonSettings;
		this.itemRenderPolicy = builder.itemRenderPolicy;
		this.itemVisibilityController = builder.itemVisibilityController;
		this.itemVisibilityPolicy = builder.itemVisibilityPolicy;
	}

	/**
	 * Performs the builder operation.
	 * @param modelData the model data to use.
	 * @return the value produced by this operation.
	 */
	public static Builder builder(PModelData modelData)
	{
		return new Builder(modelData);
	}

	/**
	 * Performs the model data operation.
	 * @return the value produced by this operation.
	 */
	public PModelData modelData()
	{
		return this.modelData;
	}

	/**
	 * Performs the should apply operation.
	 * @param player the player to use.
	 * @return the value produced by this operation.
	 */
	public boolean shouldApply(Player player)
	{
		return this.predicate.test(player);
	}

	/**
	 * Performs the bindings operation.
	 * @return the value produced by this operation.
	 */
	public Map<PPlayerPart, String> bindings()
	{
		return this.bindings;
	}

	/**
	 * Performs the applies to operation.
	 * @param player the player to use.
	 * @param part the part to use.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
	public boolean appliesTo(Player player, PPlayerPart part, float partialTick)
	{
		return this.partMask.contains(player, part, partialTick);
	}

	/**
	 * Blends the mode.
	 * @return the value produced by this operation.
	 */
	public PPlayerAnimationBlendMode blendMode()
	{
		return this.blendMode;
	}

	/**
	 * Performs the weight operation.
	 * @param player the player to use.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
	public float weight(Player player, float partialTick)
	{
		return Math.clamp(this.weight.weight(player, partialTick), 0.0f, 1.0f);
	}

	/**
	 * Performs the part weight operation.
	 * @param player the player to use.
	 * @param part the part to use.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
	public float partWeight(Player player, PPlayerPart part, float partialTick)
	{
		PPlayerAnimationWeight partWeight = this.partWeights.getOrDefault(part, PPlayerAnimationWeight.FULL);
		return Math.clamp(partWeight.weight(player, partialTick), 0.0f, 1.0f);
	}

	/**
	 * Performs the bone weight operation.
	 * @param player the player to use.
	 * @param boneName the bone name to use.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
	public float boneWeight(Player player, String boneName, float partialTick)
	{
		PPlayerAnimationWeight boneWeight = this.boneWeights.getOrDefault(boneName, PPlayerAnimationWeight.FULL);
		return Math.clamp(boneWeight.weight(player, partialTick), 0.0f, 1.0f);
	}

	/**
	 * Performs the deformers operation.
	 * @return the value produced by this operation.
	 */
	public List<PPlayerAnimationDeformer> deformers()
	{
		return this.deformers;
	}

	/**
	 * Performs the root pivot operation.
	 * @return the value produced by this operation.
	 */
	public Vector3f rootPivot()
	{
		return new Vector3f(this.rootPivot);
	}

	/**
	 * Performs the priority operation.
	 * @return the value produced by this operation.
	 */
	public int priority()
	{
		return this.priority;
	}

	/**
	 * Performs the crossfade duration operation.
	 * @return the value produced by this operation.
	 */
	public float crossfadeDuration()
	{
		return this.crossfadeDuration;
	}

	/**
	 * Performs the crossfade easing operation.
	 * @return the value produced by this operation.
	 */
	public PPoseEasing crossfadeEasing()
	{
		return this.crossfadeEasing;
	}

	/**
	 * Performs the transition interruption policy operation.
	 * @return the value produced by this operation.
	 */
	public PTransitionInterruptionPolicy transitionInterruptionPolicy()
	{
		return this.transitionInterruptionPolicy;
	}

	/**
	 * Synchronizes the group.
	 * @return the value produced by this operation.
	 */
	public String syncGroup()
	{
		return this.syncGroup;
	}

	/**
	 * Performs the anchors operation.
	 * @return the value produced by this operation.
	 */
	public Map<PPlayerAnimationAnchor, String> anchors()
	{
		return this.anchors;
	}

	/**
	 * Performs the first person settings operation.
	 * @return the value produced by this operation.
	 */
	public PPlayerFirstPersonSettings firstPersonSettings()
	{
		return this.firstPersonSettings;
	}

	/**
	 * Performs the item render policy operation.
	 * @return the value produced by this operation.
	 */
	public ItemRenderPolicy itemRenderPolicy()
	{
		return this.itemRenderPolicy;
	}

	/**
	 * The controller that supplies {@link #itemVisibilityPolicy()}'s timeline, if configured.
	 * Its sampled time is expressed in seconds.
	 */
	public @Nullable String itemVisibilityController()
	{
		return this.itemVisibilityController;
	}

	/**
	 * A phase-based first-person item visibility policy, if configured.
	 * It supplements {@link #itemRenderPolicy()} rather than replacing it.
	 */
	public @Nullable ItemVisibilityPolicy itemVisibilityPolicy()
	{
		return this.itemVisibilityPolicy;
	}
	
	/**
	 * Registers the controllers.
	 * @param registrar the registrar to use.
	 */
	void registerControllers(PAnimationManager.PAnimationRegistrar<PPlayerAnimationInstance> registrar)
	{
		this.controllerRegistrar.register(registrar);
	}

	/**
	 * Performs the populate molang context operation.
	 * @param player the player to use.
	 * @param instance the instance to use.
	 * @param controller the controller to use.
	 * @param context the context to use.
	 * @param partialTick the partial tick to use.
	 */
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

		/**
		 * Performs the register operation.
		 * @param registrar the registrar to use.
		 */
		void register(PAnimationManager.PAnimationRegistrar<PPlayerAnimationInstance> registrar);
	}

	@FunctionalInterface
	public interface MolangContextProvider
	{
		MolangContextProvider EMPTY = (player, instance, controller, context, partialTick) -> {};

		/**
		 * Performs the populate operation.
		 * @param player the player to use.
		 * @param instance the instance to use.
		 * @param controller the controller to use.
		 * @param context the context to use.
		 * @param partialTick the partial tick to use.
		 */
		void populate(Player player,
		              PPlayerAnimationInstance instance,
		              PAnimationController<PPlayerAnimationInstance> controller,
		              MolangParser.Context context,
		              float partialTick);
	}

	@FunctionalInterface
	public interface ItemRenderPolicy
	{
		ItemRenderPolicy RENDER = (player, hand, stack) -> false;
		ItemRenderPolicy HIDE = (player, hand, stack) -> true;

		/**
		 * Performs the hide operation.
		 * @param player the player to use.
		 * @param hand the hand to use.
		 * @param stack the stack to use.
		 * @return the value produced by this operation.
		 */
		boolean hide(LocalPlayer player,
		             InteractionHand hand,
		             ItemStack stack);
	}

	public enum ItemVisibility
	{
		VISIBLE,
		HIDDEN
	}

	@FunctionalInterface
	public interface ItemVisibilityPolicy
	{
		ItemVisibilityPolicy VISIBLE = (player, hand, animationTime, stack) -> ItemVisibility.VISIBLE;
		ItemVisibilityPolicy HIDDEN = (player, hand, animationTime, stack) -> ItemVisibility.HIDDEN;

		/**
		 * Performs the visibility operation.
		 * @param player the player to use.
		 * @param hand the hand to use.
		 * @param animationTime the animation time to use.
		 * @param stack the stack to use.
		 * @return the value produced by this operation.
		 */
		ItemVisibility visibility(LocalPlayer player,
		                          InteractionHand hand,
		                          float animationTime,
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
		private ItemRenderPolicy itemRenderPolicy = ItemRenderPolicy.RENDER;
		@Nullable
		private String itemVisibilityController;
		@Nullable
		private ItemVisibilityPolicy itemVisibilityPolicy;

		/**
		 * Creates an instance of the enclosing type.
		 * @param modelData the model data to use.
		 */
		private Builder(PModelData modelData)
		{
			this.modelData = Objects.requireNonNull(modelData);
		}

		/**
		 * Performs the when operation.
		 * @param predicate the predicate to use.
		 * @return the value produced by this operation.
		 */
		public Builder when(Predicate<Player> predicate)
		{
			this.predicate = Objects.requireNonNull(predicate);
			return this;
		}

		/**
		 * Performs the bind operation.
		 * @param part the part to use.
		 * @param boneName the bone name to use.
		 * @return the value produced by this operation.
		 */
		public Builder bind(PPlayerPart part, String boneName)
		{
			if (boneName == null || boneName.isBlank())
				throw new IllegalArgumentException("Player animation bone name cannot be blank");
			this.bindings.put(Objects.requireNonNull(part), boneName);
			return this;
		}
		
		/**
		 * Performs the mask operation.
		 * @param parts the parts to use.
		 * @return the value produced by this operation.
		 */
		public Builder mask(PPlayerPart... parts)
		{
			this.mask.clear();
			for (PPlayerPart part : parts)
				this.mask.add(Objects.requireNonNull(part));
			this.partMask = null;
			return this;
		}
		
		/**
		 * Performs the mask operation.
		 * @param mask the mask to use.
		 * @return the value produced by this operation.
		 */
		public Builder mask(PPlayerAnimationMask mask)
		{
			this.mask.clear();
			this.partMask = Objects.requireNonNull(mask);
			return this;
		}

		/**
		 * Blends the mode.
		 * @param blendMode the blend mode to use.
		 * @return the value produced by this operation.
		 */
		public Builder blendMode(PPlayerAnimationBlendMode blendMode)
		{
			this.blendMode = Objects.requireNonNull(blendMode);
			return this;
		}

		/**
		 * Performs the weight operation.
		 * @param weight the weight to use.
		 * @return the value produced by this operation.
		 */
		public Builder weight(float weight)
		{
			return weight((player, partialTick) -> weight);
		}

		/**
		 * Performs the weight operation.
		 * @param weight the weight to use.
		 * @return the value produced by this operation.
		 */
		public Builder weight(PPlayerAnimationWeight weight)
		{
			this.weight = Objects.requireNonNull(weight);
			return this;
		}

		/**
		 * Performs the part weight operation.
		 * @param part the part to use.
		 * @param weight the weight to use.
		 * @return the value produced by this operation.
		 */
		public Builder partWeight(PPlayerPart part, float weight)
		{
			return partWeight(part, (player, partialTick) -> weight);
		}

		/**
		 * Performs the part weight operation.
		 * @param part the part to use.
		 * @param weight the weight to use.
		 * @return the value produced by this operation.
		 */
		public Builder partWeight(PPlayerPart part, PPlayerAnimationWeight weight)
		{
			this.partWeights.put(Objects.requireNonNull(part), Objects.requireNonNull(weight));
			return this;
		}

		/**
		 * Performs the bone weight operation.
		 * @param boneName the bone name to use.
		 * @param weight the weight to use.
		 * @return the value produced by this operation.
		 */
		public Builder boneWeight(String boneName, float weight)
		{
			return boneWeight(boneName, (player, partialTick) -> weight);
		}

		/**
		 * Performs the bone weight operation.
		 * @param boneName the bone name to use.
		 * @param weight the weight to use.
		 * @return the value produced by this operation.
		 */
		public Builder boneWeight(String boneName, PPlayerAnimationWeight weight)
		{
			if (boneName == null || boneName.isBlank())
				throw new IllegalArgumentException("Player animation bone name cannot be blank");
			this.boneWeights.put(boneName, Objects.requireNonNull(weight));
			return this;
		}

		/**
		 * Performs the deform operation.
		 * @param part the part to use.
		 * @param stack the stack to use.
		 * @param values the values to use.
		 * @return the value produced by this operation.
		 */
		public Builder deform(PPlayerPart part,
		                      PDeformerStack stack,
		                      PPlayerAnimationDeformerValueSource values)
		{
			this.deformers.add(new PPlayerAnimationDeformer(part, stack, values));
			return this;
		}
		
		/**
		 * Performs the root pivot operation.
		 * @param rootPivot the root pivot to use.
		 * @return the value produced by this operation.
		 */
		public Builder rootPivot(Vector3f rootPivot)
		{
			this.rootPivot = new Vector3f(Objects.requireNonNull(rootPivot));
			return this;
		}

		/**
		 * Performs the root pivot operation.
		 * @param x the x to use.
		 * @param y the y to use.
		 * @param z the z to use.
		 * @return the value produced by this operation.
		 */
		public Builder rootPivot(float x, float y, float z)
		{
			return rootPivot(new Vector3f(x, y, z));
		}
		
		/**
		 * Performs the priority operation.
		 * @param priority the priority to use.
		 * @return the value produced by this operation.
		 */
		public Builder priority(int priority)
		{
			this.priority = priority;
			return this;
		}

		/**
		 * Performs the crossfade operation.
		 * @param duration the duration to use.
		 * @param easing the easing to use.
		 * @param interruptionPolicy the interruption policy to use.
		 * @return the value produced by this operation.
		 */
		public Builder crossfade(float duration, PPoseEasing easing, PTransitionInterruptionPolicy interruptionPolicy)
		{
			if (duration < 0.0f)
				throw new IllegalArgumentException("Crossfade duration must be non-negative");
			this.crossfadeDuration = duration;
			this.crossfadeEasing = Objects.requireNonNull(easing);
			this.transitionInterruptionPolicy = Objects.requireNonNull(interruptionPolicy);
			return this;
		}

		/**
		 * Synchronizes the group.
		 * @param syncGroup the sync group to use.
		 * @return the value produced by this operation.
		 */
		public Builder syncGroup(String syncGroup)
		{
			this.syncGroup = syncGroup == null ? "" : syncGroup;
			return this;
		}

		/**
		 * Performs the controllers operation.
		 * @param controllerRegistrar the controller registrar to use.
		 * @return the value produced by this operation.
		 */
		public Builder controllers(ControllerRegistrar controllerRegistrar)
		{
			this.controllerRegistrar = Objects.requireNonNull(controllerRegistrar);
			return this;
		}

		/**
		 * Performs the populate molang context operation.
		 * @param provider the provider to use.
		 * @return the value produced by this operation.
		 */
		public Builder populateMolangContext(MolangContextProvider provider)
		{
			this.molangContextProvider = Objects.requireNonNull(provider);
			return this;
		}
		
		/**
		 * Performs the anchor operation.
		 * @param playerAnimationAnchor the player animation anchor to use.
		 * @param boneName the bone name to use.
		 * @return the value produced by this operation.
		 */
		public Builder anchor(PPlayerAnimationAnchor playerAnimationAnchor, String boneName)
		{
			Objects.requireNonNull(playerAnimationAnchor);
			Objects.requireNonNull(boneName);
			this.anchors.put(playerAnimationAnchor, boneName);
			return this;
		}
		
		/**
		 * Performs the first person operation.
		 * @param settings the settings to use.
		 * @return the value produced by this operation.
		 */
		public Builder firstPerson(PPlayerFirstPersonSettings settings)
		{
			this.firstPersonSettings = Objects.requireNonNull(settings);
			return this;
		}

		/**
		 * Performs the item render policy operation.
		 * @param itemRenderPolicy the item render policy to use.
		 * @return the value produced by this operation.
		 */
		public Builder itemRenderPolicy(ItemRenderPolicy itemRenderPolicy)
		{
			this.itemRenderPolicy = Objects.requireNonNull(itemRenderPolicy);
			return this;
		}

		/**
		 * Controls item visibility at a particular phase of a named controller's timeline.
		 * The supplied time is interpolated and expressed in seconds, independently of
		 * activation crossfade weight. A hidden result is combined with
		 * {@link #itemRenderPolicy(ItemRenderPolicy)}.
		 */
		public Builder itemVisibility(String controllerName, ItemVisibilityPolicy itemVisibilityPolicy)
		{
			if (controllerName == null || controllerName.isBlank())
				throw new IllegalArgumentException("Item visibility controller name cannot be blank");
			this.itemVisibilityController = controllerName;
			this.itemVisibilityPolicy = Objects.requireNonNull(itemVisibilityPolicy);
			return this;
		}

		/**
		 * Performs the build operation.
		 * @return the value produced by this operation.
		 */
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
