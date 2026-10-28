/**
 * @author ArcAnc
 * Created at: 28.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1.mixin;

import com.arcanc.pulselib.test.stage1.Stage52ArmorProbe;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;

/** Measures actual native armor draws, preserving the original meshes, pipeline and upload path. */
@Mixin(RenderType.class)
public abstract class Stage52ArmorGpuMixin
{
	@WrapMethod(method = "draw")
	private void pulselib$armorGpu(MeshData mesh, Operation<Void> original)
	{
		Stage52ArmorProbe.gpu((RenderType)(Object)this, mesh, () -> original.call(mesh));
	}
}
