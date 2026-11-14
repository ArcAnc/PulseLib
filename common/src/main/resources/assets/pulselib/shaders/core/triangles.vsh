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

#ifndef NO_OVERLAY
uniform sampler2D Sampler1;
#endif

#ifndef EMISSIVE
uniform sampler2D Sampler2;
#endif

out vec4 vertexColorBack;
out vec4 vertexColorFront;

#ifndef EMISSIVE
out vec4 lightMapColor;
#endif

#ifndef NO_OVERLAY
out vec4 overlayColor;
#endif
out vec2 texCoord0;

void main()
{
    PulseInstance instance = data[gl_InstanceID];
    mat4 model = instance.InstanceModel;
    vec4 instanceColor = instance.InstanceColor;
    vec2 instanceLight = instance.InstanceLight;
    vec2 instanceOverlay = instance.InstanceOverlay;
    ivec3 instanceDeformer = instance.InstanceDeformer.xyz;

    vec3 deformedPosition;
    vec3 deformedNormal;
    pulselib_deform_vertex(Position, Normal, instanceDeformer, deformedPosition, deformedNormal);

    gl_Position = ProjMat * ModelViewMat * model * vec4(deformedPosition, 1.0);

    vec4 color = instanceColor;
    #ifdef EMISSIVE
    vertexColorBack = color;
    vertexColorFront = color;
    #else
    vec3 normalTransformed = normalize(mat3(model) * deformedNormal);
    vec2 light = minecraft_compute_light(Light0_Direction, Light1_Direction, normalTransformed);
    vertexColorBack = minecraft_mix_light_separate(-light, color);
    vertexColorFront = minecraft_mix_light_separate(light, color);
    #endif

    #ifndef NO_OVERLAY
    overlayColor = texelFetch(Sampler1, ivec2(instanceOverlay), 0);
    #endif

    #ifndef EMISSIVE
    lightMapColor = sample_lightmap(Sampler2, ivec2(instanceLight*16));
    #endif

    texCoord0 = UV0;
}
