/**
 * @author ArcAnc
 * Created at: 26.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data.gltf;


import com.arcanc.pulselib.content.model.PBone;
import com.arcanc.pulselib.content.model.PMaterial;
import com.arcanc.pulselib.content.model.PMesh;
import com.arcanc.pulselib.content.model.PMeshPrimitive;
import com.arcanc.pulselib.content.model.PModel;
import com.arcanc.pulselib.content.model.animation.*;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.util.helpers.PLibParserHelper;
import com.mojang.datafixers.util.Pair;
import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.io.GltfModelReader;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parses gltf model.
 */
public class PGltfModelParser
{
	private static final List<PGltfChannelDecoder<?>> CHANNEL_DECODERS = List.of(
			new PGltfChannelDecoder<Vector3f>()
			{
				/**
				 * Performs the field names operation.
				 * @return the value produced by this operation.
				 */
				@Override
				public Set<String> fieldNames()
				{
					return Set.of("translation");
				}

				/**
				 * Performs the channel operation.
				 * @return the value produced by this operation.
				 */
				@Override
				public PAnimationChannelType<Vector3f> channel()
				{
					return PLibRegistration.AnimationChannelReg.POSITION;
				}

				/**
				 * Decodes the value.
				 * @param values the values to use.
				 * @param keyframeIndex the keyframe index to use.
				 * @param context the context to use.
				 * @return the value produced by this operation.
				 */
				@Override
				public PAnimationValue<Vector3f> decodeValue(ByteBuffer values, int keyframeIndex, PGltfDecodeContext context)
				{
					int offset = keyframeIndex * 12;
					Vector3f value = new Vector3f(values.getFloat(offset), values.getFloat(offset + 4), values.getFloat(offset + 8)).sub(context.bone().pivot());
					return constantVector(value);
				}
			},
			new PGltfChannelDecoder<Quaternionf>()
			{
				/**
				 * Performs the field names operation.
				 * @return the value produced by this operation.
				 */
				@Override
				public Set<String> fieldNames()
				{
					return Set.of("rotation");
				}

				/**
				 * Performs the channel operation.
				 * @return the value produced by this operation.
				 */
				@Override
				public PAnimationChannelType<Quaternionf> channel()
				{
					return PLibRegistration.AnimationChannelReg.ROTATION;
				}

				/**
				 * Decodes the value.
				 * @param values the values to use.
				 * @param keyframeIndex the keyframe index to use.
				 * @param context the context to use.
				 * @return the value produced by this operation.
				 */
				@Override
				public PAnimationValue<Quaternionf> decodeValue(ByteBuffer values, int keyframeIndex, PGltfDecodeContext context)
				{
					int offset = keyframeIndex * 16;
					Quaternionf rotation = new Quaternionf(
							values.getFloat(offset), values.getFloat(offset + 4),
							values.getFloat(offset + 8), values.getFloat(offset + 12));
					rotation.mul(new Quaternionf(context.bone().baseRotation()).invert()).normalize();
					return constantQuaternion(rotation);
				}
			},
			new PGltfChannelDecoder<Vector3f>()
			{
				/**
				 * Performs the field names operation.
				 * @return the value produced by this operation.
				 */
				@Override
				public Set<String> fieldNames()
				{
					return Set.of("scale");
				}

				/**
				 * Performs the channel operation.
				 * @return the value produced by this operation.
				 */
				@Override
				public PAnimationChannelType<Vector3f> channel()
				{
					return PLibRegistration.AnimationChannelReg.SCALE;
				}

				/**
				 * Decodes the value.
				 * @param values the values to use.
				 * @param keyframeIndex the keyframe index to use.
				 * @param context the context to use.
				 * @return the value produced by this operation.
				 */
				@Override
				public PAnimationValue<Vector3f> decodeValue(ByteBuffer values, int keyframeIndex, PGltfDecodeContext context)
				{
					int offset = keyframeIndex * 12;
					return constantVector(new Vector3f(values.getFloat(offset), values.getFloat(offset + 4), values.getFloat(offset + 8)));
				}
			});

