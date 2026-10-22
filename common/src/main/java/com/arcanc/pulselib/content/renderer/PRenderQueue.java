/**
 * @author ArcAnc
 * Created at: 01.04.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;


import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PSubdividedMeshCache;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.renderer.gl.PGlMultiDrawExecutor;
import com.arcanc.pulselib.content.renderer.gl.PGlResourceRegistry;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PPipelineHandle;
import com.arcanc.pulselib.util.PRenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Provides support for render queue.
 */
public class PRenderQueue
{
	private static final PGlResourceRegistry RESOURCES = new PGlResourceRegistry();
	private static final PFrameCompiler<RenderStage> COMPILER = new PFrameCompiler<>();
	private static final PGlMultiDrawExecutor EXECUTOR = new PGlMultiDrawExecutor(RESOURCES);
	
	/**
	 * Performs the submit block entity mesh operation.
	 * @param renderType the render type to use.
	 * @param mesh the mesh to use.
	 * @param deformation the deformation to use.
	 * @param data the data to use.
	 */
	public static void submitBlockEntityMesh(RenderType renderType,
	                                         PBakedMesh mesh,
	                                         @Nullable PMeshDeformation deformation,
	                                         InstanceData data)
	{
		submit(RenderStage.SOLID_BLOCKS,
				renderType, mesh, deformation, data);
	}
	
	/**
	 * Performs the submit block entity translucent mesh operation.
	 * @param renderType the render type to use.
	 * @param mesh the mesh to use.
	 * @param deformation the deformation to use.
	 * @param data the data to use.
	 */
	public static void submitBlockEntityTranslucentMesh(RenderType renderType,
	                                                    PBakedMesh mesh,
	                                                    @Nullable PMeshDeformation deformation,
	                                                    InstanceData data)
	{
		submit(RenderStage.TRANSLUCENT_BLOCKS, renderType, mesh, deformation, data, true);
	}
	
	/**
	 * Performs the submit item operation.
	 * @param context the context to use.
	 * @param renderType the render type to use.
	 * @param mesh the mesh to use.
	 * @param deformation the deformation to use.
	 * @param data the data to use.
	 */
	public static void submitItem(ItemDisplayContext context,
	                              RenderType renderType,
	                              PBakedMesh mesh,
	                              @Nullable PMeshDeformation deformation,
	                              InstanceData data)
	{
		RenderStage stage = switch (context)
		{
			case GUI -> RenderStage.GUI;
			case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> RenderStage.FIRST_PERSON;
			case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND, HEAD, ON_SHELF -> RenderStage.ENTITIES;
			case GROUND, FIXED, NONE -> RenderStage.TRANSLUCENT_BLOCKS;
		};
		submit(stage, renderType, mesh, deformation, data, PRenderTypes.isTransparent(renderType));
	}
	
	/**
	 * Performs the submit entity mesh operation.
	 * @param renderType the render type to use.
	 * @param mesh the mesh to use.
	 * @param deformation the deformation to use.
	 * @param data the data to use.
	 */
	public static void submitEntityMesh(RenderType renderType,
	                                    PBakedMesh mesh,
	                                    @Nullable PMeshDeformation deformation,
	                                    InstanceData data)
	{
		submit(RenderStage.ENTITIES, renderType, mesh, deformation, data, PRenderTypes.isTransparent(renderType));
	}
	
	/**
	 * Performs the submit operation.
	 * @param stage the stage to use.
	 * @param type the type to use.
	 * @param mesh the mesh to use.
	 * @param deformation the deformation to use.
	 * @param data the data to use.
	 */
	public static void submit(RenderStage stage,
	                          RenderType type,
	                          PBakedMesh mesh,
	                          @Nullable PMeshDeformation deformation,
	                          InstanceData data)
	{
		submit(stage, type, mesh, deformation, data, PRenderTypes.isTransparent(type));
	}

	/**
	 * Performs the submit operation.
	 * @param stage the stage to use.
	 * @param type the type to use.
	 * @param mesh the mesh to use.
	 * @param deformation the deformation to use.
	 * @param data the data to use.
	 * @param transparent the transparent to use.
	 */
	private static void submit(RenderStage stage,
	                           RenderType type,
	                           PBakedMesh mesh,
	                           @Nullable PMeshDeformation deformation,
	                           InstanceData data,
	                           boolean transparent)
	{
		PBakedMesh subdividedMesh = PSubdividedMeshCache.resolve(mesh, deformation == null ? 0 : deformation.subdivisionLevel());
		PGpuDeformerBuffers.Submission deformer = PGpuDeformerBuffers.submit(deformation);
		PInstanceHeader header = new PInstanceHeader(data.posMatrix(), data.packedColor(), data.packedLight(), data.packedOverlay(),
				deformer.operationOffset(), deformer.valueOffset(), deformer.operationCount());
		COMPILER.submit(stage, RESOURCES.pipeline(type), RESOURCES.mesh(subdividedMesh), subdividedMesh.indicesCount(), header, transparent);
	}
	
