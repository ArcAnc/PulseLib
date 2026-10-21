#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;
uniform sampler2D LayerDepthSampler;

in vec4 vertexColorBack;
in vec4 vertexColorFront;
in vec2 texCoord0;

layout(location = 0) out float layerDepth;

const float PULSELIB_OIT_EPSILON = 0.000001;

void main()
{
    vec4 color = texture(Sampler0, texCoord0);
    #ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
    #endif

    vec4 faceVertexColor = gl_FrontFacing ? vertexColorFront : vertexColorBack;
    if (color.a * faceVertexColor.a * ColorModulator.a <= PULSELIB_OIT_EPSILON) {
        discard;
    }

    float previousLayerDepth = texelFetch(LayerDepthSampler, ivec2(gl_FragCoord.xy), 0).r;
    if (gl_FragCoord.z <= previousLayerDepth + PULSELIB_OIT_EPSILON) {
        discard;
    }

    layerDepth = gl_FragCoord.z;
}
