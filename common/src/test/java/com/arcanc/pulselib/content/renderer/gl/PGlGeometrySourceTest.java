/**
 * @author ArcAnc
 * Created at: 22.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.gl;

import com.arcanc.pulselib.content.renderer.legacy.GlDynamicGeometry;
import com.arcanc.pulselib.content.renderer.plan.PDynamicGeometry;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests CPU ownership and resource lifetime before any OpenGL context is required. */
class PGlGeometrySourceTest
{
	@Test
	void distinctEqualBytesRemainIndependentResourcesWithoutGpuAllocation()
	{
		var registry = new PGlResourceRegistry();
		var data = geometry(3);
		var first = registry.geometry(data);
		assertSame(first, registry.geometry(data));
		assertNotSame(first, registry.geometry(geometry(3)));
		assertEquals(3, registry.indexCount(first));
	}

	@Test
	void dynamicUpdatesKeepTheHandleButExposeTheLatestIndexRange()
	{
		var registry = new PGlResourceRegistry();
		var source = new PGlDynamicGeometry(geometry(3));
		var handle = registry.dynamic(source);
		source.update(geometry(6));
		assertSame(handle, registry.dynamic(source));
		assertEquals(6, registry.indexCount(handle));
	}

	@Test
	void cleanupInvalidatesBothGeometryKindsAndDoesNotRecycleHandleValues()
	{
		var registry = new PGlResourceRegistry();
		var data = geometry(3);
		var source = new PGlDynamicGeometry(data);
		var immutable = registry.geometry(data);
		var dynamic = registry.dynamic(source);
		registry.clear();
		assertThrows(IllegalStateException.class, () -> registry.indexCount(immutable));
		assertThrows(IllegalStateException.class, () -> registry.indexCount(dynamic));
		assertNotSame(immutable, registry.geometry(data));
		assertNotSame(dynamic, registry.dynamic(source));
		assertTrue(registry.geometry(data).value() > dynamic.value());
	}

	@Test
	void unsupportedDynamicImplementationsFailExplicitly()
	{
		var unsupported = new PDynamicGeometry()
		{
		};
		assertThrows(IllegalArgumentException.class, () -> new PGlResourceRegistry().dynamic(unsupported));
	}

	@Test
	void backendRejectsWrongStrideAndTruncatedVertices()
	{
		PGlGeometryArena.validate(geometry(3), 24);
		assertThrows(IllegalArgumentException.class, () -> PGlGeometryArena.validate(geometry(3), 32));
		var truncated = new PGeometryData(ByteBuffer.allocate(25), ByteBuffer.allocate(6), 24, 3, PGeometryData.IndexType.UNSIGNED_SHORT);
		assertThrows(IllegalArgumentException.class, () -> PGlGeometryArena.validate(truncated, 24));
	}

	@Test
	void canonicalFacadeAndBackendSourceShareOneHandleAndCloseInvalidatesIt()
	{
		var registry = new PGlResourceRegistry();
		var facade = new GlDynamicGeometry();
		facade.vertexBuffer().update(geometry(6));
		var handle = registry.dynamic(facade);
		assertSame(handle, registry.dynamic(facade.vertexBuffer()));
		assertEquals(6, facade.indexCount());
		facade.close();
		facade.close();
		assertThrows(IllegalStateException.class, () -> registry.indexCount(handle));
		assertThrows(IllegalStateException.class, () -> registry.dynamic(facade));
		assertThrows(IllegalStateException.class, () -> facade.vertexBuffer().update(geometry(3)));
	}

	@Test
	void closeNotifiesEachOwnerOnceAndClearDetachesOnlyThatRegistry()
	{
		var source = new PGlDynamicGeometry(geometry(3));
		var first = new PGlResourceRegistry();
		var second = new PGlResourceRegistry();
		var detached = first.dynamic(source);
		var active = second.dynamic(source);
		first.clear();
		assertThrows(IllegalStateException.class, () -> first.indexCount(detached));
		assertEquals(3, second.indexCount(active));
		int[] closes = {0};
		source.onClose(this, () -> closes[0]++);
		source.close();
		source.close();
		assertEquals(1, closes[0]);
		assertThrows(IllegalStateException.class, () -> second.indexCount(active));
		assertThrows(IllegalStateException.class, source::geometry);
	}

	private static PGeometryData geometry(int indices)
	{
		return new PGeometryData(ByteBuffer.allocate(3 * 24), ByteBuffer.allocate(indices * 2), 24, indices, PGeometryData.IndexType.UNSIGNED_SHORT);
	}
}