	/**
	 * Performs the flush operation.
	 * @param stage the stage to use.
	 */
	public static void flush(RenderStage stage)
	{
		EXECUTOR.execute(COMPILER.compile(stage, PRenderQueue::canBatchWithOit));
	}

	/** Consumes opaque geometry while retaining transparent submissions for their later pass. */
	public static void flushOpaque(RenderStage stage)
	{
		EXECUTOR.execute(COMPILER.compileOpaque(stage));
	}

	/**
	 * Performs the flush combined operation.
	 * @param stages the stages to use.
	 */
	public static void flushCombined(RenderStage... stages)
	{
		EXECUTOR.execute(COMPILER.compileCombined(List.of(stages), PRenderQueue::canBatchWithOit));
	}

	private static boolean canBatchWithOit(PPipelineHandle pipeline)
	{
		RenderType type = RESOURCES.pipeline(pipeline);
		return PRenderTypes.usesOit(type) && type.outputTarget().getRenderTarget().useDepth;
	}

	/**
	 * Performs the composite translucency operation.
	 */
	public static void compositeTranslucency()
	{
		EXECUTOR.compositeOit();
	}

	/**
	 * Performs the clean up operation.
	 */
	public static void cleanUp()
	{
		COMPILER.clear();
		EXECUTOR.cleanup();
		PGpuDeformerBuffers.cleanup();
	}

	/** Canonical cleanup entry point; both names operate on the same queue and executor. */
	public static void cleanup()
	{
		cleanUp();
	}
	
/**
 * Provides support for render stage.
 */
	public static class RenderStage
	{
		public static final RenderStage SOLID_BLOCKS = new RenderStage("solid_blocks");
		public static final RenderStage TRANSLUCENT_BLOCKS = new RenderStage("translucent_blocks");
		public static final RenderStage ENTITIES = new RenderStage("entities");
		public static final RenderStage FIRST_PERSON = new RenderStage("first_person");
		public static final RenderStage GUI = new RenderStage("gui");
		
		private final String name;
		
		/**
		 * Creates an instance of the enclosing type.
		 * @param name the name to use.
		 */
		public RenderStage(String name)
		{
			this.name = name;
		}
		
		/**
		 * Performs the hash code operation.
		 * @return the value produced by this operation.
		 */
		@Override
		public int hashCode()
		{
			return this.name.hashCode();
		}
		
		/**
		 * Performs the equals operation.
		 * @param obj the obj to use.
		 * @return the value produced by this operation.
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (obj == null)
				return false;
			if (!(obj instanceof RenderStage other))
				return false;
			if (this == other)
				return true;
			return this.name.equals(other.name);
		}
	}
	
/**
 * Immutable value object representing instance data.
 */
	public record InstanceData(
			Matrix4f posMatrix,
			int packedColor,
			int packedLight,
			int packedOverlay,
			int deformerOperationOffset,
			int deformerValueOffset,
			int deformerOperationCount)
	{
		/**
		 * Creates an instance of the enclosing type.
		 * @param posMatrix the pos matrix to use.
		 * @param packedColor the packed color to use.
		 * @param packedLight the packed light to use.
		 * @param packedOverlay the packed overlay to use.
		 * @param deformerOperationOffset the deformer operation offset to use.
		 * @param deformerValueOffset the deformer value offset to use.
		 * @param deformerOperationCount the deformer operation count to use.
		 */
		public InstanceData
		{
			posMatrix = new Matrix4f(posMatrix);
		}

		/**
		 * Creates an instance of the enclosing type.
		 * @param posMatrix the pos matrix to use.
		 * @param packedColor the packed color to use.
		 * @param packedLight the packed light to use.
		 * @param packedOverlay the packed overlay to use.
		 */
		public InstanceData(Matrix4f posMatrix, int packedColor, int packedLight, int packedOverlay)
		{
			this(posMatrix, packedColor, packedLight, packedOverlay, -1, -1, 0);
		}

		/**
		 * Returns the canonical instance header without changing packed attributes.
		 * @return the value produced by this operation.
		 */
		public PInstanceHeader header()
		{
			return new PInstanceHeader(this.posMatrix, this.packedColor, this.packedLight, this.packedOverlay,
					this.deformerOperationOffset, this.deformerValueOffset, this.deformerOperationCount);
		}
	}
	
}
