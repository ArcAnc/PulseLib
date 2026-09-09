/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.event;


import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public class CommonEvents
{
	public static void registerCommonEvents(final IEventBus modEventBus)
	{
		modEventBus.addListener(CommonEvents :: registerAttributes);
	}
	
	private static void registerAttributes(final EntityAttributeCreationEvent event)
	{
		event.put(PLibRegistration.EntityTypeReg.TEST_ENTITY.get(), TestEntity.createAttributes().build());
	}
}
