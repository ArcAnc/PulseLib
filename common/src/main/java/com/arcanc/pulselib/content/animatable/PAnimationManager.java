/**
 * @author ArcAnc
 * Created at: 24.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable;


import com.arcanc.pulselib.content.model.animation.PAnimationGraph;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Part of this code copied from Geckolib: <a href="https://github.com/bernie-g/geckolib/blob/1.21.1/common/src/main/java/software/bernie/geckolib/animatable/instance/AnimatableInstanceCache.java">AnimatableInstanceCache</a>
 * <p>Stop crying, Tslat!</p>
 * <p>Modified by ArcAnc</p>
 * <p></p>
 * Owns the named controllers and bound model for one animatable identity.
 * @param <T> the animatable owner type
 */
public class PAnimationManager<T extends PAnimatable<T>>
{
	/**
	 * Maximum idle interval before an unused animation manager can be removed.
	 */
	protected static final long THRESHOLD_TIME = 5_000;

	/**
	 * Animatable owner used to populate controller state.
	 */
	protected final T animatable;
	/**
	 * Returns the owner identity associated with this manager.
	 */
	protected final AnimManagerKey key;
	/**
	 * Model bound for animation evaluation, or {@code null} before a model has been assigned.
	 */
	protected @Nullable PBakedModel model;
	private boolean initialized;
	private boolean initializing;
	/**
	 * Raw controller state-handler factories indexed by controller name.
	 */
	protected final Map<String, Supplier<PAnimationController.StateHandler<T>>> factories = new Object2ObjectArrayMap<>();
	/**
	 * Animation graph factories indexed by controller name.
	 */
	protected final Map<String, Supplier<PAnimationGraph>> graphFactories = new Object2ObjectArrayMap<>();
	/**
	 * Instantiated controllers indexed by registration name.
	 */
	protected final Map<String, PAnimationController<T>> controllers = new Object2ObjectArrayMap<>();

	/**
	 * Returns the controllers registered for this animation manager.
	 *
	 * @return the controllers registered for this animation manager
	 */
	public Map<String, PAnimationController<T>> getControllers()
	{
		initializeControllers();
		return this.controllers;
	}

	/**
	 * Returns the animatable owner whose state this manager controls.
	 *
	 * @return the animatable owner whose state this manager controls
	 */
	public T getAnimatable()
	{
		return this.animatable;
	}

	/**
	 * Creates owner animation storage while deferring controller registration until initialization.
	 *
	 * @param animatable the owner whose animation state is being processed
	 */
	public PAnimationManager(final T animatable)
	{
		this(animatable, AnimManagerKey.ofObject(animatable));
	}

	/**
	 * Creates owner animation storage while deferring controller registration until initialization.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param key the key
	 */
	public PAnimationManager(final T animatable, final AnimManagerKey key)
	{
		this.animatable = Objects.requireNonNull(animatable);
		this.key = Objects.requireNonNull(key);
	}

	/**
	 * Returns the initialized manager for a rendering context.
	 * Instance-owned managers reuse this object regardless of the supplied context key.
	 * Singleton managers override this method to resolve the owner's keyed cache.
	 * Call after the owner's construction, not from its field initializers.
	 * @param key the animation context key.
	 * @return the initialized manager for that context.
	 */
	public PAnimationManager<T> getManager(AnimManagerKey key)
	{
		Objects.requireNonNull(key);
		return initializeControllers();
	}

	/**
	 * Registers and creates controllers on first use, after owner construction.
	 * Constructors deliberately never call the owner's overridable registration method.
	 * @return this manager with its controllers ready for use.
	 */
	public final synchronized PAnimationManager<T> initializeControllers()
	{
		if (this.initialized)
			return this;
		if (this.initializing)
			throw new IllegalStateException("Recursive animation controller initialization");
		this.initializing = true;
		try
		{
			PAnimationRegistrar<T> registrar = new PAnimationRegistrar<>(new ObjectArrayList<>());
			this.animatable.registerAnimationControllers(registrar);
			Map<String, PAnimationController<T>> created = new Object2ObjectArrayMap<>();
			registrar.entries.forEach(entry -> created.put(entry.name(),
					new PAnimationController<>(entry.name(), entry.factory().get(), randomSeedFor(entry.name()))));
			registrar.graphEntries.forEach(entry -> created.put(entry.name(),
					new PAnimationController<>(entry.name(), entry.factory().get())));
			registrar.entries.forEach(entry -> this.factories.put(entry.name(), entry.factory()));
			registrar.graphEntries.forEach(entry -> this.graphFactories.put(entry.name(), entry.factory()));
			this.controllers.putAll(created);
			this.initialized = true;
			return this;
		}
		finally
		{
			this.initializing = false;
		}
	}

