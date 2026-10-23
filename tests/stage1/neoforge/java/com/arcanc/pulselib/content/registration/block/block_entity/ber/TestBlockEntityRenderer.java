/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.block.block_entity.ber;


import com.arcanc.pulselib.content.model.baked.PBakedModel;
import org.jspecify.annotations.Nullable;
import com.arcanc.pulselib.test.stage1.Stage1Client;
import com.arcanc.pulselib.test.stage1.Stage41b2BakedProbe;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.renderState.TestBlockEntityRenderState;
import com.arcanc.pulselib.content.registration.renderer.TestDayTimeColor;
import com.arcanc.pulselib.content.renderer.PBlockRenderer;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;

/**
 * Renders test block entity.
 */
public class TestBlockEntityRenderer extends PBlockRenderer<TestBlockEntity, TestBlockEntityRenderState>
{
	public static final PModelData MODEL_DATA = PModelData.block(PLibDatabase.rl("test_block"));

	public static final Identifier TUBE = PLibDatabase.rl("block/test_block/tube_texture");
	public static final Identifier TORUS = PLibDatabase.rl("block/test_block/torus_texture");
	public static final Identifier PYRAMID = PLibDatabase.rl("block/test_block/pyramid_texture");
	public static final Identifier CUBE = PLibDatabase.rl("block/test_block/cube_texture");
	
	@Override
	public PModelData getModelData(TestBlockEntity animatable)
	{
		if (Boolean.getBoolean("pulselib.stage41b2Probe"))
			Stage41b2BakedProbe.ownerLookup(animatable);
		return super.getModelData(animatable);
	}

	@Override
	public @Nullable PBakedModel getModel(TestBlockEntity animatable)
	{
		if (Boolean.getBoolean("pulselib.stage41b2Probe"))
			Stage41b2BakedProbe.ownerModelLookup(animatable);
		return super.getModel(animatable);
	}

	/**
	 * Creates an instance of the enclosing type.
	 * @param ctx the ctx to use.
	 */
	public TestBlockEntityRenderer(final BlockEntityRendererProvider.Context ctx)
	{
		super(MODEL_DATA, PRenderTypes.RenderTypeProvider :: trianglesCutout);
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

	@Override
	public void postSubmit(PoseStack poseStack, TestBlockEntityRenderState renderState,
			CameraRenderState camera,
			SubmitNodeCollector collector)
	{
		super.postSubmit(poseStack, renderState, camera, collector);
		if (renderState.getBakedModel() != null)
		{
			Stage1Client.blockSubmissions++;
		}
	}
}
