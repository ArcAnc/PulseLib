/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer.gpu;

import java.util.ArrayList;
import java.util.List;

/**
 * Append-only per-frame float stream used to upload pose data to shaders.
 */
public final class PPoseStream
{
	private final List<Float> values = new ArrayList<>();
	private boolean dirty;

	/**
	 * Appends one four-component pose record and returns its stream offset.
	 *
	 * @param x the x component or coordinate
	 * @param y the y component or coordinate
	 * @param z the z component or coordinate
	 * @param w the w component or coordinate
	 * @return the offset of the appended four-component record
	 */
	public int append(float x, float y, float z, float w)
	{
		int offset = this.values.size() / 4;
		this.values.add(x);
		this.values.add(y);
		this.values.add(z);
		this.values.add(w);
		this.dirty = true;
		return offset;
	}

	/**
	 * Returns the pose values collected for the current frame.
	 *
	 * @return the pose values collected for the current frame
	 */
	public List<Float> values()
	{
		return this.values;
	}

	/**
	 * Returns whether the stream contains changes that have not been uploaded.
	 *
	 * @return whether the stream contains changes that have not been uploaded
	 */
	public boolean dirty()
	{
		return this.dirty;
	}

	/**
	 * Marks the current stream contents as uploaded.
	 */
	public void markUploaded()
	{
		this.dirty = false;
	}

	/**
	 * Clears pose records after the frame has finished rendering.
	 */
	public void finishFrame()
	{
		this.values.clear();
		this.dirty = true;
	}
}
