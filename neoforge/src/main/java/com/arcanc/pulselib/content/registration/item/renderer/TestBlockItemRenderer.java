/**
 * @author ArcAnc
 * Created at: 28.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.item.renderer;


import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.registration.item.TestBlockItem;
import com.arcanc.pulselib.content.registration.renderer.TestDayTimeColor;
import com.arcanc.pulselib.content.renderer.PItemRenderer;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class TestBlockItemRenderer extends PItemRenderer<TestBlockItem>
{
	public static final ResourceLocation CIRCLE = PLibDatabase.rl("item/test_block/circle");
	public static final ResourceLocation PYRAMID = PLibDatabase.rl("item/test_block/pyramid");
	public static final PModelData MODEL_DATA = PModelData.item(PLibDatabase.rl("test_block"));
	
	public TestBlockItemRenderer(BlockEntityRenderDispatcher blockEntityRenderDispatcher, EntityModelSet entityModelSet)
	{
		super(MODEL_DATA,
				PRenderTypes.RenderTypeProvider :: trianglesSolid,
				blockEntityRenderDispatcher,
				entityModelSet);
	}
	
	@Override
	protected PMeshRenderContext resolveMeshRender(TestBlockItem animatable,
	                                               ItemStack stack,
	                                               ItemDisplayContext context,
	                                               PBakedBone bone,
	                                               PBakedMesh mesh,
	                                               PMeshRenderContext inherited,
	                                               float partialTick)
	{
		Minecraft mc = PLibRenderHelper.mc();
		if (mc.level == null)
			return inherited;
		
		return inherited.withColor(TestDayTimeColor.color(mc.level, partialTick));
	}
}
