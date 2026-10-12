#version 330

#moj_import <pulselib:material_fragment.glsl>

uniform sampler2D Sampler0;

in vec4 instanceColorOut;
in vec4 overlayColor;
in vec2 texCoord0;

layout(location = 0) out float layerDepth;

void main()
{
    vec4 color = pulselib_emissive_material(Sampler0, texCoord0, instanceColorOut, overlayColor);
    if (color.a <= 0.00001)
        discard;
    layerDepth = gl_FragCoord.z;
}
