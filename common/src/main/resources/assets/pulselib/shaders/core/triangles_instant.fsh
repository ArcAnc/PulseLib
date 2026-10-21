#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec4 vertexColorBack;
in vec4 vertexColorFront;

#ifndef EMISSIVE
in vec4 lightMapColor;
#endif

in vec4 overlayColor;
in vec2 texCoord0;

out vec4 fragColor;

void main()
{
    vec4 color = texture(Sampler0, texCoord0);
    #ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
    #endif

    vec4 faceVertexColor = gl_FrontFacing ? vertexColorFront : vertexColorBack;
    color *= faceVertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);

    #ifndef EMISSIVE
    color *= lightMapColor;
    #endif

    #ifdef FORCE_OPAQUE
    color.a = 1.0;
    #endif

    fragColor = color;
}