	/**
	 * Performs the parse operation.
	 * @param stream the stream to use.
	 * @return the value produced by this operation.
	 */
	public static PModel parse(InputStream stream) throws IOException
	{
		GltfModel model = new GltfModelReader().readWithoutReferences(stream);

		PModel pModel = new PModel();
		
		Map<UUID, PBone> uuidToBone = new HashMap<>();
		Map<UUID, PMesh> uuidToMesh = new HashMap<>();
		Map<NodeModel, PBone> nodeToBone = parseBones(model, uuidToBone, uuidToMesh);
		Map<String, PAnimation> animations = parseAnimations(model, nodeToBone);
		
		pModel.bones.putAll(uuidToBone);
		pModel.meshes.putAll(uuidToMesh);
		pModel.boneMeshes.putAll(uuidToBone.values().stream().
				collect(Collectors.toMap(PBone :: uuid, pBone -> Pair.of(pBone.uuid(), pBone.meshUUIDS().stream().toList()))));
		pModel.animations.putAll(animations);
		
		return pModel;
		
	}
	
	/**
	 * Parses the bones.
	 * @param model the model to use.
	 * @param uuidToBone the uuid to bone to use.
	 * @param uuidToMesh the uuid to mesh to use.
	 * @return the value produced by this operation.
	 */
	private static Map<NodeModel, PBone> parseBones(GltfModel model, Map<UUID, PBone> uuidToBone, Map<UUID, PMesh> uuidToMesh)
	{
		List<NodeModel> nodes = model.getNodeModels();
		final Map<NodeModel, PBone> nodeToBone = new HashMap<>();
		
		if (nodes == null || nodes.isEmpty())
			return nodeToBone;
		
		List<NodeModel> joints = nodes.stream().
				filter(PGltfModelParser :: isBoneNode).
				toList();
		Set<String> boneNames = new HashSet<>();
		
		for (NodeModel node : joints)
			parseBone(node, nodeToBone, boneNames);
		
		for (NodeModel node : nodes)
		{
			List<MeshModel> meshes = node.getMeshModels();
			if (meshes == null || meshes.isEmpty() || nearestBone(node.getParent(), nodeToBone) != null)
				continue;
			nodeToBone.put(node, createLocalMeshBone(node, boneNames));
		}

		for (Map.Entry<NodeModel, PBone> entry : nodeToBone.entrySet())
		{
			NodeModel node = entry.getKey();
			PBone bone = entry.getValue();
			PBone parent = nearestBone(node.getParent(), nodeToBone);
			if (parent != null)
			{
				bone.setParent(parent);
				parent.children().add(bone);
			}
		}
		
		for (NodeModel node : nodes)
		{
			List<MeshModel> meshes = node.getMeshModels();
			if (meshes == null || meshes.isEmpty())
				continue;

			PBone bone = nodeToBone.get(node);
			boolean meshNodeIsBone = bone != null;
			if (bone == null)
				bone = nearestBone(node.getParent(), nodeToBone);
			if (bone == null)
				continue;

			for (MeshModel mesh : meshes)
				bone.meshUUIDS().add(parseMesh(mesh, node, uuidToMesh, !meshNodeIsBone));
		}

		nodeToBone.forEach((nodeModel, pBone) ->
				uuidToBone.put(pBone.uuid(), pBone));
		
		return nodeToBone;
	}
	
	/**
	 * Parses the bone.
	 * @param node the node to use.
	 * @param nodeToBone the node to bone to use.
	 * @param boneNames the bone names to use.
	 */
	private static void parseBone(NodeModel node, Map<NodeModel, PBone> nodeToBone, Set<String> boneNames)
	{
		float[] rawTranslation = node.getTranslation();
		Vector3f pivot = new Vector3f();
		if (rawTranslation != null && rawTranslation.length == 3)
			pivot.add(rawTranslation[0], rawTranslation[1], rawTranslation[2]);
		float[] rawRotation = node.getRotation();
		Quaternionf baseRotation = new Quaternionf();
		if (rawRotation != null && rawRotation.length == 4)
			baseRotation.set(rawRotation[0], rawRotation[1], rawRotation[2], rawRotation[3]);

		UUID boneUUID = UUID.randomUUID();
		String name = uniqueBoneName(node.getName(), boneUUID, boneNames);
		
		PBone bone = new PBone(
				boneUUID,
				name,
				pivot,
				baseRotation);
		
		nodeToBone.put(node, bone);
	}

