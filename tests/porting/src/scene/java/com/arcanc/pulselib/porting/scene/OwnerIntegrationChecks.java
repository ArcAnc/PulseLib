/**
 * @author ArcAnc
 * Created at: 20.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.renderer.PBlockRenderer;
import com.arcanc.pulselib.content.renderer.PEntityRenderer;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.base.PBlockRenderState;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public final class OwnerIntegrationChecks
{
	static final int ENTITY_OPAQUE = 0xFF01F1A1;
	static final int BLOCK_OPAQUE = 0xFFF1A101;
	static final int ENTITY_ALPHA = 0x800091E1;
	static final int BLOCK_ALPHA = 0x80E19100;
	static final int GROUND_OPAQUE = 0xFF101EA1;
	static final int GUI_ENTITY = 0xFF10E1C1;

	private OwnerIntegrationChecks()
	{
	}

	private static PModelData model()
	{
		var mesh = SceneChecks.baked(IntegrationChecks.GEOMETRY, IntegrationChecks.TEXTURE, false, true);
		var model = new PBakedModel(List.of(new PBakedBone("root", new Vector3f(), new Quaternionf(), List.of(), null, List.of(mesh))), Map.of());
		return new PModelData(IntegrationChecks.TEXTURE, PModelData.DEFAULT_MODEL_LOADER)
		{
			@Override
			public PBakedModel getModel()
			{
				return model;
			}
		};
	}

	private static PInstanceHeader header(PoseStack poses, int color)
	{
		return new PInstanceHeader(new Matrix4f(poses.last().pose()).scale(0.5f), color, 0xF000F0, OverlayTexture.NO_OVERLAY);
	}

	static final class EntityOwner extends ArmorStand implements PAnimatable<EntityOwner>
	{
		private final PAnimationManager<EntityOwner> manager = PLibHelper.createManager(this);

		EntityOwner(Level level, double x, double y, double z)
		{
			super(level, x, y, z);
			setNoGravity(true);
		}

		@Override
		public PAnimationManager<EntityOwner> getAnimationManager(AnimManagerKey key)
		{
			return this.manager;
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<EntityOwner> registrar)
		{
		}
	}

	static final class BlockOwner extends ChestBlockEntity implements PAnimatable<BlockOwner>
	{
		private final PAnimationManager<BlockOwner> manager = PLibHelper.createManager(this);

		BlockOwner(BlockPos pos, BlockState state)
		{
			super(pos, state);
		}

		@Override
		public PAnimationManager<BlockOwner> getAnimationManager(AnimManagerKey key)
		{
			return this.manager;
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<BlockOwner> registrar)
		{
		}
	}

	public static final class EntityRenderer extends PEntityRenderer<EntityOwner, PEntityRenderState.Impl<EntityOwner>>
	{
		public EntityRenderer(EntityRendererProvider.Context context)
		{
			super(context, model(), PRenderTypes.RenderTypeProvider::trianglesSolid);
		}

		@Override
		public PEntityRenderState.Impl<EntityOwner> createRenderState()
		{
			return new PEntityRenderState.Impl<>();
		}

		@Override
		protected PMeshRenderContext resolveBoneRender(PEntityRenderState.Impl<EntityOwner> state, PBakedBone bone, PMeshRenderContext context)
		{
			return context.withColor(PRenderContexts.current() == PRenderContexts.Context.GUI ? GUI_ENTITY : ENTITY_OPAQUE);
		}

		@Override
		public void postSubmit(PoseStack poses, PEntityRenderState.Impl<EntityOwner> state, CameraRenderState camera, SubmitNodeCollector collector)
		{
			if (PRenderContexts.current() == PRenderContexts.Context.WORLD)
			{
				PRenderQueue.submitItem(ItemDisplayContext.GROUND, PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(IntegrationChecks.TEXTURE),
						IntegrationChecks.GEOMETRY, header(poses, GROUND_OPAQUE));
				PRenderQueue.submitEntityMesh(PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(IntegrationChecks.TEXTURE),
						IntegrationChecks.GEOMETRY, header(poses, ENTITY_ALPHA), PTransparencyMode.ORDINARY);
			}
		}
	}

	public static final class BlockRenderer extends PBlockRenderer<BlockOwner, PBlockRenderState.Impl<BlockOwner>>
	{
		public BlockRenderer()
		{
			super(model(), PRenderTypes.RenderTypeProvider::trianglesSolid);
		}

		@Override
		public PBlockRenderState.Impl<BlockOwner> createRenderState()
		{
			return new PBlockRenderState.Impl<>();
		}

		@Override
		protected PMeshRenderContext resolveBoneRender(PBlockRenderState.Impl<BlockOwner> state, PBakedBone bone, PMeshRenderContext context)
		{
			return context.withColor(BLOCK_OPAQUE);
		}

		@Override
		public void postSubmit(PoseStack poses, PBlockRenderState.Impl<BlockOwner> state, CameraRenderState camera, SubmitNodeCollector collector)
		{
			PRenderQueue.submit(PRenderQueue.RenderStage.TRANSLUCENT_BLOCKS, PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(IntegrationChecks.TEXTURE),
					IntegrationChecks.GEOMETRY, header(poses, BLOCK_ALPHA), PTransparencyMode.ORDINARY);
		}
	}
}
