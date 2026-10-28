/**
 * @author ArcAnc
 * Created at: 28.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.player.deformer.PPlayerArmorGeometryLayer;
import com.arcanc.pulselib.content.player.deformer.PPlayerGeometryRenderer;
import com.arcanc.pulselib.content.player.deformer.PPlayerModelGeometry;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Mixin(LivingEntityRenderer.class)
public abstract class PlayerGeometryRendererMixin implements PPlayerGeometryRenderer
{
	@Shadow
	protected EntityModel<?> model;
	@Shadow
	@Final
	protected List<RenderLayer<?, ?>> layers;

	@Override
	public PPlayerModelGeometry pulselib$captureGeometry(Player player, AvatarRenderState state, float partialTick)
	{
		var main = PPlayerModelGeometry.capture(player, state, partialTick, this.model, this.model);
		Map<Object, Map<EquipmentSlot, PPlayerModelGeometry.Entry>> armor = new IdentityHashMap<>();
		for (RenderLayer<?, ?> layer : this.layers)
			if (layer instanceof PPlayerArmorGeometryLayer access)
				access.pulselib$captureArmorGeometry(player, state, partialTick, armor);
		return new PPlayerModelGeometry(main, armor);
	}
}
