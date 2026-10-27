/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.base;

import com.arcanc.pulselib.content.player.deformer.PPlayerDeformerSnapshot;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/** Restores shared model transforms, visibility and cube deformer bindings, including children. */
@ApiStatus.Internal
public final class PModelPartSnapshot implements AutoCloseable
{
	private final List<SavedPart> parts;
	private final PPlayerDeformerSnapshot deformers;

	private PModelPartSnapshot(ModelPart root)
	{
		this.parts = root.getAllParts().stream().map(SavedPart::new).toList();
		this.deformers = PPlayerDeformerSnapshot.capture(root);
	}

	public static PModelPartSnapshot capture(ModelPart root)
	{
		return new PModelPartSnapshot(root);
	}

	@Override
	public void close()
	{
		this.restore();
	}

	/** Replays the saved pose without replacing the snapshot with the consumer's current state. */
	public void restore()
	{
		this.parts.forEach(SavedPart::restore);
		this.deformers.restore();
	}

	private record SavedPart(ModelPart part, PartPose pose, float xScale, float yScale, float zScale, boolean visible, boolean skipDraw)
	{
		private SavedPart(ModelPart part)
		{
			this(part, part.storePose(), part.xScale, part.yScale, part.zScale, part.visible, part.skipDraw);
		}

		private void restore()
		{
			this.part.loadPose(this.pose);
			this.part.xScale = this.xScale;
			this.part.yScale = this.yScale;
			this.part.zScale = this.zScale;
			this.part.visible = this.visible;
			this.part.skipDraw = this.skipDraw;
		}
	}
}
