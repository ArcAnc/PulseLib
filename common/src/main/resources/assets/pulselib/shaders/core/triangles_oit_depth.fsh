#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;
uniform sampler2D SceneDepthSampler;

in vec4 vertexColorBack;
in vec4 vertexColorFront;
in vec2 texCoord0;

layout(location = 0) out float ignoredColor;

const float PULSELIB_OIT_EPSILON = 0.00001;

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
    if (color.a <= PULSELIB_OIT_EPSILON) {
        discard;
    }

    // Hardware depth stores the nearest remaining surface in reversed-Z.
    if (gl_FragCoord.z < texelFetch(SceneDepthSampler, ivec2(gl_FragCoord.xy), 0).r) {
        discard;
    }
    ignoredColor = 0.0;
}
