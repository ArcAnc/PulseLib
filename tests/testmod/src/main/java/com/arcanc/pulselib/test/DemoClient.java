package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.registration.item.TestTailItem;
import com.arcanc.pulselib.content.registration.player.PPlayerBallDemo;
import com.arcanc.pulselib.content.registration.player.PPlayerAcrobaticDemo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class DemoClient
{
	private static ClientLevel previous;
	private DemoClient() { }
	public static void tick()
	{
		ClientLevel current = Minecraft.getInstance().level;
		if (previous != current) TestTailItem.clearControllers();
		previous = current;
		PPlayerBallDemo.clientTick();
		PPlayerAcrobaticDemo.clientTick();
	}
}