	/**
	 * Creates the local mesh bone.
	 * @param node the node to use.
	 * @param boneNames the bone names to use.
	 * @return the value produced by this operation.
	 */
	private static PBone createLocalMeshBone(NodeModel node, Set<String> boneNames)
	{
		float[] rawTranslation = node.getTranslation();
		Vector3f pivot = new Vector3f();
		if (rawTranslation != null && rawTranslation.length == 3)
			pivot.set(rawTranslation[0], rawTranslation[1], rawTranslation[2]);
		float[] rawRotation = node.getRotation();
		Quaternionf baseRotation = new Quaternionf();
		if (rawRotation != null && rawRotation.length == 4)
			baseRotation.set(rawRotation[0], rawRotation[1], rawRotation[2], rawRotation[3]);

		UUID boneUUID = UUID.randomUUID();
		return new PBone(
				boneUUID,
				uniqueBoneName(node.getName(), boneUUID, boneNames),
				pivot,
				baseRotation);
	}

	/**
	 * Performs the unique bone name operation.
	 * @param requestedName the requested name to use.
	 * @param fallback the fallback to use.
	 * @param boneNames the bone names to use.
	 * @return the value produced by this operation.
	 */
	private static String uniqueBoneName(String requestedName, UUID fallback, Set<String> boneNames)
	{
		String baseName = requestedName == null || requestedName.isBlank() ? fallback.toString() : requestedName;
		String name = baseName;
		for (int suffix = 1; !boneNames.add(name); suffix++)
			name = baseName + "_" + suffix;
		return name;
	}

	/**
	 * Determines whether bone node.
	 * @param node the node to use.
	 * @return the value produced by this operation.
	 */
	private static boolean isBoneNode(NodeModel node)
	{
		if (isBlockbenchLocatorMarker(node))
			return false;
		List<MeshModel> meshes = node.getMeshModels();
		return meshes == null || meshes.isEmpty();
	}

	/**
	 * Performs the nearest bone operation.
	 * @param node the node to use.
	 * @param nodeToBone the node to bone to use.
	 * @return the value produced by this operation.
	 */
	private static PBone nearestBone(NodeModel node, Map<NodeModel, PBone> nodeToBone)
	{
		for (NodeModel current = node; current != null; current = current.getParent())
		{
			PBone bone = nodeToBone.get(current);
			if (bone != null)
				return bone;
		}
		return null;
	}
	
	/**
	 * Determines whether blockbench locator marker.
	 * @param node the node to use.
	 * @return the value produced by this operation.
	 */
	private static boolean isBlockbenchLocatorMarker(NodeModel node)
	{
		NodeModel parent = node.getParent();
		if (parent == null)
			return false;
		
		String nodeName = node.getName();
		if (nodeName == null || !nodeName.equals(parent.getName()))
			return false;
		
		List<MeshModel> meshes = node.getMeshModels();
		if (meshes != null && !meshes.isEmpty())
			return false;
		
		List<NodeModel> children = node.getChildren();
		if (children != null && !children.isEmpty())
			return false;
		
		float[] scale = node.getScale();
		return scale != null && scale.length == 3 && scale[0] < 0.1f && scale[1] < 0.1f && scale[2] < 0.1f;
	}
	
	/**
	 * Parses the mesh.
	 * @param mesh the mesh to use.
	 * @param node the node to use.
	 * @param uuidToMesh the uuid to mesh to use.
	 * @param bakeNodePose the bake node pose to use.
	 * @return the value produced by this operation.
	 */
	private static UUID parseMesh(MeshModel mesh,
	                              NodeModel node,
	                              Map<UUID, PMesh> uuidToMesh,
	                              boolean bakeNodePose)
	{
		Matrix4f meshTransform = meshTransform(node, bakeNodePose);
		List<PMeshPrimitive> primitives = mesh.getMeshPrimitiveModels().stream().
				map(primitive -> parsePrimitive(primitive, meshTransform)).
				toList();
		PMesh pMesh = new PMesh(UUID.randomUUID(), primitives);
		uuidToMesh.put(pMesh.uuid(), pMesh);
		return pMesh.uuid();
	}

