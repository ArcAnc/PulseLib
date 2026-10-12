#version 330

#moj_import <pulselib:deformers.glsl>
#moj_import <pulselib:instanced_transform.glsl>

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

out vec4 instanceColorOut;
out vec4 overlayColor;
out vec2 texCoord0;

void main()
{
    gl_Position = pulselib_transform_position(
        Position, InstanceDeformer,
        InstanceRow0, InstanceRow1, InstanceRow2,
        ModelViewMat, ProjMat
    );

    overlayColor = texelFetch(Sampler1, ivec2(InstanceOverlay), 0);

    instanceColorOut = InstanceColor;
    texCoord0 = UV0;
}
