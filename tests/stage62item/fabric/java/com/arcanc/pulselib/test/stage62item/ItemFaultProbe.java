/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62item;

import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.legacy.GlRenderState;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.List;
import java.util.Map;

import static com.arcanc.pulselib.test.stage62item.ItemClient.field;

/** Fail real native calls after Pulse mesh submission; verify state, no pixels, native pose and collector recovery. */
public final class ItemFaultProbe
{
	private static final PRenderQueue.RenderStage SENTINEL = new PRenderQueue.RenderStage("stage43b_fault_sentinel");
	private static int suites;

	private ItemFaultProbe()
	{
	}

	public static int suites()
	{
		return suites;
	}

	public static void verify(int phase)
	{
		if (phase != suites * 4 || suites == 2)
			return;
		var mc = Minecraft.getInstance();
		var projection = RenderSystem.getProjectionMatrixBuffer();
		var color = RenderSystem.outputColorTextureOverride;
		var depth = RenderSystem.outputDepthTextureOverride;
		var matrix = new Matrix4f(RenderSystem.getModelViewMatrix());
		var compiler = (PFrameCompiler<PRenderQueue.RenderStage>)field(PRenderQueue.class, null, "COMPILER");
		PRenderQueue.submit(SENTINEL, PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(PResourceCache.ATLAS_LOCATION),
				ItemClient.quad(), new PInstanceHeader(new Matrix4f(), -1, 0xF000F0, 0));
		try (var caller = new GlRenderState();
		     var atlas = new GuiItemAtlas(mc.gameRenderer.getFeatureRenderDispatcher().getSubmitNodeStorage(),
			     mc.gameRenderer.getFeatureRenderDispatcher(), mc.renderBuffers().bufferSource(), 64, 16))
		{
			for (int fault = 0; fault < 2; fault++)
			{
				int beforeAlpha = alpha((GpuTexture)field(GuiItemAtlas.class, atlas, "texture"));
				ItemClient.failItem = fault == 0;
				ItemClient.failCallback = fault == 1;
				expectFailure(() -> atlas.getOrUpdate(item()), fault == 0 ? "item failure" : "callback failure");
				ItemClient.failItem = false;
				ItemClient.failCallback = false;
				check(alpha((GpuTexture)field(GuiItemAtlas.class, atlas, "texture")) == beforeAlpha, "Failed native slot must not draw queued Pulse meshes");
				var poses = (PoseStack)field(GuiItemAtlas.class, atlas, "poseStack");
				check(poses.isEmpty() && new Matrix4f().equals(poses.last().pose()), "Atlas pose must recover after failed submit/feature execution");
				check(RenderSystem.outputColorTextureOverride == color && RenderSystem.outputDepthTextureOverride == depth &&
						RenderSystem.getProjectionMatrixBuffer() == projection && matrix.equals(RenderSystem.getModelViewMatrix()), "Failed atlas slot must restore host inputs");
				for (var collection : mc.gameRenderer.getFeatureRenderDispatcher().getSubmitNodeStorage().getSubmitsPerOrder().values())
				{
					var storage = collection.getCustomGeometrySubmits();
					for (String name : List.of("solidCustomGeometrySubmits", "translucentCustomGeometrySubmits"))
						for (Object values : ((Map<?, ?>)field(storage.getClass(), storage, name)).values())
							check(((List<?>)values).isEmpty(), "Failed deferred feature must not remain queued for another owner");
				}
				var slot = atlas.getOrUpdate(item());
				check(slot != null && alpha(slot.textureView().texture()) > 0, "Native atlas must recover after either failure");
			}
			check(compiler.compile(SENTINEL).instances().size() == 1 && compiler.isEmpty(), "Faulted GUI must preserve world sentinel without leaking local geometry");
			check(PRenderContexts.current() == PRenderContexts.Context.GUI, "Faulted local scopes must leave the context stack");
			check(GL11.glGetError() == GL11.GL_NO_ERROR, "Fault/recovery produced a native GL error");
			suites++;
			PLibDatabase.LOGGER.info("STAGE62C2 FAULT PASS: phase={}, suites={}, native Item/deferred callback exceptions, discarded meshes, slot pose and collector cleared, host/queue restored, actual atlas pixels recover", phase, suites);
		}
		finally
		{
			ItemClient.failItem = false;
			ItemClient.failCallback = false;
		}
	}

	private static TrackingItemStackRenderState item()
	{
		var mc = Minecraft.getInstance();
		var state = new TrackingItemStackRenderState();
		mc.getItemModelResolver().updateForTopItem(state, new ItemStack(ItemFixture.ITEM), ItemDisplayContext.GUI, mc.level, mc.player, 0);
		return state;
	}

	private static void expectFailure(Runnable action, String text)
	{
		try
		{
			action.run();
			throw new AssertionError("Expected native " + text);
		}
		catch (RuntimeException expected)
		{
			Throwable root = expected;
			while (root.getCause() != null)
				root = root.getCause();
			check(root instanceof IllegalArgumentException && root.getMessage().contains(text), "Unexpected native failure: " + root);
		}
	}

	private static int alpha(GpuTexture texture)
	{
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int rowLength = GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH);
		GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
		int fbo = GL30.glGenFramebuffers();
		try
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, ((GlTexture)texture).glId(), 0);
			var pixels = BufferUtils.createByteBuffer(texture.getWidth(0) * texture.getHeight(0) * 4);
			GL11.glReadPixels(0, 0, texture.getWidth(0), texture.getHeight(0), GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
			int alpha = 0;
			for (int offset = 3; offset < pixels.capacity(); offset += 4)
				alpha += Byte.toUnsignedInt(pixels.get(offset));
			return alpha;
		}
		finally
		{
			GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, rowLength);
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
			GL30.glDeleteFramebuffers(fbo);
		}
	}

	private static void check(boolean value, String text)
	{
		if (!value)
			throw new AssertionError("Stage 4.3b faults: " + text);
	}
}
