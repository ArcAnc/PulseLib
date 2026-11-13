/**
 * @author ArcAnc
 * Created at: 12.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** A fresh loader prevents stale production output or an already loaded client class from hiding a side leak. */
public final class CoreCheckLauncher
{
	public static void main(String[] args) throws Exception
	{
		List<URL> urls = new ArrayList<>();
		for (String entry : System.getProperty("java.class.path").split(Pattern.quote(File.pathSeparator)))
			urls.add(Path.of(entry).toUri().toURL());
		try (var loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())
		{
			@Override
			protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
			{
				if (name.startsWith("net.minecraft.client.") || name.startsWith("com.mojang.blaze3d.") ||
						(name.startsWith("com.arcanc.pulselib.content.renderer.") && !name.contains(".plan.")))
					throw new ClassNotFoundException("Client/backend class forbidden by headless core check: " + name);
				return super.loadClass(name, resolve);
			}
		})
		{
			Thread.currentThread().setContextClassLoader(loader);
			try
			{
				String suite = args.length == 0 ? "CoreChecks" : switch (args[0])
				{
					case "discovery" -> "DiscoveryChecks";
					case "integration" -> "IntegrationChecks";
					default -> throw new IllegalArgumentException("Unknown check suite: " + args[0]);
				};
				Class.forName("com.arcanc.pulselib.porting." + suite, true, loader).
						getMethod("main", String[].class).invoke(null, (Object)args);
			}
			catch (InvocationTargetException exception)
			{
				throw new AssertionError("Headless core checks failed", exception.getCause());
			}
		}
	}
}
