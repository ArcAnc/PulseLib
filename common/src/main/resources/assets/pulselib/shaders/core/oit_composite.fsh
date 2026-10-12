#version 150

uniform sampler2D AccumSampler;
uniform sampler2D RevealSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;

out vec4 fragColor;

const float PULSELIB_OIT_EPSILON = 0.00001;

void main()
{
    vec4 accumulated = texture(AccumSampler, texCoord);
    float alpha = clamp(1.0 - exp(-texture(RevealSampler, texCoord).r), 0.0, 1.0);
    if (alpha <= PULSELIB_OIT_EPSILON || accumulated.a <= PULSELIB_OIT_EPSILON) {
        discard;
    }
    vec3 averageColor = accumulated.rgb / max(accumulated.a, PULSELIB_OIT_EPSILON);
    if (any(isnan(averageColor)) || any(isinf(averageColor))) {
        averageColor = vec3(0.0);
    }
    gl_FragDepth = texture(DepthSampler, texCoord).r;
    fragColor = vec4(max(averageColor, vec3(0.0)), alpha);
}
