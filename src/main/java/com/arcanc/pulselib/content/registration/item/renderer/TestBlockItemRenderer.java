/**
 * @author ArcAnc
 * Created at: 28.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.item.renderer;


import com.arcanc.pulselib.content.registration.item.TestBlockItem;
import com.arcanc.pulselib.content.registration.item.renderer.renderState.TestBlockItemRenderState;
import com.arcanc.pulselib.content.renderer.PItemRenderer;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;

/**
 * Renders test block item.
 */
public class TestBlockItemRenderer extends PItemRenderer<TestBlockItem, TestBlockItemRenderState>
{
	public static final Identifier CIRCLE = PLibDatabase.rl("item/test_block/circle");
	public static final Identifier PYRAMID = PLibDatabase.rl("item/test_block/pyramid");
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param modelData the model data to use.
	 */
	public TestBlockItemRenderer(PModelData modelData)
	{
		super(modelData, PRenderTypes.RenderTypeProvider :: trianglesSolid);
	}
	
	/**
	 * Creates the render state.
	 * @return the value produced by this operation.
	 */
	@Override
	protected TestBlockItemRenderState createRenderState()
	{
		return new TestBlockItemRenderState();
	}
	
/**
 * Immutable value object representing unbaked.
 */
	public record Unbaked(PModelData data) implements SpecialModelRenderer.Unbaked<TestBlockItemRenderState>
	{
		public static final MapCodec<Unbaked> MAP_CODEC = PModelData.CODEC.
				xmap(Unbaked :: new, Unbaked :: data);
		
		/**
		 * Performs the bake operation.
		 * @param context the context to use.
		 * @return the value produced by this operation.
		 */
		@Override
		public TestBlockItemRenderer bake(BakingContext context)
		{
			return new TestBlockItemRenderer(this.data);
		}
		
		/**
		 * Performs the type operation.
		 * @return the value produced by this operation.
		 */
		@Override
		public MapCodec<Unbaked> type()
		{
			return MAP_CODEC;
		}
	}
}
