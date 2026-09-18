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
import com.arcanc.pulselib.content.renderer.plan.PDrawGroup;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.util.PRenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides support for render queue.
 */
public class PRenderQueue
{
	private static final PFrameCompiler<RenderStage, RenderType, PBakedMesh, InstanceData> COMPILER = new PFrameCompiler<>(
			Comparator.comparing(RenderType::toString),
			data -> data.posMatrix().m30() * data.posMatrix().m30() +
					data.posMatrix().m31() * data.posMatrix().m31() +
					data.posMatrix().m32() * data.posMatrix().m32());
	private static final PGlMultiDrawExecutor EXECUTOR = new PGlMultiDrawExecutor();
	
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
		COMPILER.submit(stage, type, subdividedMesh, data.withDeformer(deformer), transparent);
	}
	
	/**
	 * Performs the flush operation.
	 * @param stage the stage to use.
	 */
	public static void flush(RenderStage stage)
	{
		PRenderPlan<RenderType, PBakedMesh, InstanceData> plan = COMPILER.compile(stage);
		EXECUTOR.execute(plan);
	}

	/**
	 * Performs the flush combined operation.
	 * @param stages the stages to use.
	 */
	public static void flushCombined(RenderStage... stages)
	{
		List<PDrawGroup<RenderType, PBakedMesh, InstanceData>> groups = new ArrayList<>();
		for (RenderStage stage : stages)
			groups.addAll(COMPILER.compile(stage).groups());
		if (!groups.isEmpty())
			EXECUTOR.execute(new PRenderPlan<>(groups));
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
		 * Performs the with deformer operation.
		 * @param deformer the deformer to use.
		 * @return the value produced by this operation.
		 */
		private InstanceData withDeformer(PGpuDeformerBuffers.Submission deformer)
		{
			return new InstanceData(this.posMatrix, this.packedColor, this.packedLight, this.packedOverlay,
					deformer.operationOffset(), deformer.valueOffset(), deformer.operationCount());
		}
	}
	
}
