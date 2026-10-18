/**
 * @author ArcAnc
 * Created at: 15.03.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;

import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.renderer.legacy.GlDrawExecutor;
import com.arcanc.pulselib.content.renderer.legacy.GlRenderState;
import com.arcanc.pulselib.content.renderer.legacy.GlResourceRegistry;
import com.arcanc.pulselib.content.renderer.legacy.McLegacyGlHostBridge;
import com.arcanc.pulselib.content.renderer.plan.*;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.List;

/**
 * Collects model draw submissions and flushes them at world stages or isolated local render boundaries.
 */
public final class PRenderQueue
{
	private static final GlResourceRegistry RESOURCES = new GlResourceRegistry();
	private static final PFrameCompiler<RenderStage> COMPILER = new PFrameCompiler<>();
	private static final GlDrawExecutor EXECUTOR = new GlDrawExecutor(RESOURCES);
	private static final McLegacyGlHostBridge HOST = new McLegacyGlHostBridge();

	private PRenderQueue()
	{
	}

	/**
	 * Queues block-entity geometry for the opaque world block stage.
	 *
	 * @param renderType the render type
	 * @param geometry the dynamically updated geometry
	 * @param data the data
	 */
	public static void submitBlockEntityMesh(RenderType renderType, PDynamicGeometry geometry, PInstanceHeader data)
	{
		submit(RenderStage.SOLID_BLOCKS, renderType, geometry, data);
	}

	/**
	 * Queues block-entity geometry for the opaque world block stage.
	 *
	 * @param renderType the render type
	 * @param geometry the immutable indexed mesh geometry
	 * @param data the data
	 */
	public static void submitBlockEntityMesh(RenderType renderType, PGeometryData geometry, PInstanceHeader data)
	{
		submit(RenderStage.SOLID_BLOCKS, renderType, geometry, data);
	}

	/**
	 * Queues block-entity geometry for translucent world rendering.
	 *
	 * @param renderType the render type
	 * @param geometry the dynamically updated geometry
	 * @param data the data
	 */
	public static void submitBlockEntityTranslucentMesh(RenderType renderType, PDynamicGeometry geometry, PInstanceHeader data)
	{
		submitTranslucent(RenderStage.TRANSLUCENT_BLOCKS, renderType, geometry, data);
	}

	/**
	 * Queues block-entity geometry for translucent world rendering.
	 *
	 * @param renderType the render type
	 * @param geometry the immutable indexed mesh geometry
	 * @param data the data
	 */
	public static void submitBlockEntityTranslucentMesh(RenderType renderType, PGeometryData geometry, PInstanceHeader data)
	{
		submitTranslucent(RenderStage.TRANSLUCENT_BLOCKS, renderType, geometry, data);
	}

	/**
	 * Queues item geometry in the stage selected by its display context.
	 *
	 * @param context the context
	 * @param renderType the render type
	 * @param geometry the dynamically updated geometry
	 * @param data the data
	 */
	public static void submitItem(ItemDisplayContext context, RenderType renderType, PDynamicGeometry geometry, PInstanceHeader data)
	{
		RenderStage stage = stage(context);
		if (stage != RenderStage.GUI)
			submit(stage, renderType, geometry, data);
	}

	/**
	 * Queues item geometry in the stage selected by its display context.
	 *
	 * @param context the context
	 * @param renderType the render type
	 * @param geometry the immutable indexed mesh geometry
	 * @param data the data
	 */
	public static void submitItem(ItemDisplayContext context, RenderType renderType, PGeometryData geometry, PInstanceHeader data)
	{
		RenderStage stage = stage(context);
		if (stage != RenderStage.GUI)
			submit(stage, renderType, geometry, data, PRenderTypes.isTranslucent(renderType));
	}

	/**
	 * Queues entity geometry in the stage selected by the current render context.
	 *
	 * @param renderType the render type
	 * @param geometry the dynamically updated geometry
	 * @param data the data
	 */
	public static void submitEntityMesh(RenderType renderType, PDynamicGeometry geometry, PInstanceHeader data)
	{
		submit(PRenderContexts.entityStage(), renderType, geometry, data);
	}

