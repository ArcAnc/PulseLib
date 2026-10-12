#version 150

#moj_import <pulselib:material_fragment.glsl>

uniform sampler2D Sampler0;

in vec4 vertexColorBack;
in vec4 vertexColorFront;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;

out vec4 fragColor;

void main()
{
    fragColor = pulselib_lit_material(
        Sampler0,
        texCoord0,
        vec4(1.0),
        vertexColorBack,
        vertexColorFront,
        overlayColor,
        lightMapColor
    );
}
