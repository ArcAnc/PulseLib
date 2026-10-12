/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.registration.item.TestTailItem;
import com.arcanc.pulselib.content.registration.player.PPlayerAcrobaticDemo;
import com.arcanc.pulselib.content.registration.player.PPlayerBallDemo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

public final class DemoClient
{
	private static @Nullable ClientLevel previous;

	private DemoClient()
	{
	}

	public static void tick()
	{
		ClientLevel current = Minecraft.getInstance().level;
		if (previous != current)
			TestTailItem.clearControllers();
		previous = current;
		PPlayerBallDemo.clientTick();
		PPlayerAcrobaticDemo.clientTick();
	}
}
