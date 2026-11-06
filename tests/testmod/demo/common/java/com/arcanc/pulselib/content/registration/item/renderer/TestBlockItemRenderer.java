/**
 * @author ArcAnc
 * Created at: 28.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.item.renderer;

import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.registration.item.TestBlockItem;
import com.arcanc.pulselib.content.registration.renderer.TestDayTimeColor;
import com.arcanc.pulselib.content.renderer.PItemRenderer;
import com.arcanc.pulselib.content.renderer.base.PItemRenderState;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.test.DemoChecks;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class TestBlockItemRenderer extends PItemRenderer<TestBlockItem, PItemRenderState.Impl<TestBlockItem>>
{
	public static int renders;
	public static final Identifier CIRCLE = PLibDatabase.rl("item/test_block/circle");
	public static final Identifier PYRAMID = PLibDatabase.rl("item/test_block/pyramid");
	public static final PModelData MODEL_DATA = PModelData.item(PLibDatabase.rl("test_block"));

	public TestBlockItemRenderer()
	{
		super(MODEL_DATA,
				PRenderTypes.RenderTypeProvider :: trianglesSolid);
	}

	@Override
	protected void populateMolangContext(TestBlockItem animatable,
	                                    PAnimationController<TestBlockItem> controller,
	                                    MolangParser.Context context,
	                                    float partialTick)
	{
		DemoChecks.checkTime(controller, context, partialTick);
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
		renders++;
		Minecraft mc = PLibRenderHelper.mc();
		if (mc.level == null)
			return inherited;

		return inherited.withColor(TestDayTimeColor.color(mc.level, partialTick));
	}
	@Override
	protected PItemRenderState.Impl<TestBlockItem> createRenderState()
	{
		return new PItemRenderState.Impl<TestBlockItem>();
	}

	public record Unbaked() implements SpecialModelRenderer.Unbaked<PItemRenderState.Impl<TestBlockItem>>
	{
		public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

		@Override
		public TestBlockItemRenderer bake(BakingContext context)
		{
			return TestBlockItemClient.renderer();
		}

		@Override
		public MapCodec<Unbaked> type()
		{
			return CODEC;
		}
	}
}
