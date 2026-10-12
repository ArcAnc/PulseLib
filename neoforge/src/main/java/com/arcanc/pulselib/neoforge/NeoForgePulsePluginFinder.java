package com.arcanc.pulselib.neoforge;

import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.plugin.IPulsePluginFinder;
import com.arcanc.pulselib.plugin.PulsePluginCandidate;
import com.arcanc.pulselib.util.PLibDatabase;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.modscan.ModAnnotation;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public final class NeoForgePulsePluginFinder implements IPulsePluginFinder
{
	private List<PulsePluginCandidate> candidates;

	@Override
	public synchronized List<PulsePluginCandidate> findPlugins()
	{
		if (this.candidates != null)
			return this.candidates;
		Type annotationType = Type.getType(PulsePlugin.class);
		var found = new LinkedHashSet<PulsePluginCandidate>();
		for (ModFileScanData data : ModList.get().getAllScanData())
		{
			String modId = data.getIModInfoData().stream().flatMap(file -> file.getMods().stream())
					.map(info -> info.getModId()).sorted().collect(Collectors.joining(","));
			for (ModFileScanData.AnnotationData annotation : data.getAnnotations())
			{
				if (!annotation.annotationType().equals(annotationType) || annotation.targetType() != ElementType.TYPE)
					continue;
				String name = annotation.clazz().getClassName();
				try
				{
					Object value = annotation.annotationData().get("value");
					PulsePlugin.Side side = value == null ? PulsePlugin.Side.CLIENT :
							PulsePlugin.Side.valueOf(((ModAnnotation.EnumHolder) value).value());
					found.add(new PulsePluginCandidate(name, modId, side));
				}
				catch (RuntimeException exception)
				{
					if (Arrays.asList(modId.split(",")).contains(PLibDatabase.MOD_ID))
						throw exception;
					PLibDatabase.LOGGER.error("Invalid @PulsePlugin on {} from mod {}", name, modId, exception);
				}
			}
		}
		this.candidates = found.stream().sorted(Comparator.comparing(PulsePluginCandidate::modId)
				.thenComparing(PulsePluginCandidate::className)).toList();
		return this.candidates;
	}
}
