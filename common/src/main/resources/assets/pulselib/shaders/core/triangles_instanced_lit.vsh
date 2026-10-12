#version 330

#moj_import <minecraft:light.glsl>
#moj_import <pulselib:deformers.glsl>
#moj_import <pulselib:instanced_transform.glsl>
#moj_import <pulselib:material_lighting.glsl>

in vec3 Position;
in vec2 UV0;
in vec3 Normal;

layout(location = 4) in vec4 InstanceRow0;
layout(location = 5) in vec4 InstanceRow1;
layout(location = 6) in vec4 InstanceRow2;
layout(location = 7) in vec4 InstanceColor;
layout(location = 8) in vec2 InstanceLight;
layout(location = 9) in vec2 InstanceOverlay;
layout(location = 10) in ivec3 InstanceDeformer;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out vec4 instanceColorOut;
out vec4 vertexColorBack;
out vec4 vertexColorFront;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;

void main()
{
    vec3 normalTransformed;
    pulselib_transform_vertex(
        Position, Normal, InstanceDeformer,
        InstanceRow0, InstanceRow1, InstanceRow2,
        ModelViewMat, ProjMat,
        gl_Position, normalTransformed
    );
    pulselib_two_sided_light(
        normalTransformed,
        Light0_Direction,
        Light1_Direction,
        vertexColorBack,
        vertexColorFront
    );

    overlayColor = texelFetch(Sampler1, ivec2(InstanceOverlay), 0);
    lightMapColor = texelFetch(Sampler2, ivec2(InstanceLight), 0);

    instanceColorOut = InstanceColor;
    texCoord0 = UV0;
}
