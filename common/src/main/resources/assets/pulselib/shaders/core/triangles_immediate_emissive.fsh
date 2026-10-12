#version 150

#moj_import <pulselib:material_fragment.glsl>

uniform sampler2D Sampler0;

in vec4 instanceColor;
in vec4 overlayColor;
in vec2 texCoord0;

out vec4 fragColor;

void main()
{
    fragColor = pulselib_emissive_material(Sampler0, texCoord0, instanceColor, overlayColor);
}
