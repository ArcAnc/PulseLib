/**
 * @author ArcAnc
 * Created at: 04.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;


public record PPlayerFirstPersonSettings(boolean enable)
{
	public static final PPlayerFirstPersonSettings DISABLED =
			new PPlayerFirstPersonSettings(false);
	
	public static final PPlayerFirstPersonSettings ENABLED =
			new PPlayerFirstPersonSettings(true);
	
	public PPlayerFirstPersonSettings copy()
	{
		return new PPlayerFirstPersonSettings(this.enable);
	}
}
