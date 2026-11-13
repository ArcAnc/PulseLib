/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.arcanc.pulselib.content.mixin.GlDeviceFrameBufferAccessor;
import com.arcanc.pulselib.content.mixin.GpuDeviceAccessor;
import com.arcanc.pulselib.content.renderer.gl.PGlRenderHost;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

/** Exercises the production Mixin on real native projection buffers, including the cached upload path. */
final class RhiHostChecks
{
	static void run(GpuDevice device)
	{
		passes(device);
		var previousProjection = RenderSystem.getProjectionMatrixBuffer();
		var previousType = RenderSystem.getProjectionType();
		var previousModelView = RenderSystem.getModelViewMatrixCopy();
		var host = new PRhiRenderHost();
		try
		{
			GpuBufferSlice closed;
			try (var first = new ProjectionMatrixBuffer("Host first");
					var second = new ProjectionMatrixBuffer("Host second"))
			{
				var projectionMatrix = new Matrix4f().translation(-0.25f, 0.5f, 0);
				var modelView = new Matrix4f().translation(2, 3, 4);
				RenderSystem.getModelViewStack().set(modelView);
				closed = first.getBuffer(projectionMatrix);
				RenderSystem.setProjectionMatrix(closed, ProjectionType.ORTHOGRAPHIC);
				PRenderFrame saved = host.captureFrame();
				assertFrame(saved, modelView, projectionMatrix);
				assertFrame(new PGlRenderHost().captureFrame(), modelView, projectionMatrix);
				projectionMatrix.zero();
				RenderSystem.getModelViewStack().identity();
				assertFrame(saved, modelView, new Matrix4f().translation(-0.25f, 0.5f, 0));
				RhiLayoutChecks.assertTrue(host.captureFrame().projection().equals(saved.projection()), "Upload mirror copied mutable matrix input");

				var projection = new Projection();
				projection.setupOrtho(0.1f, 100, 320, 240, true);
				var expected = projection.getMatrix(new Matrix4f());
				var cached = second.getBuffer(projection);
				RenderSystem.setProjectionMatrix(cached, ProjectionType.ORTHOGRAPHIC);
				assertFrame(host.captureFrame(), new Matrix4f(), expected);
				RhiLayoutChecks.assertTrue(second.getBuffer(projection) == cached, "Native unchanged Projection cache hit");
				assertFrame(host.captureFrame(), new Matrix4f(), expected);
				projection.setupPerspective(0.05f, 1000, 70, 1920, 1080);
				expected = projection.getMatrix(new Matrix4f());
				RhiLayoutChecks.assertTrue(second.getBuffer(projection) == cached, "Native projection rewrite reuses slice");
				RenderSystem.setProjectionMatrix(cached, ProjectionType.PERSPECTIVE);
				assertFrame(host.captureFrame(), new Matrix4f(), expected);
				RenderSystem.setProjectionMatrix(closed, ProjectionType.ORTHOGRAPHIC);
				assertFrame(host.captureFrame(), new Matrix4f(), saved.projection());
				device.createCommandEncoder().submit();
			}
			expectMissingFrame(host);
			try
			{
				PRhiRenderHost.recordProjection(closed, new Matrix4f());
				throw new AssertionError("Closed projection buffer accepted");
			}
			catch (IllegalStateException expected)
			{
				RhiLayoutChecks.assertTrue(expected.getMessage().contains("closed"), "Closed buffers rejected");
			}

			try (var external = device.createBuffer(() -> "Untracked host projection", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 64))
			{
				var slice = external.slice();
				RenderSystem.setProjectionMatrix(slice, ProjectionType.ORTHOGRAPHIC);
				expectMissingFrame(host);
				var matrix = new Matrix4f().translation(1, 2, 3);
				device.createCommandEncoder().writeToBuffer(slice, matrix.get(ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder())));
				PGlRenderHost.recordProjection(slice, matrix);
				assertFrame(host.captureFrame(), new Matrix4f(), matrix);
				PRhiRenderHost.forgetProjection(slice);
				expectMissingFrame(host);
				device.createCommandEncoder().submit();
			}
			System.out.println("PASS production projection Mixin/native host: copied matrices, cached/replaced uploads, active buffer switching, legacy facade and close/untracked rejection");
		}
		finally
		{
			RenderSystem.setProjectionMatrix(previousProjection, previousType);
			RenderSystem.getModelViewStack().set(previousModelView);
		}
	}

	private static void passes(GpuDevice device)
	{
		RhiLayoutChecks.assertTrue(!PRhiRenderPassTracker.hasActivePass(), "No native pass before scope");
		var backend = ((GpuDeviceAccessor)device).pulselib$getBackend();
		if (backend instanceof GlDeviceFrameBufferAccessor gl)
			RhiLayoutChecks.assertTrue(gl.pulselib$getFrameBufferCache() != null, "Actual GL device FBO cache available to legacy wrappers");
		try (var texture = device.createTexture("Native pass tracker", GpuTexture.USAGE_RENDER_ATTACHMENT,
				GpuFormat.RGBA8_UNORM, 2, 2, 1, 1);
				var view = device.createTextureView(texture))
		{
			var encoder = device.createCommandEncoder();
			try (var pass = encoder.createRenderPass(() -> "Native tracker pass", view, Optional.empty()))
			{
				RhiLayoutChecks.assertTrue(encoder != device.createCommandEncoder(), "26.2 returns fresh frontend encoders");
				RhiLayoutChecks.assertTrue(PRhiRenderPassTracker.hasActivePass(), "Active pass visible across frontend encoder instances");
			}
			RhiLayoutChecks.assertTrue(!PRhiRenderPassTracker.hasActivePass(), "Native close clears tracker");
			var sentinel = new IllegalStateException("Pass body failure");
			try (var pass = encoder.createRenderPass(() -> "Native exceptional scope", view, Optional.empty()))
			{
				throw sentinel;
			}
			catch (IllegalStateException failure)
			{
				RhiLayoutChecks.assertTrue(failure == sentinel, "Pass body exception propagated");
			}
			RhiLayoutChecks.assertTrue(!PRhiRenderPassTracker.hasActivePass(), "Exception-safe native close clears tracker");
			encoder.submit();
		}
		System.out.println("PASS native pass tracker: fresh encoders, shared active state and normal/exceptional close; backend cache accessor where applicable");
	}

	private static void assertFrame(PRenderFrame frame, Matrix4f modelView, Matrix4f projection)
	{
		RhiLayoutChecks.assertTrue(frame.modelView().equals(modelView) && frame.projection().equals(projection), "Captured frame equals active native matrices");
	}

	private static void expectMissingFrame(PRhiRenderHost host)
	{
		try
		{
			host.captureFrame();
			throw new AssertionError("Unavailable projection silently accepted");
		}
		catch (IllegalStateException expected)
		{
			RhiLayoutChecks.assertTrue(expected.getMessage().contains("projection"), "Explicit missing-projection failure");
		}
	}
}
