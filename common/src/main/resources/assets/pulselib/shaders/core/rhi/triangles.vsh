#version 330

// @author ArcAnc; Created at: 13.11.2026; Copyright (c) 2026
// Arc's License of Common Sense (LICENSE.txt).
// Canonical MultiLoader lighting/overlay behavior; master 26.2 std140 instance transport.

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#ifndef EMISSIVE
#moj_import <minecraft:light.glsl>
#moj_import <minecraft:sample_lightmap.glsl>
#endif

in vec3 Position;
in vec2 UV0;
in vec3 Normal;

struct InstanceDataStruct
{
	mat4 InstanceModel;
	vec4 InstanceColor;
	vec2 InstanceLight;
	vec2 InstanceOverlay;
	ivec4 InstanceDeformer;
};

layout(std140) uniform InstanceData
{
	InstanceDataStruct data[PULSELIB_INSTANCE_CAPACITY];
};

uniform sampler2D Sampler1;
#ifndef EMISSIVE
uniform sampler2D Sampler2;
out vec4 lightMapColor;
#endif

out vec4 vertexColorBack;
out vec4 vertexColorFront;
out vec4 overlayColor;
out vec2 texCoord0;

void main()
{
	InstanceDataStruct inst = data[gl_InstanceID];
	gl_Position = ProjMat * ModelViewMat * inst.InstanceModel * vec4(Position, 1.0);
	#ifdef EMISSIVE
	vertexColorBack = inst.InstanceColor;
	vertexColorFront = inst.InstanceColor;
	#else
	vec3 normalTransformed = normalize(mat3(inst.InstanceModel) * Normal);
	vec2 light = minecraft_compute_light(Light0_Direction, Light1_Direction, normalTransformed);
	vertexColorBack = minecraft_mix_light_separate(-light, inst.InstanceColor);
	vertexColorFront = minecraft_mix_light_separate(light, inst.InstanceColor);
	lightMapColor = sample_lightmap(Sampler2, ivec2(inst.InstanceLight * 16));
	#endif
	overlayColor = texelFetch(Sampler1, ivec2(inst.InstanceOverlay), 0);
	texCoord0 = UV0;
}
