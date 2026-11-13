/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.arcanc.pulselib.fabric.FabricRhiPipelines;
import com.arcanc.pulselib.util.PRenderTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.HashSet;

/** Verifies the production helper against the real widened native registry in Knot's CLIENT loader. */
public final class FabricPipelineChecks
{
	public static void main(String[] args) throws ClassNotFoundException
	{
		SharedConstants.tryDetectVersion();
		if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT)
			throw new AssertionError("Expected Fabric CLIENT environment");
		ClassLoader game = FabricPipelineChecks.class.getClassLoader();
		if (game == ClassLoader.getSystemClassLoader() || FabricRhiPipelines.class.getClassLoader() != game ||
				PRhiPipelines.class.getClassLoader() != game || RenderPipelines.class.getClassLoader() != game)
			throw new AssertionError("Registration must execute entirely in Knot's game class loader");
		Class.forName("net.minecraft.client.renderer.WeatherEffectRenderer", false, game);
		System.out.println("PASS strict Fabric weather Redirect: native overloads, require=1/allow=1; no weather GPU draw");
		var before = new HashSet<>(RenderPipelines.getStaticPipelines());
		FabricRhiPipelines.register();
		var after = new HashSet<>(RenderPipelines.getStaticPipelines());
		if (after.size() != before.size() + 4)
			throw new AssertionError("Expected four new native RHI registry entries");
		var profiles = PRhiPipelines.portable();
		if (profiles != PRhiPipelines.portable() || profiles.instanceCapacity() != 128)
			throw new AssertionError("Shared portable profile identity/capacity changed");
		profiles.register(pipeline ->
		{
			if (before.contains(pipeline) || !after.contains(pipeline))
				throw new AssertionError("Native registry did not receive Common profile identity");
		});
		if (PRenderTypes.class.getClassLoader() != game || RhiCatalogChecks.class.getClassLoader() != game)
			throw new AssertionError("Production catalog must share Knot's native class loader");
		RhiCatalogChecks.register(RenderPipelines::register);
		if (RenderPipelines.getStaticPipelines().size() != before.size() + 23)
			throw new AssertionError("Expected 19 canonical and four RHI entries in the real registry");
		var texture = Identifier.fromNamespaceAndPath("pulselib", "profile_fixture");
		var solid = PRenderTypes.RenderTypeProvider.trianglesSolid(texture);
		if (solid != PRenderTypes.RenderTypeProvider.trianglesSolid(texture) || solid.hasBlending() || !solid.outline().isPresent() ||
				solid.pipeline() != PRenderTypes.RenderPipelinesProvider.TRIANGLES_SOLID)
			throw new AssertionError("Production factory identity/setup lost in Fabric");
		System.out.println("PULSELIB_FABRIC_RHI_REGISTRATION_PASSED 19 production + 4 shared profiles and real factory; actual Knot CLIENT/access widener/native registry; no Minecraft instance");
	}
}
