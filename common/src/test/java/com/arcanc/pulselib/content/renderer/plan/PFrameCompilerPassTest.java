/**
 * @author ArcAnc
 * Created at: 17.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.plan;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PFrameCompilerPassTest
{
	private final PFrameCompiler<String> compiler = new PFrameCompiler<>();
	private final PPipelineHandle pipeline = new PPipelineHandle(1);
	private final PMeshHandle mesh = new PMeshHandle(1);

	@Test
	void earlyOpaquePassPreservesInstancingAndRetainsTranslucency()
	{
		submit("entity", 3, false);
		submit("entity", 4, false);
		submit("entity", 5, true);
		var opaque = this.compiler.compileOpaque("entity");
		assertEquals(1, opaque.groups().size());
		assertEquals(2, opaque.groups().getFirst().command().instanceCount());
		assertTrue(opaque.groups().getFirst().writeDepth());
		assertTrue(this.compiler.compileOpaque("entity").isEmpty());
		var translucent = this.compiler.compile("entity");
		assertEquals(1, translucent.instances().size());
		assertFalse(translucent.groups().getFirst().writeDepth());
		assertTrue(this.compiler.compile("entity").isEmpty());
	}

	@Test
	void ordinaryTransparencySortsAcrossEntityAndBlockSources()
	{
		submit("entity", 3, true);
		submit("block", 8, true);
		submit("entity", 5, true);
		var plan = this.compiler.compileCombined(List.of("entity", "block"), ignored -> false);
		assertEquals(List.of(8f, 5f, 3f), plan.instances().stream().map(instance -> instance.transform().m32()).toList());
		assertTrue(this.compiler.compile("entity").isEmpty());
		assertTrue(this.compiler.compile("block").isEmpty());
	}

	@Test
	void oitStillBatchesAndCleanupRemovesBothPasses()
	{
		submit("entity", 2, true);
		submit("entity", 4, true);
		var oit = this.compiler.compileCombined(List.of("entity"), ignored -> true);
		assertEquals(1, oit.groups().size());
		assertEquals(2, oit.groups().getFirst().command().instanceCount());
		submit("entity", 1, false);
		submit("block", 1, true);
		this.compiler.clear();
		assertTrue(this.compiler.compileCombined(List.of("entity", "block"), ignored -> true).isEmpty());
	}

	@Test
	void localDepthCanSortOrthographicGeometryAndIncludeCustomStages()
	{
		var local = new PFrameCompiler<String>(instance -> instance.transform().m32());
		local.submit("custom_gui", this.pipeline, this.mesh, 6,
				new PInstanceHeader(new Matrix4f().translation(0, 0, -0.8f), -1, 0, 0), true);
		local.submit("entity", this.pipeline, this.mesh, 6,
				new PInstanceHeader(new Matrix4f().translation(0, 0, 0.2f), -1, 0, 0), true);
		var plan = local.compileAll(ignored -> false);
		assertEquals(List.of(0.2f, -0.8f), plan.instances().stream().map(instance -> instance.transform().m32()).toList());
		assertTrue(local.compileAll(ignored -> false).isEmpty());
	}

	@Test
	void interruptedWorldDiscardsBothPassesWithoutDrainingHandCommands()
	{
		submit("entity", 1, false);
		submit("entity", 1, true);
		submit("hand", 1, false);
		this.compiler.discard(List.of("entity"));
		assertTrue(this.compiler.compile("entity").isEmpty());
		assertFalse(this.compiler.compile("hand").isEmpty());
	}

	private void submit(String stage, float distance, boolean transparent)
	{
		this.compiler.submit(stage, this.pipeline, this.mesh, 6,
				new PInstanceHeader(new Matrix4f().translation(0, 0, distance), -1, 0, 0), transparent);
	}
}
