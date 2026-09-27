/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.block.block_entity.ber;


import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.renderer.PBlockRenderer;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class TestBlockEntityRenderer extends PBlockRenderer<TestBlockEntity>
{
	public static final ResourceLocation TUBE = PLibDatabase.rl("block/test_block/tube_texture");
	public static final ResourceLocation TORUS = PLibDatabase.rl("block/test_block/torus_texture");
	public static final ResourceLocation PYRAMID = PLibDatabase.rl("block/test_block/pyramid_texture");
	public static final ResourceLocation CUBE = PLibDatabase.rl("block/test_block/cube_texture");
	public static final ResourceLocation ZERO = PLibDatabase.rl("block/test_block/0");
	public static final PModelData MODEL_DATA = PModelData.block(PLibDatabase.rl("test_block"));
	
	public TestBlockEntityRenderer(final BlockEntityRendererProvider.Context ctx)
	{
		super(MODEL_DATA,
				PRenderTypes.RenderTypeProvider :: trianglesTranslucent);
	}
	
	@Override
	protected PMeshRenderContext resolveBoneRender(TestBlockEntity animatable,
	                                               PBakedBone bone,
	                                               PMeshRenderContext inherited,
	                                               float partialTick)
	{
		return bone.parent() == null ? inherited.withAlphaMode(PAlphaMode.AUTO) : inherited;
	}
}