	/** Creates the controllers once; retained for subclasses using the original API. */
	protected final void createControllers()
	{
		initializeControllers();
	}

	/**
	 * Creates a deterministic random source for one controller of this animation owner.
	 * @param controllerName the registered controller name.
	 * @return the controller random source.
	 */
	private long randomSeedFor(String controllerName)
	{
		long controllerSeed = (long)controllerName.hashCode() * 0x9E3779B97F4A7C15L;
		return this.key.key() ^ controllerSeed;
	}

	/**
	 * Returns the owner identity associated with this manager.
	 *
	 * @return the owner identity associated with this manager
	 */
	public AnimManagerKey key()
	{
		return this.key;
	}

	/**
	 * Binds the model.
	 * @param model the model to use.
	 */
	public void bindModel(PBakedModel model)
	{
		if (model != this.model)
			this.model = model;
	}

	/**
	 * Advances each initialized controller for the current animatable tick.
	 */
	public void tick()
	{
		initializeControllers();
		for (PAnimationController<T> controller : this.controllers.values())
			controller.tick(this.animatable, 1, this.model, this.controllers.values());
	}

	/**
	 * Collects controller state-handler and graph factories for a newly created animation manager.
	 *
	 * @param <T> the animatable owner type
	 * @param entries the collected controller factory declarations
	 * @param graphEntries the graph entries
	 */
	public record PAnimationRegistrar<T extends PAnimatable<T>>(List<Entry<T>> entries, List<GraphEntry> graphEntries)
	{
		/**
		 * Creates a controller registrar backed by the supplied raw-controller declarations.
		 *
		 * @param entries the collected controller factory declarations
		 */
		public PAnimationRegistrar(List<Entry<T>> entries)
		{
			this(entries, new ObjectArrayList<>());
		}

		/**
		 * Registers a state-handler factory for a raw animation controller.
		 *
		 * @param factory the factory creating an independent controller state handler or graph
		 * @return this registrar
		 */
		public PAnimationRegistrar<T> add(Supplier<PAnimationController.StateHandler<T>> factory)
		{
			return add("default", factory);
		}

		/**
		 * Registers a state-handler factory for a raw animation controller.
		 *
		 * @param name the registered controller name
		 * @param factory the factory creating an independent controller state handler or graph
		 * @return this registrar
		 */
		public PAnimationRegistrar<T> add(String name, Supplier<PAnimationController.StateHandler<T>> factory)
		{
			this.entries.add(new Entry<>(name, factory));
			return this;
		}

		/**
		 * Registers an animation graph or graph factory for a named controller.
		 *
		 * @param graph the immutable animation state graph
		 * @return this registrar
		 */
		public PAnimationRegistrar<T> addGraph(PAnimationGraph graph)
		{
			return addGraph("default", () -> graph);
		}

		/**
		 * Registers an animation graph or graph factory for a named controller.
		 *
		 * @param name the registered controller name
		 * @param graph the immutable animation state graph
		 * @return this registrar
		 */
		public PAnimationRegistrar<T> addGraph(String name, PAnimationGraph graph)
		{
			return addGraph(name, () -> graph);
		}

		/**
		 * Registers an animation graph or graph factory for a named controller.
		 *
		 * @param name the registered controller name
		 * @param factory the factory creating an independent controller state handler or graph
		 * @return this registrar
		 */
		public PAnimationRegistrar<T> addGraph(String name, Supplier<PAnimationGraph> factory)
		{
			this.graphEntries.add(new GraphEntry(name, factory));
			return this;
		}

		/**
		 * Named factory declaration for a raw animation controller.
		 *
		 * @param <T> the animatable owner type
		 * @param name the registered controller name
		 * @param factory the factory creating independent runtime state for this registration
		 */
		public record Entry<T extends PAnimatable<T>>(String name, Supplier<PAnimationController.StateHandler<T>> factory)
		{

		}

		/**
		 * Named factory declaration for an animation graph controller.
		 *
		 * @param name the registered controller name
		 * @param factory the factory creating independent runtime state for this registration
		 */
		public record GraphEntry(String name, Supplier<PAnimationGraph> factory)
		{
		}
	}
}
