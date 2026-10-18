/**
 * @author ArcAnc
 * Created at: 07.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments;


import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.model.baked.PMeshRenderResolver;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves per-mesh render overrides using a living entity and its equipped attachment stack.
 */
@FunctionalInterface
public interface PLivingMeshRenderResolver extends PMeshRenderResolver
{
	/**
	 * Resolves render inputs for one attachment mesh from the inherited context.
	 *
	 * @param entity the entity being animated or rendered; may be {@code null}
	 * @param stack the item stack supplying render or animation state
	 * @param bone the baked bone being sampled or submitted
	 * @param mesh the baked mesh geometry and material
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the inherited or overridden mesh render inputs
	 */
	PMeshRenderContext resolve(@Nullable LivingEntity entity,
	                           ItemStack stack,
	                           PBakedBone bone,
	                           PBakedMesh mesh,
	                           PMeshRenderContext inherited,
	                           float partialTick);
	
	/**
	 * Resolves render inputs for one attachment mesh from the inherited context.
	 *
	 * @param bone the baked bone being sampled or submitted
	 * @param mesh the baked mesh geometry and material
	 * @param inherited the render inputs inherited from the parent bone or renderer
	 * @return the inherited or overridden mesh render inputs
	 */
	@Override
	default PMeshRenderContext resolve(PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited)
	{
		return resolve(null, ItemStack.EMPTY, bone, mesh, inherited, 0);
	}
}