	/**
	 * Parses a mesh primitive.
	 * @param primitive the primitive to use.
	 * @param meshTransform the mesh transform to use.
	 * @return the parsed primitive.
	 */
	private static PMeshPrimitive parsePrimitive(MeshPrimitiveModel primitive, Matrix4f meshTransform)
	{
		AccessorModel positionsAccessor = primitive.getAttributes().get("POSITION");
		AccessorModel normalsAccessor = primitive.getAttributes().get("NORMAL");
		AccessorModel uvsAccessor = primitive.getAttributes().get("TEXCOORD_0");
		
		FloatBuffer positions = transformPositions(PLibParserHelper.getFloatBuffer(positionsAccessor), meshTransform);
		FloatBuffer normals = transformNormals(PLibParserHelper.getFloatBuffer(normalsAccessor), meshTransform);
		FloatBuffer uvs = PLibParserHelper.getFloatBuffer(uvsAccessor);
		
		int vertexCount = positionsAccessor.getCount();
		
		AccessorModel indicesAccessor = primitive.getIndices();
		ByteBuffer indices = PLibParserHelper.getByteBuffer(indicesAccessor);
		int indicesCount = indicesAccessor.getCount();
		int indicesType = indicesAccessor.getComponentType();
		
		MaterialModel material = primitive.getMaterialModel();
		return new PMeshPrimitive(
				vertexCount,
				positions,
				normals,
				uvs,
				indicesCount,
				indices,
				indicesType,
				new PMaterial(PLibParserHelper.extractTextureName(material))
		);
	}

	/**
	 * Transforms the positions.
	 * @param source the source to use.
	 * @param transform the transform to use.
	 * @return the value produced by this operation.
	 */
	private static FloatBuffer transformPositions(FloatBuffer source, Matrix4f transform)
	{
		FloatBuffer result = FloatBuffer.allocate(source.limit());
		for (int offset = 0; offset < source.limit(); offset += 3)
		{
			Vector3f position = new Vector3f(source.get(offset), source.get(offset + 1), source.get(offset + 2));
			transform.transformPosition(position);
			result.put(position.x).put(position.y).put(position.z);
		}
		return result.flip();
	}

	/**
	 * Transforms the normals.
	 * @param source the source to use.
	 * @param transform the transform to use.
	 * @return the value produced by this operation.
	 */
	private static FloatBuffer transformNormals(FloatBuffer source, Matrix4f transform)
	{
		Matrix3f normalTransform = new Matrix3f().set(transform).invert().transpose();
		FloatBuffer result = FloatBuffer.allocate(source.limit());
		for (int offset = 0; offset < source.limit(); offset += 3)
		{
			Vector3f normal = new Vector3f(source.get(offset), source.get(offset + 1), source.get(offset + 2));
			normalTransform.transform(normal).normalize();
			result.put(normal.x).put(normal.y).put(normal.z);
		}
		return result.flip();
	}

	/**
	 * Performs the mesh transform operation.
	 * @param node the node to use.
	 * @param bakeNodePose the bake node pose to use.
	 * @return the value produced by this operation.
	 */
	private static Matrix4f meshTransform(NodeModel node, boolean bakeNodePose)
	{
		Matrix4f transform = new Matrix4f();
		if (bakeNodePose)
		{
			float[] translation = node.getTranslation();
			if (translation != null && translation.length == 3)
				transform.translate(translation[0], translation[1], translation[2]);
			float[] rotation = node.getRotation();
			if (rotation != null && rotation.length == 4)
				transform.rotate(new Quaternionf(rotation[0], rotation[1], rotation[2], rotation[3]));
		}
		float[] scale = node.getScale();
		if (scale != null && scale.length == 3)
			transform.scale(scale[0], scale[1], scale[2]);
		return transform;
	}
	
