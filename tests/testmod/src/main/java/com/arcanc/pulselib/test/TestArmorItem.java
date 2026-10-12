package com.arcanc.pulselib.test;

import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorModelProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;

public class TestArmorItem extends ArmorItem implements PArmorModelProvider
{
	public static int setups;
	public TestArmorItem() { super(ArmorMaterials.DIAMOND, Type.CHESTPLATE, new Properties()); }
	@Override public HumanoidModel<?> getPulseArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original)
	{
		return TestArmorClient.model();
	}
	@Override public void setupPulseArmorAnimations(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> model, float partialTick)
	{
		setups++;
	}
}
