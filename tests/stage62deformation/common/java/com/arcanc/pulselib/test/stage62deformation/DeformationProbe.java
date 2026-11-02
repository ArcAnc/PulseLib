/**
 * @author ArcAnc
 * Created at: 02.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62deformation;

import com.arcanc.pulselib.util.PLibDatabase;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class DeformationProbe
{
	private static int ticks;
	private static int epoch;
	private static int frames;
	private static boolean started;
	private static boolean advance;
	private static boolean completed;
	private static @Nullable CompletableFuture<Void> reload;

	private DeformationProbe()
	{
	}

	public static void tick(Minecraft mc)
	{
		check(Files.isRegularFile(mc.gameDirectory.toPath().resolve(".pulselib-deformation")), "Disposable deformation client marker");
		if (++ticks > 2400)
			throw new AssertionError("STAGE62D2B timeout epoch=" + epoch);
		if (mc.level == null || mc.player == null || mc.getOverlay() != null || DeformationPlugin.MODEL.getModel() == null)
			return;
		if (advance)
		{
			advance = false;
			PLibDatabase.LOGGER.info("STAGE62D2B EPOCH PASS: epoch={}, frames={}, regions={}", epoch, frames, frames * 6);
			if (++epoch == 2)
			{
				completed = true;
				try
				{
					Files.writeString(mc.gameDirectory.toPath().resolve("deformation-results.json"), new GsonBuilder().setPrettyPrinting().create().toJson(Map.of(
							"status", "PASS", "epochs", epoch, "regions", InstanceParityProbe.RECORDS.size() * 6,
							"nativeSuites", NativeParityProbe.suites, "armorNodes", NativeParityProbe.armorNodes,
							"handNodes", NativeParityProbe.handNodes, "nativeArmCalls", NativeParityProbe.nativeArmCalls,
							"vertexComparisons", NativeParityProbe.vertices, "failures", NativeParityProbe.failures,
							"records", InstanceParityProbe.RECORDS)));
				}
				catch (java.io.IOException exception)
				{
					throw new AssertionError(exception);
				}
				InstanceParityProbe.reset();
				PLibDatabase.LOGGER.info("STAGE62D2B COMPLETE: nativeSuites={}, armorNodes={}, handNodes={}, nativeArmCalls={}, vertexComparisons={}, failures={}, regions={}", NativeParityProbe.suites, NativeParityProbe.armorNodes, NativeParityProbe.handNodes, NativeParityProbe.nativeArmCalls, NativeParityProbe.vertices, NativeParityProbe.failures, InstanceParityProbe.RECORDS.size() * 6);
				mc.stop();
				return;
			}
			InstanceParityProbe.reset();
			reload = mc.reloadResourcePacks();
		}
		if (!started || reload != null && reload.isDone())
		{
			if (reload != null)
				reload.join();
			reload = null;
			NativeParityProbe.verify();
			started = true;
			frames = 0;
			mc.setScreen(new ProbeScreen());
		}
	}

	public static void afterGui()
	{
		var mc = Minecraft.getInstance();
		if (!(mc.screen instanceof ProbeScreen) || mc.getOverlay() != null || reload != null || advance || completed)
			return;
		InstanceParityProbe.draw(epoch, frames);
		if (epoch == 1 && frames == 7)
			net.minecraft.client.Screenshot.grab(mc.gameDirectory, "deformation-instances.png", mc.getMainRenderTarget(), 1, message -> PLibDatabase.LOGGER.info("{}", message.getString()));
		if (++frames == 8)
			advance = true;
	}

	public static void shutdown()
	{
		check(completed && DeformationPlugin.collections == 2 && NativeParityProbe.suites == 2 && NativeParityProbe.armorNodes == 32 && NativeParityProbe.handNodes == 8 && NativeParityProbe.nativeArmCalls == 4 && InstanceParityProbe.RECORDS.size() == 16, "Native bridge/deformation/instance matrix completed before shutdown");
		PLibDatabase.LOGGER.info("STAGE62D2B SHUTDOWN PASS");
	}

	private static final class ProbeScreen extends Screen
	{
		private ProbeScreen()
		{
			super(Component.literal("Native deformation / portable instancing"));
		}

		@Override
		public boolean isPauseScreen()
		{
			return false;
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
		{
			graphics.fill(0, 0, this.width, this.height, 0xFF000000);
			graphics.text(Minecraft.getInstance().font, "Frozen native arm vertices / 6 instances / 16+32 bit / dynamic", 8, 16, -1, false);
		}
	}

	private static void check(boolean value, String message)
	{
		if (!value)
			throw new AssertionError("STAGE62D2B: " + message);
	}
}
