/**
 * @author ArcAnc
 * Created at: 28.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.base;

import com.arcanc.pulselib.content.mixin.PoseStackAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import org.jetbrains.annotations.ApiStatus;

/** Owns callback-local transforms and model bindings; submitted geometry and render targets keep their existing owners. */
@ApiStatus.Internal
public final class PRenderCallbackScope implements AutoCloseable
{
	private final PoseStack poses;
	private final int depth;
	private final PoseStack.Pose pose;
	private final PModelPartSnapshot model;

	private PRenderCallbackScope(PoseStack poses, ModelPart root)
	{
		this.poses = poses;
		this.depth = ((PoseStackAccessor)poses).pulselib$getDepth();
		this.pose = poses.last().copy();
		this.model = PModelPartSnapshot.capture(root);
	}

	public static PRenderCallbackScope capture(PoseStack poses, ModelPart root)
	{
		return new PRenderCallbackScope(poses, root);
	}

	public static PRenderCallbackScope player(PoseStack poses, AbstractClientPlayer player)
	{
		return capture(poses, Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player).getModel().root());
	}

	@Override
	public void close()
	{
		try
		{
			this.model.restore();
		}
		finally
		{
			while (((PoseStackAccessor)this.poses).pulselib$getDepth() > this.depth)
				this.poses.popPose();
			this.poses.last().set(this.pose);
		}
	}
}
