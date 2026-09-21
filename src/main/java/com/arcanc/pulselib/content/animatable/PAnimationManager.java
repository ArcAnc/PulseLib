/**
 * @author ArcAnc
 * Created at: 24.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable;


import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.animation.PAnimationGraph;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Part of this code copied from Geckolib: <a href="https://github.com/bernie-g/geckolib/blob/1.21.1/common/src/main/java/software/bernie/geckolib/animatable/instance/AnimatableInstanceCache.java">AnimatableInstanceCache</a>
 * <p>Stop crying, Tslat!</p>
 * <p>Modified by ArcAnc</p>
 */
public class PAnimationManager<T extends PAnimatable<T>>
{
	protected static final long THRESHOLD_TIME = 5_000;
	
	protected final T animatable;
	protected final AnimManagerKey key;
	protected PBakedModel model;
	protected final Map<String, Supplier<PAnimationController.StateHandler<T>>> factories = new Object2ObjectArrayMap<>();
	protected final Map<String, Supplier<PAnimationGraph>> graphFactories = new Object2ObjectArrayMap<>();
	protected final Map<String, PAnimationController<T>> controllers = new Object2ObjectArrayMap<>();
	
	/**
	 * Returns the controllers.
	 * @return the value produced by this operation.
	 */
	public Map<String, PAnimationController<T>> getControllers()
	{
		return this.controllers;
	}
	
	/**
	 * Returns the animatable.
	 * @return the value produced by this operation.
	 */
	public T getAnimatable()
	{
		return this.animatable;
	}
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param animatable the animatable to use.
	 */
	public PAnimationManager(final T animatable)
	{
		this(animatable, AnimManagerKey.ofObject(animatable));
	}

	/**
	 * Creates an instance of the enclosing type.
	 * @param animatable the animatable to use.
	 * @param key the key to use.
	 */
	public PAnimationManager(final T animatable, final AnimManagerKey key)
	{
		this.animatable = animatable;
		this.key = key;
		
		PAnimationRegistrar<T> registrar = new PAnimationRegistrar<>(new ObjectArrayList<>());
		
		this.animatable.registerAnimationControllers(registrar);
		
		registrar.entries.forEach(entry -> this.factories.put(entry.name(), entry.factory()));
		registrar.graphEntries.forEach(entry -> this.graphFactories.put(entry.name(), entry.factory()));
	}

	/**
	 * Creates the controllers.
	 */
	protected final void createControllers()
	{
		this.factories.forEach((name, supplier) -> this.controllers.put(name,
				new PAnimationController<>(name, supplier.get())));
		this.graphFactories.forEach((name, supplier) -> this.controllers.put(name,
				new PAnimationController<>(name, supplier.get())));
	}

	/**
	 * Performs the key operation.
	 * @return the value produced by this operation.
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
	 * Performs the tick operation.
	 */
	public void tick()
	{
		for (PAnimationController<T> controller : this.controllers.values())
			controller.tick(this.animatable, 1, this.model, this.controllers.values());
	}
	
/**
 * Immutable value object representing animation registrar.
 */
	public record PAnimationRegistrar<T extends PAnimatable<T>>(List<Entry<T>> entries, List<GraphEntry> graphEntries)
	{
		/**
		 * Creates an instance of the enclosing type.
		 * @param entries the entries to use.
		 */
		public PAnimationRegistrar(List<Entry<T>> entries)
		{
			this(entries, new ObjectArrayList<>());
		}

		/**
		 * Performs the add operation.
		 * @param factory the factory to use.
		 * @return the value produced by this operation.
		 */
		public PAnimationRegistrar<T> add(Supplier<PAnimationController.StateHandler<T>> factory)
		{
			return add("default", factory);
		}
		
		/**
		 * Performs the add operation.
		 * @param name the name to use.
		 * @param factory the factory to use.
		 * @return the value produced by this operation.
		 */
		public PAnimationRegistrar<T> add(String name, Supplier<PAnimationController.StateHandler<T>> factory)
		{
			this.entries.add(new Entry<>(name, factory));
			return this;
		}

		/**
		 * Adds the graph.
		 * @param graph the graph to use.
		 * @return the value produced by this operation.
		 */
		public PAnimationRegistrar<T> addGraph(PAnimationGraph graph)
		{
			return addGraph("default", () -> graph);
		}

		/**
		 * Adds the graph.
		 * @param name the name to use.
		 * @param graph the graph to use.
		 * @return the value produced by this operation.
		 */
		public PAnimationRegistrar<T> addGraph(String name, PAnimationGraph graph)
		{
			return addGraph(name, () -> graph);
		}

		/**
		 * Adds the graph.
		 * @param name the name to use.
		 * @param factory the factory to use.
		 * @return the value produced by this operation.
		 */
		public PAnimationRegistrar<T> addGraph(String name, Supplier<PAnimationGraph> factory)
		{
			this.graphEntries.add(new GraphEntry(name, factory));
			return this;
		}
		
/**
 * Immutable value object representing entry.
 */
		public record Entry<T extends PAnimatable<T>>(String name, Supplier<PAnimationController.StateHandler<T>> factory)
		{
		
		}

/**
 * Immutable value object representing graph entry.
 */
		public record GraphEntry(String name, Supplier<PAnimationGraph> factory)
		{
		}
	}
}
