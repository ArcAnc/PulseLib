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

@FunctionalInterface
public interface PLivingMeshRenderResolver extends PMeshRenderResolver
{
	PMeshRenderContext resolve(@Nullable LivingEntity entity,
	                           ItemStack stack,
	                           PBakedBone bone,
	                           PBakedMesh mesh,
	                           PMeshRenderContext inherited,
	                           float partialTick);
	
	@Override
	default PMeshRenderContext resolve(PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited)
	{
		return resolve(null, ItemStack.EMPTY, bone, mesh, inherited, 0);
	}
}