	/**
	 * Queues entity geometry in the stage selected by the current render context.
	 *
	 * @param renderType the render type
	 * @param geometry the immutable indexed mesh geometry
	 * @param data the data
	 */
	public static void submitEntityMesh(RenderType renderType, PGeometryData geometry, PInstanceHeader data)
	{
		submit(PRenderContexts.entityStage(), renderType, geometry, data);
	}

	/**
	 * Queues geometry for the depth buffer produced by Minecraft's hand pass.
	 *
	 * @param renderType the render type
	 * @param geometry the immutable indexed mesh geometry
	 * @param data the data
	 */
	public static void submitFirstPersonMesh(RenderType renderType, PGeometryData geometry, PInstanceHeader data)
	{
		submit(RenderStage.FIRST_PERSON, renderType, geometry, data);
	}

	/**
	 * Queues one geometry instance for the supplied render stage and pipeline.
	 *
	 * @param stage the stage
	 * @param type the vanilla render pipeline
	 * @param geometry the dynamically updated geometry
	 * @param data the data
	 */
	public static void submit(RenderStage stage, RenderType type, PDynamicGeometry geometry, PInstanceHeader data)
	{
		submit(stage, type, geometry, data, PRenderTypes.isTranslucent(type));
	}

	/**
	 * Queues one geometry instance for the supplied render stage and pipeline.
	 *
	 * @param stage the stage
	 * @param type the vanilla render pipeline
	 * @param geometry the immutable indexed mesh geometry
	 * @param data the data
	 */
	public static void submit(RenderStage stage, RenderType type, PGeometryData geometry, PInstanceHeader data)
	{
		submit(stage, type, geometry, data, PRenderTypes.isTranslucent(type));
	}

	private static void submit(RenderStage stage,
	                           RenderType type,
	                           PDynamicGeometry geometry,
	                           PInstanceHeader data,
	                           boolean transparent)
	{
		PPipelineHandle pipeline = RESOURCES.pipeline(type);
		PMeshHandle mesh = RESOURCES.dynamic(geometry);
		int indexCount = RESOURCES.indexCount(geometry);
		compiler().submit(stage, pipeline, mesh, RESOURCES.resolveCommand(mesh, indexCount, 1), data, transparent);
	}

	private static void submit(RenderStage stage,
	                           RenderType type,
	                           PGeometryData geometry,
	                           PInstanceHeader data,
	                           boolean transparent)
	{
		PPipelineHandle pipeline = RESOURCES.pipeline(type);
		PMeshHandle mesh = RESOURCES.geometry(geometry);
		compiler().submit(stage, pipeline, mesh, RESOURCES.resolveCommand(mesh, geometry.indexCount(), 1), data, transparent);
	}

	private static PFrameCompiler<RenderStage> compiler()
	{
		PRenderContexts.Scope local = PRenderContexts.localScope();
		return local == null ? COMPILER : local.compiler;
	}

	/**
	 * Compiles and executes the isolated local queue against the captured frame and render state.
	 *
	 * @param compiler the compiler
	 * @param frame the frame matrices or sampled animation values used by this operation
	 * @param state the state
	 */
	static void flushLocal(PFrameCompiler<RenderStage> compiler, PRenderFrame frame, GlRenderState state)
	{
		try
		{
			EXECUTOR.executeLocal(compiler.compileAll(ignored -> false), frame, state);
		}
		finally
		{
			compiler.clear();
		}
	}

	/**
	 * Resets frame-local streams after an isolated local render completes.
	 */
	static void finishLocalFrame()
	{
		if (COMPILER.isEmpty())
			PGpuDeformerBuffers.finishFrame();
	}

	/** Drops unconsumed world commands, including when world rendering throws. */
	public static void endWorldFrame()
	{
		COMPILER.discard(List.of(RenderStage.ENTITIES, RenderStage.SOLID_BLOCKS, RenderStage.TRANSLUCENT_BLOCKS));
	}

	private static void submitTranslucent(RenderStage stage, RenderType type, PDynamicGeometry geometry, PInstanceHeader data)
	{
		submit(stage, type, geometry, data, true);
	}

