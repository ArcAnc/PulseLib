/**
 * @author ArcAnc
 * Created at: 27.05.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data.gecko;

import com.arcanc.pulselib.content.model.PBone;
import com.arcanc.pulselib.content.model.PMaterial;
import com.arcanc.pulselib.content.model.PMesh;
import com.arcanc.pulselib.content.model.PMeshPrimitive;
import com.arcanc.pulselib.content.model.PModel;
import com.arcanc.pulselib.content.model.animation.*;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import de.javagl.jgltf.model.GltfConstants;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class PGeckoModelParser
{
	private static final float MODEL_SCALE = 1f / 16f;
	private static final String DEFAULT_TEXTURE_NAME = "0";
	private static final PGeckoDecodeContext ANIMATION_DECODE_CONTEXT = new PGeckoDecodeContext(MODEL_SCALE);
	private static final List<PGeckoChannelDecoder<?>> CHANNEL_DECODERS = List.of(
			new PGeckoChannelDecoder<Vector3f>()
			{
				@Override
				public Set<String> fieldNames()
				{
					return Set.of("position", "translation");
				}

				@Override
				public PAnimationChannelType<Vector3f> channel()
				{
					return PLibRegistration.AnimationChannelReg.POSITION;
				}

				@Override
				public PAnimationValue<Vector3f> decodeValue(JsonElement element, PGeckoDecodeContext context)
				{
					return vectorValue(element, 0f, value -> value.mul(context.modelScale()));
				}
			},
			new PGeckoChannelDecoder<Quaternionf>()
			{
				@Override
				public Set<String> fieldNames()
				{
					return Set.of("rotation");
				}

				@Override
				public PAnimationChannelType<Quaternionf> channel()
				{
					return PLibRegistration.AnimationChannelReg.ROTATION;
				}

				@Override
				public PAnimationValue<Quaternionf> decodeValue(JsonElement element, PGeckoDecodeContext context)
				{
					return new PMolangEulerRotationValue(vectorValue(element, 0f, PVectorConversion.IDENTITY));
				}
			},
			new PGeckoChannelDecoder<Vector3f>()
			{
				@Override
				public Set<String> fieldNames()
				{
					return Set.of("scale");
				}

				@Override
				public PAnimationChannelType<Vector3f> channel()
				{
					return PLibRegistration.AnimationChannelReg.SCALE;
				}

				@Override
				public PAnimationValue<Vector3f> decodeValue(JsonElement element, PGeckoDecodeContext context)
				{
					return vectorValue(element, 1f, PVectorConversion.IDENTITY);
				}
			});
	
	public static PModel parse(InputStream modelStream, InputStream animationStream) throws IOException
	{
		PModel model = parseModel(modelStream);
		model.animations.putAll(parseAnimations(animationStream, model));
		return model;
	}
	
	public static PModel parseModel(InputStream stream) throws IOException
	{
		JsonElement root = parseJson(stream);
		JsonElement geometry = firstGeometry(root);
		PModel model = new PModel();
		Map<String, PBone> bonesByName = new LinkedHashMap<>();
		List<BoneLink> links = new ArrayList<>();
		
		JsonElement bonesNode = member(geometry, "bones");
		if (!isArray(bonesNode))
			return model;
		
		JsonElement description = member(geometry, "description");
		TextureSize textureSize = new TextureSize(
				positive(floatValue(member(description, "texture_width"), 16f), 16f),
				positive(floatValue(member(description, "texture_height"), 16f), 16f));
		
		List<RawBone> rawBones = new ArrayList<>();
		for (JsonElement boneNode : bonesNode.getAsJsonArray())
			rawBones.add(readBone(boneNode));
		
		Map<String, RawBone> rawBonesByName = rawBones.stream().
				collect(Collectors.toMap(RawBone :: name, bone -> bone, (left, $) -> left, LinkedHashMap :: new));
		
		for (RawBone rawBone : rawBones)
			createBone(model, bonesByName, links, rawBonesByName, rawBone, textureSize);
		
		for (BoneLink link : links)
		{
			PBone child = bonesByName.get(link.childName());
			PBone parent = bonesByName.get(link.parentName());
			if (child == null || parent == null)
				continue;
			
			child.setParent(parent);
			parent.children().add(child);
		}
		
		for (PBone bone : bonesByName.values())
			model.bones.put(bone.uuid(), bone);
		model.boneMeshes.putAll(model.bones.values().stream().
				collect(Collectors.toMap(PBone :: uuid, bone -> Pair.of(bone.uuid(), bone.meshUUIDS().stream().toList()))));
		
		return model;
	}
	
	public static Map<String, PAnimation> parseAnimations(InputStream stream, PModel model) throws IOException
	{
		return parseAnimations(parseJson(stream), model);
	}
	
	public static Map<String, PAnimation> parseAnimations(JsonElement root, PModel model)
	{
		Map<String, PAnimation> animations = new LinkedHashMap<>();
		Map<String, PBone> bonesByName = model.bones.values().stream().
				collect(Collectors.toMap(PBone :: name, bone -> bone, (left, $) -> left, LinkedHashMap :: new));
		
		JsonElement animationsNode = member(root, "animations");
		if (!isObject(animationsNode))
			return animations;
		
		for (Map.Entry<String, JsonElement> animationEntry : animationsNode.getAsJsonObject().entrySet())
		{
			String animationName = animationEntry.getKey();
			JsonElement animationNode = animationEntry.getValue();
			Map<String, PBoneAnimation> boneAnimations = new LinkedHashMap<>();
			float maxTime = secondsToTicks(floatValue(member(animationNode, "animation_length"), 0f));
			List<PAnimationEvent<?>> events = PGeckoAnimationEventParser.parseAnimationEvents(animationNode);
			maxTime = Math.max(maxTime, PGeckoAnimationEventParser.maxEventTime(events));
			
			JsonElement bonesNode = member(animationNode, "bones");
			if (isObject(bonesNode))
			{
				for (Map.Entry<String, JsonElement> boneEntry : bonesNode.getAsJsonObject().entrySet())
				{
					PBone bone = bonesByName.get(boneEntry.getKey());
					if (bone == null)
						continue;
					
					Map<String, PAnimationTrack<?>> tracks = new HashMap<>();
					JsonElement positionNode = member(boneEntry.getValue(), "position");
					if (isMissing(positionNode))
						positionNode = member(boneEntry.getValue(), "translation");
					maxTime = Math.max(maxTime, parseAnimationChannel(positionNode, "position", PLibRegistration.AnimationChannelReg.POSITION, tracks));
					maxTime = Math.max(maxTime, parseAnimationChannel(member(boneEntry.getValue(), "rotation"), "rotation", PLibRegistration.AnimationChannelReg.ROTATION, tracks));
					maxTime = Math.max(maxTime, parseAnimationChannel(member(boneEntry.getValue(), "scale"), "scale", PLibRegistration.AnimationChannelReg.SCALE, tracks));
					
					if (! tracks.isEmpty())
						boneAnimations.put(bone.name(), new PBoneAnimation(bone.uuid(), tracks));
				}
			}
			
			animations.put(animationName, new PAnimation(animationName, maxTime, boneAnimations, events));
		}
		
		return animations;
	}
	
	private static JsonElement firstGeometry(JsonElement root)
	{
		JsonElement geometry = member(root, "minecraft:geometry");
		if (isArray(geometry) && !geometry.getAsJsonArray().isEmpty())
			return geometry.getAsJsonArray().get(0);
		
		if (isObject(geometry))
			return geometry;
		
		return root;
	}
	
	private static RawBone readBone(JsonElement boneNode)
	{
		String name = stringValue(member(boneNode, "name"), UUID.randomUUID().toString());
		return new RawBone(
				name,
				stringValue(member(boneNode, "parent"), ""),
				vector3f(member(boneNode, "pivot"), new Vector3f()),
				vector3f(member(boneNode, "rotation"), new Vector3f()),
				member(boneNode, "cubes"),
				member(boneNode, "locators"));
	}
	
	private static void createBone(PModel model,
	                               Map<String, PBone> bonesByName,
	                               List<BoneLink> links,
	                               Map<String, RawBone> rawBonesByName,
	                               RawBone rawBone,
	                               TextureSize textureSize)
	{
		Vector3f localPivot = scale(new Vector3f(rawBone.absolutePivot()));
		RawBone rawParent = rawBonesByName.get(rawBone.parentName());
		if (rawParent != null)
			localPivot.sub(scale(new Vector3f(rawParent.absolutePivot())));
		
		Quaternionf baseRotation = eulerDegreesToQuaternion(rawBone.rotation());
		PBone bone = new PBone(UUID.randomUUID(), rawBone.name(), localPivot, baseRotation);
		bonesByName.put(rawBone.name(), bone);
		
		if (!rawBone.parentName().isEmpty())
			links.add(new BoneLink(rawBone.name(), rawBone.parentName()));
		
		JsonElement cubesNode = rawBone.cubes();
		if (isArray(cubesNode))
			for (JsonElement cubeNode : cubesNode.getAsJsonArray())
			{
				PMesh mesh = parseCubeMesh(cubeNode, scale(new Vector3f(rawBone.absolutePivot())), textureName(cubeNode), textureSize);
				bone.meshUUIDS().add(mesh.uuid());
				model.meshes.put(mesh.uuid(), mesh);
			}
		
		addLocatorBones(bonesByName, links, rawBone);
	}
	
	private static void addLocatorBones(Map<String, PBone> bonesByName,
	                                    List<BoneLink> links,
	                                    RawBone parentBone)
	{
		JsonElement locatorsNode = parentBone.locators();
		if (!isObject(locatorsNode))
			return;
		
		for (Map.Entry<String, JsonElement> entry : locatorsNode.getAsJsonObject().entrySet())
		{
			String name = entry.getKey();
			if (name.isBlank() || bonesByName.containsKey(name))
				continue;
			
			LocatorTransform transform = locatorTransform(entry.getValue());
			PBone locator = new PBone(UUID.randomUUID(), name, transform.position(), transform.rotation());
			bonesByName.put(name, locator);
			links.add(new BoneLink(name, parentBone.name()));
		}
	}
	
	private static LocatorTransform locatorTransform(JsonElement node)
	{
		if (isArray(node))
			return new LocatorTransform(scale(vector3f(node, new Vector3f())), new Quaternionf());
		
		if (!isObject(node))
			return new LocatorTransform(new Vector3f(), new Quaternionf());
		
		JsonElement offset = member(node, "offset");
		if (isMissing(offset))
			offset = member(node, "position");
		if (isMissing(offset))
			offset = member(node, "pivot");
		
		return new LocatorTransform(
				scale(vector3f(offset, new Vector3f())),
				eulerDegreesToQuaternion(vector3f(member(node, "rotation"), new Vector3f())));
	}
	
	private static PMesh parseCubeMesh(JsonElement cubeNode, Vector3f pivot, String textureName, TextureSize textureSize)
	{
		Vector3f rawSize = vector3f(member(cubeNode, "size"), new Vector3f());
		Vector3f origin = scale(vector3f(member(cubeNode, "origin"), new Vector3f()));
		Vector3f size = scale(new Vector3f(rawSize));
		float inflate = floatValue(member(cubeNode, "inflate"), 0f) * MODEL_SCALE;
		boolean mirror = boolValue(member(cubeNode, "mirror"), false);
		
		Vector3f min = new Vector3f(origin).sub(inflate, inflate, inflate).sub(pivot);
		Vector3f max = new Vector3f(origin).add(size).add(inflate, inflate, inflate).sub(pivot);
		GeometryBuffers buffers = new GeometryBuffers();
		appendCube(buffers, min, max, cubeUv(cubeNode, rawSize, mirror, textureSize));
		
		UUID uuid = UUID.randomUUID();
		return new PMesh(uuid, List.of(new PMeshPrimitive(
				buffers.vertexCount(),
				buffers.positions(),
				buffers.normals(),
				buffers.uvs(),
				buffers.indexCount(),
				buffers.indices(),
				GltfConstants.GL_UNSIGNED_INT,
				new PMaterial(textureName))));
	}
	
	private static <T> float parseAnimationChannel(JsonElement channelNode,
	                                               String name,
	                                               PAnimationChannelType<T> channel,
	                                               Map<String, PAnimationTrack<?>> tracks)
	{
		if (isMissing(channelNode))
			return 0f;
		
		List<PKeyframe<T>> keyframes = new ArrayList<>();
		float maxTime = 0f;
		
		if (isArray(channelNode))
		{
			PAnimationValue<T> value = decodeValue(channel, channelNode);
			keyframes.add(new PKeyframe<>(0f, value, value, PInterpolationType.LINEAR));
		}
		else if (isObject(channelNode))
		{
			for (Map.Entry<String, JsonElement> entry : channelNode.getAsJsonObject().entrySet())
			{
				float time = secondsToTicks(parseFloat(entry.getKey(), 0f));
				JsonElement keyframeNode = entry.getValue();
				JsonElement pre = member(keyframeNode, "pre");
				JsonElement post = member(keyframeNode, "post");
				if (isMissing(pre))
					pre = keyFrameValueNode(keyframeNode);
				if (isMissing(post))
					post = pre;
				keyframes.add(new PKeyframe<>(time, decodeValue(channel, pre), decodeValue(channel, post), interpolation(keyframeNode)));
				maxTime = Math.max(maxTime, time);
			}
		}
		
		if (!keyframes.isEmpty())
		{
			keyframes.sort(Comparator.comparing(PKeyframe :: time));
			tracks.put(name, new PAnimationTrack<>(channel, keyframes));
		}
		
		return maxTime;
	}
	
	private static JsonElement keyFrameValueNode(JsonElement node)
	{
		if (isArray(node))
			return node;
		
		JsonElement post = member(node, "post");
		if (!isMissing(post))
			return post;
		
		JsonElement vector = member(node, "vector");
		if (!isMissing(vector))
			return vector;
		
		return node;
	}
	
	@SuppressWarnings("unchecked")
	private static <T> PAnimationValue<T> decodeValue(PAnimationChannelType<T> channel, JsonElement node)
	{
		for (PGeckoChannelDecoder<?> decoder : CHANNEL_DECODERS)
			if (decoder.channel() == channel)
				return (PAnimationValue<T>) decoder.decodeValue(node, ANIMATION_DECODE_CONTEXT);
		throw new IllegalArgumentException("Unsupported Gecko animation channel type: " + channel.id());
	}

	private static PMolangVectorValue vectorValue(JsonElement node, float fallback, PVectorConversion conversion)
	{
		VectorExpression expression = vectorExpression(node, fallback);
		return new PMolangVectorValue(expression.x(), expression.y(), expression.z(), conversion);
	}

	private static PInterpolation interpolation(JsonElement keyframe)
	{
		String name = stringValue(member(keyframe, "lerp_mode"), "linear");
		return PInterpolationType.INTERPOLATION_TYPES.getOrDefault(name, PInterpolationType.LINEAR);
	}

	private static boolean containsMolang(JsonElement node)
	{
		if (!isArray(node))
			return false;

		for (JsonElement component : node.getAsJsonArray())
			if (component.isJsonPrimitive() && component.getAsJsonPrimitive().isString())
				return true;
		return false;
	}

	private static VectorExpression vectorExpression(JsonElement node, float fallback)
	{
		JsonArray values = node.getAsJsonArray();
		return new VectorExpression(
				componentExpression(values, 0, fallback),
				componentExpression(values, 1, fallback),
				componentExpression(values, 2, fallback));
	}

	private static MolangParser.Expression componentExpression(JsonArray values, int index, float fallback)
	{
		if (values.size() <= index)
			return context -> fallback;

		JsonElement value = values.get(index);
		if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString())
			return MolangParser.parse(value.getAsString());
		return context -> floatValue(value, fallback);
	}
	
	private static Object channelValue(JsonElement node, PAnimationChannelType<?> channel)
	{
		Vector3f vector = vector3f(node, channel == PLibRegistration.AnimationChannelReg.SCALE ? new Vector3f(1f, 1f, 1f) : new Vector3f());
		if (channel == PLibRegistration.AnimationChannelReg.ROTATION)
			return eulerDegreesToQuaternion(vector);
		if (channel == PLibRegistration.AnimationChannelReg.POSITION)
			return scale(vector);
		return vector;
	}
	
	private static void appendCube(GeometryBuffers buffers, Vector3f min, Vector3f max, CubeUv uv)
	{
		appendFace(buffers,
				new Vector3f(min.x, min.y, max.z), new Vector3f(max.x, min.y, max.z), new Vector3f(max.x, max.y, max.z), new Vector3f(min.x, max.y, max.z),
				new Vector3f(0, 0, 1), uv.south());
		appendFace(buffers,
				new Vector3f(max.x, min.y, min.z), new Vector3f(min.x, min.y, min.z), new Vector3f(min.x, max.y, min.z), new Vector3f(max.x, max.y, min.z),
				new Vector3f(0, 0, -1), uv.north());
		appendFace(buffers,
				new Vector3f(max.x, min.y, max.z), new Vector3f(max.x, min.y, min.z), new Vector3f(max.x, max.y, min.z), new Vector3f(max.x, max.y, max.z),
				new Vector3f(1, 0, 0), uv.east());
		appendFace(buffers,
				new Vector3f(min.x, min.y, min.z), new Vector3f(min.x, min.y, max.z), new Vector3f(min.x, max.y, max.z), new Vector3f(min.x, max.y, min.z),
				new Vector3f(-1, 0, 0), uv.west());
		appendFace(buffers,
				new Vector3f(min.x, max.y, max.z), new Vector3f(max.x, max.y, max.z), new Vector3f(max.x, max.y, min.z), new Vector3f(min.x, max.y, min.z),
				new Vector3f(0, 1, 0), uv.up());
		appendFace(buffers,
				new Vector3f(min.x, min.y, min.z), new Vector3f(max.x, min.y, min.z), new Vector3f(max.x, min.y, max.z), new Vector3f(min.x, min.y, max.z),
				new Vector3f(0, -1, 0), uv.down());
	}
	
	private static void appendFace(GeometryBuffers buffers,
	                               Vector3f a,
	                               Vector3f b,
	                               Vector3f c,
	                               Vector3f d,
	                               Vector3f normal,
	                               FaceUv uv)
	{
		int start = buffers.vertexCount();
		buffers.addVertex(a, normal, uv.min());
		buffers.addVertex(b, normal, new Vector2f(uv.max().x, uv.min().y));
		buffers.addVertex(c, normal, uv.max());
		buffers.addVertex(d, normal, new Vector2f(uv.min().x, uv.max().y));
		buffers.addTriangle(start, start + 1, start + 2);
		buffers.addTriangle(start, start + 2, start + 3);
	}
	
	private static CubeUv cubeUv(JsonElement cubeNode, Vector3f size, boolean mirror, TextureSize textureSize)
	{
		JsonElement uvNode = member(cubeNode, "uv");
		if (isObject(uvNode))
			return new CubeUv(
					faceUv(member(uvNode, "north"), mirror, textureSize),
					faceUv(member(uvNode, "south"), mirror, textureSize),
					faceUv(member(uvNode, "east"), mirror, textureSize),
					faceUv(member(uvNode, "west"), mirror, textureSize),
					faceUv(member(uvNode, "up"), mirror, textureSize),
					faceUv(member(uvNode, "down"), mirror, textureSize));
		
		float u = floatAt(uvNode, 0, 0f);
		float v = floatAt(uvNode, 1, 0f);
		float x = Math.abs(size.x());
		float y = Math.abs(size.y());
		float z = Math.abs(size.z());
		
		return new CubeUv(
				faceUv(u + z, v + z, x, y, mirror, textureSize),
				faceUv(u + z + x + z, v + z, x, y, mirror, textureSize),
				faceUv(u, v + z, z, y, mirror, textureSize),
				faceUv(u + z + x, v + z, z, y, mirror, textureSize),
				faceUv(u + z, v, x, z, mirror, textureSize),
				faceUv(u + z + x, v, x, z, mirror, textureSize));
	}
	
	private static FaceUv faceUv(JsonElement node, boolean mirror, TextureSize textureSize)
	{
		if (isMissing(node))
			return new FaceUv(new Vector2f(), new Vector2f());
		
		if (isArray(node))
		{
			if (node.getAsJsonArray().size() >= 4)
				return faceUvCorners(floatAt(node, 0, 0f), floatAt(node, 1, 0f), floatAt(node, 2, 0f), floatAt(node, 3, 0f), mirror, textureSize);
			return faceUv(floatAt(node, 0, 0f), floatAt(node, 1, 0f), 0f, 0f, mirror, textureSize);
		}
		
		JsonElement uv = member(node, "uv");
		JsonElement uvSize = member(node, "uv_size");
		return faceUv(floatAt(uv, 0, 0f), floatAt(uv, 1, 0f), floatAt(uvSize, 0, 0f), floatAt(uvSize, 1, 0f), mirror, textureSize);
	}
	
	private static FaceUv faceUv(float u, float v, float width, float height, boolean mirror, TextureSize textureSize)
	{
		float minU = u;
		float maxU = u + width;
		if (mirror)
		{
			minU = u + width;
			maxU = u;
		}
		return normalizeUv(minU, v, maxU, v + height, textureSize);
	}
	
	private static FaceUv faceUvCorners(float minU, float minV, float maxU, float maxV, boolean mirror, TextureSize textureSize)
	{
		if (mirror)
		{
			float swap = minU;
			minU = maxU;
			maxU = swap;
		}
		return normalizeUv(minU, minV, maxU, maxV, textureSize);
	}
	
	private static FaceUv normalizeUv(float minU, float minV, float maxU, float maxV, TextureSize textureSize)
	{
		return new FaceUv(
				new Vector2f(minU / textureSize.width(), minV / textureSize.height()),
				new Vector2f(maxU / textureSize.width(), maxV / textureSize.height()));
	}
	
	private static String textureName(JsonElement cubeNode)
	{
		String texture = stringValue(member(cubeNode, "texture"), DEFAULT_TEXTURE_NAME);
		if (texture.startsWith("#"))
			return texture.substring(1);
		return texture;
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
	
	private static Quaternionf eulerDegreesToQuaternion(Vector3f degrees)
	{
		return new Quaternionf().rotationXYZ(
				(float) Math.toRadians(degrees.x()),
				(float) Math.toRadians(degrees.y()),
				(float) Math.toRadians(degrees.z()));
	}
	
	private static Vector3f scale(Vector3f vector)
	{
		return vector.mul(MODEL_SCALE);
	}
	
	private static float secondsToTicks(float seconds)
	{
		return seconds * 20f;
	}
	
	private static float positive(float value, float fallback)
	{
		return value > 0f ? value : fallback;
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
	
	private static boolean boolValue(JsonElement node, boolean fallback)
	{
		if (isMissing(node) || !node.isJsonPrimitive() || !node.getAsJsonPrimitive().isBoolean())
			return fallback;
		return node.getAsBoolean();
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
	
	private static JsonElement parseJson(InputStream stream) throws IOException
	{
		try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
		{
			return JsonParser.parseReader(reader);
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
	
	private record BoneLink(String childName, String parentName)
	{
	}
	
	private record RawBone(String name, String parentName, Vector3f absolutePivot, Vector3f rotation, JsonElement cubes, JsonElement locators)
	{
	}
	
	private record LocatorTransform(Vector3f position, Quaternionf rotation)
	{
	}
	
	private record CubeUv(FaceUv north, FaceUv south, FaceUv east, FaceUv west, FaceUv up, FaceUv down)
	{
	}
	
	private record FaceUv(Vector2f min, Vector2f max)
	{
	}
	
	private record TextureSize(float width, float height)
	{
	}

	private record VectorExpression(MolangParser.Expression x, MolangParser.Expression y, MolangParser.Expression z)
	{
		Vector3f evaluate(Object data)
		{
			return new Vector3f(
					evaluateComponent(this.x, data, 0),
					evaluateComponent(this.y, data, 1),
					evaluateComponent(this.z, data, 2));
		}

		private static float evaluateComponent(MolangParser.Expression expression, Object data, int component)
		{
			if (data instanceof MolangParser.Context context)
				context.thisValue(context.thisComponent(component));
			return expression.evaluate(data);
		}
	}
	
	private static class GeometryBuffers
	{
		private final List<Float> positions = new ArrayList<>();
		private final List<Float> normals = new ArrayList<>();
		private final List<Float> uvs = new ArrayList<>();
		private final List<Integer> indices = new ArrayList<>();
		
		void addVertex(Vector3f position, Vector3f normal, Vector2f uv)
		{
			this.positions.add(position.x());
			this.positions.add(position.y());
			this.positions.add(position.z());
			this.normals.add(normal.x());
			this.normals.add(normal.y());
			this.normals.add(normal.z());
			this.uvs.add(uv.x());
			this.uvs.add(uv.y());
		}
		
		void addTriangle(int a, int b, int c)
		{
			this.indices.add(a);
			this.indices.add(b);
			this.indices.add(c);
		}
		
		int vertexCount()
		{
			return this.positions.size() / 3;
		}
		
		int indexCount()
		{
			return this.indices.size();
		}
		
		FloatBuffer positions()
		{
			return floatBuffer(this.positions);
		}
		
		FloatBuffer normals()
		{
			return floatBuffer(this.normals);
		}
		
		FloatBuffer uvs()
		{
			return floatBuffer(this.uvs);
		}
		
		ByteBuffer indices()
		{
			ByteBuffer buffer = ByteBuffer.allocateDirect(this.indices.size() * Integer.BYTES).order(ByteOrder.nativeOrder());
			for (int index : this.indices)
				buffer.putInt(index);
			buffer.flip();
			return buffer;
		}
		
		private static FloatBuffer floatBuffer(List<Float> values)
		{
			FloatBuffer buffer = FloatBuffer.allocate(values.size());
			for (float value : values)
				buffer.put(value);
			buffer.flip();
			return buffer;
		}
	}
}
