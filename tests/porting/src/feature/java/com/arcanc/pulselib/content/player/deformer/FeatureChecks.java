/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.deformer;

import com.arcanc.pulselib.content.mixin.PoseStackAccessor;
import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import com.arcanc.pulselib.content.renderer.base.PModelPartSubmitState;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorModelSnapshots;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeLibrariesBootstrap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.SharedConstants;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Real native submit/prepare methods and production Mixins; controlled vertex output, no Minecraft/GPU draw claim. */
public final class FeatureChecks
{
	private static final ThreadLocal<@Nullable VertexConsumer> CONSUMER = new ThreadLocal<>();
	private static final RuntimeException SENTINEL = new IllegalStateException("fixture vertex failure");
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("pulselib", "feature_fixture");

	public static @Nullable VertexConsumer currentConsumer()
	{
		return CONSUMER.get();
	}

	public static void main(String[] args) throws Exception
	{
		SharedConstants.tryDetectVersion();
		NativeLibrariesBootstrap.loadLibraries();
		Bootstrap.bootStrap();
		Class.forName("net.minecraft.client.renderer.WeatherEffectRenderer", false, FeatureChecks.class.getClassLoader());
		System.out.println("PASS strict NeoForge weather Redirect: native overloads, require=1/allow=1; no weather GPU draw");
		items();
		modelParts();
		playerGeometry();
		armorProvider();
		check(CONSUMER.get() == null, "Fixture output scope restored");
		System.out.println("PULSELIB_FEATURES_PASSED strict production Mixins/native submit and prepare; CPU vertex output only, no full game or GPU draw");
	}

	private static void items() throws Exception
	{
		var type = PRenderTypes.RenderTypeProvider.trianglesSolid(TEXTURE);
		try (var contents = new SpriteContents(TEXTURE, new FrameSize(2, 2), new NativeImage(2, 2, false)))
		{
			var sprite = new TextureAtlasSprite(TEXTURE, contents, 2, 2, 0, 0, 0)
			{
			};
			var aQuads = new ArrayList<>(List.of(quad(sprite, type, 0), quad(sprite, type, 9)));
			int[] aTints = {0xFF123456};
			var a = new ItemFeatureRenderer.Submit(new PoseStack().last().copy(), ItemDisplayContext.GUI, 0x100020, 0x300040, 0,
					aTints, aQuads, ItemStackRenderState.FoilType.STANDARD);
			aQuads.clear();
			aTints[0] = 0;
			check(a.quads().size() == 2 && a.tintLayers()[0] == 0xFF123456, "Item node owns copied list/tints");
			var b = new ItemFeatureRenderer.Submit(new PoseStack().last().copy(), ItemDisplayContext.GUI, 0x500060, 0x700080, 0,
					new int[]{0xFFABCDEF}, List.of(quad(sprite, type, 0)), ItemStackRenderState.FoilType.STANDARD);
			var renderer = new ItemFeatureRenderer();
			var field = ItemFeatureRenderer.class.getDeclaredField("quadInstance");
			field.setAccessible(true);
			var instance = (QuadInstance)field.get(renderer);
			int[] colors = new int[4];
			int[] lights = new int[4];
			for (int i = 0; i < 4; i++)
			{
				colors[i] = 0xFF110000 + i;
				lights[i] = 0x220000 + i;
				instance.setColor(i, colors[i]);
				instance.setLightCoords(i, lights[i]);
			}
			instance.setOverlayCoords(0x123456);
			var nested = new QuadSink();
			var outer = new QuadSink();
			outer.once = () -> withConsumer(nested, () -> build(renderer, List.of(b)));
			withConsumer(outer, () -> build(renderer, List.of(a)));
			check(outer.quads.equals(List.of(values(a, 0xFF123456), values(a, -1), values(a, 0xFF123456), values(a, -1))), "Main/foil retain each quad's tint/light/overlay");
			check(nested.quads.equals(List.of(values(b, 0xFFABCDEF), values(b, 0xFFABCDEF))), "Nested item's main/foil state");
			var batch = new QuadSink();
			withConsumer(batch, () -> build(renderer, List.of(a, b)));
			check(batch.quads.equals(List.of(values(a, 0xFF123456), values(a, -1), values(b, 0xFFABCDEF),
					values(a, 0xFF123456), values(a, -1), values(b, 0xFFABCDEF))), "Separate foil batch never inherits last main item values");
			var failing = new QuadSink();
			failing.once = FeatureChecks::fail;
			expectFailure(() -> withConsumer(failing, () -> build(renderer, List.of(a))));
			for (int i = 0; i < 4; i++)
				check(instance.getColor(i) == colors[i] && instance.getLightCoords(i) == lights[i], "Every vertex restored after nested/failing preparation");
			check(instance.overlayCoords() == 0x123456, "Item overlay restored");
		}
		System.out.println("PASS native item nodes: immutable inputs, main/foil batches, tint fallback, nesting and per-vertex exception restoration");
	}

