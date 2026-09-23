/**
 * @author ArcAnc
 * Created at: 30.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.PlayerModel;

import java.util.List;

public enum PPlayerPart
{
	ROOT,
	HEAD,
	BODY,
	RIGHT_ARM,
	LEFT_ARM,
	RIGHT_LEG,
	LEFT_LEG;

	public List<ModelPart> resolve(PlayerModel<?> model)
	{
		return switch (this)
		{
			case ROOT -> List.of();
			case HEAD -> List.of(model.head, model.hat);
			case BODY -> List.of(model.body, model.jacket);
			case RIGHT_ARM -> List.of(model.rightArm, model.rightSleeve);
			case LEFT_ARM -> List.of(model.leftArm, model.leftSleeve);
			case RIGHT_LEG -> List.of(model.rightLeg, model.rightPants);
			case LEFT_LEG -> List.of(model.leftLeg, model.leftPants);
		};
	}
}
