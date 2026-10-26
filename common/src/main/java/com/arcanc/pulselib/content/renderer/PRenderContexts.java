/**
 * @author ArcAnc
 * Created at: 17.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;

import com.arcanc.pulselib.content.renderer.legacy.GlRenderState;
import com.arcanc.pulselib.content.renderer.gl.PGlRenderHost;
import com.arcanc.pulselib.content.renderer.plan.PFrameCompiler;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.jspecify.annotations.Nullable;
import org.joml.Vector4f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Render-thread scopes. Select the Minecraft output attachment overrides before entering a local scope.
 * SubmitNodeCollector extraction is deferred: enter around execution, not GUI state extraction.
 */
public final class PRenderContexts
{
	private static final Deque<Scope> SCOPES = new ArrayDeque<>();

	private PRenderContexts()
	{
	}

	/** Render environment used to select queue ownership and submission timing. */
	public enum Context
	{
		/** World rendering with stage-based queue flushing. */
		WORLD,
		/** Isolated GUI rendering. */
		GUI,
		/** Isolated rendering into an offscreen target. */
		OFFSCREEN,
		/** First-person arm, held-item and attachment rendering. */
		FIRST_PERSON
	}

	/** Resolves the explicit invocation scope or the active Minecraft attachment overrides. */
	public static Context current()
	{
		RenderSystem.assertOnRenderThread();
		Scope scope = SCOPES.peek();
		if (scope != null && scope.matchesTarget())
			return scope.context;
		return RenderSystem.outputColorTextureOverride == null ? Context.GUI : Context.OFFSCREEN;
	}

	/** Enters a render context; close local scopes before releasing their output attachments. */
	public static Scope enter(Context context)
	{
		RenderSystem.assertOnRenderThread();
		Scope scope = new Scope(Objects.requireNonNull(context));
		SCOPES.push(scope);
		return scope;
	}

	/** Supports direct entity calls as well as dispatcher calls within explicit world/GUI scopes. */
	public static @Nullable Scope enterEntity()
	{
		Context context = current();
		if (context == Context.WORLD || context == Context.FIRST_PERSON ||
				(localScope() != null && localScope().matchesTarget()))
			return null;
		return enter(context);
	}

	static PRenderQueue.RenderStage entityStage()
	{
		RenderSystem.assertOnRenderThread();
		Scope scope = SCOPES.peek();
		return scope != null && scope.context == Context.FIRST_PERSON ?
				PRenderQueue.RenderStage.FIRST_PERSON : PRenderQueue.RenderStage.ENTITIES;
	}

	static @Nullable Scope localScope()
	{
		RenderSystem.assertOnRenderThread();
		Scope scope = SCOPES.peek();
		return scope != null && scope.compiler != null ? scope : null;
	}

	/** Each local scope owns submissions and never drains the world compiler or its pending OIT. */
	public static final class Scope implements AutoCloseable
	{
		private final Context context;
		private final Thread thread = Thread.currentThread();
		private final @Nullable GpuTextureView colorOverride = RenderSystem.outputColorTextureOverride;
		private final @Nullable GpuTextureView depthOverride = RenderSystem.outputDepthTextureOverride;
		private final @Nullable GlRenderState state;
		private final @Nullable PRenderFrame frame;
		final @Nullable PFrameCompiler<PRenderQueue.RenderStage> compiler;
		private boolean closed;

		private Scope(Context context)
		{
			this.context = context;
			boolean local = context == Context.GUI || context == Context.OFFSCREEN;
			this.state = local ? new GlRenderState() : null;
			this.frame = local ? new PGlRenderHost().captureFrame() : null;
			this.compiler = local ? new PFrameCompiler<>(instance ->
			{
				Vector4f position = new Vector4f(instance.transform().m30(), instance.transform().m31(), instance.transform().m32(), 1);
				this.frame.modelView().transform(position);
				this.frame.projection().transform(position);
				return position.w == 0 ? position.z : position.z / position.w;
			}) : null;
		}

		private boolean matchesTarget()
		{
			return this.colorOverride == RenderSystem.outputColorTextureOverride && this.depthOverride == RenderSystem.outputDepthTextureOverride;
		}

		/** Drops incomplete geometry when a renderer throws; close still restores state. */
		public void discard()
		{
			RenderSystem.assertOnRenderThread();
			if (this.compiler != null)
				this.compiler.clear();
		}

		/** Flushes the local plan with captured matrices/attachments and restores the caller, even on failure. */
		@Override
		public void close()
		{
			if (this.closed)
				return;
			if (Thread.currentThread() != this.thread || SCOPES.peek() != this)
				throw new IllegalStateException("Render scopes must close on their render thread in reverse order");
			try
			{
				if (this.compiler != null)
				{
					Objects.requireNonNull(this.state).bindTarget();
					PRenderQueue.flushLocal(this.compiler, Objects.requireNonNull(this.frame));
				}
			}
			finally
			{
				try
				{
					if (this.state != null)
						this.state.close();
				}
				finally
				{
					SCOPES.pop();
					if (SCOPES.isEmpty() && this.compiler != null)
						PRenderQueue.finishLocalFrame();
					this.closed = true;
				}
			}
		}
	}
}
