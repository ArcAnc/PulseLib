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

public final class PRenderQueue
{
	private static final GlResourceRegistry RESOURCES = new GlResourceRegistry();
	private static final PFrameCompiler<RenderStage> COMPILER = new PFrameCompiler<>();
	private static final GlDrawExecutor EXECUTOR = new GlDrawExecutor(RESOURCES);
	private static final McLegacyGlHostBridge HOST = new McLegacyGlHostBridge();

	private PRenderQueue()
	{
	}

	public static void submitBlockEntityMesh(RenderType renderType, PDynamicGeometry geometry, PInstanceHeader data)
	{
		submit(RenderStage.SOLID_BLOCKS, renderType, geometry, data);
	}

	public static void submitBlockEntityMesh(RenderType renderType, PGeometryData geometry, PInstanceHeader data)
	{
		submit(RenderStage.SOLID_BLOCKS, renderType, geometry, data);
	}

	public static void submitBlockEntityTranslucentMesh(RenderType renderType, PDynamicGeometry geometry, PInstanceHeader data)
	{
		submitTranslucent(RenderStage.TRANSLUCENT_BLOCKS, renderType, geometry, data);
	}

	public static void submitBlockEntityTranslucentMesh(RenderType renderType, PGeometryData geometry, PInstanceHeader data)
	{
		submitTranslucent(RenderStage.TRANSLUCENT_BLOCKS, renderType, geometry, data);
	}

	public static void submitItem(ItemDisplayContext context, RenderType renderType, PDynamicGeometry geometry, PInstanceHeader data)
	{
		RenderStage stage = stage(context);
		if (stage != RenderStage.GUI)
			submit(stage, renderType, geometry, data);
	}

	public static void submitItem(ItemDisplayContext context, RenderType renderType, PGeometryData geometry, PInstanceHeader data)
	{
		RenderStage stage = stage(context);
		if (stage != RenderStage.GUI)
			submit(stage, renderType, geometry, data, PRenderTypes.isTranslucent(renderType));
	}

	public static void submitEntityMesh(RenderType renderType, PDynamicGeometry geometry, PInstanceHeader data)
	{
		submit(PRenderContexts.entityStage(), renderType, geometry, data);
	}

	public static void submitEntityMesh(RenderType renderType, PGeometryData geometry, PInstanceHeader data)
	{
		submit(PRenderContexts.entityStage(), renderType, geometry, data);
	}

	/** Queues geometry for the depth buffer produced by Minecraft's hand pass. */
	public static void submitFirstPersonMesh(RenderType renderType, PGeometryData geometry, PInstanceHeader data)
	{
		submit(RenderStage.FIRST_PERSON, renderType, geometry, data);
	}

	public static void submit(RenderStage stage, RenderType type, PDynamicGeometry geometry, PInstanceHeader data)
	{
		submit(stage, type, geometry, data, PRenderTypes.isTranslucent(type));
	}

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

	public static void flush(RenderStage stage)
	{
		flush(stage, Minecraft.getInstance().getMainRenderTarget());
	}

	public static void flush(RenderStage stage, RenderTarget depthSource)
	{
		PRenderPlan plan = COMPILER.compile(stage, PRenderQueue :: canBatchWithOit);
		EXECUTOR.execute(plan, HOST.captureFrame(), depthSource);
	}

	/** Writes world depth before vanilla starts its translucent terrain pass. */
	public static void flushOpaque(RenderStage stage)
	{
		EXECUTOR.execute(COMPILER.compileOpaque(stage), HOST.captureFrame());
	}

	public static void flushCombined(RenderStage... stages)
	{
		PRenderPlan plan = COMPILER.compileCombined(List.of(stages), PRenderQueue :: canBatchWithOit);
		EXECUTOR.execute(plan, HOST.captureFrame());
	}

	public static void compositeTranslucency(RenderTarget destination)
	{
		EXECUTOR.compositeOit(destination);
	}

	/**
	 * Inserts OIT layers after the vanilla Fabulous transparency chain.
	 * Targets are ordered as terrain, items, particles, weather and clouds.
	 */
	public static void compositeFabulousTranslucency(RenderTarget destination, List<RenderTarget> targets)
	{
		EXECUTOR.compositeFabulousOit(destination, targets);
	}

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

	public static final class RenderStage
	{
		public static final RenderStage SOLID_BLOCKS = new RenderStage("solid_blocks");
		public static final RenderStage TRANSLUCENT_BLOCKS = new RenderStage("translucent_blocks");
		public static final RenderStage ENTITIES = new RenderStage("entities");
		public static final RenderStage FIRST_PERSON = new RenderStage("first_person");
		public static final RenderStage GUI = new RenderStage("gui");

		private final String name;

		public RenderStage(String name)
		{
			this.name = name;
		}

		@Override
		public int hashCode()
		{
			return this.name.hashCode();
		}

		@Override
		public boolean equals(Object object)
		{
			return object instanceof RenderStage other && this.name.equals(other.name);
		}
	}
}
