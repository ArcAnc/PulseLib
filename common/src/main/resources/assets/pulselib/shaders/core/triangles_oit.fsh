#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;
uniform sampler2D LayerDepthSampler;

in vec4 vertexColorBack;
in vec4 vertexColorFront;

#ifndef EMISSIVE
in vec4 lightMapColor;
#endif

#ifndef NO_OVERLAY
in vec4 overlayColor;
#endif
in vec2 texCoord0;

layout(location = 0) out vec4 accumulation;
layout(location = 1) out float revealage;

const float PULSELIB_OIT_MIN_WEIGHT = 0.01;
const float PULSELIB_OIT_MAX_WEIGHT = 3000.0;
const float PULSELIB_OIT_EPSILON = 0.00001;

bool pulselib_oit_matches_layer_depth()
{
    float layerDepth = texelFetch(LayerDepthSampler, ivec2(gl_FragCoord.xy), 0).r;
    float tolerance = max(0.000001, layerDepth * 0.00001);
    return layerDepth > 0.0 && abs(gl_FragCoord.z - layerDepth) <= tolerance;
}

float pulselib_oit_weight(float alpha, float depth) {
    float rawWeight = clamp(
        pow(alpha + 0.01, 4.0) * 10000.0 * pow(1.0 - depth * 0.9, 3.0),
        PULSELIB_OIT_MIN_WEIGHT,
        PULSELIB_OIT_MAX_WEIGHT
    );
    return rawWeight;
}

void main()
{
    if (!pulselib_oit_matches_layer_depth()) {
        discard;
    }

    vec4 color = texture(Sampler0, texCoord0);
    #ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
    #endif

    vec4 faceVertexColor = gl_FrontFacing ? vertexColorFront : vertexColorBack;
    color *= faceVertexColor * ColorModulator;
    #ifndef NO_OVERLAY
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    #endif

    #ifndef EMISSIVE
    color *= lightMapColor;
    #endif

    float alpha = clamp(color.a, 0.0, 1.0);
    float weight = pulselib_oit_weight(alpha, gl_FragCoord.z);
    accumulation = vec4(color.rgb * alpha, alpha) * weight;
    revealage = -log(max(1.0 - alpha, PULSELIB_OIT_EPSILON));
}
