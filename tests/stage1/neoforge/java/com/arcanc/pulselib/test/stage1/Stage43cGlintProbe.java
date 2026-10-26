/**
 * @author ArcAnc
 * Created at: 26.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.renderer.base.PItemRenderState;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.blaze3d.opengl.GlTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Compares equal vanilla icons with/without glint beside Pulse atlas/PIP draws, while checking the special-renderer foil flag. */
public final class Stage43cGlintProbe
{
	private static int frames;
	private static int foil;
	private static int plain;
	private static final ItemStack VANILLA = new ItemStack(Items.DIAMOND);
	private static final ItemStack GLINT = new ItemStack(Items.DIAMOND);

	static
	{
		GLINT.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
	}

	private Stage43cGlintProbe()
	{
	}

	public static void extract(GuiGraphicsExtractor graphics, ItemStack green)
	{
		green.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		graphics.item(VANILLA, 340, 130);
		graphics.item(GLINT, 380, 130);
	}

	public static void item(PItemRenderState<?> state)
	{
		if (state.hasFoil() != state.stack().hasFoil())
			throw new AssertionError("Special renderer foil flag disagrees with extracted stack");
		if (state.hasFoil())
			foil++;
		else
			plain++;
	}

	public static void verify(int phase)
	{
		var mc = Minecraft.getInstance();
		var target = mc.getMainRenderTarget();
		int scale = mc.getWindow().getGuiScale();
		int size = 16 * scale;
		int binding = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int stride = GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH);
		int fbo = GL30.glGenFramebuffers();
		try
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, ((GlTexture)target.getColorTexture()).glId(), 0);
			GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
			GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
			var a = BufferUtils.createByteBuffer(size * size * 4);
			var b = BufferUtils.createByteBuffer(size * size * 4);
			GL11.glReadPixels(340 * scale, target.height - 146 * scale, size, size, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, a);
			GL11.glReadPixels(380 * scale, target.height - 146 * scale, size, size, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, b);
			int visible = 0;
			int changed = 0;
			for (int offset = 0; offset < a.capacity(); offset += 4)
			{
				int brightness = 0;
				int difference = 0;
				for (int channel = 0; channel < 3; channel++)
				{
					brightness += Byte.toUnsignedInt(a.get(offset + channel));
					difference += Math.abs(Byte.toUnsignedInt(a.get(offset + channel)) - Byte.toUnsignedInt(b.get(offset + channel)));
				}
				if (brightness > 60)
					visible++;
				if (difference > 6)
					changed++;
			}
			if (visible < 40 || changed < 8 || foil == 0 || plain == 0)
				throw new AssertionError("Native glint boundary: visible=" + visible + ", changed=" + changed + ", foil=" + foil + ", plain=" + plain);
			frames++;
			PLibDatabase.LOGGER.info("STAGE 4.3c GLINT CASE: phase={}, frame={}, visible={}, changed={}, foil={}, plain={}", phase, frames, visible, changed, foil, plain);
		}
		finally
		{
			GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, stride);
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, binding);
			GL30.glDeleteFramebuffers(fbo);
		}
	}

	public static void finish()
	{
		if (frames != 32 || foil == 0 || plain == 0)
			throw new AssertionError("Incomplete native glint matrix");
		PLibDatabase.LOGGER.info("STAGE 4.3c GLINT PASS: frames={}, foil={}, plain={}", frames, foil, plain);
	}
}
