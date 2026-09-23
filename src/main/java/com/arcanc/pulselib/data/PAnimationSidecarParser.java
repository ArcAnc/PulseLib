/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data;

import com.arcanc.pulselib.content.model.animation.PAnimation;
import com.arcanc.pulselib.content.model.animation.PAnimationEvent;
import com.arcanc.pulselib.content.model.animation.PAnimationEventType;
import com.arcanc.pulselib.content.model.animation.PAnimationVisibilityTrack;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.util.PLibDatabase;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared sidecar event reader for model loaders. */
public final class PAnimationSidecarParser
{
	private static final float SECONDS_TO_TICKS = 20f;

	private PAnimationSidecarParser()
	{
	}

	public static JsonElement parseJson(InputStream stream) throws IOException
	{
		try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
		{
			return JsonParser.parseReader(reader);
		}
	}

	public static void mergeSidecar(JsonElement root, Map<String, PAnimation> animations)
	{
		JsonElement animationsNode = member(root, "animations");
		if (!isObject(animationsNode))
			animationsNode = root;
		if (!isObject(animationsNode))
			return;
		for (Map.Entry<String, JsonElement> entry : animationsNode.getAsJsonObject().entrySet())
		{
			PAnimation animation = animations.get(entry.getKey());
			if (animation == null)
				continue;
			List<PAnimationEvent<?>> events = new ArrayList<>(animation.events());
			events.addAll(parseAnimationEvents(entry.getValue()));
			events.sort(Comparator.comparingDouble(PAnimationEvent::time));
			Map<String, PAnimationVisibilityTrack> visibilityTracks = new LinkedHashMap<>(animation.visibilityTracks());
			visibilityTracks.putAll(parseVisibilityTracks(entry.getValue()));
			animations.put(entry.getKey(), new PAnimation(animation.name(),
					Math.max(animation.length(), Math.max(maxEventTime(events), maxVisibilityTime(visibilityTracks))),
					animation.boneAnimations(), events, visibilityTracks));
		}
	}

