#version 330

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>
#moj_import <pulselib:deformers.glsl>

in vec3 Position;
in vec2 UV0;
in vec3 Normal;

layout(location = 4) in vec4 InstanceRow0;
layout(location = 5) in vec4 InstanceRow1;
layout(location = 6) in vec4 InstanceRow2;
layout(location = 7) in vec4 InstanceColor;
layout(location = 8) in vec2 InstanceLight;
layout(location = 9) in vec2 InstanceOverlay;
layout(location = 10) in ivec4 InstanceDeformer;

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
    mat4 model = mat4(
        vec4(InstanceRow0.x, InstanceRow1.x, InstanceRow2.x, 0.0),
        vec4(InstanceRow0.y, InstanceRow1.y, InstanceRow2.y, 0.0),
        vec4(InstanceRow0.z, InstanceRow1.z, InstanceRow2.z, 0.0),
        vec4(InstanceRow0.w, InstanceRow1.w, InstanceRow2.w, 1.0)
    );
    vec4 instanceColor = InstanceColor;
    vec2 instanceLight = InstanceLight;
    vec2 instanceOverlay = InstanceOverlay;
    ivec3 instanceDeformer = InstanceDeformer.xyz;

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
