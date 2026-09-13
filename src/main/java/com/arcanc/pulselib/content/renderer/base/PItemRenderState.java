/**
 * @author ArcAnc
 * Created at: 28.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.base;


import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.renderer.PItemRenderer;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

public interface PItemRenderState<T extends Item & PAnimatable<T>> extends PRenderState<T>
{
	/**
	 * Extracts the stack data.
	 * @param stack the stack to use.
	 * @param renderer the renderer to use.
	 */
	<RS extends PItemRenderState<T>> void extractStackData(
			ItemStack stack,
			PItemRenderer<T, RS> renderer);
	
	/**
	 * Extracts the additional data.
	 * @param lightCoords the light coords to use.
	 * @param overlayCoords the overlay coords to use.
	 * @param hasFoil the has foil to use.
	 * @param outlineColor the outline color to use.
	 */
	@ApiStatus.Internal
	void extractAdditionalData(int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor);
	
	/**
	 * Extracts the item render state.
	 * @param renderState the render state to use.
	 */
	@ApiStatus.Internal
	void extractItemRenderState(ItemStackRenderState renderState);
	
	/**
	 * Performs the light coords operation.
	 * @return the value produced by this operation.
	 */
	int lightCoords();
	/**
	 * Performs the overlay coords operation.
	 * @return the value produced by this operation.
	 */
	int overlayCoords();
	/**
	 * Determines whether the object has foil.
	 * @return the value produced by this operation.
	 */
	boolean hasFoil();
	/**
	 * Performs the outline color operation.
	 * @return the value produced by this operation.
	 */
	int outlineColor();
	/**
	 * Performs the stack operation.
	 * @return the value produced by this operation.
	 */
	ItemStack stack();
	
	/**
	 * Performs the item render state operation.
	 * @return the value produced by this operation.
	 */
	ItemStackRenderState itemRenderState();
	
	class Impl<T extends Item & PAnimatable<T>> implements PItemRenderState<T>
	{
		private @Nullable PBakedModel model;
		private T animatable;
		private float partialTicks;
		private int lightCoords;
		private int overlayCoords;
		private boolean hasFoil;
		private int outlineColor;
		private ItemStack stack = ItemStack.EMPTY;
		private ItemStackRenderState guiItemRenderState;
		private AnimManagerKey key;
		
		/**
		 * Extracts the data.
		 */
		@Override
		public void extractData()
		{
			this.partialTicks = PLibRenderHelper.mc().isPaused() ? 0 : PLibRenderHelper.mc().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		}
		
		/**
		 * Performs the partial tick operation.
		 * @return the value produced by this operation.
		 */
		@Override
		public float partialTick()
		{
			return this.partialTicks;
		}
		
		/**
		 * Returns the baked model.
		 * @return the value produced by this operation.
		 */
		@Override
		public PBakedModel getBakedModel()
		{
			return this.model;
		}
		
		/**
		 * Returns the animatable.
		 * @return the value produced by this operation.
		 */
		@Override
		public T getAnimatable()
		{
			return this.animatable;
		}
		
		/**
		 * Extracts the stack data.
		 * @param stack the stack to use.
		 * @param renderer the renderer to use.
		 */
		@SuppressWarnings ("unchecked")
		@Override
		public <RS extends PItemRenderState<T>> void extractStackData(ItemStack stack, PItemRenderer<T, RS> renderer)
		{
			this.extractData();
			this.stack = stack.copy();
			this.model = renderer.getModel((RS) this);
			this.key = AnimManagerKey.of(stack);
			//TODO: remove this hack
			this.animatable = (T) stack.getItem();
		}
		
		/**
		 * Extracts the additional data.
		 * @param lightCoords the light coords to use.
		 * @param overlayCoords the overlay coords to use.
		 * @param hasFoil the has foil to use.
		 * @param outlineColor the outline color to use.
		 */
		@Override
		public void extractAdditionalData(int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor)
		{
			this.lightCoords = lightCoords;
			this.overlayCoords = overlayCoords;
			this.hasFoil = hasFoil;
			this.outlineColor = outlineColor;
		}
		
		/**
		 * Extracts the item render state.
		 * @param renderState the render state to use.
		 */
		@Override
		public void extractItemRenderState(ItemStackRenderState renderState)
		{
			this.guiItemRenderState = renderState;
		}
		
		/**
		 * Performs the light coords operation.
		 * @return the value produced by this operation.
		 */
		@Override
		public int lightCoords()
		{
			return this.lightCoords;
		}
		
		/**
		 * Performs the overlay coords operation.
		 * @return the value produced by this operation.
		 */
		@Override
		public int overlayCoords()
		{
			return this.overlayCoords;
		}
		
		/**
		 * Determines whether the object has foil.
		 * @return the value produced by this operation.
		 */
		@Override
		public boolean hasFoil()
		{
			return this.hasFoil;
		}
		
		/**
		 * Performs the outline color operation.
		 * @return the value produced by this operation.
		 */
		@Override
		public int outlineColor()
		{
			return this.outlineColor;
		}
		
		/**
		 * Performs the stack operation.
		 * @return the value produced by this operation.
		 */
		@Override
		public ItemStack stack()
		{
			return this.stack;
		}
		
		/**
		 * Performs the item render state operation.
		 * @return the value produced by this operation.
		 */
		@Override
		public ItemStackRenderState itemRenderState()
		{
			return this.guiItemRenderState;
		}
		
		/**
		 * Returns the anim key.
		 * @return the value produced by this operation.
		 */
		@Override
		public AnimManagerKey getAnimKey()
		{
			return this.key;
		}
	}
}
