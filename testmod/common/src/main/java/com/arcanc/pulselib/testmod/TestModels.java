/**
 * @author ArcAnc
 * Created at: 21.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.testmod;

import com.arcanc.pulselib.api.plugin.IPulseClientPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import net.minecraft.resources.Identifier;

import static com.arcanc.pulselib.testmod.TestContent.id;

@PulsePlugin
public final class TestModels implements IPulseClientPlugin
{
	static final PModelData CREATURE = PModelData.entity(TestContent.CREATURE_ID);
	static final PModelData ARMOR = PModelData.entityLayer(TestContent.CREATURE_ID, id("armor"));
	static final PModelData BLOCK = PModelData.block(TestContent.BLOCK_ID);
	static final PModelData ITEM = PModelData.item(TestContent.BLOCK_ID);

	@Override
	public Identifier getPluginId()
	{
		return id("models");
	}

	@Override
	public void registerModels(PModelRegistrar registrar)
	{
		registrar.model(CREATURE).texture("0", id("entity/test_entity/0"));
		registrar.model(ARMOR).texture("0", id("entity/test_entity/armor/0"));
		registrar.model(BLOCK).
				texture("tube_texture", id("block/test_block/tube_texture")).
				texture("cube_texture", id("block/test_block/cube_texture")).
				texture("torus_texture", id("block/test_block/torus_texture")).
				texture("pyramid_texture", id("block/test_block/pyramid_texture"));
		registrar.model(ITEM).
				texture("circle", id("item/test_block/circle")).
				texture("pyramid", id("item/test_block/pyramid"));
	}
}
