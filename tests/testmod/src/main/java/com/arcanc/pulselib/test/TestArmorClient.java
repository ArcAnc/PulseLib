package com.arcanc.pulselib.test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;

public final class TestArmorClient
{
	private static HumanoidModel<?> model;
	private TestArmorClient() { }
	public static HumanoidModel<?> model()
	{
		if (model == null)
			model = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
		return model;
	}
}
