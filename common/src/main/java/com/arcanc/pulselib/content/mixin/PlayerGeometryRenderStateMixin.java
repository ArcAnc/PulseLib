/**
 * @author ArcAnc
 * Created at: 28.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.player.deformer.PPlayerModelGeometry.Entry;
import com.arcanc.pulselib.content.player.deformer.PPlayerGeometryRenderState;
import com.arcanc.pulselib.content.player.deformer.PPlayerModelGeometry;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class PlayerGeometryRenderStateMixin implements PPlayerGeometryRenderState
{
	@Unique
	private @Nullable PPlayerModelGeometry pulselib$geometry;
	@Unique
	private @Nullable Entry pulselib$submitting;

	@Override
	public @Nullable PPlayerModelGeometry pulselib$getPlayerGeometry()
	{
		return this.pulselib$geometry;
	}

	@Override
	public void pulselib$setPlayerGeometry(@Nullable PPlayerModelGeometry geometry)
	{
		this.pulselib$geometry = geometry;
	}

	@Override
	public @Nullable Entry pulselib$getSubmittingGeometry()
	{
		return this.pulselib$submitting;
	}

	@Override
	public void pulselib$setSubmittingGeometry(@Nullable Entry entry)
	{
		this.pulselib$submitting = entry;
	}
}
