#version 150

in vec3 Position;
in vec2 UV0;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

uniform vec4 Color;
uniform ivec2 Overlay;

uniform sampler2D Sampler1;

out vec4 instanceColor;
out vec4 overlayColor;
out vec2 texCoord0;

void main()
{
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    overlayColor = texelFetch(Sampler1, Overlay, 0);

    instanceColor = Color;
    texCoord0 = UV0;
}
