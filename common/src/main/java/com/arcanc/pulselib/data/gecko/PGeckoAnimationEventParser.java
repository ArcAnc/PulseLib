/**
 * @author ArcAnc
 * Created at: 25.05.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data.gecko;


import com.arcanc.pulselib.content.model.animation.PAnimationEvent;
import com.arcanc.pulselib.content.model.animation.PAnimationEventTypes;
import com.arcanc.pulselib.util.PLibDatabase;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class PGeckoAnimationEventParser
{
	private static final float SECONDS_TO_TICKS = 20f;
	
	public static List<PAnimationEvent<?>> parseAnimationEvents(JsonElement animationNode)
	{
		List<PAnimationEvent<?>> events = new ArrayList<>();
		parseSoundEffects(member(animationNode, "sound_effects"), events);
		parseParticleEffects(member(animationNode, "particle_effects"), events);
		events.sort(Comparator.comparingDouble(PAnimationEvent::time));
		return events;
	}
	
	public static float maxEventTime(List<PAnimationEvent<?>> events)
	{
		float maxTime = 0f;
		for (PAnimationEvent<?> event : events)
			maxTime = Math.max(maxTime, event.time());
		return maxTime;
	}
	
	private static void parseSoundEffects(JsonElement node, List<PAnimationEvent<?>> events)
	{
		parseEventMap(node, events, PGeckoAnimationEventParser :: soundEvent);
	}
	
	private static void parseParticleEffects(JsonElement node, List<PAnimationEvent<?>> events)
	{
		parseEventMap(node, events, PGeckoAnimationEventParser :: particleEvent);
	}
	
	private static void parseEventMap(JsonElement node, List<PAnimationEvent<?>> events, EventFactory factory)
	{
		if (!isObject(node))
			return;
		
		for (Map.Entry<String, JsonElement> entry : node.getAsJsonObject().entrySet())
		{
			float time = secondsToTicks(parseFloat(entry.getKey(), 0f));
			JsonElement value = entry.getValue();
			if (isArray(value))
			{
				for (JsonElement eventNode : value.getAsJsonArray())
					addMappedEvent(time, eventNode, events, factory);
			}
			else
				addMappedEvent(time, value, events, factory);
		}
	}
	
	private static void addMappedEvent(float time, JsonElement node, List<PAnimationEvent<?>> events, EventFactory factory)
	{
		if (!isObject(node))
			return;
		
		PAnimationEvent<?> event = factory.create(time, node);
		if (event != null)
			events.add(event);
	}
	
	private static PAnimationEvent<PAnimationEventTypes.SoundData> soundEvent(float time, JsonElement node)
	{
		ResourceLocation sound = ResourceLocation(
				stringValue(member(node, "sound"),
						stringValue(member(node, "id"),
								stringValue(member(node, "event"),
										stringValueFromFileOrEffect(node)))));
		if (sound == null)
			return null;
		
		return new PAnimationEvent<>(time, PAnimationEventTypes.SOUND, new PAnimationEventTypes.SoundData(
				sound, stringValue(member(node, "locator"), ""), floatValue(member(node, "volume"), 1f),
				floatValue(member(node, "pitch"), 1f)));
	}
	
	private static String stringValueFromFileOrEffect(JsonElement node)
	{
		String fromFile = soundIdFromFile(stringValue(member(node, "file"), ""));
		if (!fromFile.isBlank())
			return fromFile;
		return stringValue(member(node, "effect"), "");
	}
	
	private static PAnimationEvent<PAnimationEventTypes.ParticleData> particleEvent(float time, JsonElement node)
	{
		ResourceLocation particle = ResourceLocation(stringValue(member(node, "particle"), stringValue(member(node, "effect"), "")));
		if (particle == null)
			return null;
		
		return new PAnimationEvent<>(time, PAnimationEventTypes.PARTICLE, new PAnimationEventTypes.ParticleData(
				particle, stringValue(member(node, "locator"), ""), vector3f(member(node, "offset"), new Vector3f()),
				vector3f(member(node, "motion"), new Vector3f())));
	}
	
	private static ResourceLocation ResourceLocation(String value)
	{
		if (value.isBlank())
			return null;
		
		ResourceLocation id = ResourceLocation.tryParse(value);
		if (id == null)
			PLibDatabase.LOGGER.warn("Invalid Gecko animation event ResourceLocation: {}", value);
		return id;
	}
	
	private static String soundIdFromFile(String file)
	{
		int assets = file.indexOf("/assets/");
		int sounds = file.indexOf("/sounds/", assets);
		if (assets < 0 || sounds < 0 || !file.endsWith(".ogg"))
			return "";
		
		String namespace = file.substring(assets + "/assets/".length(), sounds);
		String path = file.substring(sounds + "/sounds/".length(), file.length() - ".ogg".length());
		return namespace + ":" + path;
	}
	
	private static Vector3f vector3f(JsonElement node, Vector3f fallback)
	{
		if (!isArray(node) || node.getAsJsonArray().size() < 3)
			return new Vector3f(fallback);
		
		return new Vector3f(
				floatAt(node, 0, fallback.x()),
				floatAt(node, 1, fallback.y()),
				floatAt(node, 2, fallback.z()));
	}
	
	private static float secondsToTicks(float seconds)
	{
		return seconds * SECONDS_TO_TICKS;
	}
	
	private static float floatAt(JsonElement node, int index, float fallback)
	{
		if (!isArray(node) || node.getAsJsonArray().size() <= index)
			return fallback;
		return floatValue(node.getAsJsonArray().get(index), fallback);
	}
	
	private static float floatValue(JsonElement node, float fallback)
	{
		if (isMissing(node) || !node.isJsonPrimitive() || !node.getAsJsonPrimitive().isNumber())
			return fallback;
		return node.getAsFloat();
	}
	
	private static String stringValue(JsonElement node, String fallback)
	{
		if (isMissing(node) || !node.isJsonPrimitive() || !node.getAsJsonPrimitive().isString())
			return fallback;
		return node.getAsString();
	}
	
	private static float parseFloat(String value, float fallback)
	{
		try
		{
			return Float.parseFloat(value);
		}
		catch (NumberFormatException ignored)
		{
			return fallback;
		}
	}
	
	private static JsonElement member(JsonElement element, String name)
	{
		if (!isObject(element))
			return JsonNull.INSTANCE;
		
		JsonObject object = element.getAsJsonObject();
		JsonElement value = object.get(name);
		return value == null ? JsonNull.INSTANCE : value;
	}
	
	private static boolean isMissing(JsonElement element)
	{
		return element == null || element.isJsonNull();
	}
	
	private static boolean isObject(JsonElement element)
	{
		return element != null && element.isJsonObject();
	}
	
	private static boolean isArray(JsonElement element)
	{
		return element != null && element.isJsonArray();
	}
	
	private interface EventFactory
	{
		PAnimationEvent<?> create(float time, JsonElement node);
	}
}
