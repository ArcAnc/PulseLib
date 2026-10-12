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

public final class PPoseStream
{
	private final List<Float> values = new ArrayList<>();
	private boolean dirty;

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

	public List<Float> values()
	{
		return this.values;
	}

	public boolean dirty()
	{
		return this.dirty;
	}

	public void markUploaded()
	{
		this.dirty = false;
	}

	public void finishFrame()
	{
		this.values.clear();
		this.dirty = true;
	}
}
