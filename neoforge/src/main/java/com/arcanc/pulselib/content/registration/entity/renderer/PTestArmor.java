/**
 * @author ArcAnc
 * Created at: 23.05.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.entity.renderer;


import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import com.arcanc.pulselib.content.renderer.PEntityRenderLayer;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;

public class PTestArmor extends PEntityRenderLayer<TestEntity>
{
	public static final PModelData MODEL_DATA = PModelData.entityLayer(
			PLibRegistration.EntityTypeReg.TEST_ENTITY.getId(), PLibDatabase.rl("armor"));

	public PTestArmor()
	{
		super(MODEL_DATA,
				PRenderTypes.RenderTypeProvider :: trianglesSolid);
	}
	
	@Override
	public boolean shouldRender(TestEntity animatable)
	{
		return animatable.showArmor;
	}
}
