package com.arcanc.pulselib.api.registration;

import com.arcanc.pulselib.util.attachments.PLivingAttachmentDefinition;
import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

public class PAttachmentRegistrar extends PulseRegistrar
{
	private final List<Runnable> actions = new ArrayList<>();

	public void registerLiving(ItemLike item, PLivingAttachmentDefinition definition)
	{
		checkOpen();
		java.util.Objects.requireNonNull(definition);
		var value = item.asItem();
		this.actions.add(() -> PLivingAttachments.register(value, definition));
	}

	public void registerGlobalLiving(PLivingAttachmentDefinition definition)
	{
		checkOpen();
		java.util.Objects.requireNonNull(definition);
		this.actions.add(() -> PLivingAttachments.registerGlobal(definition));
	}

	public void apply()
	{
		checkOpen();
		this.actions.forEach(Runnable::run);
		close();
	}
}