	private static void submitTranslucent(RenderStage stage, RenderType type, PGeometryData geometry, PInstanceHeader data)
	{
		submit(stage, type, geometry, data, true);
	}

	private static RenderStage stage(ItemDisplayContext context)
	{
		return switch (context)
		{
			case GUI -> RenderStage.GUI;
			case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> RenderStage.FIRST_PERSON;
			case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND, HEAD -> RenderStage.ENTITIES;
			case GROUND, FIXED, NONE -> RenderStage.TRANSLUCENT_BLOCKS;
		};
	}

	/**
	 * Compiles and executes queued submissions for one render stage.
	 *
	 * @param stage the stage
	 */
	public static void flush(RenderStage stage)
	{
		flush(stage, Minecraft.getInstance().getMainRenderTarget());
	}

	/**
	 * Compiles and executes queued submissions for one render stage.
	 *
	 * @param stage the stage
	 * @param depthSource the render target supplying opaque scene depth
	 */
	public static void flush(RenderStage stage, RenderTarget depthSource)
	{
		PRenderPlan plan = COMPILER.compile(stage, PRenderQueue :: canBatchWithOit);
		EXECUTOR.execute(plan, HOST.captureFrame(), depthSource);
	}

	/**
	 * Writes world depth before vanilla starts its translucent terrain pass.
	 *
	 * @param stage the stage
	 */
	public static void flushOpaque(RenderStage stage)
	{
		EXECUTOR.execute(COMPILER.compileOpaque(stage), HOST.captureFrame());
	}

	/**
	 * Compiles the supplied stages together so their transparent submissions share ordering.
	 *
	 * @param stages the stages
	 */
	public static void flushCombined(RenderStage... stages)
	{
		PRenderPlan plan = COMPILER.compileCombined(List.of(stages), PRenderQueue :: canBatchWithOit);
		EXECUTOR.execute(plan, HOST.captureFrame());
	}

	/**
	 * Composites accumulated PulseLib translucent geometry into the destination target.
	 *
	 * @param destination the mutable object receiving the result
	 */
	public static void compositeTranslucency(RenderTarget destination)
	{
		EXECUTOR.compositeOit(destination);
	}

	/**
	 * Inserts OIT layers after the vanilla Fabulous transparency chain.
	 * Targets are ordered as terrain, items, particles, weather and clouds.
	 *
	 * @param destination the mutable object receiving the result
	 * @param targets the vanilla Fabulous transparency targets
	 */
	public static void compositeFabulousTranslucency(RenderTarget destination, List<RenderTarget> targets)
	{
		EXECUTOR.compositeFabulousOit(destination, targets);
	}

	/**
	 * Discards queued submissions and releases renderer-owned GPU resources.
	 */
	public static void cleanup()
	{
		COMPILER.clear();
		EXECUTOR.cleanup();
		RESOURCES.clear();
	}

	private static boolean canBatchWithOit(PPipelineHandle pipeline)
	{
		return PRenderTypes.RenderTypeProvider.usesOit(RESOURCES.pipeline(pipeline));
	}

	/**
	 * Named render-queue partition used to defer submissions to the appropriate rendering boundary.
	 */
	public static final class RenderStage
	{
		/**
		 * Opaque block-entity submissions flushed before translucent terrain.
		 */
		public static final RenderStage SOLID_BLOCKS = new RenderStage("solid_blocks");
		/**
		 * Translucent block-entity submissions.
		 */
		public static final RenderStage TRANSLUCENT_BLOCKS = new RenderStage("translucent_blocks");
		/**
		 * World entity model submissions.
		 */
		public static final RenderStage ENTITIES = new RenderStage("entities");
		/**
		 * First-person geometry submissions.
		 */
		public static final RenderStage FIRST_PERSON = new RenderStage("first_person");
		/**
		 * Isolated GUI geometry submissions.
		 */
		public static final RenderStage GUI = new RenderStage("gui");

		private final String name;

		/**
		 * Creates a queue partition identified by the supplied stage name.
		 *
		 * @param name the name
		 */
		public RenderStage(String name)
		{
			this.name = name;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode()
		{
			return this.name.hashCode();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object object)
		{
			return object instanceof RenderStage other && this.name.equals(other.name);
		}
	}
}