	private static BakedQuad quad(TextureAtlasSprite sprite, RenderType type, int tint)
	{
		var material = new BakedQuad.MaterialInfo(sprite, ChunkSectionLayer.SOLID, type, tint, true, 0);
		return new BakedQuad(new Vector3f(), new Vector3f(1, 0, 0), new Vector3f(1, 1, 0), new Vector3f(0, 1, 0),
				0, 0, 0, 0, Direction.NORTH, material);
	}

	private static QuadValues values(ItemFeatureRenderer.Submit submit, int color)
	{
		return new QuadValues(color, submit.lightCoords(), submit.overlayCoords());
	}

	private static void modelParts() throws Exception
	{
		var cube = cube();
		var root = new ModelPart(List.of(cube), Map.of());
		var delta = new AtomicInteger(16);
		var calls = new AtomicInteger();
		cube.setDeformer(position ->
		{
			calls.incrementAndGet();
			position.add(delta.get(), 0, 0);
		});
		root.x = 16;
		var first = new Model.Simple(root, id -> type());
		var firstNode = submit(first, Unit.INSTANCE);
		root.x = 32;
		delta.set(32);
		var secondNode = submit(first, Unit.INSTANCE);
		check((Object)firstNode instanceof PModelPartSubmitState, "Native submit owns ModelPart snapshot");
		int frozenCalls = calls.get();
		delta.set(128);
		root.x = 112;
		root.xScale = 3;
		root.visible = false;
		root.skipDraw = true;
		var binding = cube.getBinding();
		var renderer = new ModelFeatureRenderer();
		var poses = poses(renderer);
		poses.translate(7, 8, 9);
		var entry = new Matrix4f(poses.last().pose());
		var inner = new Sink();
		var outer = new Sink();
		outer.once = () -> withConsumer(inner, () -> build(renderer, List.of(secondNode)));
		withConsumer(outer, () -> build(renderer, List.of(firstNode)));
		check(Math.abs(outer.minX() - 2) < 0.0001f && Math.abs(inner.minX() - 4) < 0.0001f, "Distinct frozen transforms/deformed geometry replay through native Model renderer");
		var failing = new Sink();
		failing.once = () ->
		{
			poses.pushPose();
			throw SENTINEL;
		};
		expectFailure(() -> withConsumer(failing, () -> build(renderer, List.of(firstNode))));
		check(root.x == 112 && root.xScale == 3 && !root.visible && root.skipDraw, "Caller part state restored");
		check(cube.getBinding() == binding && calls.get() == frozenCalls, "No live deformer evaluation during deferred replay; caller binding restored");
		check(((PoseStackAccessor)poses).pulselib$getDepth() == 0 && poses.last().pose().equals(entry), "ModelPart pose stack restored after nesting/exception");
		System.out.println("PASS native Model.Simple/Submit: frozen ModelPart pose/deformation, nested replay, caller flags/bindings and pose-stack rollback");
	}

	private static void playerGeometry() throws Exception
	{
		var cube = cube();
		var root = new ModelPart(List.of(cube), Map.of());
		var delta = new AtomicInteger(16);
		var calls = new AtomicInteger();
		cube.setDeformer(position ->
		{
			calls.incrementAndGet();
			position.add(delta.get(), 0, 0);
		});
		var state = new AvatarRenderState();
		var model = new Model<AvatarRenderState>(root, id -> type())
		{
			@Override
			public void setupAnim(AvatarRenderState avatar)
			{
				this.root.x = 5;
			}
		};
		var geometry = new PPlayerModelGeometry.Entry(model, model, PPlayerDeformerSnapshot.captureGeometry(root));
		((PPlayerGeometryRenderState)state).pulselib$setSubmittingGeometry(geometry);
		var node = submit(model, state);
		((PPlayerGeometryRenderState)state).pulselib$setSubmittingGeometry(null);
		check(((PPlayerModelSubmitState)(Object)node).pulselib$getModelGeometry() == geometry, "Player entry owned by node after state changes");
		root.x = 99;
		var binding = cube.getBinding();
		delta.set(100);
		int frozenCalls = calls.get();
		var renderer = new ModelFeatureRenderer();
		var sink = new Sink();
		withConsumer(sink, () -> build(renderer, List.of(node)));
		check(Math.abs(sink.minX() - 1.3125f) < 0.0001f, "Frozen player geometry applied after native setup");
		var failing = new Sink();
		failing.once = FeatureChecks::fail;
		expectFailure(() -> withConsumer(failing, () -> build(renderer, List.of(node))));
		check(root.x == 99 && cube.getBinding() == binding && calls.get() == frozenCalls, "Player caller pose/binding restored, owner-free geometry replay");
		check(((PoseStackAccessor)poses(renderer)).pulselib$getDepth() == 0, "Player exception pose-stack rollback");
		System.out.println("PASS native model nodes: captured player geometry, post-setup replay and caller restoration without live owner callbacks");
	}

