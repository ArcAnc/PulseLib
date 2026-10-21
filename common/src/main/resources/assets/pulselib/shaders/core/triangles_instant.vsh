#version 330

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>
#moj_import <pulselib:deformers.glsl>

in vec3 Position;
in vec2 UV0;
in vec3 Normal;

layout(std140) uniform ColorOverlay {
    vec4 Color;
    vec2 Light;
    ivec2 Overlay;
    ivec4 Deformer;
};

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
    vec3 deformedPosition;
    vec3 deformedNormal;
    pulselib_deform_vertex(Position, Normal, Deformer.xyz, deformedPosition, deformedNormal);

    gl_Position = ProjMat * ModelViewMat * vec4(deformedPosition, 1.0);

    #ifdef EMISSIVE
    vertexColorBack = Color;
    vertexColorFront = Color;
    #else
    vec3 normalTransformed = normalize(mat3(ModelViewMat) * deformedNormal);
    vec2 light = minecraft_compute_light(Light0_Direction, Light1_Direction, normalTransformed);
    vertexColorBack = minecraft_mix_light_separate(-light, Color);
    vertexColorFront = minecraft_mix_light_separate(light, Color);
    lightMapColor = sample_lightmap(Sampler2, ivec2(Light * 16));
    #endif

    overlayColor = texelFetch(Sampler1, Overlay, 0);
    texCoord0 = UV0;
}
