/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;

public interface PAnimationEventType<T>
{
	ResourceLocation id();

	MapCodec<T> codec();

	PEventSide side();

	void execute(PAnimationEventContext context, T data);
}
