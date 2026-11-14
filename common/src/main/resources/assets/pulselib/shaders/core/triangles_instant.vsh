#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#ifndef EMISSIVE
#moj_import <minecraft:light.glsl>
#moj_import <minecraft:sample_lightmap.glsl>
#endif
#moj_import <pulselib:deformers.glsl>
#moj_import <pulselib:instances.glsl>

in vec3 Position;
in vec2 UV0;
in vec3 Normal;

uniform sampler2D Sampler1;

#ifndef EMISSIVE
uniform sampler2D Sampler2;
#endif

out vec4 vertexColorBack;
out vec4 vertexColorFront;

#ifndef EMISSIVE
out vec4 lightMapColor;
#endif

out vec4 overlayColor;
out vec2 texCoord0;

void main()
{
    PulseInstance instance = data[gl_InstanceID];
    vec3 deformedPosition;
    vec3 deformedNormal;
    pulselib_deform_vertex(Position, Normal, instance.InstanceDeformer.xyz, deformedPosition, deformedNormal);

    gl_Position = ProjMat * ModelViewMat * instance.InstanceModel * vec4(deformedPosition, 1.0);

    #ifdef EMISSIVE
    vertexColorBack = instance.InstanceColor;
    vertexColorFront = instance.InstanceColor;
    #else
    vec3 normalTransformed = normalize(mat3(ModelViewMat * instance.InstanceModel) * deformedNormal);
    vec2 light = minecraft_compute_light(Light0_Direction, Light1_Direction, normalTransformed);
    vertexColorBack = minecraft_mix_light_separate(-light, instance.InstanceColor);
    vertexColorFront = minecraft_mix_light_separate(light, instance.InstanceColor);
    lightMapColor = sample_lightmap(Sampler2, ivec2(instance.InstanceLight * 16));
    #endif

    overlayColor = texelFetch(Sampler1, ivec2(instance.InstanceOverlay), 0);
    texCoord0 = UV0;
}
