/**
 * @author ArcAnc
 * Created at: 20.05.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.entity.renderer;


import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import com.arcanc.pulselib.content.renderer.PEntityRenderLayer;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import com.arcanc.pulselib.content.renderer.modelData.DefaultEntityLayerModelData;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;

public class PTestArmor extends PEntityRenderLayer<TestEntity, PEntityRenderState.LivingImpl<TestEntity>>
{
	/**
	 * Creates an instance of the enclosing type.
	 */
	public PTestArmor()
	{
		super(new DefaultEntityLayerModelData.
						DefaultEntityLayerModelDataBuilder(PLibRegistration.EntityTypeReg.TEST_ENTITY.getId(),
						PLibDatabase.rl("armor")).
							build(),
				PRenderTypes.RenderTypeProvider :: trianglesSolid);
	}
	
	/**
	 * Performs the should render operation.
	 * @param renderState the render state to use.
	 * @return the value produced by this operation.
	 */
	@Override
	public boolean shouldRender(PEntityRenderState.LivingImpl<TestEntity> renderState)
	{
		return renderState.getAnimatable().showArmor;
	}
}
