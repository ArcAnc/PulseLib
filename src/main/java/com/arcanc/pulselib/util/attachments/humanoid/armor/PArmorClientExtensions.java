/**
 * @author ArcAnc
 * Created at: 05.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments.humanoid.armor;


import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.Map;

/**
 * Provides support for armor client extensions.
 */
public class PArmorClientExtensions implements IClientItemExtensions
{
	private static final Model<Unit> EMPTY_ARMOR_MODEL = new Model.Simple(new ModelPart(List.of(), Map.of()), RenderTypes :: armorCutoutNoCull);

	/**
	 * Builds the for.
	 * @param item the item to use.
	 * @return the value produced by this operation.
	 */
	public static IClientItemExtensions buildFor(Item item)
	{
		if (!PLivingAttachments.contains(item))
			return IClientItemExtensions.DEFAULT;
		return new PArmorClientExtensions();
	}
	
	/**
	 * Returns the generic armor model.
	 * @param itemStack the item stack to use.
	 * @param layerType the layer type to use.
	 * @param original the original to use.
	 * @return the value produced by this operation.
	 */
	@Override
	public Model<?> getGenericArmorModel(ItemStack itemStack, EquipmentClientInfo.LayerType layerType, Model original)
	{
		if (PLivingAttachments.hidesVanillaArmor(itemStack))
			return EMPTY_ARMOR_MODEL;
		
		return original;
	}
	
	/**
	 * Returns the armor layer tint color.
	 * @param stack the stack to use.
	 * @param layer the layer to use.
	 * @param layerIdx the layer idx to use.
	 * @param fallbackColor the fallback color to use.
	 * @return the value produced by this operation.
	 */
	@Override
	public int getArmorLayerTintColor(ItemStack stack, EquipmentClientInfo.Layer layer, int layerIdx, int fallbackColor)
	{
		return PLivingAttachments.hidesVanillaArmor(stack) ? 0 : fallbackColor;
	}
}
