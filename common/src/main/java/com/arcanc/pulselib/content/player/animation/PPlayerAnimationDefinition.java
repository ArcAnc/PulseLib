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
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonSettings;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.content.model.animation.PPoseEasing;
import com.arcanc.pulselib.content.model.animation.PTransitionInterruptionPolicy;
import com.arcanc.pulselib.data.gecko.MolangParser;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Immutable player animation configuration containing model bindings, masks, weights, controllers,
 * transitions and first-person presentation policies.
 */
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
	private final Set<String> meshAttachmentRoots;
	private final Map<PPlayerAnimationAnchor, String> anchors;
	private final PPlayerFirstPersonSettings firstPersonSettings;
	private final ItemRenderPolicy itemRenderPolicy;
	private final @Nullable String itemVisibilityController;
	private final @Nullable ItemVisibilityPolicy itemVisibilityPolicy;
	private final List<PPlayerAnimationDeformer> deformers;
	private final Vector3f rootPivot;
	private final int priority;
	private final float crossfadeDuration;
	private final PPoseEasing crossfadeEasing;
	private final PTransitionInterruptionPolicy transitionInterruptionPolicy;
	private final String syncGroup;
	private final ControllerRegistrar controllerRegistrar;
	private final MolangContextProvider molangContextProvider;

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
		this.meshAttachmentRoots = Set.copyOf(builder.meshAttachmentRoots);
		this.anchors = Map.copyOf(builder.anchors);
		this.firstPersonSettings = builder.firstPersonSettings;
		this.itemRenderPolicy = builder.itemRenderPolicy;
		this.itemVisibilityController = builder.itemVisibilityController;
		this.itemVisibilityPolicy = builder.itemVisibilityPolicy;
		this.deformers = List.copyOf(builder.deformers);
		this.rootPivot = new Vector3f(builder.rootPivot);
		this.priority = builder.priority;
		this.crossfadeDuration = builder.crossfadeDuration;
		this.crossfadeEasing = builder.crossfadeEasing;
		this.transitionInterruptionPolicy = builder.transitionInterruptionPolicy;
		this.syncGroup = builder.syncGroup;
		this.controllerRegistrar = builder.controllerRegistrar;
		this.molangContextProvider = builder.molangContextProvider;
	}

	/**
	 * Starts a player animation definition for the supplied model.
	 *
	 * @param modelData the canonical model and loader selection
	 * @return a builder bound to the supplied model selection
	 */
	public static Builder builder(PModelData modelData)
	{
		return new Builder(modelData);
	}

	/**
	 * Returns the canonical model and loader selection.
	 *
	 * @return the canonical model and loader selection
	 */
	public PModelData modelData()
	{
		return this.modelData;
	}

	/**
	 * Tests the activation predicate against the current player.
	 *
	 * @param player the player whose animation or presentation is being processed
	 * @return whether the supplied inputs match this predicate
	 */
	public boolean shouldApply(Player player)
	{
		return this.predicate.test(player);
	}

	/**
	 * Returns the attachment bone-to-anchor bindings.
	 *
	 * @return the attachment bone-to-anchor bindings
	 */
	public Map<PPlayerPart, String> bindings()
	{
		return this.bindings;
	}

	/**
	 * Returns the mask selecting participating bones or player parts.
	 *
	 * @return the mask selecting participating bones or player parts
	 */
	public Set<PPlayerPart> mask()
	{
		return this.mask;
	}

	/**
	 * Tests the part mask for the player at the supplied render interpolation fraction.
	 *
	 * @param player the player whose animation or presentation is being processed
	 * @param part the player model part receiving the contribution
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return whether the supplied inputs match this predicate
	 */
	public boolean appliesTo(Player player, PPlayerPart part, float partialTick)
	{
		return this.partMask.contains(player, part, partialTick);
	}

	/**
	 * Returns the blend mode.
	 *
	 * @return the blend mode
	 */
	public PPlayerAnimationBlendMode blendMode()
	{
		return this.blendMode;
	}

	/**
	 * Samples the overall or part-specific animation contribution weight.
	 *
	 * @param player the player whose animation or presentation is being processed
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the contribution weight clamped to the range zero to one
	 */
	public float weight(Player player, float partialTick)
	{
		return Math.clamp(this.weight.weight(player, partialTick), 0.0f, 1.0f);
	}

	/**
	 * Samples the overall or part-specific animation contribution weight.
	 *
	 * @param player the player whose animation or presentation is being processed
	 * @param part the player model part receiving the contribution
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the contribution weight clamped to the range zero to one
	 */
	public float weight(Player player, PPlayerPart part, float partialTick)
	{
		return Math.clamp(weight(player, partialTick) * partWeight(player, part, partialTick), 0.0f, 1.0f);
	}

	/**
	 * Samples the configured contribution weight for one player model part.
	 *
	 * @param player the player whose animation or presentation is being processed
	 * @param part the player model part receiving the contribution
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the part weight clamped to the range zero to one
	 */
	public float partWeight(Player player, PPlayerPart part, float partialTick)
	{
		PPlayerAnimationWeight partWeight = this.partWeights.getOrDefault(part, PPlayerAnimationWeight.FULL);
		return Math.clamp(partWeight.weight(player, partialTick), 0.0f, 1.0f);
	}

	/**
	 * Samples the configured contribution weight for one animation bone.
	 *
	 * @param player the player whose animation or presentation is being processed
	 * @param boneName the animation bone name
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the bone weight clamped to the range zero to one
	 */
	public float boneWeight(Player player, String boneName, float partialTick)
	{
		PPlayerAnimationWeight boneWeight = this.boneWeights.getOrDefault(boneName, PPlayerAnimationWeight.FULL);
		return Math.clamp(boneWeight.weight(player, partialTick), 0.0f, 1.0f);
	}

	/**
	 * Explicit mesh roots; an empty set enables automatic discovery.
	 *
	 * @return the explicit mesh roots, with an empty set selecting automatic discovery
	 */
	public Set<String> meshAttachmentRoots()
	{
		return this.meshAttachmentRoots;
	}

	/**
	 * Returns the anchors.
	 *
	 * @return the anchors
	 */
	public Map<PPlayerAnimationAnchor, String> anchors()
	{
		return this.anchors;
	}

	/**
	 * Returns the first person settings.
	 *
	 * @return the first person settings
	 */
	public PPlayerFirstPersonSettings firstPersonSettings()
	{
		return this.firstPersonSettings;
	}

	/**
	 * Decides whether this definition suppresses a physical first-person hand item.
	 *
	 * @return the legacy held-item rendering policy
	 */
	public ItemRenderPolicy itemRenderPolicy()
	{
		return this.itemRenderPolicy;
	}

	/**
	 * Controller used to sample {@link #itemVisibilityPolicy()} , when configured.
	 *
	 * @return the controller name used for item visibility, or {@code null} when unconfigured
	 */
	public @Nullable String itemVisibilityController()
	{
		return this.itemVisibilityController;
	}

	/**
	 * Optional phase-based item visibility policy, combined with {@link #itemRenderPolicy()} .
	 *
	 * @return the item visibility callback, or {@code null} when unconfigured
	 */
	public @Nullable ItemVisibilityPolicy itemVisibilityPolicy()
	{
		return this.itemVisibilityPolicy;
	}

	/**
	 * Returns the deformers.
	 *
	 * @return the deformers
	 */
	public List<PPlayerAnimationDeformer> deformers()
	{
		return this.deformers;
	}
	
	/**
	 * Returns a copy of the root pivot used for animation-space conversion.
	 *
	 * @return a copy of the root pivot used for animation-space conversion
	 */
	public Vector3f rootPivot()
	{
		return new Vector3f(this.rootPivot);
	}

	/**
	 * Returns the ordering priority of the contribution.
	 *
	 * @return the ordering priority of the contribution
	 */
	public int priority()
	{
		return this.priority;
	}

	/**
	 * Returns the crossfade duration.
	 *
	 * @return the crossfade duration
	 */
	public float crossfadeDuration()
	{
		return this.crossfadeDuration;
	}

	/**
	 * Returns the crossfade easing.
	 *
	 * @return the crossfade easing
	 */
	public PPoseEasing crossfadeEasing()
	{
		return this.crossfadeEasing;
	}

	/**
	 * Returns the transition interruption policy.
	 *
	 * @return the transition interruption policy
	 */
	public PTransitionInterruptionPolicy transitionInterruptionPolicy()
	{
		return this.transitionInterruptionPolicy;
	}

	/**
	 * Returns the sync group.
	 *
	 * @return the sync group
	 */
	public String syncGroup()
	{
		return this.syncGroup;
	}

	/**
	 * Declares this definition's animation controllers in a new player animation manager.
	 *
	 * @param registrar the active registration scope for this callback
	 */
	void registerControllers(PAnimationManager.PAnimationRegistrar<PPlayerAnimationInstance> registrar)
	{
		this.controllerRegistrar.register(registrar);
	}

	/**
	 * Populates frame-specific Molang queries for the supplied player animation controller.
	 *
	 * @param player the player whose animation or presentation is being processed
	 * @param instance the instance
	 * @param controller the controller supplying playback and persistent expression state
	 * @param context the Molang context containing current frame and instance values
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 */
	void populateMolangContext(Player player,
	                           PPlayerAnimationInstance instance,
	                           PAnimationController<PPlayerAnimationInstance> controller,
	                           MolangParser.Context context,
	                           float partialTick)
	{
		this.molangContextProvider.populate(player, instance, controller, context, partialTick);
	}

	/**
	 * Declares the animation controllers owned by a player animation instance.
	 */
	@FunctionalInterface
	public interface ControllerRegistrar
	{
		/**
		 * Controller registration callback that adds no controllers.
		 */
		ControllerRegistrar EMPTY = registrar -> {};

		/**
		 * Adds controller factories to the supplied animation manager registrar.
		 *
		 * @param registrar the active registration scope for this callback
		 */
		void register(PAnimationManager.PAnimationRegistrar<PPlayerAnimationInstance> registrar);
	}

	/**
	 * Populates player-specific Molang values before a controller is sampled.
	 */
	@FunctionalInterface
	public interface MolangContextProvider
	{
		/**
		 * Context callback that adds no values.
		 */
		MolangContextProvider EMPTY = (player, instance, controller, context, partialTick) -> {};

		/**
		 * Updates the supplied Molang context with current player and frame values.
		 *
		 * @param player the player whose animation or presentation is being processed
		 * @param instance the instance
		 * @param controller the controller supplying playback and persistent expression state
		 * @param context the Molang context containing current frame and instance values
		 * @param partialTick the interpolation fraction between the previous and current game ticks
		 */
		void populate(Player player,
		              PPlayerAnimationInstance instance,
		              PAnimationController<PPlayerAnimationInstance> controller,
		              MolangParser.Context context,
		              float partialTick);
	}

	/**
	 * Legacy policy that decides whether vanilla first-person rendering should hide a held item.
	 */
	@FunctionalInterface
	public interface ItemRenderPolicy
	{
		/**
		 * Keeps vanilla held-item rendering enabled.
		 */
		ItemRenderPolicy RENDER = (player, hand, stack) -> false;
		/**
		 * Hides vanilla held-item rendering.
		 */
		ItemRenderPolicy HIDE = (player, hand, stack) -> true;

		/**
		 * Returns whether vanilla rendering should hide the item in the supplied hand.
		 *
		 * @param player the player whose animation or presentation is being processed
		 * @param hand the interaction hand being rendered
		 * @param stack the item stack supplying render or animation state
		 * @return whether vanilla rendering should hide the item in the supplied hand
		 */
		boolean hide(LocalPlayer player, InteractionHand hand, ItemStack stack);
	}

	/**
	 * Visibility decision for a held item in animated first-person presentation.
	 */
	public enum ItemVisibility
	{
		/**
		 * Includes the held item in the first-person presentation.
		 */
		VISIBLE,
		/**
		 * Omits the held item from the first-person presentation.
		 */
		HIDDEN
	}

	/**
	 * Samples held-item visibility against a selected animation controller clock.
	 */
	@FunctionalInterface
	public interface ItemVisibilityPolicy
	{
		/**
		 * Always includes the held item.
		 */
		ItemVisibilityPolicy VISIBLE = (player, hand, animationTime, stack) -> ItemVisibility.VISIBLE;
		/**
		 * Always hides the held item.
		 */
		ItemVisibilityPolicy HIDDEN = (player, hand, animationTime, stack) -> ItemVisibility.HIDDEN;

		/**
		 * Selects held-item visibility at the supplied animation playback time.
		 *
		 * @param player the player whose animation or presentation is being processed
		 * @param hand the interaction hand being rendered
		 * @param animationTime the animation playback position supplied to this operation
		 * @param stack the item stack supplying render or animation state
		 * @return the held-item visibility decision at the supplied animation time
		 */
		ItemVisibility visibility(LocalPlayer player, InteractionHand hand, float animationTime, ItemStack stack);
	}

	/**
	 * Builds a player animation definition with explicit model-part bindings and presentation policies.
	 */
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
		private final Set<String> meshAttachmentRoots = new LinkedHashSet<>();
		private final Map<PPlayerAnimationAnchor, String> anchors = new LinkedHashMap<>();
		private PPlayerFirstPersonSettings firstPersonSettings = PPlayerFirstPersonSettings.DISABLED;
		private ItemRenderPolicy itemRenderPolicy = ItemRenderPolicy.RENDER;
		private @Nullable String itemVisibilityController;
		private @Nullable ItemVisibilityPolicy itemVisibilityPolicy;
		private final List<PPlayerAnimationDeformer> deformers = new ArrayList<>();
		private Vector3f rootPivot = new Vector3f();
		private int priority;
		private float crossfadeDuration;
		private PPoseEasing crossfadeEasing = PPoseEasing.LINEAR;
		private PTransitionInterruptionPolicy transitionInterruptionPolicy = PTransitionInterruptionPolicy.FROM_CURRENT;
		private String syncGroup = "";
		private ControllerRegistrar controllerRegistrar = ControllerRegistrar.EMPTY;
		private MolangContextProvider molangContextProvider = MolangContextProvider.EMPTY;

		private Builder(PModelData modelData)
		{
			this.modelData = Objects.requireNonNull(modelData);
		}

		/**
		 * Sets the predicate that activates this player animation.
		 *
		 * @param predicate the predicate
		 * @return this builder
		 */
		public Builder when(Predicate<Player> predicate)
		{
			this.predicate = Objects.requireNonNull(predicate);
			return this;
		}
		
		/**
		 * Binds a vanilla player model part to a named animation bone.
		 *
		 * @param part the player model part receiving the contribution
		 * @param boneName the animation bone name
		 * @return this builder
		 */
		public Builder bind(PPlayerPart part, String boneName)
		{
			if (boneName == null || boneName.isBlank())
				throw new IllegalArgumentException("Player animation bone name cannot be blank");
			this.bindings.put(Objects.requireNonNull(part), boneName);
			return this;
		}
		
		/**
		 * Sets the player parts eligible to receive this animation.
		 *
		 * @param parts the player parts included by the mask
		 * @return this builder
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
		 * Sets the player parts eligible to receive this animation.
		 *
		 * @param mask the mask selecting participating bones or player parts
		 * @return this builder
		 */
		public Builder mask(PPlayerAnimationMask mask)
		{
			this.mask.clear();
			this.partMask = Objects.requireNonNull(mask);
			return this;
		}

		/**
		 * Selects the transform-space blending policy.
		 *
		 * @param blendMode the blend mode
		 * @return this builder
		 */
		public Builder blendMode(PPlayerAnimationBlendMode blendMode)
		{
			this.blendMode = Objects.requireNonNull(blendMode);
			return this;
		}

		/**
		 * Sets the overall constant or dynamically sampled animation weight.
		 *
		 * @param weight the contribution weight
		 * @return this builder
		 */
		public Builder weight(float weight)
		{
			return weight((player, partialTick) -> weight);
		}

		/**
		 * Sets the overall constant or dynamically sampled animation weight.
		 *
		 * @param weight the contribution weight
		 * @return this builder
		 */
		public Builder weight(PPlayerAnimationWeight weight)
		{
			this.weight = Objects.requireNonNull(weight);
			return this;
		}

		/**
		 * Sets the weight provider for one player part.
		 *
		 * @param part the player model part receiving the contribution
		 * @param weight the contribution weight
		 * @return this builder
		 */
		public Builder partWeight(PPlayerPart part, float weight)
		{
			return partWeight(part, (player, partialTick) -> weight);
		}

		/**
		 * Sets the weight provider for one player part.
		 *
		 * @param part the player model part receiving the contribution
		 * @param weight the contribution weight
		 * @return this builder
		 */
		public Builder partWeight(PPlayerPart part, PPlayerAnimationWeight weight)
		{
			this.partWeights.put(Objects.requireNonNull(part), Objects.requireNonNull(weight));
			return this;
		}

		/**
		 * Sets the weight provider for one animation bone.
		 *
		 * @param boneName the animation bone name
		 * @param weight the contribution weight
		 * @return this builder
		 */
		public Builder boneWeight(String boneName, float weight)
		{
			return boneWeight(boneName, (player, partialTick) -> weight);
		}

		/**
		 * Sets the weight provider for one animation bone.
		 *
		 * @param boneName the animation bone name
		 * @param weight the contribution weight
		 * @return this builder
		 */
		public Builder boneWeight(String boneName, PPlayerAnimationWeight weight)
		{
			if (boneName == null || boneName.isBlank())
				throw new IllegalArgumentException("Player animation bone name cannot be blank");
			this.boneWeights.put(boneName, Objects.requireNonNull(weight));
			return this;
		}

		/**
		 * Marks the model subtree rooted at {@code boneName} for player-mesh attachment rendering.
		 *
		 * @param boneName the animation bone name
		 * @return this builder
		 */
		public Builder meshAttachment(String boneName)
		{
			if (boneName == null || boneName.isBlank())
				throw new IllegalArgumentException("Player mesh attachment bone name cannot be blank");
			this.meshAttachmentRoots.add(boneName);
			return this;
		}

		/**
		 * Binds an action or attachment anchor to a named animation bone.
		 *
		 * @param anchor the anchor
		 * @param boneName the animation bone name
		 * @return this builder
		 */
		public Builder anchor(PPlayerAnimationAnchor anchor, String boneName)
		{
			if (boneName == null || boneName.isBlank())
				throw new IllegalArgumentException("Player animation anchor bone name cannot be blank");
			this.anchors.put(Objects.requireNonNull(anchor), boneName);
			return this;
		}

		/**
		 * Sets first-person activation, transition and camera behavior.
		 *
		 * @param settings the settings
		 * @return this builder
		 */
		public Builder firstPerson(PPlayerFirstPersonSettings settings)
		{
			this.firstPersonSettings = Objects.requireNonNull(settings);
			return this;
		}

		/**
		 * Sets the legacy vanilla held-item visibility callback.
		 *
		 * @param itemRenderPolicy the item render policy
		 * @return this builder
		 */
		public Builder itemRenderPolicy(ItemRenderPolicy itemRenderPolicy)
		{
			this.itemRenderPolicy = Objects.requireNonNull(itemRenderPolicy);
			return this;
		}

		/**
		 * Controls per-hand visibility at a named controller's interpolated time in seconds.
		 *
		 * @param controllerName the registered controller name
		 * @param itemVisibilityPolicy the item visibility policy
		 * @return this builder
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
		 * Adds an animated deformation stack for one player model part.
		 *
		 * @param part the player model part receiving the contribution
		 * @param stack the item stack supplying render or animation state
		 * @param values the values
		 * @return this builder
		 */
		public Builder deform(PPlayerPart part,
		                      PDeformerStack stack,
		                      PPlayerAnimationDeformerValueSource values)
		{
			this.deformers.add(new PPlayerAnimationDeformer(part, stack, values));
			return this;
		}
		
		/**
		 * Sets the root pivot used when converting animation transforms to player space.
		 *
		 * @param rootPivot the root pivot
		 * @return this builder
		 */
		public Builder rootPivot(Vector3f rootPivot)
		{
			this.rootPivot = new Vector3f(Objects.requireNonNull(rootPivot));
			return this;
		}

		/**
		 * Sets the root pivot used when converting animation transforms to player space.
		 *
		 * @param x the x component or coordinate
		 * @param y the y component or coordinate
		 * @param z the z component or coordinate
		 * @return this builder
		 */
		public Builder rootPivot(float x, float y, float z)
		{
			return rootPivot(new Vector3f(x, y, z));
		}
		
		/**
		 * Sets the order in which this animation is combined with other definitions.
		 *
		 * @param priority the ordering priority of the contribution
		 * @return this builder
		 */
		public Builder priority(int priority)
		{
			this.priority = priority;
			return this;
		}

		/**
		 * Configures activation crossfades and their interruption behavior.
		 *
		 * @param duration the duration of the effect or transition in game ticks
		 * @param easing the crossfade progress easing curve
		 * @param interruptionPolicy the policy for interrupting an active crossfade
		 * @return this builder
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
		 * Sets the group used to synchronize cyclic controller playback.
		 *
		 * @param syncGroup the sync group
		 * @return this builder
		 */
		public Builder syncGroup(String syncGroup)
		{
			this.syncGroup = syncGroup == null ? "" : syncGroup;
			return this;
		}

		/**
		 * Sets the callback that declares this definition's controllers.
		 *
		 * @param controllerRegistrar the controller registrar
		 * @return this builder
		 */
		public Builder controllers(ControllerRegistrar controllerRegistrar)
		{
			this.controllerRegistrar = Objects.requireNonNull(controllerRegistrar);
			return this;
		}

		/**
		 * Sets the callback that populates player-specific Molang values.
		 *
		 * @param provider the provider
		 * @return this builder
		 */
		public Builder populateMolangContext(MolangContextProvider provider)
		{
			this.molangContextProvider = Objects.requireNonNull(provider);
			return this;
		}

		/**
		 * Creates an immutable player animation definition from this configuration.
		 *
		 * @return the configured player animation definition
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
