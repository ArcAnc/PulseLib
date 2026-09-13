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

/**
 * Provides support for common events.
 */
public class CommonEvents
{
	/**
	 * Registers the common events.
	 * @param modEventBus the mod event bus to use.
	 */
	public static void registerCommonEvents(final IEventBus modEventBus)
	{
		modEventBus.addListener(CommonEvents :: registerAttributes);
	}
	
	/**
	 * Registers the attributes.
	 * @param event the event to use.
	 */
	private static void registerAttributes(final EntityAttributeCreationEvent event)
	{
		event.put(PLibRegistration.EntityTypeReg.TEST_ENTITY.get(), TestEntity.createAttributes().build());
	}
}