	/**
	 * Parses the animations.
	 * @param model the model to use.
	 * @param nodeToBone the node to bone to use.
	 * @return the value produced by this operation.
	 */
	private static Map<String, PAnimation> parseAnimations(GltfModel model, Map<NodeModel, PBone> nodeToBone)
	{
		Map<String, PAnimation> animations = new HashMap<>();
		
		for (int q = 0; q < model.getAnimationModels().size(); q++)
		{
			AnimationModel animationModel = model.getAnimationModels().get(q);
			
			String animationName = animationModel.getName() != null ? animationModel.getName() : "anim_" + q;
			Map<String, PBoneAnimation> boneAnimations = new HashMap<>();
			float maxTime = 0f;
			
			for (AnimationModel.Channel channel : animationModel.getChannels())
			{
				NodeModel node = channel.getNodeModel();
				
				if (node == null)
					continue;
				
				PBone bone = nodeToBone.get(node);
				if (bone == null)
					continue;
				
				UUID boneUuid = bone.uuid();
				
				PGltfChannelDecoder<?> decoder = decoder(channel.getPath());
				if (decoder == null)
					continue;
				
				AnimationModel.Sampler sampler = channel.getSampler();
				if (sampler == null)
					continue;
				
				FloatBuffer inputTimes = PLibParserHelper.getFloatBuffer(sampler.getInput());
				maxTime = Math.max(maxTime, inputTimes.limit() > 0 ? inputTimes.get(inputTimes.limit() - 1) : 0L);
				AccessorModel outputAccessor = sampler.getOutput();
				if (outputAccessor == null)
					continue;
				ByteBuffer bb = outputAccessor.getAccessorData().createByteBuffer().order(ByteOrder.LITTLE_ENDIAN);
				PBoneAnimation boneAnimation = boneAnimations.computeIfAbsent(bone.name(), k -> new PBoneAnimation(boneUuid, new HashMap<>()));
				PAnimationTrack<?> track = decodeTrack(decoder, inputTimes, bb, new PGltfDecodeContext(bone));
				boneAnimation.tracks().put(track.channel().id().toString(), track);
			}
			animations.put(animationName, new PAnimation(animationName, maxTime * 20, boneAnimations));
		}
		
		return animations;
	}
	
	/**
	 * Performs the decoder operation.
	 * @param path the path to use.
	 * @return the value produced by this operation.
	 */
	private static PGltfChannelDecoder<?> decoder(String path)
	{
		for (PGltfChannelDecoder<?> decoder : CHANNEL_DECODERS)
			if (decoder.fieldNames().contains(path))
				return decoder;
		return null;
	}

	/**
	 * Decodes the track.
	 * @param decoder the decoder to use.
	 * @param times the times to use.
	 * @param values the values to use.
	 * @param context the context to use.
	 * @return the value produced by this operation.
	 */
	private static <T> PAnimationTrack<T> decodeTrack(PGltfChannelDecoder<T> decoder,
	                                                  FloatBuffer times,
	                                                  ByteBuffer values,
	                                                  PGltfDecodeContext context)
	{
		List<PKeyframe<T>> keyframes = new ArrayList<>();
		for (int index = 0; index < times.limit(); index++)
		{
			PAnimationValue<T> value = decoder.decodeValue(values, index, context);
			keyframes.add(new PKeyframe<>(times.get(index) * 20, value, value, PInterpolationType.LINEAR));
		}
		return new PAnimationTrack<>(decoder.channel(), keyframes);
	}

	/**
	 * Performs the constant vector operation.
	 * @param value the value to use.
	 * @return the value produced by this operation.
	 */
	private static PAnimationValue<Vector3f> constantVector(Vector3f value)
	{
		return new PAnimationValue<>()
		{
			/**
			 * Performs the evaluate operation.
			 * @param context the context to use.
			 * @param destination the destination to use.
			 */
			@Override
			public void evaluate(PAnimationEvaluationContext context, Vector3f destination)
			{
				destination.set(value);
			}

			/**
			 * Determines whether constant.
			 * @return the value produced by this operation.
			 */
			@Override
			public boolean isConstant()
			{
				return true;
			}
		};
	}

	/**
	 * Performs the constant quaternion operation.
	 * @param value the value to use.
	 * @return the value produced by this operation.
	 */
	private static PAnimationValue<Quaternionf> constantQuaternion(Quaternionf value)
	{
		return new PAnimationValue<>()
		{
			/**
			 * Performs the evaluate operation.
			 * @param context the context to use.
			 * @param destination the destination to use.
			 */
			@Override
			public void evaluate(PAnimationEvaluationContext context, Quaternionf destination)
			{
				destination.set(value);
			}

			/**
			 * Determines whether constant.
			 * @return the value produced by this operation.
			 */
			@Override
			public boolean isConstant()
			{
				return true;
			}
		};
	}
}
