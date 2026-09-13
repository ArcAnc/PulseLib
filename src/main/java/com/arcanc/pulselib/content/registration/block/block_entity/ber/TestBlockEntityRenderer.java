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
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.renderState.TestBlockEntityRenderState;
import com.arcanc.pulselib.content.registration.renderer.TestDayTimeColor;
import com.arcanc.pulselib.content.renderer.PBlockRenderer;
import com.arcanc.pulselib.content.renderer.modelData.DefaultBlockModelData;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;

public class TestBlockEntityRenderer extends PBlockRenderer<TestBlockEntity, TestBlockEntityRenderState>
{
	public static final Identifier TUBE = PLibDatabase.rl("block/test_block/tube_texture");
	public static final Identifier TORUS = PLibDatabase.rl("block/test_block/torus_texture");
	public static final Identifier PYRAMID = PLibDatabase.rl("block/test_block/pyramid_texture");
	public static final Identifier CUBE = PLibDatabase.rl("block/test_block/cube_texture");
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param ctx the ctx to use.
	 */
	public TestBlockEntityRenderer(final BlockEntityRendererProvider.Context ctx)
	{
		super(new DefaultBlockModelData.DefaultBlockModelDataBuilder(PLibDatabase.rl("test_block")).
				build(), PRenderTypes.RenderTypeProvider :: trianglesTranslucent);
	}
	
	/**
	 * Creates the render state.
	 * @return the value produced by this operation.
	 */
	@Override
	public TestBlockEntityRenderState createRenderState()
	{
		return new TestBlockEntityRenderState();
	}
	
	/**
	 * Resolves the mesh render.
	 * @param renderState the render state to use.
	 * @param bone the bone to use.
	 * @param mesh the mesh to use.
	 * @param inherited the inherited to use.
	 * @return the value produced by this operation.
	 */
	@Override
	protected PMeshRenderContext resolveMeshRender(TestBlockEntityRenderState renderState, PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited)
	{
		return inherited.withColor(
				TestDayTimeColor.color(renderState.getAnimatable().getLevel(), renderState.partialTick()));
	}
}