	private static Map<String, PAnimationVisibilityTrack> parseVisibilityTracks(JsonElement animationNode)
	{
		JsonElement visibilityNode = member(animationNode, "visibility");
		if (!isObject(visibilityNode))
			return Map.of();
		Map<String, PAnimationVisibilityTrack> tracks = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> entry : visibilityNode.getAsJsonObject().entrySet())
		{
			List<PAnimationVisibilityTrack.Keyframe> keyframes = parseVisibilityKeyframes(entry.getValue());
			if (!keyframes.isEmpty())
				tracks.put(entry.getKey(), new PAnimationVisibilityTrack(keyframes));
		}
		return tracks;
	}

	private static List<PAnimationVisibilityTrack.Keyframe> parseVisibilityKeyframes(JsonElement node)
	{
		List<PAnimationVisibilityTrack.Keyframe> keyframes = new ArrayList<>();
		if (isArray(node))
			for (JsonElement keyframe : node.getAsJsonArray())
				addVisibilityKeyframe(keyframes, keyframe);
		else if (isObject(node))
			for (Map.Entry<String, JsonElement> entry : node.getAsJsonObject().entrySet())
				addVisibilityKeyframe(keyframes, entry.getKey(), entry.getValue());
		return keyframes;
	}

	private static void addVisibilityKeyframe(List<PAnimationVisibilityTrack.Keyframe> keyframes, JsonElement node)
	{
		float seconds = floatValue(member(node, "time"), Float.NaN);
		Boolean visible = booleanValue(member(node, "visible"));
		if (Float.isFinite(seconds) && visible != null)
			keyframes.add(new PAnimationVisibilityTrack.Keyframe(seconds * SECONDS_TO_TICKS, visible));
	}

	private static void addVisibilityKeyframe(List<PAnimationVisibilityTrack.Keyframe> keyframes, String rawTime, JsonElement node)
	{
		float seconds;
		try
		{
			seconds = Float.parseFloat(rawTime);
		}
		catch (NumberFormatException ignored)
		{
			return;
		}
		Boolean visible = booleanValue(node);
		if (Float.isFinite(seconds) && visible != null)
			keyframes.add(new PAnimationVisibilityTrack.Keyframe(seconds * SECONDS_TO_TICKS, visible));
	}

	private static List<PAnimationEvent<?>> parseAnimationEvents(JsonElement animationNode)
	{
		List<PAnimationEvent<?>> events = new ArrayList<>();
		JsonElement node = member(animationNode, "events");
		if (isArray(node))
			for (JsonElement eventNode : node.getAsJsonArray())
			{
				String type = stringValue(member(eventNode, "type"), "");
				PAnimationEvent<?> event = typedEvent(floatValue(member(eventNode, "time"), 0f) * SECONDS_TO_TICKS, type, eventNode);
				if (event != null)
					events.add(event);
			}
		events.sort(Comparator.comparingDouble(PAnimationEvent::time));
		return events;
	}

	private static float maxEventTime(List<PAnimationEvent<?>> events)
	{
		float maxTime = 0f;
		for (PAnimationEvent<?> event : events)
			maxTime = Math.max(maxTime, event.time());
		return maxTime;
	}

	private static float maxVisibilityTime(Map<String, PAnimationVisibilityTrack> tracks)
	{
		float maxTime = 0f;
		for (PAnimationVisibilityTrack track : tracks.values())
			for (PAnimationVisibilityTrack.Keyframe keyframe : track.keyframes())
				maxTime = Math.max(maxTime, keyframe.time());
		return maxTime;
	}

	private static @Nullable PAnimationEvent<?> typedEvent(float time, String rawType, JsonElement node)
	{
		if (rawType.isBlank())
			return null;
		Identifier id = rawType.indexOf(':') >= 0 ? Identifier.tryParse(rawType) : PLibDatabase.rl(rawType);
		if (id == null)
		{
			PLibDatabase.LOGGER.warn("Unknown animation sidecar event type: {}", rawType);
			return null;
		}
		PAnimationEventType<?> type = PLibRegistration.AnimationEventReg.EVENT_TYPES.get(id).orElse(null);
		if (type == null)
		{
			PLibDatabase.LOGGER.warn("Unregistered animation sidecar event type: {}", id);
			return null;
		}
		return decode(time, type, node);
	}

	private static <T> @Nullable PAnimationEvent<T> decode(float time, PAnimationEventType<T> type, JsonElement node)
	{
		return type.codec().codec().parse(JsonOps.INSTANCE, node).resultOrPartial(error ->
				PLibDatabase.LOGGER.warn("Invalid animation event {}: {}", type.id(), error)).map(data -> new PAnimationEvent<>(time, type, data)).orElse(null);
	}

	private static float floatValue(JsonElement node, float fallback)
	{
		return isMissing(node) || !node.isJsonPrimitive() || !node.getAsJsonPrimitive().isNumber() ? fallback : node.getAsFloat();
	}

	private static String stringValue(JsonElement node, String fallback)
	{
		return isMissing(node) || !node.isJsonPrimitive() || !node.getAsJsonPrimitive().isString() ? fallback : node.getAsString();
	}

	private static @Nullable Boolean booleanValue(JsonElement node)
	{
		return isMissing(node) || !node.isJsonPrimitive() || !node.getAsJsonPrimitive().isBoolean() ? null : node.getAsBoolean();
	}

	private static JsonElement member(JsonElement element, String name)
	{
		if (!isObject(element))
			return JsonNull.INSTANCE;
		JsonObject object = element.getAsJsonObject();
		JsonElement value = object.get(name);
		return value == null ? JsonNull.INSTANCE : value;
	}

	private static boolean isMissing(@Nullable JsonElement element)
	{
		return element == null || element.isJsonNull();
	}

	private static boolean isObject(@Nullable JsonElement element)
	{
		return element != null && element.isJsonObject();
	}

	private static boolean isArray(@Nullable JsonElement element)
	{
		return element != null && element.isJsonArray();
	}
}
