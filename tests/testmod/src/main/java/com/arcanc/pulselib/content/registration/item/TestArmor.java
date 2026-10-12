/**
 * @author ArcAnc
 * Created at: 02.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.item;


import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.registration.renderer.TestDayTimeColor;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorModelProvider;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

public class TestArmor extends ArmorItem implements PArmorModelProvider
{
	public static int setups;
	public static final ResourceLocation TEXTURE = PLibDatabase.rl("entity/armor/test_armor/0");
	public static final PModelData MODEL_DATA = PModelData.entity(PLibDatabase.rl("armor/test_armor"));

	public TestArmor(Holder<ArmorMaterial> material, Type type, Properties properties)
	{
		super(material, type, properties);
	}

	@Override
	public HumanoidModel<?> getPulseArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original)
	{
		return original;
	}

	@Override
	public void setupPulseArmorAnimations(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> model, float partialTick)
	{
		setups++;
	}

	public static PMeshRenderContext resolveArmorRender(LivingEntity entity,
	                                                     ItemStack stack,
	                                                     PBakedBone bone,
	                                                     PBakedMesh mesh,
	                                                     PMeshRenderContext inherited,
	                                                     float partialTick)
	{
		return new PMeshRenderContext(
				inherited.renderType(),
				dayTimeColor(entity, partialTick),
				inherited.packedLight(),
				inherited.packedOverlay());
	}

	private static int dayTimeColor(LivingEntity entity, float partialTick)
	{
		return TestDayTimeColor.color(entity.level(), partialTick);
	}
}