	private static void armorProvider() throws Exception
	{
		var root = LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0), 64, 32).bakeRoot();
		var model = new HumanoidModel<HumanoidRenderState>(root)
		{
			@Override
			public void setupAnim(HumanoidRenderState state)
			{
				this.root.x = -20;
				this.head.x = -30;
			}
		};
		root.x = 42;
		model.head.x = 12;
		var selected = new PArmorModelSnapshots.Entry(model, PModelPartSnapshot.captureForSubmit(root));
		var state = new HumanoidRenderState();
		((PArmorModelSnapshots.State)state).pulselib$setSubmittingArmorProvider(selected);
		var node = submit(model, state);
		((PArmorModelSnapshots.State)state).pulselib$setSubmittingArmorProvider(null);
		check(((PArmorModelSnapshots.Node)(Object)node).pulselib$getArmorProvider() == selected, "Armor provider entry captured on native node");
		root.x = 100;
		model.head.x = 111;
		var renderer = new ModelFeatureRenderer();
		var sink = new Sink();
		sink.once = () -> check(root.x == 42 && model.head.x == 12, "Armor provider snapshot replay follows setupAnim");
		withConsumer(sink, () -> build(renderer, List.of(node)));
		check(!sink.vertices.isEmpty() && root.x == 100 && model.head.x == 111, "Provider caller pose restored");
		var failing = new Sink();
		failing.once = FeatureChecks::fail;
		expectFailure(() -> withConsumer(failing, () -> build(renderer, List.of(node))));
		check(root.x == 100 && model.head.x == 111 && ((PoseStackAccessor)poses(renderer)).pulselib$getDepth() == 0, "Provider exception pose-stack/caller rollback");
		System.out.println("PASS native armor nodes: frozen provider ownership, post-setup replay and exception-safe shared-model restoration");
	}

	private static PDeformedCuboid cube()
	{
		return new PDeformedCuboid(0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, false, 16, 16, EnumSet.allOf(Direction.class));
	}

	private static RenderType type()
	{
		return PRenderTypes.RenderTypeProvider.trianglesSolid(TEXTURE);
	}

	private static <S> ModelFeatureRenderer.Submit<S> submit(Model<? super S> model, S state)
	{
		return new ModelFeatureRenderer.Submit<>(type(), new PoseStack().last().copy(), model, state, 0xF000F0, 0, -1, null, null);
	}

	private static PoseStack poses(ModelFeatureRenderer renderer) throws Exception
	{
		var field = ModelFeatureRenderer.class.getDeclaredField("poseStack");
		field.setAccessible(true);
		return (PoseStack)field.get(renderer);
	}

	private static void build(Object renderer, List<?> submits)
	{
		try
		{
			var method = renderer.getClass().getDeclaredMethod("buildGroup", FeatureFrameContext.class, List.class);
			method.setAccessible(true);
			method.invoke(renderer, null, submits);
		}
		catch (InvocationTargetException failure)
		{
			if (failure.getCause() instanceof RuntimeException runtime)
				throw runtime;
			if (failure.getCause() instanceof Error error)
				throw error;
			throw new AssertionError(failure.getCause());
		}
		catch (ReflectiveOperationException failure)
		{
			throw new AssertionError(failure);
		}
	}

	private static void withConsumer(VertexConsumer sink, Runnable action)
	{
		@Nullable VertexConsumer previous = CONSUMER.get();
		CONSUMER.set(sink);
		try
		{
			action.run();
		}
		finally
		{
			CONSUMER.set(previous);
		}
	}

	private static void fail()
	{
		throw SENTINEL;
	}

	private static void expectFailure(Runnable action)
	{
		try
		{
			action.run();
			throw new AssertionError("Expected native preparation failure");
		}
		catch (RuntimeException failure)
		{
			check(failure == SENTINEL, "Original preparation failure propagated");
		}
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}

	private record QuadValues(int color, int light, int overlay)
	{
	}

	private static class Sink implements VertexConsumer
	{
		final List<Vector3f> vertices = new ArrayList<>();
		@Nullable Runnable once;

		float minX()
		{
			return (float)this.vertices.stream().mapToDouble(Vector3f::x).min().orElseThrow();
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z)
		{
			this.vertices.add(new Vector3f(x, y, z));
			@Nullable Runnable callback = this.once;
			this.once = null;
			if (callback != null)
				callback.run();
			return this;
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha)
		{
			return this;
		}

		@Override
		public VertexConsumer setColor(int color)
		{
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v)
		{
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v)
		{
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v)
		{
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z)
		{
			return this;
		}

		@Override
		public VertexConsumer setLineWidth(float width)
		{
			return this;
		}
	}

	private static final class QuadSink extends Sink
	{
		final List<QuadValues> quads = new ArrayList<>();

		@Override
		public void putBakedQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance)
		{
			var value = new QuadValues(instance.getColor(0), instance.getLightCoords(0), instance.overlayCoords());
			this.quads.add(value);
			@Nullable Runnable callback = this.once;
			this.once = null;
			if (callback != null)
				callback.run();
			for (int vertex = 0; vertex < 4; vertex++)
				check(instance.getColor(vertex) == value.color() && instance.getLightCoords(vertex) == value.light() &&
						instance.overlayCoords() == value.overlay(), "Nested item preparation restored active quad values");
		}
	}
}
