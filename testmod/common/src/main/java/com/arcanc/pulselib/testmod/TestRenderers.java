/**
 * @author ArcAnc
 * Created at: 21.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.testmod;

import com.arcanc.pulselib.content.renderer.PBlockRenderer;
import com.arcanc.pulselib.content.renderer.PEntityRenderLayer;
import com.arcanc.pulselib.content.renderer.PEntityRenderer;
import com.arcanc.pulselib.content.renderer.PItemRenderer;
import com.arcanc.pulselib.content.renderer.base.PBlockRenderState;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import com.arcanc.pulselib.content.renderer.base.PItemRenderState;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.special.SpecialModelRenderer;

public final class TestRenderers
{
	private TestRenderers()
	{
	}

	public static final class Creature extends PEntityRenderer<TestCreature, PEntityRenderState.LivingImpl<TestCreature>>
	{
		public Creature(EntityRendererProvider.Context context)
		{
			super(context, TestModels.CREATURE, PRenderTypes.RenderTypeProvider::trianglesTranslucent);
			addRenderLayer("body", new Armor().
					bindBone("armor_chest", "body").
					bindBone("armor_helm", "head").
					bindBone("armor_left_hand", "hand_left"));
		}

		@Override
		public PEntityRenderState.LivingImpl<TestCreature> createRenderState()
		{
			return PLibHelper.livingRenderState();
		}
	}

	private static final class Armor extends PEntityRenderLayer<TestCreature, PEntityRenderState.LivingImpl<TestCreature>>
	{
		private Armor()
		{
			super(TestModels.ARMOR, PRenderTypes.RenderTypeProvider::trianglesSolid);
		}

		@Override
		public boolean shouldRender(TestCreature creature)
		{
			return creature.showsArmor();
		}
	}

	public static final class Block extends PBlockRenderer<TestBlockEntity, PBlockRenderState.Impl<TestBlockEntity>>
	{
		public Block(BlockEntityRendererProvider.Context context)
		{
			super(TestModels.BLOCK, PRenderTypes.RenderTypeProvider::trianglesSolid);
		}

		@Override
		public PBlockRenderState.Impl<TestBlockEntity> createRenderState()
		{
			return new PBlockRenderState.Impl<>();
		}
	}

	public static final class Item extends PItemRenderer<TestItem, PItemRenderState.Impl<TestItem>>
	{
		private Item(PModelData model)
		{
			super(model, PRenderTypes.RenderTypeProvider::trianglesSolid);
		}

		@Override
		protected PItemRenderState.Impl<TestItem> createRenderState()
		{
			return new PItemRenderState.Impl<>();
		}
	}

	public record UnbakedItem(PModelData model) implements SpecialModelRenderer.Unbaked<PItemRenderState.Impl<TestItem>>
	{
		public static final MapCodec<UnbakedItem> CODEC = PModelData.CODEC.xmap(UnbakedItem::new, UnbakedItem::model);

		@Override
		public Item bake(SpecialModelRenderer.BakingContext context)
		{
			return new Item(this.model);
		}

		@Override
		public MapCodec<UnbakedItem> type()
		{
			return CODEC;
		}
	}
}
